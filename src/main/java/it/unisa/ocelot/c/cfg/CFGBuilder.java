package it.unisa.ocelot.c.cfg;

import java.io.IOException;

import it.unisa.ocelot.c.compiler.GCC;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class CFGBuilder {
	public static CFG build(String pSourceFile, String pFunctionName) 
			throws IOException, CoreException {
		CFG graph = new CFG();

		IASTTranslationUnit translationUnit = GCC.getTranslationUnit(
                pSourceFile);
		CFGVisitor cfgBuilder = new CFGVisitor(graph, pFunctionName);
		ConstantsCheckerVisitor constantsChecker = new ConstantsCheckerVisitor(graph, pFunctionName);

		translationUnit.accept(cfgBuilder);
		translationUnit.accept(constantsChecker);

		return graph;
	}
}
