package it.unisa.ocelot.c.cdg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import org.eclipse.cdt.core.dom.ast.*;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.ListenableDirectedGraph;
/**
 * Builder class that constructs a Control Dependence Graph (CDG) from an existing
 * Control Flow Graph (CFG). The CDG represents control dependencies between nodes,
 * where node A is control-dependent on node B if B determines whether A executes.
 * 
 * Algorithm:
 * 1. Augment CFG with entry and exit nodes
 * 2. Compute post-dominance tree
 * 3. For each edge in CFG, determine control dependencies
 * 4. Build CDG based on control dependencies
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CDGBuilder2  {
	private CFG cfg;
	private CDG_old cdg;
	private Map<CFGNode, Set<CFGNode>> postDominators;
	private Map<CFGNode, CFGNode> immediatePostDominators;
	private CFGNode augmentedEntry;
	private CFGNode augmentedExit;
	private String unitFunName;
	private static final File outputFile = new File("cdg_output.txt");
	public CDGBuilder2(CFG pGraph, String pFunctionName) {
		this.cfg = pGraph;
		this.cdg = new CDG_old();
		this.postDominators = new HashMap<>();
		this.immediatePostDominators = new HashMap<>();
		this.unitFunName=pFunctionName;
	}

	public CDG_old buildCDG() throws IllegalArgumentException, SecurityException, ReflectiveOperationException {
		augmentCFG();
		computePostDominators();
		computeImmediatePostDominators();
		constructCDG();
		return cdg;
	}

	private void augmentCFG() throws InstantiationException, IllegalAccessException,
	IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {

		augmentedEntry = new CFGNode("AugmentedEntry");
		augmentedExit  = new CFGNode("AugmentedExit");

		cfg.addVertex(augmentedEntry);
		cfg.addVertex(augmentedExit);

		Class<? extends LabeledEdge> edgeClass = MyLabeledEdge.class;

		// Connect entry to original start
		CFGNode originalEntry = cfg.getStart();
		if (originalEntry != null) {
			LabeledEdge entryEdge = edgeClass.getDeclaredConstructor().newInstance();
			cfg.addEdge(augmentedEntry, originalEntry, entryEdge);
			entryEdge.setLabel("entry");
		}

		// Connect only true CFG exit nodes to augmentedExit
		for (CFGNode node : new HashSet<>(cfg.vertexSet())) {
			if (!node.equals(augmentedEntry) && !node.equals(augmentedExit)
					&& cfg.outgoingEdgesOf(node).isEmpty()) {
				LabeledEdge exitEdge = edgeClass.getDeclaredConstructor().newInstance();
				cfg.addEdge(node, augmentedExit, exitEdge);
				exitEdge.setLabel("exit");
			}
		}
	}




	private void computePostDominators() {
	    // Use only original nodes (exclude augmentedEntry and augmentedExit)
	    Set<CFGNode> allNodes = new HashSet<>(cfg.vertexSet());
	    allNodes.remove(augmentedEntry);
	    allNodes.remove(augmentedExit);

	    CFGNode originalExit = cfg.getEnd();  // the actual function exit node

	    // Initialize post-dominators
	    postDominators.put(originalExit, new HashSet<>(Collections.singletonList(originalExit)));
	    for (CFGNode node : allNodes) {
	        if (!node.equals(originalExit)) {
	            postDominators.put(node, new HashSet<>(allNodes));
	        }
	    }

	    boolean changed = true;
	    while (changed) {
	        changed = false;
	        for (CFGNode node : allNodes) {
	            if (node.equals(originalExit)) continue;

	            Set<CFGNode> newPostDom = new HashSet<>(allNodes);
	            Set<CFGNode> successors = getOriginalSuccessors(node); // only real successors

	            if (!successors.isEmpty()) {
	                boolean first = true;
	                for (CFGNode succ : successors) {
	                    if (first) {
	                        newPostDom = new HashSet<>(postDominators.get(succ));
	                        first = false;
	                    } else {
	                        newPostDom.retainAll(postDominators.get(succ));
	                    }
	                }
	            }

	            newPostDom.add(node);

	            if (!newPostDom.equals(postDominators.get(node))) {
	                postDominators.put(node, newPostDom);
	                changed = true;
	            }
	        }
	    }

	    // Set augmentedExit as post-dominator of original exit
	    postDominators.put(augmentedExit, new HashSet<>(Collections.singletonList(augmentedExit)));
	    postDominators.get(originalExit).add(augmentedExit);
	}

	private Set<CFGNode> getOriginalSuccessors(CFGNode node) {
	    Set<CFGNode> successors = new HashSet<>();
	    for (LabeledEdge edge : cfg.outgoingEdgesOf(node)) {
	        CFGNode succ = cfg.getEdgeTarget(edge);
	        if (!succ.equals(augmentedEntry)) { // ignore synthetic start
	            successors.add(succ);
	        }
	    }
	    return successors;
	}

	// Updated getSuccessors() to ignore augmentedEntry
	private Set<CFGNode> getSuccessors(CFGNode node) {
		Set<CFGNode> successors = new HashSet<>();
		for (LabeledEdge edge : cfg.outgoingEdgesOf(node)) {
			CFGNode succ = cfg.getEdgeTarget(edge);
			if (!succ.equals(augmentedEntry)) { // ignore synthetic entry
				successors.add(succ);
			}
		}
		return successors;
	}
	/*private Set<CFGNode> getSuccessors(CFGNode node) {
		Set<CFGNode> successors = new HashSet<>();
		for (LabeledEdge edge : cfg.outgoingEdgesOf(node)) {
			successors.add(cfg.getEdgeTarget(edge));
		}
		return successors;
	}*/

	private void computeImmediatePostDominators() {
		// For each node, among its strict post-dominators choose the one that is 'closest'
		// We use the smallest postDominators set (heuristic that corresponds to being closest to node).
		for (CFGNode node : new HashSet<>(cfg.vertexSet())) {
			if (node.equals(augmentedExit)) continue;
			Set<CFGNode> postDoms = postDominators.get(node);
			if (postDoms == null) continue;

			Set<CFGNode> strictPostDoms = new HashSet<>(postDoms);
			strictPostDoms.remove(node);

			if (strictPostDoms.isEmpty()) continue;

			CFGNode best = null;
			int bestSize = Integer.MAX_VALUE;
			for (CFGNode candidate : strictPostDoms) {
				Set<CFGNode> candSet = postDominators.get(candidate);
				if (candSet == null) continue;
				int size = candSet.size();
				if (size < bestSize) {
					bestSize = size;
					best = candidate;
				}
			}

			if (best != null) {
				immediatePostDominators.put(node, best);
			}
		}
	}


	private void constructCDG() {
		Map<CFGNode, CDGNode_old> cfgToCdgMap = new HashMap<>();

		for (CFGNode cfgNode : cfg.vertexSet()) {
			if (!cfgNode.equals(augmentedEntry) && !cfgNode.equals(augmentedExit)) {
				CDGNode_old cdgNode = new CDGNode_old(cfgNode);
				cdg.addVertex(cdgNode);
				cfgToCdgMap.put(cfgNode, cdgNode);
			}
		}

		CDGNode_old cdgEntry = new CDGNode_old("CDG Entry");
		cdg.setStart(cdgEntry);
		cdg.addVertex(cdgEntry);

		for (CFGNode cfgNode : cfg.vertexSet()) {
			Set<CFGNode> successors = getSuccessors(cfgNode);

			if (successors.size() >= 1) {
				for (CFGNode successor : successors) {
					Set<CFGNode> dependentNodes = findControlDependentNodes(cfgNode, successor);
					CDGNode_old sourceCdgNode = cfgToCdgMap.get(cfgNode);

					if (sourceCdgNode == null || cfgNode.equals(cfg.getStart()) || cfgNode.equals(augmentedEntry)) {
						sourceCdgNode = cdgEntry;
					}

					for (CFGNode dependent : dependentNodes) {
						CDGNode_old targetCdgNode = cfgToCdgMap.get(dependent);
						if (sourceCdgNode != null && targetCdgNode != null && !sourceCdgNode.equals(targetCdgNode)) {
							String edgeLabel = determineEdgeLabel(cfgNode, successor, dependent);
							CDGEdge edge = new CDGEdge(edgeLabel);
							cdg.addEdge(sourceCdgNode, targetCdgNode, edge);
						}
					}
				}
			}
		}

		for (CFGNode cfgNode : cfg.vertexSet()) {
			CDGNode_old cdgNode = cfgToCdgMap.get(cfgNode);
			if (cdgNode != null && cdg.incomingEdgesOf(cdgNode).isEmpty()) {
				CDGEdge edge = new CDGEdge("");
				cdg.addEdge(cdgEntry, cdgNode, edge);
			}
		}
	}

	private Set<CFGNode> findControlDependentNodes(CFGNode source, CFGNode target) {
		Set<CFGNode> dependentNodes = new HashSet<>();
		Set<CFGNode> sourcePostDoms = postDominators.get(source);
		if (sourcePostDoms == null || sourcePostDoms.contains(target)) return dependentNodes;

		Set<CFGNode> visited = new HashSet<>();
		Queue<CFGNode> queue = new LinkedList<>();
		queue.add(target);
		visited.add(target);

		while (!queue.isEmpty()) {
			CFGNode current = queue.poll();
			if (sourcePostDoms.contains(current) && !current.equals(source)) continue;

			if (!current.equals(augmentedEntry) && !current.equals(augmentedExit)) {
				dependentNodes.add(current);
			}

			for (CFGNode succ : getSuccessors(current)) {
				if (!visited.contains(succ)) {
					visited.add(succ);
					queue.add(succ);
				}
			}
		}

		return dependentNodes;
	}

	private String determineEdgeLabel(CFGNode source, CFGNode target, CFGNode dependent) {
		LabeledEdge cfgEdge = cfg.getEdge(source, target);
		if (cfgEdge != null) {
			Object labelObj = cfgEdge.getLabel();
			if (labelObj instanceof String) {
				String s = ((String) labelObj).trim();
				if (!s.isEmpty()) return s;
			} else if (labelObj instanceof Map<?, ?>) {
				@SuppressWarnings("rawtypes")
				Map map = (Map) labelObj;
				if (!map.isEmpty()) return map.toString();
			}
		}

		Set<CFGNode> successors = getSuccessors(source);
		if (successors.size() > 1) {
			String src = source.toString().toLowerCase();
			List<CFGNode> succList = new ArrayList<>(successors);
			// For if/loop heuristics: first successor -> "true", others -> "false"
			if (src.contains("if") || src.contains("while") || src.contains("for")) {
				return (succList.indexOf(target) == 0) ? "true" : "false";
			} else if (source.isSwitch()) {
				return target.isCase() ? "case" : "default";
			}
		}

		return "";
	}


	public Map<CFGNode, Set<CFGNode>> getPostDominators() {
		return postDominators;
	}

	public Map<CFGNode, CFGNode> getImmediatePostDominators() {
		return immediatePostDominators;
	}

	public void printCDG() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("\n\n=========================Control Dependence Graph: "+unitFunName+"=========================\n");
			for (CDGNode_old node : cdg.vertexSet()) {
				writer.write("\nNode: " + node);
				System.out.println("\nNode: " + node);
				Set<CDGEdge> outEdges = cdg.outgoingEdgesOf(node);
				if (!outEdges.isEmpty()) {
					writer.write("\n  Controls:");
					System.out.println("  Controls:");
					for (CDGEdge edge : outEdges) {
						CDGNode_old target = cdg.getEdgeTarget(edge);
						String label = edge.getLabel();
						writer.write("\n    -> " + target + (label.isEmpty() ? "" : " [" + label + "]"));
						System.out.println("    -> " + target + (label.isEmpty() ? "" : " [" + label + "]"));
					}
				}
			}
		}catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
		printPostDominators();
	}

	public void printPostDominators() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile,true))) {
			writer.write("\n\n=========================Post-Dominators: "+unitFunName+"=========================");
			for (Map.Entry<CFGNode, Set<CFGNode>> entry : postDominators.entrySet()) {
				System.out.println();
				writer.write("\nNode " + entry.getKey().getId() + ":");
				writer.write("  Post-dominated by: " +
						entry.getValue().stream()
						.map(n -> String.valueOf(n.getId()))
						.reduce((a, b) -> a + ", " + b)
						.orElse("none"));
				System.out.println();
			}

			writer.write("\n\n=========================Immediate Post-Dominators: "+unitFunName+"=========================\n");
			for (Map.Entry<CFGNode, CFGNode> entry : immediatePostDominators.entrySet()) {
				writer.write("\nNode " + entry.getKey().getId() +
						" -> iPostDom: " + entry.getValue().getId());
			}
			writer.write("\n==============================================================================");
			writer.write("\n******************************************************************************");
			writer.write("\n==============================================================================");
		}catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}

	}

	public CDG_old getCDG() {
		return cdg;
	}
}