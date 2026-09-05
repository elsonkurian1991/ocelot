package it.unisa.ocelot.genetic.objectives.branch_chains;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

/**
 * Represents a branch condition (predicate) in the execution path.
 * Links the AST node (the condition) with the branch taken (true/false/case).
 */
public class BranchCondition {

	private IASTNode conditionNode;
    private Object branchTaken;
    private String label;  // Format: "unitname:branch1-true" or "unitname:branch2-false"
    private double fitVal; // Fitness value for this specific condition
    public BranchCondition(IASTNode condition, Object branch) {
        this.conditionNode = condition;
        this.branchTaken = branch;
        this.label = "";
        this.fitVal = 1.0;
    }
    public BranchCondition(IASTNode condition, Object branch, String label) {
        this.conditionNode = condition;
        this.branchTaken = branch;
        this.label = label;
        this.fitVal = 1.0;
    }
    public IASTNode getConditionNode() {
        return conditionNode;
    }
    
    public Object getBranchTaken() {
        return branchTaken;
    }
    /**
     * Gets the label for this condition.
     * Format: "unitname:branch1-true" or "unitname:branch2-false"
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Sets the label for this condition.
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * Gets the fitness value for this condition.
     */
    public double getFitVal() {
        return fitVal;
    }
    
    /**
     * Sets the fitness value for this condition.
     */
    public void setFitVal(double fitVal) {
        this.fitVal = fitVal;
    }
    /**
     * Extracts the condition expression as a string.
     * For if/while: returns the boolean expression
     * For switch: returns the switch expression
     */
    public String getConditionExpression() {
        if (conditionNode instanceof IASTIfStatement) {
            IASTIfStatement ifStmt = (IASTIfStatement) conditionNode;
            return ifStmt.getConditionExpression().getRawSignature();
        } else if (conditionNode instanceof IASTWhileStatement) {
            IASTWhileStatement whileStmt = (IASTWhileStatement) conditionNode;
            return whileStmt.getCondition().getRawSignature();
        } else if (conditionNode instanceof IASTForStatement) {
            IASTForStatement forStmt = (IASTForStatement) conditionNode;
            return forStmt.getConditionExpression().getRawSignature();
        } else if (conditionNode instanceof IASTSwitchStatement) {
            IASTSwitchStatement switchStmt = (IASTSwitchStatement) conditionNode;
            return switchStmt.getControllerExpression().getRawSignature();
        }
        return conditionNode.getRawSignature();
    }
    
    @Override
    public String toString() {
        return "(" + getConditionExpression() + ") = " + branchTaken;
    }
}
