package it.unisa.ocelot.genetic.objectives.chains;

import java.util.HashMap;
import java.util.List;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
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
	    public BranchChainPair(String component1, BranchChain chain1,
	                           String component2, BranchChain chain2) {
	        this.component1 = component1;
	        this.chain1 = chain1;
	        this.component2 = component2;
	        this.chain2 = chain2;
	        this.pairLabel = chain1.getLabel() + " <-> " + chain2.getLabel();
	    }
	    
	    public String getComponent1() {
	        return component1;
	    }
	    
	    public BranchChain getBranchChainOne() {
	        return chain1;
	    }
	    
	    public String getComponent2() {
	        return component2;
	    }
	    
	    public BranchChain getBranchChainTwo() {
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
	     * Calculates and returns the pair fitness value as sum of both chain fitnesses.
	     */
	    public double calculateBranchChainPairFitness(HashMap<String, Object> fitnessMap) {
	    	double fitnessChain1 = chain1.calculateBranchChainFitness(fitnessMap);
	    	double fitnessChain2 = chain2.calculateBranchChainFitness(fitnessMap);
	        double fitness = (fitnessChain1 + fitnessChain2) / 2;
			if (fitness == Double.POSITIVE_INFINITY) {
				fitness = Double.MAX_VALUE;
			}
			return fitness;
	    }
	    	    
	    @Override
	    public String toString() {
	        StringBuilder sb = new StringBuilder();
	        List<PathStep> path1 = chain1.getPath();
	        List<PathStep> path2 = chain2.getPath();
	        sb.append("\n");
	        for (int i = 0; i < path1.size(); i++) {
	            PathStep step = path1.get(i);
	            String branchConditionsNew= step.getBranchConditionLabel();
	            if (branchConditionsNew != null && !branchConditionsNew.isEmpty()) {
	            	sb.append(" "+branchConditionsNew+" -> ");
	            }
		        
	        }  
	        sb.append("=====>");
	        for (int i = 0; i < path2.size(); i++) {
	            PathStep step = path2.get(i);
	            String branchConditionsNew= step.getBranchConditionLabel();
	            if (branchConditionsNew != null && !branchConditionsNew.isEmpty()) {
	            	sb.append(" "+branchConditionsNew+" -> ");
	            }
	        } 
	     
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
