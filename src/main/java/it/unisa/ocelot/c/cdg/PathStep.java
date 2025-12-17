package it.unisa.ocelot.c.cdg;
/**
 * Represents one step in a branch-chain path.
 * Contains source node, target node, and the branch condition taken.
 */
public class PathStep {

	private CDGNode from;
    private CDGNode to;
    private ControlDependenceEdge edge;
    private String branchConditionLabel;
    
    
    public String getBranchConditionLabel() {
		return branchConditionLabel;
	}

	public void setBranchConditionLabel(String branchConditionLabel) {
		this.branchConditionLabel = branchConditionLabel;
	}

	public PathStep(CDGNode from, CDGNode to, ControlDependenceEdge edge) {
        this.from = from;
        this.to = to;
        this.edge = edge;
    }
    
    public CDGNode getFrom() {
        return from;
    }
    
    public CDGNode getTo() {
        return to;
    }
    
    public String getBranchLabel() {
        return edge.toString();
    }
    
    public ControlDependenceEdge getEdge() {
        return edge;
    }
    
    public boolean hasBranchCondition() {
        return !edge.toString().equals("FLOW");
    }
    
    @Override
    public String toString() {
        return from.getId() + " --[" + getBranchLabel() + "]--> " + to.getId();
    }
}
