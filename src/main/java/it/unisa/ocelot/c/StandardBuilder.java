package it.unisa.ocelot.c;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.instrumentor.ExternalReferencesVisitor;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.MacroDefinerVisitor;
import it.unisa.ocelot.c.instrumentor.UnitComponentInstrumentorVisitor;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.util.Utils;

public class StandardBuilder extends Builder {
	private String testFilename;
	private String testFunction;
	private String[] testIncludes;
	private List<String> unitLevelComponents;

	private String callMacro;
	private String externDeclarations;

	private ConfigManager config;

	public StandardBuilder(String pTestFilename, String pTestFunction, String[] pTestIncludes) {
		super();
		setOutput(System.out);

		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testIncludes = pTestIncludes;
		this.unitLevelComponents = readUnitLevelComponents();
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
			this.stream.print("Instrumenting C file... \n");
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

		HashMap<String, ArrayList<String>> componentsTestObjectives = new HashMap<String, ArrayList<String>>();
		
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
		reportComponentsTestObjectives(componentsTestObjectives);

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
			FileWriter myWriter = new FileWriter("testObjectives.to", false);
			for (Entry<String, ArrayList<String>> componentTestObjectives : componentsTestObjectives.entrySet()) {
				String write = "{" + componentTestObjectives.getKey() + "=";
				for (String testObjective : componentTestObjectives.getValue()) {
					write = write + testObjective + ",";
				}
				write = write.substring(0, write.length() - 1);
				write = write + "};" + System.lineSeparator();
				myWriter.write(write);
			}
			myWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
