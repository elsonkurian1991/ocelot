package it.unisa.ocelot.c.cdg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extracts branch-chains (paths from entry to leaf nodes) from a CDG.
 * Provides both AST-based representation (for fitness calculation) 
 * and text-based representation (for debugging).
 */
public class BranchChainExtractor {

	private CDG cdg;
    private List<BranchChain> branchChains;
    private String unitComponentName;  // Function name or unit component identifier
    public BranchChainExtractor(CDG cdg) {
        this.cdg = cdg;
        this.branchChains = new ArrayList<>();
        this.unitComponentName = "unknown";
    }
    
   
    public static int branchNoCounter = 0;
    public static Map<CDGNode,Integer> branchMap = new HashMap<>();
    /**
     * Constructor with unit component name for proper labeling.
     * 
     * @param cdg The Control Dependence Graph
     * @param unitComponentName The name of the function/unit being analyzed
     */
    public BranchChainExtractor(CDG cdg, String unitComponentName) {
        this.cdg = cdg;
        this.branchChains = new ArrayList<>();
        this.unitComponentName = unitComponentName;
    }
    /**
     * Extracts all branch-chains from entry to leaf nodes.
     * Uses DFS traversal to enumerate all paths.
     * Automatically assigns labels in format: "functionName:branch1", "functionName:branch2", etc.
     * @return List of BranchChain objects (AST-based representation)
     */
    public List<BranchChain> extractBranchChains() {
        branchChains.clear();
        BranchChainExtractor.branchNoCounter = 0;
        BranchChainExtractor.branchMap.clear();
        // Find all leaf nodes (nodes with no outgoing edges)
        List<CDGNode> leafNodes = new ArrayList<>();
        for (CDGNode node : cdg.vertexSet()) {
            if (node.isLeafNode(cdg)) {
                leafNodes.add(node);
            }
        }
        
        // For each leaf, find all paths from entry to that leaf
        for (CDGNode leaf : leafNodes) {
            List<PathStep> currentPath = new ArrayList<>();
            findPathsToLeaf(cdg.getEntryNode(), leaf, currentPath, new HashSet<>());
        }
        
        // 
        branchChains =  branchChains.stream().filter(BranchChainExtractor::hasBranchChainWithCondition).collect(Collectors.toList());
        branchChains =  branchChains.stream().filter(BranchChainExtractor::hasBranchChainWithEnd).collect(Collectors.toList());
     // Assign labels to all chains (1-indexed)
        for (int i = 0; i < branchChains.size(); i++) {
            branchChains.get(i).setLabel(unitComponentName, i + 1);
        }
        return branchChains;
    }
    
    private static boolean hasBranchChainWithEnd(BranchChain branchChain) {
      
    	if(branchChain.getPath().size()==1) {
    		if(branchChain.getPath().get(0).getTo().getLabel().contains("End")) {
    			return false;
    		}
    	}
    	return true;
    }
    
    private static boolean hasBranchChainWithCondition(BranchChain branchChain) {
      
    	return branchChain.getPath().stream().anyMatch(pathStep -> pathStep.hasBranchCondition());
    }
    
    /**
     * Recursive DFS to find all paths from current node to target leaf.
     * 
     * @param current Current node in traversal
     * @param target Target leaf node
     * @param currentPath Path accumulated so far
     * @param visited Nodes visited in current path (for cycle detection)
     */
    private void findPathsToLeaf(CDGNode current, CDGNode target, 
                                  List<PathStep> currentPath, Set<CDGNode> visited) {
        
        // Cycle detection
        if (visited.contains(current)) return;
        visited.add(current);
        
        // Base case: reached the target leaf String unitComponentName, int chainNumber
        if (current.equals(target)) {
            BranchChain chain = new BranchChain(target, new ArrayList<>(currentPath), unitComponentName,0);
            branchChains.add(chain);
            visited.remove(current);
            return;
        }
        
        // Recursive case: explore all successors
        Set<ControlDependenceEdge> outEdges = cdg.outgoingEdgesOf(current);
        for (ControlDependenceEdge edge : outEdges) {
            CDGNode successor = cdg.getEdgeTarget(edge);
            
            // Add this step to path
            PathStep step = new PathStep(current, successor, edge);
            if(step.hasBranchCondition()) {
            	int branchNo ;
            	if(BranchChainExtractor.branchMap.containsKey(current)) {
            		branchNo= BranchChainExtractor.branchMap.get(current);
           
            	}else {
            		branchNo = BranchChainExtractor.branchNoCounter;
            		BranchChainExtractor.branchMap.put(current, branchNo);
            		BranchChainExtractor.branchNoCounter++;
            	}
            	step.setBranchConditionLabel(unitComponentName+":branch"+branchNo+"-"+edge.branchCondition());
            	
            }
            	
            currentPath.add(step);
            
            // Recurse
            findPathsToLeaf(successor, target, currentPath, visited);
            
            // Backtrack
            currentPath.remove(currentPath.size() - 1);
        }
        
        visited.remove(current);
    }
    
    /**
     * Generates human-readable text representation of all branch-chains.
     * Format: Shows each path with branch conditions and AST nodes.
     * 
     * @return Formatted string for debugging
     */
    public String extractBranchChainsText() {
        if (branchChains.isEmpty()) {
            extractBranchChains();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== BRANCH-CHAIN ANALYSIS ===\n");
        sb.append("Total chains: ").append(branchChains.size()).append("\n\n");
        
        int chainId = 1;
        for (BranchChain chain : branchChains) {
        	if(chain.getLeafNode().getLabel().endsWith("End")) {// to skip the last end node
        		 break;
        	}
        	sb.append("Label: ").append(chain.getLabel()).append("\n");
            sb.append(chain.toTextRepresentation());
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Generates DOT format representation for Graphviz visualization.
     * 
     * @return DOT format string
     */
    public String extractBranchChainsDOT() {
        if (branchChains.isEmpty()) {
            extractBranchChains();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("digraph BranchChains {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box, style=rounded];\n\n");
        
        // Add all nodes
        Set<CDGNode> allNodes = new HashSet<>();
        for (BranchChain chain : branchChains) {
            for (PathStep step : chain.getPath()) {
                allNodes.add(step.getFrom());
                allNodes.add(step.getTo());
            }
        }
        
        for (CDGNode node : allNodes) {
            String shape = node.isLeafNode(cdg) ? "ellipse" : "box";
            sb.append("  node").append(node.getId())
              .append(" [label=\"").append(escapeForDOT(node.getLabel()))
              .append("\", shape=").append(shape).append("];\n");
        }
        
        sb.append("\n");
        
        // Add edges with labels
        for (BranchChain chain : branchChains) {
            for (PathStep step : chain.getPath()) {
                sb.append("  node").append(step.getFrom().getId())
                  .append(" -> node").append(step.getTo().getId())
                  .append(" [label=\"").append(escapeForDOT(step.getBranchLabel()))
                  .append("\"];\n");
            }
        }
        
        sb.append("}\n");
        return sb.toString();
    }
    
    private String escapeForDOT(String str) {
        return str.replace("\"", "\\\"").replace("\n", "\\n");
    }
    
    public List<BranchChain> getBranchChains() {
        return branchChains;
    }
}
