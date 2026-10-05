package it.unisa.ocelot.c;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.compiler.writer.ASTWriter;
import it.unisa.ocelot.c.instrumentor.BooleanAssignmentTransformer;
import it.unisa.ocelot.c.instrumentor.ExternalReferencesVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumenterVisitForIfMethodCalls;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitorToAddBranch;
import it.unisa.ocelot.c.instrumentor.MacroDefinerVisitor;
import it.unisa.ocelot.c.instrumentor.UnitComponentInstrumentorVisitor;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class StandardBuilder extends Builder {
	private ConfigManager config;
	private String testFilename;
	private String testFunction;
	private String[] testIncludes;
	private List<String> unitLevelComponents;
	private HashMap<String, ArrayList<String>> componentsTestObjectives;
	// Maps the caller function to an hashmap, that maps the called function 
	// to the branches required to take that function
	private Map<String, Map<String, List<String>>> nodeBranchMap;
	private ArrayList<String> syntheticBranches;

	private String callMacro;
	private String externDeclarations;

	public StandardBuilder(String pTestFilename, String pTestFunction, String[] pTestIncludes) {
		super();
		setOutput(System.out);

		try {
			this.config = ConfigManager.getInstance();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testIncludes = pTestIncludes;
		this.unitLevelComponents = this.config.getUnitLevelComponents();
		this.componentsTestObjectives = new HashMap<>();
		this.nodeBranchMap = new HashMap<>();
		this.syntheticBranches = new ArrayList<>();
	}

	@Override
	public void build() throws IOException, BuildingException {
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
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildingException(e.getMessage());
		}

		// Generates EN_CBridge.c (from CBridge.c)
		// and pointers.h
		this.stream.print("Defining test function call... ");
		enrichJNICall();
		this.stream.println("Done!");

		// Builds the library
		this.stream.print("Building library... ");
		this.makefileGenerator.generate();
		this.stream.print("...........");
		Process proc = this.makefileGenerator.runCompiler();

		//emits the compiler output/error text 
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<String> stdoutFuture = executor.submit(() -> IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8));
		Future<String> stderrFuture = executor.submit(() -> IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8));
		executor.shutdown();

		String stdout = null, stderr = null;
		try {
			stdout = stdoutFuture.get();
			stderr = stderrFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		this.stream.println(stdout);
		if (!stderr.isEmpty()) {
			this.stream.println("Compiler errors:");
			this.stream.println(stderr);
		}

		try {
			int result;
			if ((result = proc.waitFor()) == 0) {
				this.stream.println("Done!");
			} else {
				this.stream.println("ABORTED. An error occurred: " + result);
				throw new BuildingException(IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8));
			}
		} catch (InterruptedException e) {
			throw new BuildingException("Interrupted");
		}

		this.stream.println("\nEverything done.");
	}

	private void instrumentUnitComponents() throws Exception {
		try (FileWriter cfgWriter = new FileWriter("callgraph.txt")) {
			for (String testInclude : this.testIncludes) {
				int lastIndex = testInclude.lastIndexOf('/');
				String testFunName = testInclude.substring(lastIndex + 1);
				//System.out.println(testFunName);
				String unitComponent = testFunName.substring(0, testFunName.length() - 2);
				//System.out.println(unitComponent);
				if (this.unitLevelComponents.contains(unitComponent)) {
					String nameOfComponent = "jni/" + testInclude.substring(lastIndex + 1);
					//System.out.println(nameofComponent);
					IASTTranslationUnit translationUnit = GCC.getTranslationUnit(testInclude, testInclude).copy();

					if (this.config.isSplitBooleans()) {
						BooleanAssignmentTransformer at = new BooleanAssignmentTransformer();
						at.transform(translationUnit);
					}

					//Here we add a instrumentation visitor for method call in the if condition.
					ASTRewrite rewriter = ASTRewrite.create(translationUnit);
					InstrumenterVisitForIfMethodCalls instrumentor_if_method_call = new InstrumenterVisitForIfMethodCalls(unitComponent, rewriter);
					translationUnit.accept(instrumentor_if_method_call);

					//instrumentor
					ArrayList<String> testObjectives = new ArrayList<>();
					UnitComponentInstrumentorVisitor instrumentor1 = new UnitComponentInstrumentorVisitor(unitComponent,
							testObjectives, this.unitLevelComponents);
					translationUnit.accept(instrumentor1);//old ours

					if(testObjectives.isEmpty()) {
						// not using the isSpclFunWObranch instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
						testObjectives.add(unitComponent + ":" + "branch0-true");
						ASTRewrite rewriterBranch = ASTRewrite.create(translationUnit);
						InstrumentorVisitorToAddBranch InstAddBranch = new InstrumentorVisitorToAddBranch(unitComponent,rewriterBranch);
						translationUnit.accept(InstAddBranch);
						System.out.println("File modified with temp branch:"+unitComponent);

					}

					this.componentsTestObjectives.put(unitComponent, testObjectives);
					// Need to store this one outside the loop to generate the branch pairs
					this.nodeBranchMap.put(unitComponent, instrumentor1.functionBranchPairMap);
					this.syntheticBranches.addAll(instrumentor1.syntheticBranches);

					// Write down information for callgraph/CFG representation
					try {
						cfgWriter.append("Functions called by the component " + unitComponent + "\n");
						cfgWriter.append(unitComponent + "::" + instrumentor1.functionBranchPairMap.keySet());
						cfgWriter.append('\n');
						cfgWriter.append("Branches taken to call the functions\n");
						cfgWriter.append(instrumentor1.functionBranchPairMap.toString());
						cfgWriter.append('\n');
						cfgWriter.append("All branches in the component\n");
						cfgWriter.append(unitComponent + "::" + testObjectives);
						cfgWriter.append('\n');
					} catch (IOException e) {
						System.out.println("Unable to generate file callgraph.txt");
						e.printStackTrace();
					}

					ASTWriter writer = new ASTWriter();
					String outputCode = writer.write(translationUnit);

					StringBuilder result = new StringBuilder();
					result.append("#include \"ocelot.h\"\n");
					result.append(outputCode);
					Utils.writeFile(nameOfComponent, result.toString());

					reportComponentsTestObjectives(this.componentsTestObjectives);
				} else {
					//here, just copy the other supported files to jni folder, eg kcg_types, database etc.
					File sourceFile = new File(testInclude);
					File jniDir = new File("jni");
					if (!sourceFile.exists()) {
						System.out.println("Source file does not exist: " + testInclude);
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


			// This code generates the pairs - we don't care, we are using this tool only for branch coverage!
			//generatePairs();

			// Synthetic branches
			try { 
				FileOutputStream fos = new FileOutputStream("SyntheticBranches"); 
				ObjectOutputStream oos = new ObjectOutputStream(fos); 
				oos.writeObject(this.syntheticBranches); 
				oos.close(); 
				fos.close(); 
			} catch (IOException ioe) { 
				ioe.printStackTrace(); 
			}

			// This code generates branchObjectives.txt used to compare
			// MOSA with branches optimization with MOSA with pairs optimization
			StringBuilder branchObjList = new StringBuilder();
			for (ArrayList<String> component : this.componentsTestObjectives.values()) {
				for (String branch : component) {
					branchObjList.append(branch);
					branchObjList.append(',');
				}
			}
			Utils.writeFile("branchObjectives.txt", branchObjList.toString());
		}
	}

	private void generatePairs() {
		int indirectionLevel = 2;
		List<List<TestObjStateMachine>> indirectPairs = new ArrayList<>();
		for (int j = 0; j < indirectionLevel; j++) {
			indirectPairs.add(j, new ArrayList<>());
		}
		for (String testInclude : this.testIncludes) {
			int lastIndex = testInclude.lastIndexOf('/');
			String testFunName = testInclude.substring(lastIndex + 1);
			String unitComponent = testFunName.substring(0, testFunName.length() - 2);
			if (this.unitLevelComponents.contains(unitComponent)) {
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
			List<List<String>> indirectBraches = generateIndirectPairs(calledFunc, indirectionLevel, functionName);
			List<List<TestObjStateMachine>>  localIndirectPairs  = cartesianProduct(usedBranches, indirectBraches);

			for (int j = 0; j < indirectionLevel; j++) {
				indirectPairs.get(j).addAll(localIndirectPairs.get(j));
			}


		}
		return indirectPairs;
	}

	private List<List<String>> generateIndirectPairs(String calledFunction, int indirectionLevel, String startingFunctionName) {
		List<List<String>> overallIndirectBranches = new ArrayList<List<String>>();
		//List<String> levelIndirectBranches = new ArrayList<String>();
		/*for(String indirectFunction : nodeBranchMap.get(functionName).keySet()) {
			if (indirectFunction != startingFunctionName) {
				ArrayList<String> branchesToAdd = componentsTestObjectives.get(indirectFunction);
				levelIndirectBranches.addAll(branchesToAdd);
			}
		}*/
		ArrayList<String> directBranchesToAdd = this.componentsTestObjectives.get(calledFunction);
		overallIndirectBranches.add(directBranchesToAdd);
		if (indirectionLevel > 1) {
			List<List<List<String>>> localOverallIndirectBranches = new ArrayList<>(); // for and aggregate generate pairs
			for (String indirectFunction : this.nodeBranchMap.get(calledFunction).keySet()) {
				if (indirectFunction != startingFunctionName) {
					localOverallIndirectBranches.add(generateIndirectPairs(indirectFunction, indirectionLevel - 1, startingFunctionName));
				}
			}
			if (localOverallIndirectBranches.isEmpty()) {
				for (int i = 0; i < indirectionLevel - 1; i++) {
					overallIndirectBranches.add(new ArrayList<>());
				}
			} else {
				overallIndirectBranches.addAll(aggregatePairsByIndirectionLevel(localOverallIndirectBranches));
			}
		}
		return overallIndirectBranches;
	}

	private List<List<String>> aggregatePairsByIndirectionLevel(List<List<List<String>>> localOverallIndirectBranches) {
		List<List<String>> overallIndirectBraches = new ArrayList<>();
		try {
			for (int i = 0; i < localOverallIndirectBranches.get(0).size(); i++) {
				List<String> aggregator = new ArrayList<>();
				for (List<List<String>> indirectTreeBranches : localOverallIndirectBranches) {
					aggregator.addAll(indirectTreeBranches.get(i)); 
				}
				overallIndirectBraches.add(aggregator);
			}
		} catch (Exception ioe) { 
			ioe.printStackTrace(); 
		} 
		return overallIndirectBraches;
	}

	private List<List<TestObjStateMachine>> cartesianProduct(List<String> setA, List<List<String>> indirectBranches) {
		List<List<TestObjStateMachine>> cartesianSet = new ArrayList<>();

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

	private void instrument() throws Exception {
		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(this.testFilename, this.testIncludes).copy();

		//gets all the preprocessor statements (#includes, #defines...)
		//in the translation unit
		IASTPreprocessorStatement[] preprocessorStatements = translationUnit.getAllPreprocessorStatements();

		//gets the "extern..." declarations to be added to
		//CBridge.c
		ExternalReferencesVisitor referencesVisitor = new ExternalReferencesVisitor(this.testFunction);
		referencesVisitor.calculate(translationUnit);
		this.externDeclarations = referencesVisitor.getExternalDeclarations();

		//gets the macros definitions to be added to
		//CBridge.c
		MacroDefinerVisitor macroDefiner = 
				new MacroDefinerVisitor(this.testFunction, referencesVisitor.getExternalReferences());
		macroDefiner.calculate(translationUnit);
		this.callMacro = macroDefiner.getCallMacro();

		//instruments the test function for branch distances
		InstrumentorVisitor instrumentor = new InstrumentorVisitor(this.testFunction);
		instrumentor.transform(translationUnit);

		ASTWriter writer = new ASTWriter();

		//generates main.c
		StringBuilder mainC = new StringBuilder();
		mainC.append("/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n");
		for (IASTPreprocessorStatement preprocessorStatement : preprocessorStatements) {
			if (preprocessorStatement instanceof IASTPreprocessorIncludeStatement) {
				IASTPreprocessorIncludeStatement include = (IASTPreprocessorIncludeStatement) preprocessorStatement;
				if (include.isSystemInclude()) {
					mainC.append(preprocessorStatement.getRawSignature()).append('\n');
				}
			} else {
				mainC.append(preprocessorStatement.getRawSignature()).append('\n');
			}
		}
		mainC.append("#include \"ocelot.h\"\n");
		mainC.append(writer.write(translationUnit));
		Utils.writeFile("jni/main.c", mainC.toString());

		//generates main.h
		StringBuilder mainH = new StringBuilder();
		mainH.append("/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n");
		mainH.append("#include \"ocelot.h\"\n");
		mainH.append("#include <stdio.h>\n");
		mainH.append("#include <math.h>\n");
		for (IASTNode typedef : instrumentor.getTypedefs()) {
			mainH.append(writer.write(typedef));
			mainH.append("\n");
		}
		mainH.append("#define OCELOT_TESTFUNCTION ");
		mainH.append(this.testFunction);
		mainH.append('\n');
		mainH.append(writer.write(referencesVisitor.getFunctionDefinition().getDeclSpecifier()));
		mainH.append(' ');
		mainH.append(writer.write(referencesVisitor.getFunctionDefinition().getDeclarator()));
		mainH.append(";\n");
		Utils.writeFile("jni/main.h", mainH.toString());
	}

	private void enrichJNICall() throws IOException {
		//generates EN_CBridge.c
		String metaJNI = Utils.readFile("jni/CBridge.c");
		metaJNI = "/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n" + 
				this.callMacro + "\n\n" + 
				this.externDeclarations + "\n\n" + 
				metaJNI;
		Utils.writeFile("jni/EN_CBridge.c", metaJNI);

		//generates pointers.h
		String pointersH = "/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n";
		pointersH += "#ifndef _Included_OcelotPointers\n" + "#define _Included_OcelotPointers\n"
				+ "#define OCELOT_ARRAYS_SIZE " + this.config.getTestArraysSize() + "\n"
				+ "typedef double _t_ocelot_array[OCELOT_ARRAYS_SIZE];\n" + "#endif\n";
		Utils.writeFile("jni/pointers.h", pointersH);
	}

	private void reportComponentsTestObjectives(HashMap<String, ArrayList<String>> componentsTestObjectives) {
		//TODO in the new version, we are not using this, remove it!!
		try {
			FileWriter myWriter = new FileWriter("testObjectives.to", true);//false?
			for (Entry<String, ArrayList<String>> componentTestObjectives : componentsTestObjectives.entrySet()) {
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
			myWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
