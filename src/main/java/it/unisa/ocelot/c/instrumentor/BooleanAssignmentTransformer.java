package it.unisa.ocelot.c.instrumentor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICASTSimpleDeclSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTSimpleDeclaration;
 
public class BooleanAssignmentTransformer extends ASTVisitor {
    private final Set<String> trackedBoolVars = new HashSet<>();
 
    //set at translation time
    private INodeFactory nodeFactory;
    
    public BooleanAssignmentTransformer() {
        super();
        this.shouldVisitStatements = true;
        this.shouldVisitDeclSpecifiers = true;
        this.shouldVisitExpressions = true;
		this.shouldVisitDeclarations = true;
		this.shouldVisitDeclarators = true;
		this.shouldVisitTranslationUnit = true;
		this.shouldVisitStatements = true;
		this.shouldVisitDeclSpecifiers = true;
		this.shouldVisitNames = true;
    }
    
    public void transform(IASTTranslationUnit translationUnit) {
    	this.nodeFactory = translationUnit.getASTNodeFactory();
    	translationUnit.accept(this);
    }
 
    /*public int visit(CASTSimpleDeclaration declaration) {
    	//if (((CASTSimpleDeclaration) declaration).getRawSignature().contains("IfBlock1_clock"))
        	//System.out.println(";");
    	IASTDeclSpecifier spec = declaration.getDeclSpecifier();
    	if (isBooleanType(spec)) {
    		for(IASTDeclarator declarator : declaration.getDeclarators()) {
    			if (declarator.getName() != null)
    				trackedBoolVars.add(declarator.getName().toString());
    		}
    	}
    	return PROCESS_CONTINUE;
    }*/
    
    public int visit(IASTStatement stmt) {
    	if (stmt instanceof CASTDeclarationStatement) {
            for (IASTNode child : stmt.getChildren()) {
            	CASTSimpleDeclaration simpleDec = (CASTSimpleDeclaration) child;
            	//System.out.println(simpleDec.getRawSignature());
            	//if (((CASTSimpleDeclaration) child).getRawSignature().contains("IfBlock1_clock"))
            	//	System.out.println(";");
            	//System.out.println(simpleDec.getDeclSpecifier().getRawSignature());
            	if (isBooleanType(simpleDec.getDeclSpecifier())) {
            		//System.out.println(simpleDec.getChildren());
            		for (IASTNode child2 : simpleDec.getDeclarators()) {
            			this.trackedBoolVars.add(child2.getRawSignature());
            		}
            	}
            }
    	}
    	if (stmt instanceof IASTExpressionStatement) {
            IASTExpression expr = ((IASTExpressionStatement) stmt).getExpression();
 
            // Detect an assignment
            if (expr instanceof IASTBinaryExpression) {
                IASTBinaryExpression assignment = (IASTBinaryExpression) expr;
                //System.out.println(assignment.getRawSignature());
                if (assignment.getOperator() == IASTBinaryExpression.op_assign && containsWords(assignment.getRawSignature())) {
                	//System.out.println(assignment.getRawSignature());
                    IASTExpression lhs = assignment.getOperand1();
                    IASTExpression rhs = assignment.getOperand2();
 
                    // Transform only if LHS is a tracked boolean variable (heuristically)
                    if (lhs instanceof IASTIdExpression) {
                    	String varName = ((IASTIdExpression) lhs).getName().toString();
                    	
                    	if (this.trackedBoolVars.contains(varName)) {
                    		IASTIfStatement ifStmt = createIfElseAssignment(lhs, rhs);
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
    
    private static boolean containsWords(String inputString) {
        boolean found = false;
        String[] items = {"&", "|", "<", ">"};
        for (String item : items) {
            if (inputString.contains(item)) {
                found = true;
                break;
            }
        }
        return found;
    }

	private static boolean isBooleanType(IASTDeclSpecifier spec) {
    	if (spec instanceof ICASTSimpleDeclSpecifier) {
    		ICASTSimpleDeclSpecifier cspec =  (ICASTSimpleDeclSpecifier) spec;
    		if (cspec.getType() ==  IASTSimpleDeclSpecifier.t_bool) {
    			return true;
    		}
    		return false;
    	}
    	if (spec.getRawSignature().contains("kcg_bool")) {
			return true;
    	}
    	return false;
    }
 
    private IASTIfStatement createIfElseAssignment(IASTExpression targetVar, IASTExpression condition) {
        IASTExpression thenExpr = this.nodeFactory.newBinaryExpression(IASTBinaryExpression.op_assign,
                targetVar.copy(), this.nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_integer_constant, "1"));
 
        IASTExpression elseExpr = this.nodeFactory.newBinaryExpression(IASTBinaryExpression.op_assign,
                targetVar.copy(), this.nodeFactory.newLiteralExpression(IASTLiteralExpression.lk_integer_constant, "0"));
 
        IASTCompoundStatement thenBlock = this.nodeFactory.newCompoundStatement();
        thenBlock.addStatement(this.nodeFactory.newExpressionStatement(thenExpr));
 
        IASTCompoundStatement elseBlock = this.nodeFactory.newCompoundStatement();
        elseBlock.addStatement(this.nodeFactory.newExpressionStatement(elseExpr));
 
        IASTIfStatement ifStmt = this.nodeFactory.newIfStatement(condition.copy(), thenBlock, elseBlock);
        //ifStmt.setPropertyInParent(new ASTNodeProperty("GENERATED"));
        return ifStmt;
    }
 
    private void replaceStatement(IASTStatement oldStmt, IASTStatement newStmt) {
        IASTNode parent = oldStmt.getParent();
        if (parent instanceof CASTCompoundStatement) {
            CASTCompoundStatement block = (CASTCompoundStatement) parent;
            IASTStatement[] stmts = block.getStatements();
            for (int i = 0; i < stmts.length; i++) {
                if (stmts[i] == oldStmt) {
                    block.replace(oldStmt, newStmt);
                    //System.out.println(oldStmt.getRawSignature());
                    break;
                }
            }
            newStmt.setPropertyInParent(new ASTNodeProperty("GENERATED"));
        }
    }
}
