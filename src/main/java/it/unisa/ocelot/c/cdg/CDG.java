package it.unisa.ocelot.c.cdg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.SimpleDirectedGraph;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Control Dependence Graph (CDG) - represents control dependencies between CFG nodes.
 * 
 * Theory: Node B is control-dependent on node A if:
 * 1. A has multiple successors (it's a branch point)
 * 2. B is reachable from one successor of A but not all
 * 3. B does not post-dominate A
 * 
 * Construction Algorithm:
 * 1. Build reverse CFG
 * 2. Compute post-dominator tree
 * 3. For each branch node, determine which nodes are control-dependent on it
 */
public class CDG extends SimpleDirectedGraph<CDGNode, ControlDependenceEdge> {

	private CDGNode entryNode;
    private CFG originalCFG;
    private Map<CFGNode, CDGNode> cfgToCdgMap;
    
    /**
     * Constructs a Control Dependence Graph from a given Control Flow Graph.
     * 
     * @param cfg The input Control Flow Graph
     */
    public CDG(CFG cfg) {
        super(ControlDependenceEdge.class);
        this.originalCFG = cfg;
        this.cfgToCdgMap = new HashMap<>();
        buildCDG(cfg);
    }
    /**
     * Resets the CDGNode ID counter.
     * IMPORTANT: Call this before processing each new unit component to ensure
     * node IDs start from 0 for each function/component.
     */
    public static void resetNodeIds() {
        CDGNode.resetIdCounter();
    }
    
    /**
     * Main CDG construction algorithm.
     * Uses post-dominator analysis to determine control dependencies.
     */
    private void buildCDG(CFG cfg) {
        // Step 1: Create CDG entry node
        this.entryNode = new CDGNode(cfg.getStart(), "ENTRY");
        this.addVertex(entryNode);
        cfgToCdgMap.put(cfg.getStart(), entryNode);
        
        // Step 2: Create CDG nodes for all CFG nodes
        for (CFGNode cfgNode : cfg.vertexSet()) {
            if (cfgNode.equals(cfg.getStart())) continue;
            CDGNode cdgNode = new CDGNode(cfgNode);
            this.addVertex(cdgNode);
            cfgToCdgMap.put(cfgNode, cdgNode);
        }
        
        // Step 3: Compute post-dominators
        Map<CFGNode, Set<CFGNode>> postDominators = computePostDominators(cfg);
        
        // Step 4: Build control dependence edges
        buildControlDependenceEdges(cfg, postDominators);
    }
    
    /**
     * Computes post-dominator sets for each node in the CFG.
     * 
     * Post-dominator definition: Node B post-dominates node A if every path
     * from A to EXIT passes through B.
     * 
     * Algorithm: Iterative dataflow analysis on reverse CFG
     * 
     * @param cfg The Control Flow Graph
     * @return Map from each CFGNode to its set of post-dominators
     */
    private Map<CFGNode, Set<CFGNode>> computePostDominators(CFG cfg) {
        Map<CFGNode, Set<CFGNode>> postDom = new HashMap<>();
        Set<CFGNode> allNodes = cfg.vertexSet();
        CFGNode exit = cfg.getEnd();
        
        // Initialize: EXIT post-dominates only itself, others post-dominated by all nodes
        for (CFGNode node : allNodes) {
            if (node.equals(exit)) {
                Set<CFGNode> exitSet = new HashSet<>();
                exitSet.add(exit);
                postDom.put(node, exitSet);
            } else {
                postDom.put(node, new HashSet<>(allNodes));
            }
        }
        
        // Iterative fixed-point computation
        boolean changed = true;
        while (changed) {
            changed = false;
            
            for (CFGNode node : allNodes) {
                if (node.equals(exit)) continue;
                
                // postDom(n) = {n} ∪ (∩ postDom(s) for all successors s of n)
                Set<CFGNode> newPostDom = new HashSet<>();
                newPostDom.add(node);
                
                Set<LabeledEdge> outEdges = cfg.outgoingEdgesOf(node);
                if (!outEdges.isEmpty()) {
                    // Intersection of post-dominators of all successors
                    Set<CFGNode> intersection = null;
                    for (LabeledEdge edge : outEdges) {
                        CFGNode successor = cfg.getEdgeTarget(edge);
                        if (intersection == null) {
                            intersection = new HashSet<>(postDom.get(successor));
                        } else {
                            intersection.retainAll(postDom.get(successor));
                        }
                    }
                    if (intersection != null) {
                        newPostDom.addAll(intersection);
                    }
                }
                
                // Check if post-dominator set changed
                if (!newPostDom.equals(postDom.get(node))) {
                    postDom.put(node, newPostDom);
                    changed = true;
                }
            }
        }
        
        return postDom;
    }
    
    /**
     * Builds control dependence edges using post-dominator information.
     * 
     * Theory: Node Y is control-dependent on edge (X → Z) if:
     * 1. Y post-dominates Z
     * 2. Y does not post-dominate X
     * 
     * This means: taking edge (X → Z) makes Y's execution mandatory,
     * but X can avoid Y by taking a different branch.
     */
    private void buildControlDependenceEdges(CFG cfg, Map<CFGNode, Set<CFGNode>> postDom) {
        
        for (CFGNode node : cfg.vertexSet()) {
            Set<LabeledEdge> outEdges = cfg.outgoingEdgesOf(node);
            
            // Only branch nodes (with multiple successors) create control dependencies
            if (outEdges.size() > 1) {
                
                for (LabeledEdge edge : outEdges) {
                    CFGNode successor = cfg.getEdgeTarget(edge);
                    
                    // Find all nodes control-dependent on this edge
                    for (CFGNode candidate : cfg.vertexSet()) {
                        
                        // Check control dependence condition:
                        // candidate post-dominates successor BUT NOT node
                        boolean postDomSuccessor = postDom.get(successor).contains(candidate);
                        boolean postDomNode = postDom.get(node).contains(candidate);
                        
                        if (postDomSuccessor && !postDomNode) {
                            // Add control dependence edge: node → candidate (via this branch)
                            CDGNode controllerNode = cfgToCdgMap.get(node);
                            CDGNode dependentNode = cfgToCdgMap.get(candidate);
                            
                            if (controllerNode != null && dependentNode != null) {
                                ControlDependenceEdge cdEdge = new ControlDependenceEdge(
                                    edge.getLabel(), 
                                    edge
                                );
                                this.addEdge(controllerNode, dependentNode, cdEdge);
                            }
                        }
                    }
                }
            }
        }
        
        // Handle nodes with no control dependencies (attach to ENTRY)
        for (CDGNode cdgNode : this.vertexSet()) {
            if (cdgNode.equals(entryNode)) continue;
            
            Set<ControlDependenceEdge> inEdges = this.incomingEdgesOf(cdgNode);
            if (inEdges.isEmpty()) {
                // No controller → depends on entry
                ControlDependenceEdge entryEdge = new ControlDependenceEdge("FLOW", null);
                this.addEdge(entryNode, cdgNode, entryEdge);
            }
        }
    }
    
    public CDGNode getEntryNode() {
        return entryNode;
    }
    
    public CFG getOriginalCFG() {
        return originalCFG;
    }
    
    public CDGNode getCDGNode(CFGNode cfgNode) {
        return cfgToCdgMap.get(cfgNode);
    }
}
