package it.unisa.ocelot.c.cdg;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;

/**
 * Represents a control dependence edge in the CDG.
 * Carries the branch condition label from the original CFG edge.
 */
public class ControlDependenceEdge {

	  private Object branchLabel;  // "TRUE", "FALSE", "case 1", etc.
	    private LabeledEdge originalCFGEdge;
	    
	    public ControlDependenceEdge(Object label, LabeledEdge cfgEdge) {
	        this.branchLabel = label;
	        this.originalCFGEdge = cfgEdge;
	    }
	    
	    public Object getBranchLabel() {
	        return branchLabel;
	    }
	    
	    public LabeledEdge getOriginalCFGEdge() {
	        return originalCFGEdge;
	    }
	    
	    @Override
	    public String toString() {
	        return branchLabel != null ? branchLabel.toString() : "FLOW";
	    }
	    
	   public boolean branchCondition() {
	        return  "true".equalsIgnoreCase(originalCFGEdge.toString()) ;
	    }
}
