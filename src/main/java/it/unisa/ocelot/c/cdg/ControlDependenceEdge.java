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
        // Normalize boolean-like labels to "TRUE"/"FALSE" for consistency
        if (branchLabel instanceof String) {
            String s = ((String) branchLabel).trim();
            if (isTrueString(s)) return "TRUE";
            if (isFalseString(s)) return "FALSE";
            return s;
        }
        // Try to derive from original CFG edge label
        if (originalCFGEdge != null) {
            Object lbl = originalCFGEdge.getLabel();
            if (lbl instanceof String) {
                String s = ((String) lbl).trim();
                if (isTrueString(s)) return "TRUE";
                if (isFalseString(s)) return "FALSE";
                return s;
            }
            // fallback to originalCFGEdge.toString()
            String s = originalCFGEdge.toString();
            if (isTrueString(s)) return "TRUE";
            if (isFalseString(s)) return "FALSE";
            if (!s.isEmpty()) return s;
        }
        return "FLOW";
    }

    private static boolean isTrueString(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return s.equals("true") || s.equals("t") || s.equals("1") || s.equals("yes") || s.equals("y") || s.equals("ture");
    }

    private static boolean isFalseString(String s) {
        if (s == null) return false;
        s = s.trim().toLowerCase();
        return s.equals("false") || s.equals("f") || s.equals("0") || s.equals("no") || s.equals("n");
    }

    public boolean branchCondition() {
        // Return true if this edge represents the "true" branch. Safe if originalCFGEdge is null.
        // Prefer explicit branchLabel if present, otherwise fall back to originalCFGEdge label.
        if (branchLabel instanceof String) {
            return isTrueString((String) branchLabel);
        }
        if (originalCFGEdge != null) {
            Object lbl = originalCFGEdge.getLabel();
            if (lbl instanceof String) {
                return isTrueString((String) lbl);
            }
            String s = originalCFGEdge.toString();
            return isTrueString(s);
        }
        return false;
    }
}