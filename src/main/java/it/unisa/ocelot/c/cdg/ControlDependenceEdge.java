package it.unisa.ocelot.c.cdg;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Represents a control dependence edge in the CDG.
 * Carries the branch condition label from the original CFG edge.
 */
public class ControlDependenceEdge {

    private Object branchLabel;  // "TRUE", "FALSE", "case 1", etc.

	// ControlDependenceEdge.java
	public ControlDependenceEdge(Object branchLabel) {
	    this.branchLabel = branchLabel;
	}

    public Object getBranchLabel() {
        return branchLabel;
    }

    @Override
    public String toString() {
        return branchLabel != null ? branchLabel.toString() : "FLOW";
    }

    private static boolean isTrueString(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return s.equals("true") || s.equals("t") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("ture");
    }

    public boolean branchCondition() {
        // Return true if this edge represents the "true" branch. Safe if originalCFGEdge is null.
        // Prefer explicit branchLabel if present, otherwise fall back to originalCFGEdge label.
        if (branchLabel instanceof String) {
            return isTrueString((String) branchLabel);
        }
     
        return false;
    }
}