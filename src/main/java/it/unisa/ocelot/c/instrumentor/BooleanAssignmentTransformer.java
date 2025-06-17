package it.unisa.ocelot.c.instrumentor;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
 
public class BooleanAssignmentTransformer extends ASTVisitor {
 
    private final IASTTranslationUnit tu;
    private final ICNodeFactory nodeFactory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
 
    public BooleanAssignmentTransformer(IASTTranslationUnit tu) {
        super();
        this.tu = tu;
        shouldVisitStatements = true;
    }
 
    @Override
    public int visit(IASTStatement stmt) {
        if (stmt instanceof IASTExpressionStatement) {
            IASTExpression expr = ((IASTExpressionStatement) stmt).getExpression();
 
            // Detect a = ...
            if (expr instanceof IASTBinaryExpression) {
                IASTBinaryExpression assignment = (IASTBinaryExpression) expr;
 
                if (assignment.getOperator() == IASTBinaryExpression.op_assign) {
                    IASTExpression lhs = assignment.getOperand1();
                    IASTExpression rhs = assignment.getOperand2();
 
                    // Transform only if RHS is a boolean expression (heuristically)
                    if (looksLikeBooleanExpression(rhs)) {
                        IASTIfStatement ifStmt = createIfElseAssignment(lhs.copy(), rhs.copy());
 
                        // Replace the expression statement with the new if-statement
                        replaceStatement(stmt, ifStmt);
                    }
                }
            }
        }
 
        return PROCESS_CONTINUE;
    }
 
    private boolean looksLikeBooleanExpression(IASTExpression expr) {
        //return expr instanceof IASTBinaryExpression || expr instanceof IASTUnaryExpression || expr instanceof IASTFunctionCallExpression;
    	return expr instanceof IASTBinaryExpression || expr instanceof IASTUnaryExpression;
    }
 
    private IASTIfStatement createIfElseAssignment(IASTExpression targetVar, IASTExpression condition) {
        IASTExpression thenExpr = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_assign,
                targetVar.copy(), nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_integer_constant, "1"));
 
        IASTExpression elseExpr = nodeFactory.newBinaryExpression(IASTBinaryExpression.op_assign,
                targetVar.copy(), nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_integer_constant, "0"));
 
        IASTCompoundStatement thenBlock = nodeFactory.newCompoundStatement();
        thenBlock.addStatement(nodeFactory.newExpressionStatement(thenExpr));
 
        IASTCompoundStatement elseBlock = nodeFactory.newCompoundStatement();
        elseBlock.addStatement(nodeFactory.newExpressionStatement(elseExpr));
 
        IASTIfStatement ifStmt = nodeFactory.newIfStatement(condition, thenBlock, elseBlock);
        ifStmt.setPropertyInParent(new ASTNodeProperty("GENERATED"));
        return ifStmt;
    }
 
    private void replaceStatement(IASTStatement oldStmt, IASTStatement newStmt) {
        IASTNode parent = oldStmt.getParent();
        if (parent instanceof IASTCompoundStatement) {
            CASTCompoundStatement block = (CASTCompoundStatement) parent;
            IASTStatement[] stmts = block.getStatements();
            for (int i = 0; i < stmts.length; i++) {
                if (stmts[i] == oldStmt) {
                    stmts[i] = newStmt;
                    block.replace(oldStmt, newStmt);
                    break;
                }
            }
        }
    }
}
