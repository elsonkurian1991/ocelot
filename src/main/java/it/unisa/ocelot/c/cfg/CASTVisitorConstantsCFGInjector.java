package it.unisa.ocelot.c.cfg;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionDefinition;

/**
 * This visitor scans a C AST and updates a 
 * control flow graph with information on the 
 * constants in a given function.
 * 
 * @author simone
 */
public class CASTVisitorConstantsCFGInjector extends ASTVisitor {
	private final CFG cfg;
	private final String functionName;
	
	/**
	 * Constructor.
	 * 
	 * @param pCfg the {@link CFG} which will be visited and
	 *        updated.
	 * @param pFunctionName the name of a function that
	 *        must be declared in {@code pCfg} before
	 *        the visit.
	 */
	public CASTVisitorConstantsCFGInjector(CFG pCfg, String pFunctionName) {
		this.shouldVisitExpressions = true;
		this.shouldVisitDeclarations = true;
		
		this.cfg = pCfg;
		this.functionName = pFunctionName;
	}
	
	public int visit(IASTDeclaration name) {
		if (name instanceof CASTFunctionDefinition) {
			IASTFunctionDefinition function = (CASTFunctionDefinition) name;

			if (function.getDeclarator().getName().getRawSignature().equals(this.functionName)) {
				return PROCESS_CONTINUE;
			}
		}
		
		//it is not the function with name pFunctionName
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTExpression expression) {
		if (expression instanceof IASTLiteralExpression) {
			String source = expression.getRawSignature();
			try {
				Double constant = Double.valueOf(Double.parseDouble(source));
				this.cfg.addConstantNumeric(constant);
			} catch (NumberFormatException e) {
				//it is not a numeric constant, let's consider
				//it a String.
				this.cfg.addConstantString(source);
			}
		}
		return super.visit(expression);
	}
}
