package it.unisa.ocelot.c.cdg;

import java.util.Objects;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * A labeled control-dependency edge: the dependent node is control-dependent
 * on a condition node with a specific branch label ("T", "F", or a case label).
 */
public class CDGEdge {

    /** Id of the condition (branch) node. */
    public final int conditionNodeId;
    /** Branch label: "T", "F", or case label string. */
    public final String label;

    public CDGEdge() {
        this.conditionNodeId = 0;
        this.label = "";
    }

    public CDGEdge(int conditionNodeId, String label) {
        this.conditionNodeId = conditionNodeId;
        this.label = label != null ? label : "";
    }

    public CDGEdge(String label) {
        this.conditionNodeId = 0;
        this.label = label != null ? label : "";
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label.isEmpty() ? "" : "[" + label + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CDGEdge)) return false;
        CDGEdge other = (CDGEdge) o;
        return conditionNodeId == other.conditionNodeId && label.equals(other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionNodeId, label);
    }
}
