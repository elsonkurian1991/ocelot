package it.unisa.ocelot.c.cdg;

import java.util.Objects;

import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Represents a Control Dependence Graph node.
 * Similar to CFGNode but specifically for CDG representation.
 */
public class CDGNode_old {
    private CFGNode cfgNode;
    private String label;

    public CDGNode_old(CFGNode cfgNode) {
        this.cfgNode = cfgNode;
        this.label = cfgNode.toString();
    }

    public CDGNode_old(String label) {
        this.label = label;
        this.cfgNode = null;
    }

    public CFGNode getCfgNode() {
        return cfgNode;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CDGNode_old)) return false;
        CDGNode_old other = (CDGNode_old) obj;
        if (cfgNode != null && other.cfgNode != null) {
            return cfgNode.equals(other.cfgNode);
        }
        return label.equals(other.label);
    }

    @Override
    public int hashCode() {
        return cfgNode != null ? cfgNode.hashCode() : label.hashCode();
    }
}