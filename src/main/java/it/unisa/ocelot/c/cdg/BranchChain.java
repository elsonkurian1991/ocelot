package it.unisa.ocelot.c.cdg;
import java.io.Serializable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;

/**
 * Represents a single branch-chain: a path from entry to a specific leaf node.
 * Contains both the path structure and the target leaf.
 * 
 * This is the AST-based representation suitable for fitness calculation.
 */
public class BranchChain {

	 private CDGNode leafNode;
	    private List<PathStep> path;
	    private String label;  // Format: "functionName:branch1"
	    private int chainNumber;
	    private double branchChainFitnessVal;
	    private String unitComponentName;
	    public BranchChain(CDGNode leaf, List<PathStep> path) {
	        this.leafNode = leaf;
	        this.path = path;
	        this.label = "";
	        this.chainNumber = -1;
	        this.branchChainFitnessVal=1.0;
	    }
	    public BranchChain(CDGNode leaf, List<PathStep> path, String unitComponentName, int chainNumber) {
	        this.leafNode = leaf;
	        this.path = path;
	        this.chainNumber = chainNumber;
	        this.unitComponentName=unitComponentName;
	        this.label = unitComponentName + ":branch" + chainNumber;
	        this.branchChainFitnessVal=1.0;
	    }
	    public CDGNode getLeafNode() {
	        return leafNode;
	    }
	    
	    public List<PathStep> getPath() {
	        return path;
	    }
	    /**
	     * Returns the unique label for this chain.
	     * Format: "functionName:branch1"
	     */
	    public String getLabel() {
	        return label;
	    }
	    /**
	     * Sets the label for this chain.
	     * @param unitComponentName The function/unit component name
	     * @param chainNumber The chain number (1-indexed)
	     */
	    public void setLabel(String unitComponentName, int chainNumber) {
	        this.chainNumber = chainNumber;
	        this.label = unitComponentName + ":branchChain" + chainNumber;
	        this.unitComponentName = unitComponentName; // ensure stored for later use
	    }
	    /**
	     * Gets the fitness value for this branch chain.
	     * This is the sum of all fitVal's from branch conditions.
	     */
	    public double getBranchChainFitnessVal() {
	        return branchChainFitnessVal;
	    }
	    
	    /**
	     * Sets the fitness value for this branch chain.
	     */
	    public void setBranchChainFitnessVal(double fitnessVal) {
	        this.branchChainFitnessVal = fitnessVal;
	    }
	    
	    /**
	     * Calculates and updates the fitness value as sum of all condition fitVals.
	     */
	    public void calculateBranchChainFitness() {
	        double sum = 0.0;
	        List<BranchCondition> conditions = getBranchConditions();
	        for (BranchCondition condition : conditions) {
	            sum += condition.getFitVal();
	        }
	        this.branchChainFitnessVal = sum;
	    }
	    
	    /**
	     * Returns the chain number within its unit component.
	     */
	    public int getChainNumber() {
	        return chainNumber;
	    }
	    
	    /**
	     * Returns all branch conditions (predicates) along this path.
	     * Useful for constraint solving and test generation.
	     * Conditions are labeled as "unitname:branch<num>-true/false".
	     *
	     * NOTE: prefer to reuse the precise label assigned by the extractor (PathStep.branchConditionLabel)
	     * if present. This ensures consistent numbering and naming.
	     */
	    public List<BranchCondition> getBranchConditions() {
	        List<BranchCondition> conditions = new ArrayList<>();

	        for (PathStep step : path) {
	            if (step.hasBranchCondition()) {
	                String assignedLabel = step.getBranchConditionLabel();
	                String branchLabel = step.getBranchLabel();

	                String conditionLabel = assignedLabel;
	                if (conditionLabel == null || conditionLabel.isEmpty()) {
	                    // Fallback: construct a label using available unitComponentName and the branchLabel
	                    String unit = (this.unitComponentName != null) ? this.unitComponentName : "unit";
	                    conditionLabel = unit + ":" + branchLabel;
	                }

	                BranchCondition condition = new BranchCondition(
	                    // Use the AST condition node from the source CDGNode if available
	                    step.getFrom().getLeadingASTNode(),
	                    branchLabel,
	                    conditionLabel
	                );
	                conditions.add(condition);
	            }
	        }

	        return conditions;
	    }
	    
	    /**
	     * Returns all branch conditions (predicates) along this path.
	     * Useful for constraint solving and test generation.
	     */
	    /*old public List<BranchCondition> getBranchConditions() {
	        List<BranchCondition> conditions = new ArrayList<>();
	        
	        for (PathStep step : path) {
	            if (step.hasBranchCondition()) {
	                conditions.add(new BranchCondition(
	                    step.getFrom().getLeadingASTNode(),
	                    step.getBranchLabel()
	                ));
	            }
	        }
	        
	        return conditions;
	    }
	    */
	    /**
	     * Returns all AST nodes along this execution path.
	     * Useful for code coverage and instrumentation.
	     */
	    public List<IASTNode> getExecutionNodes() {
	        List<IASTNode> nodes = new ArrayList<>();
	        
	        for (PathStep step : path) {
	            nodes.addAll(step.getFrom().getASTNodes());
	        }
	        nodes.addAll(leafNode.getASTNodes());
	        
	        return nodes;
	    }
	    
	    /**
	     * Converts this branch-chain to human-readable text.
	     */
	    public String toTextRepresentation() {
	        StringBuilder sb = new StringBuilder();
	        
	        // Show path with branch decisions
	        sb.append("  Path: ");
	        for (int i = 0; i < path.size(); i++) {
	            PathStep step = path.get(i);
	            sb.append("Node[").append(step.getFrom().getId()).append("]");
	            if (!step.getBranchLabel().equals("FLOW")) {
	                sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
	            } else {
	                sb.append(" --> ");
	            }
	            // new branch conditions printing
		        //String branchConditionsNew= step.getBranchConditionLabel();
		        //sb.append("\n  branchConditions: "+branchConditionsNew+"\n");
	        }
	        sb.append("Node[").append(leafNode.getId()).append("] (LEAF)\n");
	        
	        // Show branch conditions
	       /* List<BranchCondition> conditions = getBranchConditions();
	        if (!conditions.isEmpty()) {
	            sb.append("  Conditions:\n");
	            for (BranchCondition cond : conditions) {
	            	sb.append(cond.getLabel()).append("\n");
	                sb.append("-").append(cond.toString()).append("\n");
	            }
	        }
	       */
	        // Show AST content of leaf
	        sb.append("  Leaf content: ").append(leafNode.getLabel()).append("\n");
	        
	        return sb.toString();
	    }
	    
	    @Override
	    public String toString() {
	        return "BranchChain[leaf=" + leafNode.getId() + ", length=" + path.size() + "]";
	    }
		

}
