package it.unisa.ocelot.c.cfg;

import java.io.IOException;

import it.unisa.ocelot.c.compiler.GCC;
import it.unisa.ocelot.c.instrumentor.CASTVisitorExternalReferences;
import it.unisa.ocelot.c.instrumentor.CASTVisitorMacroDefiner;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;

public class CFGBuilder {
	public static CFG build(String pSourceFile, String pFunctionName) 
	throws IOException, CoreException {
		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(pSourceFile);
		CFG cfg = new CFG();

		CASTVisitorExternalReferences extReferences = new CASTVisitorExternalReferences(pFunctionName);
		translationUnit.accept(extReferences);

		//TODO why MacroDefinerVisitor??? Are we overloading this visitor with stuff that shouldn't do?
		CASTVisitorMacroDefiner macroDefiner = new CASTVisitorMacroDefiner(translationUnit, pFunctionName, extReferences.getExternalReferences());
		translationUnit.accept(macroDefiner);

		CASTVisitorCFGBuilder tuBuilder = new CASTVisitorCFGBuilder(translationUnit, cfg, pFunctionName, macroDefiner.getFunctionParametersTypes());
		translationUnit.accept(tuBuilder);

		CASTVisitorConstantsCFGInjector constantsDetector = new CASTVisitorConstantsCFGInjector(cfg, pFunctionName);
		translationUnit.accept(constantsDetector);

		return cfg;
	}
}
