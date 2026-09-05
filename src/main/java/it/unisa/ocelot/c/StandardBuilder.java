package it.unisa.ocelot.c;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGVisitor;
import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.instrumentor.ExternalReferencesVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitorToAddBranch;
import it.unisa.ocelot.c.instrumentor.MacroDefinerVisitor;
import it.unisa.ocelot.c.instrumentor.UnitComponentInstrumentorVisitor;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.branch_chains.BranchChainManager;
import it.unisa.ocelot.util.Utils;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class StandardBuilder extends Builder {
	private ConfigManager config;
	private String testFilename;
	private String testFunction;
	private String[] testIncludes;
	private List<String> unitLevelComponents;
	private HashMap<String, ArrayList<String>> componentsTestObjectives;
	private Map<String, Map<String, List<String>>> nodeBranchMap;
	private ArrayList<String> syntheticBranches;
	private BranchChainManager bcm;
	// Initialized during instrumentation
	private String callMacro;
	private String externDeclarations;
	
	public int syntheticBranchesGeneratedWithBool;
	public int syntheticBranchesGeneratedWithFor;
	public int foundSynthetic;
	
	public StandardBuilder(String pTestFilename, String pTestFunction, String[] pTestIncludes) throws IOException {
		super();
		setOutput(System.out);

		this.config = ConfigManager.getInstance();
		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testIncludes = pTestIncludes;
		this.unitLevelComponents = config.getUnitLevelComponents();
		this.bcm = new BranchChainManager();
		
		this.componentsTestObjectives = new HashMap<String, ArrayList<String>>();
		this.nodeBranchMap = new HashMap<String, Map<String, List<String>>>();

		this.syntheticBranches = new ArrayList<String>();
		this.syntheticBranchesGeneratedWithBool = 0;
		this.syntheticBranchesGeneratedWithFor = 0;
		this.foundSynthetic = 0;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void build() throws IOException, BuildingException {
		if (this.makefileGenerator == null)
			throw new BuildingException("No makefile generator specified");
		if (this.stream == null)
			throw new BuildingException("No output stream specified");
		try {
			this.stream.print("Instrumenting Unit-Level Components: C file... \n");
			instrumentUnitComponents();
			this.stream.print("Instrumenting Target C file... \n");
			instrument();
			this.stream.println("Done!");
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuildingException(e.getMessage());
		}

		// Adds extra macros in CBridge.c
		this.stream.print("Defining test function call... ");
		enrichJNICall();
		this.stream.println("Done!");

		// Builds the library
		this.stream.print("Building library... ");
		this.makefileGenerator.generate();
		this.stream.print("........... ");
		Process proc = this.makefileGenerator.runCompiler();

		//this.stream.println(IOUtils.toString(proc.getInputStream()));
		//Following code is add for output all the complier error from gcc. 
		ExecutorService executor = Executors.newFixedThreadPool(2);
		Future<String> stdoutFuture = (Future<String>) executor.submit(() -> IOUtils.toString(proc.getInputStream(), StandardCharsets.UTF_8));
		Future<String> stderrFuture = (Future<String>) executor.submit(() -> IOUtils.toString(proc.getErrorStream(), StandardCharsets.UTF_8));
		executor.shutdown();

		String stdout = null;
		try {
			stdout = stdoutFuture.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		String stderr = null;
		try {
			stderr = stderrFuture.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		try {
			int result;
			if ((result = proc.waitFor()) == 0)
				this.stream.println("Done!");
			else {
				this.stream.println("Compiler errors:");
				this.stream.println(stdout);
				this.stream.println(stderr);
				this.stream.println("ABORTED. An error occurred, build error code: " + result);
				throw new BuildingException(IOUtils.toString(proc.getErrorStream()));
			}
		} catch (InterruptedException e) {
			this.stream.println("ABORTED. Build process interrupted");
			throw new BuildingException("Build process interrupted");
		}

		this.stream.println("\nEverything done.");
	}

	private void instrumentUnitComponents() throws Exception {
		String[] testIncludesTemp= new String[1];
		for(String unitComponent:this.testIncludes) {
			int lastIndex=unitComponent.lastIndexOf('/');
			String testFunName=unitComponent.substring(lastIndex+1);
			String tempUnitComponent=testFunName.substring(0, testFunName.length()-2);

			if(unitLevelComponents.contains(tempUnitComponent)) {
				String nameofComponent="jni/"+unitComponent.substring(lastIndex+1);
				System.out.println(nameofComponent);
				testIncludesTemp[0]=unitComponent;
				IASTTranslationUnit translationUnit = GCC.getTranslationUnit(unitComponent, testIncludesTemp).copy();

				Set<IASTNode> trackSynthetics = new HashSet<>();
				// not using the for loop instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
				/*ForLoopTransformer_forCDG forloopTrans = new ForLoopTransformer_forCDG(translationUnit);
				translationUnit.accept(forloopTrans);
				syntheticBranchesGeneratedWithFor += forloopTrans.trackSynthetics.size();
				trackSynthetics.addAll(forloopTrans.trackSynthetics);
				 */

				// add instrumention for Boolean assignment transformation
				// not using the boolean instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
				/*BooleanAssignmentTransformer booleanTransfomer = null;
				if(config.isSplitBooleans()) {
					booleanTransfomer = new BooleanAssignmentTransformer(translationUnit);
					translationUnit.accept(booleanTransfomer);
					syntheticBranchesGeneratedWithBool += booleanTransfomer.trackSynthetics.size();
					trackSynthetics.addAll(booleanTransfomer.trackSynthetics);

				}*/

				// Instruments unit-level components ExternalReferencesVisitor
				ExternalReferencesVisitor referencesVisitor = new ExternalReferencesVisitor(tempUnitComponent);
				translationUnit.accept(referencesVisitor);
				MacroDefinerVisitor macroDefiner = new MacroDefinerVisitor(tempUnitComponent,referencesVisitor.getExternalReferences());
				// NOTE: macroDefine MUST proceed instrument in visit
				translationUnit.accept(macroDefiner);

				//Here we add a instrumention visitor for method call in the if condition.
				// not using the InstrumenterVisitForIfMethodCalls instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
				/*InstrumenterVisitForIfMethodCalls instrumentor_if_method_call = new InstrumenterVisitForIfMethodCalls(tempUnitComponent, trackSynthetics);
				translationUnit.accept(instrumentor_if_method_call);
				 */
				ArrayList<String> testObjectives = new ArrayList<String>();
				// Instruments unit-level components out main instrumenation
				UnitComponentInstrumentorVisitor instrumentor = new UnitComponentInstrumentorVisitor(tempUnitComponent,
						testObjectives, unitLevelComponents, trackSynthetics);
				translationUnit.accept(instrumentor);
				foundSynthetic += instrumentor.foundSynthetics.size();

				// not using this  instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
				/*ASTWriter writer2 = new ASTWriter();
				for ( IASTNode Synth : booleanTransfomer.trackSynthetics ) {
					if (!(instrumentor1.foundSynthetics.contains(Synth))) 
						System.out.println(writer2.write(Synth));
				}*/

				//here I need to add the case for function were does not have any branch
				boolean isSpclFunWObranch=false;
				if(testObjectives.isEmpty()) {
					isSpclFunWObranch=true;
					// not using the isSpclFunWObranch instrumentation now!!!!!!!!!!!!!!!!!!!!!!!!!
					testObjectives.add(tempUnitComponent + ":" + "branch0-true");
					ASTRewrite rewriterBranch = ASTRewrite.create(translationUnit);
					InstrumentorVisitorToAddBranch InstAddBranch = new InstrumentorVisitorToAddBranch(tempUnitComponent,rewriterBranch);
					translationUnit.accept(InstAddBranch);
					System.out.println("File modified with temp branch:"+tempUnitComponent);

				}
				
				componentsTestObjectives.put(tempUnitComponent, testObjectives);
				// Need to store this one outside the loop to generate the branch pairs
				Map<String, List<String>> branchesToFun = new HashMap<String, List<String>>();

				for(String stateSet : instrumentor.functionBranchPairMap.keySet()) { 
					List<String> statementsSet = new ArrayList<>();
					statementsSet.addAll(instrumentor.convertArrayToSet(instrumentor.functionBranchPairMap.get(stateSet)));
					branchesToFun.put(stateSet, statementsSet);
				}
				nodeBranchMap.put(tempUnitComponent, branchesToFun);
				syntheticBranches.addAll(instrumentor.syntheticBranches); 

				it.unisa.ocelot.c.compiler.writer.ASTWriter writer = new it.unisa.ocelot.c.compiler.writer.ASTWriter();
				String outputCode = writer.write(translationUnit);
				StringBuilder result = new StringBuilder();
				result.append("#include \"ocelot.h\"\n");
				result.append(outputCode);
				Utils.writeFile(nameofComponent, result.toString());
				StringBuilder mainHeader = new StringBuilder();
				mainHeader.append("#include \"ocelot.h\"\n");
				mainHeader.append("#include <stdio.h>\n");
				mainHeader.append("#include <math.h>\n");
				mainHeader.append("#define OCELOT_TESTFUNCTION ").append(testFunName).append("\n");
				Utils.writeFile("jni/main.h", mainHeader.toString());

				//From here, the new version of tool: from the CFG -> CDG -> bracnch chains->
				CFG cfg = new CFG();
				CFGVisitor cfgVistor = new CFGVisitor(cfg, tempUnitComponent);
				translationUnit.accept(cfgVistor);

				bcm.process(cfg, tempUnitComponent);
			}
			else {
				//here, just copy the other supported files to jni folder, eg kcg_types, database etc.
				//add the feature to copy the header files
				File sourceFile = new File(unitComponent);

				File jniDir = new File("jni");
				if (!sourceFile.exists()) {
					System.out.println("Source file does not exist: " + unitComponent);
					return;
				}

				File destFile = new File(jniDir, sourceFile.getName());
				try {
					Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.out.println("File copied to: " + destFile.getAbsolutePath());
				} catch (IOException e) {
					System.out.println("Error copying file: " + e.getMessage());
				}

				String headerFile = unitComponent.replace(".c", ".h");
				File sourceHeaderFile = new File(headerFile);
				if (!sourceHeaderFile.exists()) {
					System.out.println("Source file does not exist: " + headerFile);
					continue; // skip this iteration and go to the next one some times only c files are needed.
				}
				File destHederFile = new File(jniDir, sourceHeaderFile.getName());
				try {
					Files.copy(sourceHeaderFile.toPath(), destHederFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					System.out.println("File copied to: " + destHederFile.getAbsolutePath());
				} catch (IOException e) {
					System.out.println("Error copying file: " + e.getMessage());
				}
			}
		}
		// To generate the pair from the branch chains.
		bcm.generatePairsForBranchChains();
		
		System.out.println("Number of synthetic branches generated with Bool: " + syntheticBranchesGeneratedWithBool);
		System.out.println("Number of synthetic branches generated with For: " + syntheticBranchesGeneratedWithFor);
		System.out.println("Number of synthetic branches founded in the code: " + foundSynthetic);
		//Synthetic Branches
		try { 
			FileOutputStream fos = new FileOutputStream("SyntheticBranches"); 
			ObjectOutputStream oos = new ObjectOutputStream(fos); 
			oos.writeObject(syntheticBranches); 
			oos.close(); 
			fos.close(); 
		} 
		catch (IOException ioe) { 
			ioe.printStackTrace(); 
		}
	}


	private void instrument() throws Exception {
		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(this.testFilename, this.testIncludes).copy();
		IASTPreprocessorStatement[] macros = translationUnit.getAllPreprocessorStatements();
		ExternalReferencesVisitor referencesVisitor = new ExternalReferencesVisitor(this.testFunction);
		translationUnit.accept(referencesVisitor);
		InstrumentorVisitor instrumentor = new InstrumentorVisitor(this.testFunction);
		MacroDefinerVisitor macroDefiner = new MacroDefinerVisitor(this.testFunction,
				referencesVisitor.getExternalReferences());
		// NOTE: macroDefine MUST preceed instrumentor in visit
		translationUnit.accept(macroDefiner);
		translationUnit.accept(instrumentor);
		it.unisa.ocelot.c.compiler.writer.ASTWriter writer = new it.unisa.ocelot.c.compiler.writer.ASTWriter();
		String outputCode = writer.write(translationUnit);
		StringBuilder result = new StringBuilder();
		for (IASTPreprocessorStatement macro : macros) {
			if (macro instanceof IASTPreprocessorIncludeStatement) {
				IASTPreprocessorIncludeStatement include = (IASTPreprocessorIncludeStatement) macro;
				if (include.isSystemInclude())
					result.append(macro.getRawSignature()).append("\n");
			} else
				result.append(macro.getRawSignature()).append("\n");
		}
		result.append("#include \"ocelot.h\"\n");
		result.append(outputCode);
		Utils.writeFile("jni/main.c", result.toString());
		StringBuilder mainHeader = new StringBuilder();
		mainHeader.append("#include \"ocelot.h\"\n");
		mainHeader.append("#include <stdio.h>\n");
		mainHeader.append("#include <math.h>\n");
		for (IASTNode typedef : instrumentor.getTypedefs()) {
			mainHeader.append(writer.write(typedef));
			mainHeader.append("\n");
		}
		mainHeader.append("#define OCELOT_TESTFUNCTION ").append(this.testFunction).append("\n");
		//TODO mainHeader.append("int main(/*list of all parameter's data types*/);\n"
		Utils.writeFile("jni/main.h", mainHeader.toString());
		this.callMacro = macroDefiner.getCallMacro();
		this.externDeclarations = referencesVisitor.getExternalDeclarations();
	}

	private void enrichJNICall() throws IOException {
		String metaJNI = Utils.readFile("jni/CBridge.c");
		metaJNI = callMacro + "\n\n" + externDeclarations + "\n\n" + metaJNI;
		String pointersH = "/** DO NOT EDIT. THIS FILE IS AUTOMATICALLY GENERATED BY THE BUILDER **/\n";
		pointersH += "#ifndef _Included_OcelotPointers\n" + "#define _Included_OcelotPointers\n"
				+ "#define OCELOT_ARRAYS_SIZE " + this.config.getTestArraysSize() + "\n"
				+ "typedef double _t_ocelot_array[OCELOT_ARRAYS_SIZE];\n" + "#endif\n";
		Utils.writeFile("jni/pointers.h", pointersH);
		Utils.writeFile("jni/EN_CBridge.c", metaJNI);
	}
}
