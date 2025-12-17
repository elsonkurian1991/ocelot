package it.unisa.ocelot.c.cdg;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * Represents a pair of branch-chains from two different components.
 * Used for pairwise testing, integration analysis, and combined coverage.
 */
public class BranchChainPair {

	 private String component1;
	    private BranchChain chain1;
	    private String component2;
	    private BranchChain chain2;
	    private String pairLabel;
	    private double branchChainPairFitnessVal;  // Sum of both branchChainFitnessVals
	    public BranchChainPair(String component1, BranchChain chain1,
	                           String component2, BranchChain chain2) {
	        this.component1 = component1;
	        this.chain1 = chain1;
	        this.component2 = component2;
	        this.chain2 = chain2;
	        this.pairLabel = chain1.getLabel() + " <-> " + chain2.getLabel();
	        this.branchChainPairFitnessVal = 0.0;
	    }
	    
	    public String getComponent1() {
	        return component1;
	    }
	    
	    public BranchChain getChain1() {
	        return chain1;
	    }
	    
	    public String getComponent2() {
	        return component2;
	    }
	    
	    public BranchChain getChain2() {
	        return chain2;
	    }
	    
	    /**
	     * Returns a unique label for this pair.
	     * Format: "fun1:branch2 <-> fun3:branch1"
	     */
	    public String getPairLabel() {
	        return pairLabel;
	    }
	    
	    /**
	     * Returns the component pair key (e.g., "fun1 <-> fun3").
	     */
	    public String getComponentPairKey() {
	        return component1 + " <-> " + component2;
	    }
	    /**
	     * Gets the fitness value for this branch chain pair.
	     * This is the sum of both chain fitness values.
	     */
	    public double getBranchChainPairFitnessVal() {
	        return branchChainPairFitnessVal;
	    }
	    
	    /**
	     * Sets the fitness value for this branch chain pair.
	     */
	    public void setBranchChainPairFitnessVal(double fitnessVal) {
	        this.branchChainPairFitnessVal = fitnessVal;
	    }
	    
	    /**
	     * Calculates and updates the pair fitness value as sum of both chain fitnesses.
	     */
	    public void calculateBranchChainPairFitness() {
	        this.branchChainPairFitnessVal = chain1.getBranchChainFitnessVal() + chain2.getBranchChainFitnessVal();
	    }
	    /**
	     * Returns combined branch conditions from both chains.
	     * Useful for constraint solving across component boundaries.
	     */
	    public List<BranchCondition> getCombinedConditions() {
	        List<BranchCondition> combined = new ArrayList<>();
	        combined.addAll(chain1.getBranchConditions());
	        combined.addAll(chain2.getBranchConditions());
	        return combined;
	    }
	    
	    /**
	     * Returns all AST nodes from both execution paths.
	     */
	    public List<IASTNode> getCombinedExecutionNodes() {
	        List<IASTNode> combined = new ArrayList<>();
	        combined.addAll(chain1.getExecutionNodes());
	        combined.addAll(chain2.getExecutionNodes());
	        return combined;
	    }
	    
	    @Override
	    public String toString() {
	        StringBuilder sb = new StringBuilder();
	        List<PathStep> path1 = chain1.getPath();
	        List<PathStep> path2 = chain2.getPath();
	        sb.append("\n  Conditions (Chain 1):");
	        for (int i = 0; i < path1.size(); i++) {
	            PathStep step = path1.get(i);
	            String branchConditionsNew= step.getBranchConditionLabel();
		        sb.append(" "+branchConditionsNew+" -> ");
	        }  
	        sb.append("--->  Conditions (Chain 2):");
	        for (int i = 0; i < path2.size(); i++) {
	            PathStep step = path2.get(i);
	            String branchConditionsNew= step.getBranchConditionLabel();
		        sb.append(" "+branchConditionsNew+"-> ");
	        } 
	      //  sb.append("\n  Component Pair: ").append(getComponentPairKey()).append("\n");
	       // sb.append("  Pair Label: ").append(pairLabel).append("\n");
	        //sb.append("  Chain 1: ").append(chain1.getLabel())
	         // .append(" (leaf node ").append(chain1.getLeafNode().getId()).append(")\n");
	        //sb.append("  Chain 2: ").append(chain2.getLabel())
	        //  .append(" (leaf node ").append(chain2.getLeafNode().getId()).append(")\n");
	        
	        // Show conditions from both chains
	       /* List<BranchCondition> conditions1 = chain1.getBranchConditions();
	        List<BranchCondition> conditions2 = chain2.getBranchConditions();
	        
	        if (!conditions1.isEmpty()) {
	            sb.append("  Conditions (Chain 1):\n");
	            for (BranchCondition cond : conditions1) {
	            	sb.append(cond.getLabel()).append("\n");
	                sb.append("-").append(cond.toString()).append("\n");
	            }
	        }
	        
	        if (!conditions2.isEmpty()) {
	            sb.append("  Conditions (Chain 2):\n");
	            for (BranchCondition cond : conditions2) {
	            	sb.append(cond.getLabel()).append("\n");
	                sb.append("-").append(cond.toString()).append("\n");
	            }
	        }
	        */
	        return sb.toString();
	    }
	    
	    @Override
	    public boolean equals(Object obj) {
	        if (!(obj instanceof BranchChainPair)) return false;
	        BranchChainPair other = (BranchChainPair) obj;
	        return this.pairLabel.equals(other.pairLabel);
	    }
	    
	    @Override
	    public int hashCode() {
	        return pairLabel.hashCode();
	    }

}
