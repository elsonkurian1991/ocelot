package it.unisa.ocelot.c.instrumentor;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTExpressionStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTExpressionStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIdExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTIfStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTLiteralExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTName;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTNullStatement;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGVisitor;

public class InstrumentorVisitorToAddBranch extends ASTVisitor{
	private final String functionName;
	private final ASTRewrite rewriter;

	public InstrumentorVisitorToAddBranch(String pInstrumentFunction, ASTRewrite rewriter) {
		// TODO Auto-generated constructor stub
		this.shouldVisitExpressions = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitPointerOperators = true;
		this.functionName = pInstrumentFunction;
		this.rewriter = rewriter;
	}
	@Override
	public int visit(IASTTranslationUnit tu) {

		tu.accept(new CFGVisitor(new CFG(), this.functionName));

		return super.visit(tu);
	}
	@Override
    public int visit(IASTDeclaration declaration) {
        
        if (declaration instanceof IASTFunctionDefinition) {
            IASTFunctionDefinition functionDef = (IASTFunctionDefinition) declaration;
                       
            IASTStatement body = functionDef.getBody();
            
            if (body instanceof IASTCompoundStatement) {
            	String ifCond="_f_ocelot_branch_out("+this.functionName+",0,true,0,1)";
                CASTCompoundStatement compoundBody = (CASTCompoundStatement) body;
                
                CASTCompoundStatement substitute = new CASTCompoundStatement();
                for (IASTStatement stmt : compoundBody.getStatements()) {
                	 substitute.addStatement(stmt.copy()); 	
                }
                CASTLiteralExpression cond = new CASTLiteralExpression(IASTLiteralExpression.lk_integer_constant,ifCond);
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
