package it.unisa.ocelot.c.instrumentor;

import java.io.IOException;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIfStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTLiteralExpression;
import org.eclipse.core.runtime.CoreException;

import it.unisa.ocelot.c.cfg.CFGBuilder;

public class InstrumentorVisitorToAddBranch extends ASTVisitor{
	private final String sourceFile;
	private final String functionName;

	public InstrumentorVisitorToAddBranch(String pSourceFile, String pInstrumentFunction) {
		this.shouldVisitExpressions = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitPointerOperators = true;
		
		this.sourceFile = pSourceFile;
		this.functionName = pInstrumentFunction;
	}
	@Override
	public int visit(IASTTranslationUnit tu) {
		//Simulates the generation of a CFG in order to correctly retrieve the "case"
		//unique ids. The resulting CFG will never be used.
		//TODO does it work? Where are these unique id stored? Perhaps do it better; e.g., define a lightweight visitor able to initialize the "case" unique ids only correctly. See also class InstrumentorVisitor, UnitComponentInstrumentorVisitor, InstrumentorVisitorForIfMethodCalls (duplicate).
		try {
			CFGBuilder.build(this.sourceFile, this.functionName);
		} catch (IOException | CoreException e) {
			throw new RuntimeException(e);
		}
		
		return super.visit(tu);
	}
	@Override
    public int visit(IASTDeclaration declaration) {
        
        if (declaration instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition functionDef = (IASTFunctionDefinition) declaration;
            IASTStatement body = functionDef.getBody();
            
            if (body instanceof IASTCompoundStatement) {
            	String ifCond = "_f_ocelot_branch_out(" + this.functionName + ", 0, true, 0, 1)";
                CASTLiteralExpression cond = new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant, ifCond);
                
                CASTCompoundStatement compoundBody = (CASTCompoundStatement) body;
                CASTCompoundStatement substitute = new CASTCompoundStatement();
                for (IASTStatement stmt : compoundBody.getStatements()) {
                	 substitute.addStatement(stmt.copy()); 	
                }
                
                CASTIfStatement ifStmt = new CASTIfStatement();
                ifStmt.setConditionExpression(cond);
                ifStmt.setThenClause(substitute);
                
                CASTCompoundStatement newBody = new CASTCompoundStatement();
                newBody.addStatement(ifStmt);
                functionDef.setBody(newBody);
            } 
        }
        
        return PROCESS_CONTINUE;
    }
}