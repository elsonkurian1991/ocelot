package it.unisa.ocelot.c;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.instrumentor.ExternalReferencesVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.MacroDefinerVisitor;
import it.unisa.ocelot.c.instrumentor.StatementTreePrinter;
import it.unisa.ocelot.c.instrumentor.UnitComponentInstrumentorVisitor;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class StandardBuilder extends Builder {
	private String testFilename;
	private String testFunction;
	private String[] testIncludes;
	private List<String> unitLevelComponents;

	private String callMacro;
	private String externDeclarations;

	private ConfigManager config;
	private HashMap<String, ArrayList<String>> componentsTestObjectives;
	
	// Maps the caller function to an hashmap, that maps the called function 
	// to the braches required to take that function
	private Map<String, Map<String, List<String>>> nodeBranchMap;

	public StandardBuilder(String pTestFilename, String pTestFunction, String[] pTestIncludes) {
		super();
		setOutput(System.out);

		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testIncludes = pTestIncludes;
		this.unitLevelComponents = readUnitLevelComponents();
		this.componentsTestObjectives = new HashMap<String, ArrayList<String>>();
		this.nodeBranchMap = new HashMap<String, Map<String, List<String>>>();
	}

	private List<String> readUnitLevelComponents() {
		List<String> unitLevelComponents = new ArrayList<String>();
		try {
			unitLevelComponents = Files.readAllLines(Paths.get("unitLevelComponents.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unitLevelComponents;
	}

	@Override
	public void build() throws IOException, BuildingException {
		this.config = ConfigManager.getInstance();
		if (this.makefileGenerator == null)
			throw new BuildingException("No makefile generator specified");
		if (this.stream == null)
			throw new BuildingException("No output stream specified" + "");
		// Insturments the code and copies it in main.c
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

		this.stream.println(IOUtils.toString(proc.getInputStream()));

		try {
			int result;
			if ((result = proc.waitFor()) == 0)
				this.stream.println("Done!");
			else {
				this.stream.println("ABORTED. An error occurred: " + result);
				throw new BuildingException(IOUtils.toString(proc.getErrorStream()));
			}
		} catch (InterruptedException e) {
			throw new BuildingException("Interrupted");
		}

		this.stream.println("\nEverything done.");
	}

	private void instrumentUnitComponents() throws Exception {
		int arr=0;
		String[] testIncludesTemp= new String[1];
		
		// This function maps the caller function to an hashmap, that maps the called function 
		// to the braches required to take that function

		
		for(String unitComponent:this.testIncludes) {
			int lastIndex=unitComponent.lastIndexOf('/');
			String testFunName=unitComponent.substring(lastIndex+1);
			//System.out.println(testFunName);
			String tempUnitComponent=testFunName.substring(0, testFunName.length()-2);
			//System.out.println(tempUnitComponent);
			if(unitLevelComponents.contains(tempUnitComponent)) {
				String nameofComponent="jni/"+unitComponent.substring(lastIndex+1);
				//System.out.println(nameofComponent);

				testIncludesTemp[0]=unitComponent;
				IASTTranslationUnit translationUnit = GCC.getTranslationUnit(unitComponent, testIncludesTemp).copy();
				
				/*System.out.println(translationUnit);
				Field[] fields = translationUnit.getClass().getDeclaredFields();
				for (Field field: fields) {
					System.out.println(field);
				}*/
				
				/*IASTPreprocessorStatement[] macros = translationUnit.getAllPreprocessorStatements();

				ExternalReferencesVisitor referencesVisitor = new ExternalReferencesVisitor(testFunName);
				translationUnit.accept(referencesVisitor);

				InstrumentorVisitor instrumentor = new InstrumentorVisitor(testFunName);
				MacroDefinerVisitor macroDefiner = new MacroDefinerVisitor(testFunName,
						referencesVisitor.getExternalReferences());

				// NOTE: macroDefine MUST preceed instrumentor in visit
				translationUnit.accept(macroDefiner);
				translationUnit.accept(instrumentor);
				 */
				

				// Instruments unit-level components
				//for (String component : unitLevelComponents) {
				ExternalReferencesVisitor referencesVisitor1 = new ExternalReferencesVisitor(tempUnitComponent);
				translationUnit.accept(referencesVisitor1);

				ArrayList<String> testObjectives = new ArrayList<String>();
				
				UnitComponentInstrumentorVisitor instrumentor1 = new UnitComponentInstrumentorVisitor(tempUnitComponent,
						testObjectives, unitLevelComponents);
				//if(testObjectives.isEmpty()) continue;
				componentsTestObjectives.put(tempUnitComponent, testObjectives);
				MacroDefinerVisitor macroDefiner1 = new MacroDefinerVisitor(tempUnitComponent,
						referencesVisitor1.getExternalReferences());

				// NOTE: macroDefine MUST preceed instrumentor in visit
				translationUnit.accept(macroDefiner1);
				
				// Used to print the structure of the AST for debugging
				//StatementTreePrinter printer = new StatementTreePrinter();
				//translationUnit.accept(printer);
				
				translationUnit.accept(instrumentor1);
				
				// Need to store this one outside the loop to generate the branch pairs
				nodeBranchMap.put(tempUnitComponent, instrumentor1.functionBranchPairMap);
				
				System.out.println(tempUnitComponent + "::" + instrumentor1.functionBranchPairMap.keySet());
				
				
				//}
				

				it.unisa.ocelot.c.compiler.writer.ASTWriter writer = new it.unisa.ocelot.c.compiler.writer.ASTWriter();

				String outputCode = writer.write(translationUnit);
				
				StringBuilder result = new StringBuilder();
				/*for (IASTPreprocessorStatement macro : macros) {
					if (macro instanceof IASTPreprocessorIncludeStatement) {
						IASTPreprocessorIncludeStatement include = (IASTPreprocessorIncludeStatement) macro;
						if (include.isSystemInclude())
							result.append(macro.getRawSignature()).append("\n");
					} else
						result.append(macro.getRawSignature()).append("\n");
				}*/
				result.append("#include \"ocelot.h\"\n");
				result.append(outputCode);
				//Utils.writeFile(code, outputCode);
				//FileWriter writer = new FileWriter(filePath)

				Utils.writeFile(nameofComponent, result.toString());

				StringBuilder mainHeader = new StringBuilder();
				mainHeader.append("#include \"ocelot.h\"\n");
				mainHeader.append("#include <stdio.h>\n");
				mainHeader.append("#include <math.h>\n");
				/*for (IASTNode typedef : instrumentor.getTypedefs()) {
					mainHeader.append(writer.write(typedef));
					mainHeader.append("\n");
				}*/

				mainHeader.append("#define OCELOT_TESTFUNCTION ").append(testFunName).append("\n");

				Utils.writeFile("jni/main.h", mainHeader.toString());
				//TODO here we consider only true branch. need a false branch also!!
				if (componentsTestObjectives.containsKey(tempUnitComponent) && componentsTestObjectives.get(tempUnitComponent) != null && componentsTestObjectives.get(tempUnitComponent).isEmpty()) {
					// add a temp branch because there is no branch in the function. 
					//System.out.println(tempUnitComponent);
					
					ArrayList<String> tempTestObj= new ArrayList<String>();
					tempTestObj.add(tempUnitComponent + ":" + "branch0-true");
					componentsTestObjectives.replace(tempUnitComponent, tempTestObj);
					String tempBranchInfo="if(_f_ocelot_branch_out(\""+tempUnitComponent+"\",0,true,0,1)){"
							+ "\n}\n";
					try {// Read that C file, and temp branch
						String filePath="jni/"+tempUnitComponent+".c";
						//System.out.println(filePath);
						String content = new String(Files.readAllBytes(Paths.get(filePath)));
						// Find the position of the last '}'
						int lastBraceIndex = content.lastIndexOf('}');
						if (lastBraceIndex != -1) {
							// Insert the string before the last '}'
							String modifiedContent = content.substring(0, lastBraceIndex) + tempBranchInfo + content.substring(lastBraceIndex);
							Files.write(Paths.get(filePath), modifiedContent.getBytes());
							System.out.println(tempUnitComponent+" File is modified successfully with temp branch.");
						} 
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				reportComponentsTestObjectives(componentsTestObjectives);
				//this.callMacro = macroDefiner.getCallMacro();
				//this.externDeclarations = referencesVisitor.getExternalDeclarations();
			}

		}
		
		// Martino
		// This code generate the pairs
		generatePairs();

		/**
		direct
		for (String func : nodeBranchMap.keySet()) {
			Map<String, List<String>> functionBranchPairMap = nodeBranchMap.get(func);
			for (String calledFunc : functionBranchPairMap.keySet()) {
				// have to multiply some of the starting function branches with all of the arriving function branches
				cartesianProduct(functionBranchPairMap.get(calledFunc), 
			}
		}**/
		
	}
	
	private void generatePairs() {
		int indirectionLevel = 2;
		List<List<TestObjStateMachine>> indirectPairs = new ArrayList<List<TestObjStateMachine>>();
		for (int j = 0; j < indirectionLevel; j++) {
			indirectPairs.add(j, new ArrayList<TestObjStateMachine>());
		}
		for(String unitComponent:this.testIncludes) {
			int lastIndex=unitComponent.lastIndexOf('/');
			String testFunName=unitComponent.substring(lastIndex+1);

			String tempUnitComponent=testFunName.substring(0, testFunName.length()-2);
			

			if(unitLevelComponents.contains(tempUnitComponent)) {
				List<List<TestObjStateMachine>>  localIndirectPairs = generatePairforFunction(tempUnitComponent, indirectionLevel);
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
	
	private List<List<TestObjStateMachine>> generatePairforFunction(String functionName, int indirectionLevel) {
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
		List<List<String>> overallIndirectBraches = new ArrayList<List<String>>();
		//List<String> levelIndirectBranches = new ArrayList<String>();
		/*for(String indirectFunction : nodeBranchMap.get(functionName).keySet()) {
			if (indirectFunction != startingFunctionName) {
				ArrayList<String> branchesToAdd = componentsTestObjectives.get(indirectFunction);
				levelIndirectBranches.addAll(branchesToAdd);
			}
		}*/
		ArrayList<String> DirectBranchesToAdd = componentsTestObjectives.get(calledFunction);
		overallIndirectBraches.add(DirectBranchesToAdd);
		if (indirectionLevel > 1) {
			List<List<List<String>>> localOverallIndirectBraches = new ArrayList<List<List<String>>>(); // for and aggregate generate pairs
			for(String indirectFunction: nodeBranchMap.get(calledFunction).keySet()) {
				if (indirectFunction != startingFunctionName) {
					localOverallIndirectBraches.add(generateIndirectPairs(indirectFunction, indirectionLevel - 1, startingFunctionName));
				}
			}
			if (localOverallIndirectBraches.isEmpty()) {
				for (int i = 0; i < indirectionLevel - 1; i++) {
					overallIndirectBraches.add(new ArrayList<String>());
				}
			}
			else {
				overallIndirectBraches.addAll(aggregatePairsByIndirectionLevel(localOverallIndirectBraches));
				}
			}
		return overallIndirectBraches;

	}
	
	private List<List<String>> aggregatePairsByIndirectionLevel(List<List<List<String>>> localOverallIndirectBraches){
		List<List<String>> overallIndirectBraches = new ArrayList<List<String>>();
		try {
		for (int i = 0; i < localOverallIndirectBraches.get(0).size(); i++) {
			List<String> aggregator = new ArrayList<String>();
			for(List<List<String>> indirectTreeBranches: localOverallIndirectBraches) {
				aggregator.addAll(indirectTreeBranches.get(i)); 
			}
			overallIndirectBraches.add(aggregator);
		}
    } 
    catch (Exception ioe) { 
        ioe.printStackTrace(); 
    } 
		
		
		return overallIndirectBraches;
	}
	
	private List<List<TestObjStateMachine>> cartesianProduct(List<String> setA, List<List<String>> indirectBraches) {
	    List<List<TestObjStateMachine>> cartesianSet = new ArrayList<List<TestObjStateMachine>>();

	    for (int j = 0; j < indirectBraches.size(); j++) {
	    	cartesianSet.add(j, new ArrayList<TestObjStateMachine>());
	    }
	    
	    for (String i: setA) {
	    	for (int j = 0; j < indirectBraches.size(); j++) {
	        	for (String k: indirectBraches.get(j)) {
	                String testObjOne=i;
					double fitValOne=Double.MAX_VALUE;
					String testObjTwo=k;
					double fitValTwo=Double.MAX_VALUE;
					TestObjStateMachine testobjSM = new TestObjStateMachine(testObjOne,fitValOne,testObjTwo,fitValTwo); 
					cartesianSet.get(j).add(testobjSM);
				}
	        }
	    }
	    return cartesianSet;
	}

	private void instrument() throws Exception {
		String code = Utils.readFile(this.testFilename);

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

		/*HashMap<String, ArrayList<String>> componentsTestObjectives = new HashMap<String, ArrayList<String>>();

		// Instruments unit-level components
		for (String component : unitLevelComponents) {
			ExternalReferencesVisitor referencesVisitor1 = new ExternalReferencesVisitor(component);
			translationUnit.accept(referencesVisitor1);

			ArrayList<String> testObjectives = new ArrayList<String>();
			UnitComponentInstrumentorVisitor instrumentor1 = new UnitComponentInstrumentorVisitor(component,
					testObjectives);
			componentsTestObjectives.put(component, testObjectives);
			MacroDefinerVisitor macroDefiner1 = new MacroDefinerVisitor(component,
					referencesVisitor1.getExternalReferences());

			// NOTE: macroDefine MUST preceed instrumentor in visit
			translationUnit.accept(macroDefiner1);
			translationUnit.accept(instrumentor1);
			
		}
		System.out.println(componentsTestObjectives.toString());
		reportComponentsTestObjectives(componentsTestObjectives);
		*/
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

	private void reportComponentsTestObjectives(HashMap<String, ArrayList<String>> componentsTestObjectives) {
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
