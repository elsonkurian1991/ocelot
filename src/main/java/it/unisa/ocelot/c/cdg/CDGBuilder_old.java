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

public class CDGBuilder_old  {
	private CFG cfg;
	private CDG_old cdg;
	private Map<CFGNode, Set<CFGNode>> postDominators;
	private Map<CFGNode, CFGNode> immediatePostDominators;
	private CFGNode augmentedEntry;
	private CFGNode augmentedExit;
	private String unitFunName;
	private static final File outputFile = new File("cdg_output.txt");
	public CDGBuilder_old(CFG pGraph, String pFunctionName) {
		this.cfg = pGraph;
		this.cdg = new CDG_old();
		this.postDominators = new HashMap<>();
		this.immediatePostDominators = new HashMap<>();
		this.unitFunName=pFunctionName;
	}

	public CDG_old buildCDG() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		augmentCFG();
		computePostDominators();
		computeImmediatePostDominators();
		constructCDG();
		return cdg;
	}

	private void augmentCFG() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		augmentedEntry = new CFGNode("AugmentedEntry");
		augmentedExit = new CFGNode("AugmentedExit");

		cfg.addVertex(augmentedEntry);
		cfg.addVertex(augmentedExit);

		CFGNode originalEntry = cfg.getStart();
		Class<? extends LabeledEdge> edgeClass = MyLabeledEdge.class;
		LabeledEdge edge= edgeClass.getDeclaredConstructor().newInstance();
		if (originalEntry != null) {
			cfg.addEdge(augmentedEntry, originalEntry,edge);
			//LabeledEdge edge = cfg.getEdge(augmentedEntry, originalEntry);
			if (edge != null) {
				edge.setLabel("entry");
			}
		}

		for (CFGNode node : cfg.vertexSet()) {
			if (cfg.outgoingEdgesOf(node).isEmpty() && !node.equals(augmentedExit)) {
				cfg.addEdge(node, augmentedExit,edge);
				//LabeledEdge edge = cfg.getEdge(node, augmentedExit);
				if (edge != null) {
					edge.setLabel("exit");
				}
			}
		}

		CFGNode originalEnd = cfg.getEnd();
		if (originalEnd != null && !originalEnd.equals(augmentedExit)) {
			cfg.addEdge(originalEnd, augmentedExit,edge);
			//LabeledEdge edge = cfg.getEdge(originalEnd, augmentedExit);
			if (edge != null) {
				edge.setLabel("exit");
			}
		}
	}

	private Set<CFGNode> getSuccessors(CFGNode node) {
		Set<CFGNode> successors = new HashSet<>();
		for (LabeledEdge edge : cfg.outgoingEdgesOf(node)) {
			successors.add(cfg.getEdgeTarget(edge));
		}
		return successors;
	}

	private void computePostDominators() {
		Set<CFGNode> allNodes = cfg.vertexSet();
		postDominators.put(augmentedExit, new HashSet<>(Collections.singletonList(augmentedExit)));

		for (CFGNode node : allNodes) {
			if (!node.equals(augmentedExit)) {
				postDominators.put(node, new HashSet<>(allNodes));
			}
		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (CFGNode node : allNodes) {
				if (node.equals(augmentedExit)) continue;

				Set<CFGNode> newPostDom = new HashSet<>(allNodes);
				Set<CFGNode> successors = getSuccessors(node);

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
	}

	private void computeImmediatePostDominators() {
		for (CFGNode node : cfg.vertexSet()) {
			if (node.equals(augmentedExit)) continue;

			Set<CFGNode> postDoms = postDominators.get(node);
			if (postDoms == null) continue;

			Set<CFGNode> strictPostDoms = new HashSet<>(postDoms);
			strictPostDoms.remove(node);

			CFGNode iPostDom = findImmediatePostDominator(node, strictPostDoms);
			if (iPostDom != null) {
				immediatePostDominators.put(node, iPostDom);
			}
		}
	}

	private CFGNode findImmediatePostDominator(CFGNode node, Set<CFGNode> strictPostDoms) {
		for (CFGNode candidate : strictPostDoms) {
			boolean isImmediate = true;
			for (CFGNode other : strictPostDoms) {
				if (candidate.equals(other)) continue;
				Set<CFGNode> candidatePostDoms = postDominators.get(other);
				if (candidatePostDoms != null && !candidatePostDoms.contains(candidate)) {
					isImmediate = false;
					break;
				}
			}
			if (isImmediate) return candidate;
		}
		return null;
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
		if (cfgEdge != null && cfgEdge.getLabel() != null && !((Map<CFGNode, Set<CFGNode>>) cfgEdge.getLabel()).isEmpty()) {
			return cfgEdge.getLabel().toString();
		}

		Set<CFGNode> successors = getSuccessors(source);
		if (successors.size() > 1) {
			String sourceStr = source.toString().toLowerCase();
			if (sourceStr.contains("if") || sourceStr.contains("while") || sourceStr.contains("for")) {
				List<CFGNode> succList = new ArrayList<>(successors);
				return succList.indexOf(target) == 0 ? "true" : "false";
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
				writer.write("Node: " + node);
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