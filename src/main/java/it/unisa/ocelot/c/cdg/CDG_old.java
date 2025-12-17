package it.unisa.ocelot.c.cdg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.ListenableDirectedGraph;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Represents a Control Dependence Graph.
 * The CDG shows which statements control the execution of other statements.
 */
import org.jgrapht.graph.ListenableDirectedGraph;

public class CDG_old extends ListenableDirectedGraph<CDGNode_old, CDGEdge> {
    private static final long serialVersionUID = 1L;
    private CDGNode_old start;

    public CDG_old() {
        super(CDGEdge.class);
    }

    public void setStart(CDGNode_old node) {
        this.start = node;
    }

    public CDGNode_old getStart() {
        return start;
    }
}
/*public class CDG extends ListenableDirectedGraph<CFGNode, CDGEdge> {
    private static final long serialVersionUID = 1L;
    private CDGNode start;
    
    public CDG() {
        super(CDGEdge.class);
    }
    
    public void setStart(CDGNode node) {
        this.start = node;
    }
    
    public CDGNode getStart() {
        return start;
    }
}*/
	/*private Set<CDGNode> nodes;
    private Map<CDGNode, Set<CDGNode>> edges;
    private Map<String, String> edgeLabels; // Maps edge (source+target) to label
    private CDGNode entryNode;
    
    public CDG() {
        this.nodes = new HashSet<>();
        this.edges = new HashMap<>();
        this.edgeLabels = new HashMap<>();
    }
    
    public void addNode(CDGNode node) {
        nodes.add(node);
        edges.putIfAbsent(node, new HashSet<>());
    }
    
    public void addEdge(CDGNode source, CDGNode target, String label) {
        if (!nodes.contains(source)) {
            addNode(source);
        }
        if (!nodes.contains(target)) {
            addNode(target);
        }
        
        edges.get(source).add(target);
        
        if (label != null && !label.isEmpty()) {
            String edgeKey = source.getLabel() + "->" + target.getLabel();
            edgeLabels.put(edgeKey, label);
        }
    }
    
    public Set<CDGNode> getNodes() {
        return nodes;
    }
    
    public Set<CDGNode> getSuccessors(CDGNode node) {
        return edges.getOrDefault(node, new HashSet<>());
    }
    
    public Set<CDGNode> getPredecessors(CDGNode node) {
        Set<CDGNode> predecessors = new HashSet<>();
        for (Map.Entry<CDGNode, Set<CDGNode>> entry : edges.entrySet()) {
            if (entry.getValue().contains(node)) {
                predecessors.add(entry.getKey());
            }
        }
        return predecessors;
    }
    
    public String getEdgeLabel(CDGNode source, CDGNode target) {
        String edgeKey = source.getLabel() + "->" + target.getLabel();
        return edgeLabels.getOrDefault(edgeKey, "");
    }
    
    public void setEntryNode(CDGNode node) {
        this.entryNode = node;
        addNode(node);
    }
    
    public CDGNode getEntryNode() {
        return entryNode;
    }*/

