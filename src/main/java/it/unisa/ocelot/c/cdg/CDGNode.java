package it.unisa.ocelot.c.cdg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import it.unisa.ocelot.c.cfg.nodes.CFGNode;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Represents a node in the Control Dependence Graph.
 * Wraps a CFGNode and maintains control dependence relationships.
 */
public class CDGNode implements Serializable {
    private static final long serialVersionUID = 1L;

	private CFGNode originalCFGNode;
	/** True-branch successor id (-1 if none). Used for conditional nodes. */
    public int trueSuccessor;

    /** False-branch successor id (-1 if none). Used for conditional nodes. */
    public int falseSuccessor;

    /** All successor node ids (outgoing edges). */
    public final List<Integer> successors;

    /** All predecessor node ids (incoming edges). */
    public final List<Integer> predecessors;

    /** Human-readable label (e.g. the statement text). */
    public String label;
    /** Unique identifier for this node. */
    public final int id;
    private static int idCounter = 0;
    /** Whether this node is a conditional branching node. */
    public boolean isCondition;
    
    public CDGNode(CFGNode cfgNode) {
        this.originalCFGNode = cfgNode;
        this.label = cfgNode.toString();
        this.id = idCounter++;
        this.trueSuccessor = -1;
        this.falseSuccessor = -1;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.isCondition = false;
    }
    
    public CDGNode(CFGNode cfgNode, String customLabel) {
        this.originalCFGNode = cfgNode;
        this.trueSuccessor = -1;
        this.falseSuccessor = -1;
        this.successors = new ArrayList<>();
        this.predecessors = new ArrayList<>();
        this.isCondition = false;
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
        return cdg.incomingEdgesOf(this).isEmpty();
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
