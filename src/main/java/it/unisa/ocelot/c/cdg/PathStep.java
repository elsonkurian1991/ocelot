package it.unisa.ocelot.c.cdg;
/**
 * Represents one step in a branch-chain path.
 * Contains source node, target node, and the branch condition taken.
 */
public class PathStep {

    private CDGNode from;
    private CDGNode to;
    private ControlDependenceEdge edge;
    private String branchConditionLabel; // human-readable label assigned by extractor (e.g. unit:branch0-true)

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

    /**
     * Returns the branch label to display. Prefer an explicit branchConditionLabel if set
     * (this is assigned by the BranchChainExtractor and includes numbering), otherwise fall
     * back to the underlying ControlDependenceEdge label.
     */
    public String getBranchLabel() {
        if (branchConditionLabel != null && !branchConditionLabel.isEmpty()) return branchConditionLabel;
        if (edge == null) return "FLOW";
        return edge.toString();
    }

    public ControlDependenceEdge getEdge() {
        return edge;
    }

    /**
     * A step has a branch condition if the edge encodes a conditional (not a FLOW edge)
     * or if an explicit branchConditionLabel was attached by the extractor.
     */
    public boolean hasBranchCondition() {
        if (branchConditionLabel != null && !branchConditionLabel.isEmpty()) return true;
        if (edge == null) return false;
        return !edge.toString().equals("FLOW");
    }

    @Override
    public String toString() {
        return from.getId() + " --[" + getBranchLabel() + "]--> " + to.getId();
    }
}