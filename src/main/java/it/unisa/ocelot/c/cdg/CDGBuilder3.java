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
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CDGBuilder3 {
	private CFG cfg;
	private CDG_old cdg;
	private Map<CFGNode, Set<CFGNode>> postDominators;
	private Map<CFGNode, CFGNode> immediatePostDominators;
	private CFGNode augmentedEntry;
	private CFGNode augmentedExit;
	private String unitFunName;
	private static final File outputFile = new File("cdg_output.txt");

	public CDGBuilder3(CFG pGraph, String pFunctionName) {
		this.cfg = pGraph;
		this.cdg = new CDG_old();
		this.postDominators = new HashMap<>();
		this.immediatePostDominators = new HashMap<>();
		this.unitFunName = pFunctionName;
	}

	public CDG_old buildCDG() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
	InvocationTargetException, NoSuchMethodException, SecurityException {
		augmentCFG();
		computePostDominators();
		constructCDG();
		return cdg;
	}

	private void augmentCFG() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
	InvocationTargetException, NoSuchMethodException, SecurityException {
		augmentedEntry = new CFGNode("AugmentedEntry");
		augmentedExit = new CFGNode("AugmentedExit");

		cfg.addVertex(augmentedEntry);
		cfg.addVertex(augmentedExit);

		Class<? extends LabeledEdge> edgeClass = MyLabeledEdge.class;

		CFGNode originalEntry = cfg.getStart();
		if (originalEntry != null) {
			LabeledEdge entryEdge = edgeClass.getDeclaredConstructor().newInstance();
			cfg.addEdge(augmentedEntry, originalEntry, entryEdge);
			entryEdge.setLabel("entry");
		}

		for (CFGNode node : cfg.vertexSet()) {
			if (cfg.outgoingEdgesOf(node).isEmpty() && !node.equals(augmentedExit)) {
				LabeledEdge exitEdge = edgeClass.getDeclaredConstructor().newInstance();
				cfg.addEdge(node, augmentedExit, exitEdge);
				exitEdge.setLabel("exit");
			}
		}

		CFGNode originalEnd = cfg.getEnd();
		if (originalEnd != null && !originalEnd.equals(augmentedExit)) {
			LabeledEdge exitEdge = edgeClass.getDeclaredConstructor().newInstance();
			cfg.addEdge(originalEnd, augmentedExit, exitEdge);
			exitEdge.setLabel("exit");
		}
	}

	private Set<CFGNode> getOriginalSuccessors(CFGNode node) {
		Set<CFGNode> successors = new HashSet<>();
		for (LabeledEdge edge : cfg.outgoingEdgesOf(node)) {
			CFGNode succ = cfg.getEdgeTarget(edge);
			if (!succ.equals(augmentedEntry) && !succ.equals(augmentedExit)) {
				successors.add(succ);
			}
		}
		return successors;
	}

	private void computePostDominators() {
		Set<CFGNode> originalNodes = new HashSet<>(cfg.vertexSet());
		originalNodes.remove(augmentedEntry);
		originalNodes.remove(augmentedExit);

		CFGNode originalExit = cfg.getEnd();

		postDominators.put(originalExit, new HashSet<>(Collections.singletonList(originalExit)));
		for (CFGNode node : originalNodes) {
			if (!node.equals(originalExit)) {
				postDominators.put(node, new HashSet<>(originalNodes));
			}
		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (CFGNode node : originalNodes) {
				if (node.equals(originalExit)) continue;

				Set<CFGNode> successors = getOriginalSuccessors(node);
				if (successors.isEmpty()) continue;

				Iterator<CFGNode> iter = successors.iterator();
				Set<CFGNode> newPostDom = new HashSet<>(postDominators.get(iter.next()));
				while (iter.hasNext()) {
					newPostDom.retainAll(postDominators.get(iter.next()));
				}

				newPostDom.add(node);

				if (!newPostDom.equals(postDominators.get(node))) {
					postDominators.put(node, newPostDom);
					changed = true;
				}
			}
		}

		// AugmentedExit only for terminal nodes
		postDominators.put(augmentedExit, new HashSet<>(Collections.singletonList(augmentedExit)));
		postDominators.get(originalExit).add(augmentedExit);

		computeImmediatePostDominatorsForOriginalNodes(originalNodes, originalExit);
	}

	private void computeImmediatePostDominatorsForOriginalNodes(Set<CFGNode> nodes, CFGNode exit) {
		for (CFGNode node : nodes) {
			if (node.equals(exit)) continue;

			Set<CFGNode> postDoms = postDominators.get(node);
			if (postDoms == null) continue;

			Set<CFGNode> strictPostDoms = new HashSet<>(postDoms);
			strictPostDoms.remove(node);

			CFGNode iPostDom = findImmediatePostDominator(node, strictPostDoms);
			if (iPostDom != null) {
				immediatePostDominators.put(node, iPostDom);
			}
		}

		immediatePostDominators.put(exit, augmentedExit);
	}

	private CFGNode findImmediatePostDominator(CFGNode node, Set<CFGNode> strictPostDoms) {
		for (CFGNode candidate : strictPostDoms) {
			boolean isImmediate = true;
			for (CFGNode other : strictPostDoms) {
				if (candidate.equals(other)) continue;
				Set<CFGNode> otherPostDoms = postDominators.get(other);
				if (otherPostDoms != null && !otherPostDoms.contains(candidate)) {
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
			Set<CFGNode> successors = getOriginalSuccessors(cfgNode);

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

			for (CFGNode succ : getOriginalSuccessors(current)) {
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
		if (cfgEdge != null && cfgEdge.getLabel() != null && !cfgEdge.getLabel().toString().isEmpty()) {
			return cfgEdge.getLabel().toString();
		}

		Set<CFGNode> successors = getOriginalSuccessors(source);
		if (successors.size() > 1) {
			String srcStr = source.toString().toLowerCase();
			if (srcStr.contains("if") || srcStr.contains("while") || srcStr.contains("for")) {
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

	public CDG_old getCDG() {
		return cdg;
	}
	public void printCDG() {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
			writer.write("\n\n=========================Control Dependence Graph: " + unitFunName + "=========================\n");

			// Print CDG nodes (skip CDG Entry)
			for (CDGNode_old node : cdg.vertexSet()) {
				if (node.equals(cdg.getStart())) continue; // skip CDG Entry

				writer.write("Node: " + node + "\n");
				System.out.println("Node: " + node);

				Set<CDGEdge> outEdges = cdg.outgoingEdgesOf(node);
				if (!outEdges.isEmpty()) {
					writer.write("  Controls:\n");
					System.out.println("  Controls:");
					for (CDGEdge edge : outEdges) {
						CDGNode_old target = cdg.getEdgeTarget(edge);
						String label = edge.getLabel();
						if (!label.isEmpty()) {
							label = " [" + label + "]";
						}
						writer.write("    -> " + target + label + "\n");
						System.out.println("    -> " + target + label);
					}
				}
			}

			// Print CDG Entry only once
			CDGNode_old cdgEntry = cdg.getStart();
			writer.write("Node: CDG Entry\n  Controls:\n");
			System.out.println("Node: CDG Entry\n  Controls:");
			for (CDGEdge edge : cdg.outgoingEdgesOf(cdgEntry)) {
				CDGNode_old target = cdg.getEdgeTarget(edge);
				String label = edge.getLabel();
				if (!label.isEmpty()) label = " [" + label + "]";
				writer.write("    -> " + target + label + "\n");
				System.out.println("    -> " + target + label);
			}


			// Post-dominators
			writer.write("\n=========================Post-Dominators: " + unitFunName + "=========================\n");
			for (Map.Entry<CFGNode, Set<CFGNode>> entry : postDominators.entrySet()) {
				writer.write("Node " + entry.getKey().getId() + ": ");
				writer.write("Post-dominated by: " +
						entry.getValue().stream().map(n -> String.valueOf(n.getId()))
						.reduce((a, b) -> a + ", " + b).orElse("none") + "\n");
			}

			// Immediate post-dominators
			writer.write("\n=========================Immediate Post-Dominators: " + unitFunName + "=========================\n");
			for (Map.Entry<CFGNode, CFGNode> entry : immediatePostDominators.entrySet()) {
				writer.write("Node " + entry.getKey().getId() +
						" -> iPostDom: " + entry.getValue().getId() + "\n");
			}


		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
		printBranchChainsWithIds();
	}
	public List<List<String>> getBranchChainsWithIds() {
		List<List<String>> allChains = new ArrayList<>();
		CDGNode_old startNode = cdg.getStart();

		if (startNode == null) return allChains;

		// Map to keep track of branch numbers
		Map<CDGEdge, String> branchIds = new HashMap<>();
		int[] branchCounter = {1};

		traverseBranchesWithIds(startNode, new ArrayList<>(), allChains, new HashSet<>(), branchIds, branchCounter);
		return allChains;
	}

	private void traverseBranchesWithIds(CDGNode_old current,
			List<String> path,
			List<List<String>> allChains,
			Set<CDGNode_old> visited,
			Map<CDGEdge, String> branchIds,
			int[] branchCounter) {
		if (visited.contains(current)) return;
		visited.add(current);

		Set<CDGEdge> outgoing = cdg.outgoingEdgesOf(current);

		if (outgoing.isEmpty()) {
			// Leaf node reached
			allChains.add(new ArrayList<>(path));
		} else {
			for (CDGEdge edge : outgoing) {
				CDGNode_old target = cdg.getEdgeTarget(edge);
				String edgeLabel = edge.getLabel();

				// Assign branch ID if it’s a conditional branch
				String branchLabel;
				if (edgeLabel.equalsIgnoreCase("true") || edgeLabel.equalsIgnoreCase("false")) {
					branchLabel = "b" + branchCounter[0] + ":" + edgeLabel.toLowerCase();
					branchIds.put(edge, branchLabel);
				} else {
					branchLabel = edgeLabel.isEmpty() ? "" : edgeLabel;
				}

				path.add(current + " -> " + target + (branchLabel.isEmpty() ? "" : " [" + branchLabel + "]"));

				// Increment branch counter only for the first edge of a new conditional
				if (!branchIds.containsKey(edge) && (edgeLabel.equalsIgnoreCase("true") || edgeLabel.equalsIgnoreCase("false"))) {
					branchCounter[0]++;
				}

				traverseBranchesWithIds(target, path, allChains, visited, branchIds, branchCounter);
				path.remove(path.size() - 1);
			}
		}

		visited.remove(current);

	}


	public void printBranchChainsWithIds() {
		List<List<String>> chains = getBranchChainsWithIds();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
			writer.write("\n=========================Branch Chains: " + unitFunName + "=========================\n");

			int chainNum = 1;
			for (List<String> chain : chains) {
				writer.write("Chain " + chainNum + ": ");
				for (int i = 0; i < chain.size(); i++) {
					writer.write(chain.get(i));
					if (i != chain.size() - 1)  writer.write(" -> ");
				}
				writer.write("\n");
				chainNum++;
			}
			writer.write("\n==========================================================================\n");
			writer.write("\n==============================================================================\n");
			writer.write("\n******************************************************************************\n");
			writer.write("\n==============================================================================\n");

		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}

	}

	/*public void generateBranchChainsFromCDG() {
		if (cdg == null || cdg.getGraph() == null) {
			System.out.println("CDG graph is not initialized.");
			return;
		}

		DirectedGraph<CFGNode, DefaultEdge> graph = cdg.getGraph();

		// Find CDG Entry node (entry point)
		CFGNode entryNode = null;
		for (CFGNode node : graph.vertexSet()) {
			if (node.getLabel().equalsIgnoreCase("CDG Entry") ||
					node.getLabel().equalsIgnoreCase("Entry")) {
				entryNode = node;
				break;
			}
		}

		if (entryNode == null) {
			System.out.println("CDG Entry node not found.");
			return;
		}

		System.out.println("\n=========================Branch Chains from CDG=========================");

		// To store all branch chains
		List<List<CFGNode>> allChains = new ArrayList<>();

		// Depth-first search to explore all possible paths
		exploreBranchChains(graph, entryNode, new ArrayList<>(), allChains);

		// Print all chains
		int chainCount = 1;
		for (List<CFGNode> chain : allChains) {
			System.out.print("Chain " + chainCount++ + ": ");
			for (int i = 0; i < chain.size(); i++) {
				CFGNode n = chain.get(i);
				System.out.print(n.getId());// + "(" + n.getLabel() + ")");
				if (i < chain.size() - 1) System.out.print(" -> ");
			}
			System.out.println();
		}

		// Print first/last pairs from each chain
		System.out.println("\n=========================Branch Pairs=========================");
		int pairCount = 1;
		for (List<CFGNode> chain : allChains) {
			if (chain.size() >= 2) {
				CFGNode start = chain.get(0);
				CFGNode end = chain.get(chain.size() - 1);
				System.out.println("Pair " + pairCount++ + ": N" + start.getId() + " -> N" + end.getId());
			}
		}
	}

	private void exploreBranchChains(DirectedGraph<CFGNode, DefaultEdge> graph,
			CFGNode current,
			List<CFGNode> path,
			List<List<CFGNode>> allChains) {
		path.add(current);

		Set<DefaultEdge> outgoing = graph.outgoingEdgesOf(current);
		if (outgoing.isEmpty()) {
			// Leaf node reached → store the chain
			allChains.add(new ArrayList<>(path));
		} else {
			for (DefaultEdge edge : outgoing) {
				CFGNode target = graph.getEdgeTarget(edge);
				// Avoid cycles
				if (!path.contains(target)) {
					exploreBranchChains(graph, target, path, allChains);
				}
			}
		}

		// Backtrack
		path.remove(path.size() - 1);
	}
*/


}
