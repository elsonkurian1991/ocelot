package it.unisa.ocelot.c;

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.instrumentor.InstrumentorVisitor;
import it.unisa.ocelot.c.instrumentor.MacroDefinerVisitor;
import it.unisa.ocelot.c.makefile.JNIMakefileGenerator;
import it.unisa.ocelot.c.makefile.LinuxMakefileGenerator;
import it.unisa.ocelot.c.makefile.MacOSXMakefileGenerator;
import it.unisa.ocelot.c.makefile.WindowsMakefileGenerator;
import it.unisa.ocelot.util.Utils;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;

public class StandardBuilder extends Builder {
	private String testFilename;
	private String testFunction;
	private String[] testIncludes;
		
	public StandardBuilder(String pTestFilename, String pTestFunction, String[] pTestIncludes) {
		super();
		setOutput(System.out);
		
		this.testFilename = pTestFilename;
		this.testFunction = pTestFunction;
		this.testIncludes = pTestIncludes;
	}
	
	@Override
	public void build() throws IOException, BuildingException {
		if (this.makefileGenerator == null)
			throw new BuildingException("No makefile generator specified");
		if (this.stream == null)
			throw new BuildingException("No output stream specified"
					+ "");
		//Insturments the code and copies it in main.c
		String callMacro;
		try {
			this.stream.print("Instrumenting C file... \n");
			callMacro = instrument();
			this.stream.println("Done!");
		} catch (Exception e) {
			throw new BuildingException(e.getMessage());
		}
		
		//Adds extra macros in CBridge.c
		this.stream.print("Defining test function call... ");
		enrichJNICall(callMacro);
		this.stream.println("Done!");
		
		//Builds the library
		this.stream.print("Building library... ");
		this.makefileGenerator.generate();
		
		Process proc = this.makefileGenerator.runCompiler();
				
		this.stream.println(IOUtils.toString(proc.getInputStream()));
		
		try {
			int result;
			if ((result=proc.waitFor()) == 0)
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
	
	private String instrument() throws Exception {
		String code = Utils.readFile(this.testFilename);
		
		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(
				code.toCharArray(),
				this.testFilename,
				this.testIncludes).copy();
		
		IASTPreprocessorStatement[] macros =  translationUnit.getAllPreprocessorStatements();
		
		InstrumentorVisitor instrumentor = new InstrumentorVisitor(this.testFunction);
		MacroDefinerVisitor macroDefiner = new MacroDefinerVisitor(this.testFunction);
		
		//NOTE: macroDefine MUST preceed instrumentor in visit
		translationUnit.accept(macroDefiner);
		translationUnit.accept(instrumentor);
		
		it.unisa.ocelot.c.compiler.writer.ASTWriter writer = new it.unisa.ocelot.c.compiler.writer.ASTWriter();
				
		String outputCode = writer.write(translationUnit);
		
		String result = "";
		for (IASTPreprocessorStatement macro : macros) {
			if (macro instanceof IASTPreprocessorIncludeStatement) {
				IASTPreprocessorIncludeStatement include = (IASTPreprocessorIncludeStatement)macro;
				if (include.isSystemInclude())
					result += macro.getRawSignature()+"\n";
			} else
				result += macro.getRawSignature() + "\n";
		}
		result += "#include \"ocelot.h\"\n";
		result += outputCode;
		
		Utils.writeFile("jni/main.c", result);
		
		String mainHeader = "";
		mainHeader += "#include \"ocelot.h\"\n";
		mainHeader += "#include <stdio.h>\n";
		mainHeader += "#include <math.h>\n";
		for (IASTNode typedef : instrumentor.getTypedefs()) {
			mainHeader += writer.write(typedef);
			mainHeader += "\n";
		}

		mainHeader += "#define OCELOT_TESTFUNCTION " + this.testFunction + "\n";

		Utils.writeFile("jni/main.h", mainHeader);
		
		return macroDefiner.getCallMacro();
	}
	
	private void enrichJNICall(String pCallMacro) throws IOException {
		String metaJNI = Utils.readFile("jni/CBridge.c");
		
		metaJNI = "#include \"CBridge.h\"\n" +
				pCallMacro + "\n\n" + 
				metaJNI;
		
		Utils.writeFile("jni/EN_CBridge.c", metaJNI);
	}
}
