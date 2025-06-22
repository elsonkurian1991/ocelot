package it.unisa.ocelot.c.instrumentor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICNodeFactory;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTSimpleDeclaration;
 
public class BooleanAssignmentTransformer extends ASTVisitor {
 
    private final IASTTranslationUnit tu;
    private final ICNodeFactory nodeFactory = ASTNodeFactoryFactory.getDefaultCNodeFactory();
    private final Set<String> trackedBoolVars = new HashSet<>();
 
    public BooleanAssignmentTransformer(IASTTranslationUnit tu) {
        super();
        this.tu = tu;
        shouldVisitStatements = true;
        shouldVisitDeclSpecifiers = true;
        shouldVisitExpressions = true;
        
		this.shouldVisitDeclarations = true;
		
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
    }
 

    

    public int visit(CASTSimpleDeclaration declaration) {
    	if (((CASTSimpleDeclaration) declaration).getRawSignature().contains("IfBlock1_clock"))
        	System.out.println(";");
    	IASTDeclSpecifier spec = declaration.getDeclSpecifier();
    	if (isBooleanType(spec)) {
    		for(IASTDeclarator declarator : declaration.getDeclarators()) {
    			if (declarator.getName() != null)
    				trackedBoolVars.add(declarator.getName().toString());
    		}
    	}
    	return PROCESS_CONTINUE;
    }
    
    public int visit(IASTStatement stmt) {
    	if (stmt instanceof CASTDeclarationStatement) {
            for (IASTNode child : (stmt.getChildren())) {
            	CASTSimpleDeclaration simpleDec = ((CASTSimpleDeclaration) child);
            	System.out.println(simpleDec.getRawSignature());
            	if (((CASTSimpleDeclaration) child).getRawSignature().contains("IfBlock1_clock"))
            		System.out.println(";");
            	System.out.println(simpleDec.getDeclSpecifier().getRawSignature());
            	if (isBooleanType(simpleDec.getDeclSpecifier())) {
            		System.out.println(simpleDec.getChildren());
            		for (IASTNode child2 : simpleDec.getDeclarators())
            			trackedBoolVars.add(child2.getRawSignature());
            	}
            }
    	}
    	if (stmt instanceof IASTExpressionStatement) {
            IASTExpression expr = ((IASTExpressionStatement) stmt).getExpression();
 
            // Detect a = ...
            if (expr instanceof IASTBinaryExpression) {
                IASTBinaryExpression assignment = (IASTBinaryExpression) expr;
 
                if (assignment.getOperator() == IASTBinaryExpression.op_assign) {
                    IASTExpression lhs = assignment.getOperand1();
                    IASTExpression rhs = assignment.getOperand2();
 
                    // Transform only if RHS is a boolean expression (heuristically)
                    if (lhs instanceof IASTIdExpression) {
                    	String varName = ((IASTIdExpression) lhs).getName().toString();
                    	
                    	if(trackedBoolVars.contains(varName)) {
                    		IASTIfStatement ifStmt = createIfElseAssignment(lhs.copy(), rhs.copy());
                    		// Replace the expression statement with the new if-statement
                            replaceStatement(stmt, ifStmt);
                    	}
                    }
                }
            }
        }
		return PROCESS_CONTINUE;
    }

 
    /*private boolean looksLikeBooleanExpression(IASTExpression expr) {
    	if (expr instanceof IASTIdExpression) {
    		IBinding binding = ((IASTIdExpression) expr).getName().resolveBinding();
    		if (binding instanceof IVariable) {
    			IType type = ((IVariable) binding).getType();
    			return isItBool(type);
    		}
    	}
    	
    	return false;
    }*/
    

	private boolean isBooleanType(IASTDeclSpecifier spec) {
    	if (spec instanceof ICASTSimpleDeclSpecifier) {
    		ICASTSimpleDeclSpecifier cspec =  (ICASTSimpleDeclSpecifier) spec;
    		if (cspec.getType() ==  IASTSimpleDeclSpecifier.t_bool)
    			return true;
    		return false;
    	}
    	if (spec.getRawSignature().contains("kcg_bool"))
			return true;
    	return false;
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
        //ifStmt.setPropertyInParent(new ASTNodeProperty("GENERATED"));
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
            newStmt.setPropertyInParent(new ASTNodeProperty("GENERATED"));
        }
    }
}
