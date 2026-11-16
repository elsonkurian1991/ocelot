package it.unisa.ocelot.c;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.core.runtime.CoreException;

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.compiler.writer.ASTWriter;
import it.unisa.ocelot.c.instrumentor.BooleanAssignmentTransformer;
import it.unisa.ocelot.c.instrumentor.CASTVisitorExternalReferences;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitorForIfMethodCalls;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitorToAddBranch;
import it.unisa.ocelot.c.instrumentor.CASTVisitorMacroDefiner;
import it.unisa.ocelot.c.instrumentor.UnitComponentInstrumentorVisitor;
import it.unisa.ocelot.c.makefile.JNIMakefileGenerator;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import it.unisa.ocelot.util.Utils;

public final class StandardBuilder extends Builder {
	private final String testFilename;
	private final String testFunction;
	private final HashSet<String> testSourcePaths;
	private final HashSet<String> testIncludePaths;
	private final HashSet<String> unitComponents;
	private final boolean isSplitBooleans;
	private final int testArraysSize;
	private final HashMap<String, Set<String>> componentsTestObjectives;
	// Maps the caller function to an hashmap, that maps the called function 
	// to the branches required to take that function
	private final HashMap<String, Map<String, List<String>>> nodeBranchMap;
	private final ArrayList<String> syntheticBranches;

	private String callMacro;
	private String externDeclarations;

	public StandardBuilder(PrintStream pStream, JNIMakefileGenerator pMakefileGenerator, String pTestFilename, String pTestFunction, String[] pTestSourcePaths, String[] pTestIncludePaths, String[] pUnitComponents,
			boolean pIsSplitBooleans, int pTestArraysSize) {
		super(pStream, pMakefileGenerator);
		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testSourcePaths = new HashSet<>(Arrays.asList(pTestSourcePaths));
		this.testIncludePaths = new HashSet<>(Arrays.asList(pTestIncludePaths));
		this.unitComponents = new HashSet<>(Arrays.asList(pUnitComponents));
		this.isSplitBooleans = pIsSplitBooleans;
		this.testArraysSize = pTestArraysSize;
		this.componentsTestObjectives = new HashMap<>();
		this.nodeBranchMap = new HashMap<>();
		this.syntheticBranches = new ArrayList<>();
	}

	@Override
	public void build() throws BuildingException {
		if (this.makefileGenerator == null) {
			throw new BuildingException("No makefile generator specified");
		}
		if (this.stream == null) {
			throw new BuildingException("No output stream specified");
		}

		// Instruments the code and copies it in main.c
		try {
			this.stream.println("Instrumenting Unit-Level Components: C file...");
			instrumentUnitComponents();
			this.stream.println("Instrumenting Target C file...");
			instrument();
			this.stream.println("Done!");

			// Generates EN_CBridge.c (from CBridge.c)
			// and pointers.h
			this.stream.print("Defining test function call... ");
			enrichJNICall();
			this.stream.println("Done!");

			// Builds the library
			this.stream.print("Building library... ");
			this.makefileGenerator.generate();
			this.stream.print("...........");
			Process proc = this.makefileGenerator.make();

			//emits the compiler output/error text 
			ExecutorService executor = Executors.newFixedThreadPool(2);
			Future<String> stdoutFuture = executor.submit(() -> IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8));
			Future<String> stderrFuture = executor.submit(() -> IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8));
			executor.shutdown();

			String stdout = stdoutFuture.get(), stderr = stderrFuture.get();
			this.stream.println(stdout);
			if (!stderr.isEmpty()) {
				this.stream.println("Compiler errors:");
				this.stream.println(stderr);
			}

			int result;
			if ((result = proc.waitFor()) == 0) {
				this.stream.println("Done!");
			} else {
				this.stream.println("ABORTED. An error occurred: " + result);
				throw new BuildingException(IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8));
			}

			this.stream.println("\nEverything done.");
		} catch (IOException | CoreException | InterruptedException | ExecutionException e) {
			throw new BuildingException(e);
		}
	}

	private void instrumentUnitComponents() throws IOException, CoreException {
		try (FileWriter cfgWriter = new FileWriter("callgraph.txt")) {
			for (String testSourcePath : this.testSourcePaths) {
				//TODO here we assume that, if a C file is called foo.c, then it contains just one function named foo(...). Remove this assumption!
				int lastIndex = testSourcePath.lastIndexOf('/');
				String testFileName = testSourcePath.substring(lastIndex + 1);
				String unitComponent = testFileName.substring(0, testFileName.length() - 2);
				if (this.unitComponents.contains(unitComponent)) {
					//Get the translation unit
					IASTTranslationUnit translationUnit = GCC.getTranslationUnit(testSourcePath, this.testIncludePaths).copy();

					//Possibly transform boolean assignments
					if (this.isSplitBooleans) {
						BooleanAssignmentTransformer at = new BooleanAssignmentTransformer();
						at.transform(translationUnit);
					}

					//Transform by extracting method calls in if conditions
					ASTRewrite rewriter = ASTRewrite.create(translationUnit);
					InstrumentorVisitorForIfMethodCalls instrumentorIfMethodCalls = new InstrumentorVisitorForIfMethodCalls(testSourcePath, unitComponent, rewriter);
					translationUnit.accept(instrumentorIfMethodCalls);

					//Finally, instrument the unit component
					LinkedHashSet<String> testObjectives = new LinkedHashSet<>();
					UnitComponentInstrumentorVisitor instrumentorUnitComponent = new UnitComponentInstrumentorVisitor(testSourcePath, unitComponent, this.unitComponents, testObjectives);
					translationUnit.accept(instrumentorUnitComponent);//old ours

					//If there are no branches, add one
					if (testObjectives.isEmpty()) {
						// not using the isSpclFunWObranch instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
						testObjectives.add(unitComponent);
						testObjectives.add(":branch0-true");
						InstrumentorVisitorToAddBranch instrumentorAddBranch = new InstrumentorVisitorToAddBranch(testSourcePath, unitComponent);
						translationUnit.accept(instrumentorAddBranch);
						System.out.println("File modified with temp branch: " + unitComponent);
					}

					//Stores relevant information
					this.componentsTestObjectives.put(unitComponent, testObjectives);
					this.nodeBranchMap.put(unitComponent, instrumentorUnitComponent.functionBranchPairMap);
					this.syntheticBranches.addAll(instrumentorUnitComponent.syntheticBranches);

					//Emits information about callgraph/CFG
					try {
						cfgWriter.append("Functions called by the component " + unitComponent + "\n");
						cfgWriter.append(unitComponent);
						cfgWriter.append("::");
						cfgWriter.append(instrumentorUnitComponent.functionBranchPairMap.keySet().toString());
						cfgWriter.append('\n');
						cfgWriter.append("Branches taken to call the functions\n");
						cfgWriter.append(instrumentorUnitComponent.functionBranchPairMap.toString());
						cfgWriter.append('\n');
						cfgWriter.append("All branches in the component\n");
						cfgWriter.append(unitComponent);
						cfgWriter.append("::");
						cfgWriter.append(testObjectives.toString());
						cfgWriter.append('\n');
					} catch (IOException e) {
						System.out.println("Unable to generate file callgraph.txt");
						e.printStackTrace();
					}

					//Emits the modified unit component to file
					ASTWriter writer = new ASTWriter();
					String outputCode = writer.write(translationUnit);
					StringBuilder result = new StringBuilder();
					result.append("#include \"ocelot.h\"\n\n");
					result.append(outputCode);
					String nameOfComponent = "jni/" + testFileName;
					Utils.writeFile(nameOfComponent, result.toString());

					//Adds test objectives of the unit component to file testObjectives.to
					//TODO this apparently is useless: in any case, ascertain that testObjectives.to is deleted before start of all this (this method appends)
					reportComponentsTestObjectives(this.componentsTestObjectives);
				} else {
					//here, just copy the other supported files to jni folder, eg kcg_types, database etc.
					File sourceFile = new File(testSourcePath);
					File jniDir = new File("jni");
					if (!sourceFile.exists()) {
						System.out.println("Source file does not exist: " + testSourcePath);
						return;
					}
					File destFile = new File(jniDir, sourceFile.getName());
					try {
						Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
						System.out.println("File copied to: " + destFile.getAbsolutePath());
					} catch (IOException e) {
						System.out.println("Error copying file: " + e.getMessage());
					}
				}
			} //end for loop
		}

		// This code generates the pairs - we don't care, we are using this tool only for branch coverage!
		generatePairs();

		// Synthetic branches
		try (FileOutputStream fos = new FileOutputStream("SyntheticBranches")) { 
			ObjectOutputStream oos = new ObjectOutputStream(fos); 
			oos.writeObject(this.syntheticBranches); 
			oos.close(); 
		} catch (IOException ioe) { 
			ioe.printStackTrace(); 
		}

		// This code generates branchObjectives.txt used to compare
		// MOSA with branches optimization with MOSA with pairs optimization
		StringBuilder objectives = new StringBuilder();
		for (Set<String> componentObjectives : this.componentsTestObjectives.values()) {
			for (String objective : componentObjectives) {
				objectives.append(objective);
				objectives.append(',');
			}
		}
		Utils.writeFile("branchObjectives.txt", objectives.toString());
	}

	private void generatePairs() {
		int indirectionLevel = 2;
		List<List<TestObjStateMachine>> indirectPairs = new ArrayList<>();
		for (int j = 0; j < indirectionLevel; j++) {
			indirectPairs.add(j, new ArrayList<>());
		}
		for (String testInclude : this.testIncludePaths) {
			int lastIndex = testInclude.lastIndexOf('/');
			String testFunName = testInclude.substring(lastIndex + 1);
			String unitComponent = testFunName.substring(0, testFunName.length() - 2);
			if (this.unitComponents.contains(unitComponent)) {
				List<List<TestObjStateMachine>> localIndirectPairs = generatePairForFunction(unitComponent, indirectionLevel);
				for (int j = 0; j < indirectionLevel; j++) {
					indirectPairs.get(j).addAll(localIndirectPairs.get(j));
				}
			}
		}
		try { 
			FileOutputStream fos = new FileOutputStream("PairsData"); 
			ObjectOutputStream oos = new ObjectOutputStream(fos); 
			oos.writeObject(indirectPairs); 
			oos.close(); 
			fos.close(); 
		} 
		catch (IOException ioe) { 
			ioe.printStackTrace(); 
		} 
	}

	private List<List<TestObjStateMachine>> generatePairForFunction(String functionName, int indirectionLevel) {
		Map<String, List<String>> functionToBranchesMap = nodeBranchMap.get(functionName);
		List<List<TestObjStateMachine>> indirectPairs = new ArrayList<List<TestObjStateMachine>>();
		for (int j = 0; j < indirectionLevel; j++) {
			indirectPairs.add(j, new ArrayList<TestObjStateMachine>());
		}
		for(String calledFunc : functionToBranchesMap.keySet()) {
			//List<String> usedBranches = functionToBranchesMap.get(calledFunc)
			//		.stream().map(branch -> functionName + ":" +  branch).collect(Collectors.toList());;
			List<String> usedBranches = functionToBranchesMap.get(calledFunc);
			ArrayList<Set<String>> indirectBranches = generateIndirectPairs(calledFunc, indirectionLevel, functionName);
			ArrayList<ArrayList<TestObjStateMachine>>  localIndirectPairs  = cartesianProduct(usedBranches, indirectBranches);

			for (int j = 0; j < indirectionLevel; j++) {
				indirectPairs.get(j).addAll(localIndirectPairs.get(j));
			}
		}
		return indirectPairs;
	}

	private ArrayList<Set<String>> generateIndirectPairs(String calledFunction, int indirectionLevel, String startingFunctionName) {
		ArrayList<Set<String>> result = new ArrayList<Set<String>>();
		//List<String> levelIndirectBranches = new ArrayList<String>();
		/*for(String indirectFunction : nodeBranchMap.get(functionName).keySet()) {
			if (indirectFunction != startingFunctionName) {
				ArrayList<String> branchesToAdd = componentsTestObjectives.get(indirectFunction);
				levelIndirectBranches.addAll(branchesToAdd);
			}
		}*/
		Set<String> directBranchesToAdd = this.componentsTestObjectives.get(calledFunction);
		result.add(directBranchesToAdd);
		if (indirectionLevel > 1) {
			ArrayList<List<Set<String>>> localOverallIndirectBranches = new ArrayList<>(); // for and aggregate generate pairs
			for (String indirectFunction : this.nodeBranchMap.get(calledFunction).keySet()) {
				if (indirectFunction != startingFunctionName) {
					localOverallIndirectBranches.add(generateIndirectPairs(indirectFunction, indirectionLevel - 1, startingFunctionName));
				}
			}
			if (localOverallIndirectBranches.isEmpty()) {
				for (int i = 0; i < indirectionLevel - 1; i++) {
					result.add(new LinkedHashSet<>());
				}
			} else {
				result.addAll(aggregatePairsByIndirectionLevel(localOverallIndirectBranches));
			}
		}
		return result;
	}

	private List<Set<String>> aggregatePairsByIndirectionLevel(List<List<Set<String>>> localOverallIndirectBranches) {
		List<Set<String>> overallIndirectBraches = new ArrayList<>();
		try {
			for (int i = 0; i < localOverallIndirectBranches.get(0).size(); i++) {
				Set<String> aggregator = new LinkedHashSet<>();
				for (List<Set<String>> indirectTreeBranches : localOverallIndirectBranches) {
					aggregator.addAll(indirectTreeBranches.get(i)); 
				}
				overallIndirectBraches.add(aggregator);
			}
		} catch (Exception ioe) { 
			ioe.printStackTrace(); 
		} 
		return overallIndirectBraches;
	}

	private ArrayList<ArrayList<TestObjStateMachine>> cartesianProduct(List<String> setA, ArrayList<Set<String>> indirectBranches) {
		ArrayList<ArrayList<TestObjStateMachine>> cartesianSet = new ArrayList<>();

		for (int j = 0; j < indirectBranches.size(); j++) {
			cartesianSet.add(j, new ArrayList<>());
		}

		for (String i: setA) {
			for (int j = 0; j < indirectBranches.size(); j++) {
				for (String k: indirectBranches.get(j)) {
					String testObjOne = i;
					String testObjTwo = k;
					TestObjStateMachine testobjSM = new TestObjStateMachine(testObjOne,testObjTwo); ; 
					cartesianSet.get(j).add(testobjSM);
				}
			}
		}
		return cartesianSet;
	}

	private void instrument() throws IOException, CoreException {
		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(this.testFilename, this.testIncludePaths).copy();

		//gets all the preprocessor statements (#includes, #defines...)
		//in the translation unit
		IASTPreprocessorStatement[] preprocessorStatements = translationUnit.getAllPreprocessorStatements();

		//calculates the "extern..." declarations to be added to
		//CBridge.c
		CASTVisitorExternalReferences referencesVisitor = new CASTVisitorExternalReferences(this.testFunction);
		referencesVisitor.calculate(translationUnit);
		this.externDeclarations = referencesVisitor.getExternalDeclarations();

		//calculates the macros definitions to be added to
		//CBridge.c
		CASTVisitorMacroDefiner macroDefiner = 
				new CASTVisitorMacroDefiner(translationUnit, this.testFunction, referencesVisitor.getExternalReferences());
		macroDefiner.calculate();
		this.callMacro = macroDefiner.getCallMacro();

		//instruments the test function 
		InstrumentorVisitor instrumentor = new InstrumentorVisitor(this.testFilename, this.testFunction);
		instrumentor.transform(translationUnit);

		ASTWriter writer = new ASTWriter();

		//generates main.c
		StringBuilder mainC = new StringBuilder();
		mainC.append("/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n\n");
		mainC.append("#include \"main.h\"\n\n");
		mainC.append("/* Preprocessor statements (system includes and non-includes preprocessor statements) */");
		for (IASTPreprocessorStatement preprocessorStatement : preprocessorStatements) {
			if (preprocessorStatement instanceof IASTPreprocessorIncludeStatement) {
				IASTPreprocessorIncludeStatement include = (IASTPreprocessorIncludeStatement) preprocessorStatement;
				if (include.isSystemInclude()) {
					mainC.append('\n');
					mainC.append(preprocessorStatement.getRawSignature()); 
				}
			} else {
				//non-includes preprocessor statements (macros...)
				mainC.append('\n');
				mainC.append(preprocessorStatement.getRawSignature());
			}
		}
		mainC.append("\n\n");
		mainC.append("/* Translation unit */\n");
		mainC.append(writer.write(translationUnit));
		mainC.append('\n');
		Utils.writeFile("jni/main.c", mainC.toString());

		//generates main.h
		StringBuilder mainH = new StringBuilder();
		mainH.append("/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n\n");
		mainH.append("#ifndef _Included_MainHeader\n");
		mainH.append("#define _Included_MainHeader\n\n");
		mainH.append("/* Typedefs */");
		for (IASTNode typedef : instrumentor.getTypedefs()) {
			mainH.append('\n');
			mainH.append(writer.write(typedef));
		}
		mainH.append("\n\n/* Macros */\n");
		mainH.append("#define OCELOT_TESTFUNCTION ");
		mainH.append(this.testFunction);
		mainH.append("\n\n/* Declarations */\n");
		mainH.append(writer.write(referencesVisitor.getFunctionDefinition().getDeclSpecifier()));
		mainH.append(' ');
		mainH.append(writer.write(referencesVisitor.getFunctionDefinition().getDeclarator()));
		mainH.append(";\n\n");
		mainH.append("#endif\n");
		Utils.writeFile("jni/main.h", mainH.toString());
	}

	private void enrichJNICall() throws IOException {
		//generates EN_CBridge.c
		String metaJNI = Utils.readFile("jni/CBridge.c");
		metaJNI = "/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER FROM CBridge.c **/\n" + 
				this.callMacro + "\n\n" + 
				this.externDeclarations + "\n\n" + 
				metaJNI;
		Utils.writeFile("jni/EN_CBridge.c", metaJNI);

		//generates pointers.h
		String pointersH = "/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n\n";
		pointersH += "#ifndef _Included_OcelotPointers\n" + "#define _Included_OcelotPointers\n"
				+ "#define OCELOT_ARRAY_LENGTH " + this.testArraysSize + "\n"
				+ "typedef double _T_ocelot_array[OCELOT_ARRAY_LENGTH];\n" + "#endif\n";
		Utils.writeFile("jni/pointers.h", pointersH);
	}

	private void reportComponentsTestObjectives(HashMap<String, Set<String>> componentsTestObjectives) {
		//TODO in the new version, we are not using this, remove it!!
		try (FileWriter myWriter = new FileWriter("testObjectives.to", true)) {
			for (Entry<String, Set<String>> componentTestObjectives : componentsTestObjectives.entrySet()) {
				if(!componentTestObjectives.getValue().isEmpty()) {
					String write = "{" + componentTestObjectives.getKey() + "=";
					for (String testObjective : componentTestObjectives.getValue()) {
						write = write + testObjective + ",";
					}
					write = write.substring(0, write.length() - 1);
					write = write + "};" + System.lineSeparator();
					myWriter.write(write);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
