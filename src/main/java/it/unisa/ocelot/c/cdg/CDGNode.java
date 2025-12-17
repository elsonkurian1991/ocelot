package it.unisa.ocelot.c.cdg;

import java.io.Serializable;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Represents a node in the Control Dependence Graph.
 * Wraps a CFGNode and maintains control dependence relationships.
 */
public class CDGNode implements Serializable {
    private static final long serialVersionUID = 1L;

	private CFGNode originalCFGNode;
    private String label;
    private int id;
    private static int idCounter = 0;
    
    public CDGNode(CFGNode cfgNode) {
        this.originalCFGNode = cfgNode;
        this.label = cfgNode.toString();
        this.id = idCounter++;
    }
    
    public CDGNode(CFGNode cfgNode, String customLabel) {
        this.originalCFGNode = cfgNode;
        this.label = customLabel;
        this.id = idCounter++;
    }
    
    public CFGNode getOriginalCFGNode() {
        return originalCFGNode;
    }
    
    public IASTNode getLeadingASTNode() {
        return originalCFGNode.getLeadingNode();
    }
    
    public List<IASTNode> getASTNodes() {
        return originalCFGNode.getNodes();
    }
    
    public int getId() {
        return id;
    }
    
    public String getLabel() {
        return label;
    }
    
    public boolean isLeafNode(CDG cdg) {
        return cdg.outgoingEdgesOf(this).isEmpty();
    }
    
    public boolean isBranchNode(CDG cdg) {
        return cdg.outgoingEdgesOf(this).size() > 1;
    }
    
    @Override
    public String toString() {
        return "CDGNode[" + id + "]: " + label;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CDGNode)) return false;
        CDGNode other = (CDGNode) obj;
        return this.id == other.id;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
    
    public static void resetIdCounter() {
        idCounter = 0;
    }
}
