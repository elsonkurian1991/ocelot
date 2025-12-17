package it.unisa.ocelot.c.cdg;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import org.eclipse.cdt.core.dom.ast.*;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.io.*;
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
public class CDGBuilder {
	private CFG cfg;
	private CDG_old cdg;
	private Map<CFGNode, Set<CFGNode>> postDominators;
	private Map<CFGNode, CFGNode> immediatePostDominators;
	private CFGNode augmentedEntry;
	private CFGNode augmentedExit;
	private String unitFunName;
	private static final File outputFile = new File("cdg_output.txt");

	public CDGBuilder(CFG pGraph, String pFunctionName) {
		this.cfg = pGraph;
		this.cdg = new CDG_old();
		this.postDominators = new HashMap<>();
		this.immediatePostDominators = new HashMap<>();
		this.unitFunName = pFunctionName;
	}

	public CDG_old buildCDG() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
	InvocationTargetException, NoSuchMethodException, SecurityException {
		augmentCFG();// just add the entry and exit to cfg
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
		//created the post dominators
		boolean changed = true;
		while (changed) {
			changed = false;
			for (CFGNode node : originalNodes) {
				if (node.equals(originalExit)) continue;

				Set<CFGNode> successors = getOriginalSuccessors(node);
				if (successors.isEmpty()) continue;

				Iterator<CFGNode> iter = successors.iterator();
				Set<CFGNode> newPostDom = new HashSet<>(postDominators.get(iter.next())); //created new postDom
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
				if (!outEdges.isEmpty()) {// here we are getting the nodes which have conditions tempUnitComponent + ":" + "branch0-true"
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
		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
		//printBranchChainsWithIds();
		generateBranchChainsFromCDG();
	}
			// Print CDG Entry only once
			/*CDGNode cdgEntry = cdg.getStart(); // for the moment, this part is not interested 
			writer.write("Node: CDG Entry\n  Controls:\n");
			System.out.println("Node: CDG Entry\n  Controls:");
			for (CDGEdge edge : cdg.outgoingEdgesOf(cdgEntry)) {
				CDGNode target = cdg.getEdgeTarget(edge);
				String label = edge.getLabel();
				if (!label.isEmpty()) label = " [" + label + "]";
				writer.write("    -> " + target + label + "\n");
				System.out.println("    -> " + target + label);
			}*/

			/*
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

			 */
		
	
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
	public void generateBranchChainsFromCDG() {
		// sanitize CDG
				Set<CDGEdge> toRemove = new HashSet<>();
				for (CDGEdge e : cdg.edgeSet()) {
				    if (cdg.getEdgeSource(e).equals(cdg.getEdgeTarget(e))) {
				        toRemove.add(e); // remove self-loop
				    }
				}
				cdg.removeAllEdges(toRemove);

	    CDGNode_old entry = cdg.getStart();
	    if (entry == null) {
	        for (CDGNode_old n : cdg.vertexSet()) {
	            if (cdg.incomingEdgesOf(n).isEmpty()) {
	                entry = n;
	                break;
	            }
	        }
	    }
	    if (entry == null) {
	        System.out.println("CDG entry not found.");
	        return;
	    }

	    // find leaves = nodes with no outgoing edges
	    List<CDGNode_old> leaves = cdg.vertexSet().stream()
	            .filter(n -> cdg.outgoingEdgesOf(n).isEmpty())
	            .toList();

	    List<List<String>> chains = new ArrayList<>();
	    dfsCollect(entry, new ArrayList<>(), leaves, chains);

	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
	        writer.write("\n========================= Branch Chains (from CDG): " + unitFunName + " =========================\n");
	        int i = 1;
	        for (List<String> c : chains) {
	            writer.write("Chain " + i++ + ": " + String.join(" → ", c) + "\n");
	        }
	        writer.write("====================================================================\n");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	private void dfsCollect(CDGNode_old node, List<String> path, List<CDGNode_old> leaves, List<List<String>> out) {
	    path.add(node.toString());

	    Set<CDGEdge> outs = cdg.outgoingEdgesOf(node);
	    if (outs.isEmpty() || leaves.contains(node)) {
	        out.add(new ArrayList<>(path));
	    } else {
	        for (CDGEdge e : outs) {
	            CDGNode_old tgt = cdg.getEdgeTarget(e);
	            if (path.contains(tgt)) continue; // prevent cycles
	            String label = e.getLabel();
	            if (label != null && !label.isEmpty())
	                path.set(path.size() - 1, path.get(path.size() - 1) + " [" + label + "]");
	            dfsCollect(tgt, path, leaves, out);
	            if (label != null && !label.isEmpty())
	                path.set(path.size() - 1, path.get(path.size() - 1).replace(" [" + label + "]", ""));
	        }
	    }
	    path.remove(path.size() - 1);
	}

	
	
	
	/*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
	
	public void generateBranchChainsFromCDGchumma() {
		
	    if (cdg == null) {
	        System.out.println("CDG is null - nothing to process.");
	        return;
	    }

	    // 1) find entry (prefer cdg.getStart())
	    CDGNode_old entry = cdg.getStart();
	    if (entry == null) {
	        for (CDGNode_old n : cdg.vertexSet()) {
	            if (cdg.incomingEdgesOf(n).isEmpty()) {
	                entry = n;
	                break;
	            }
	        }
	    }
	    if (entry == null) {
	        System.out.println("CDG entry not found.");
	        return;
	    }

	    // 2) collect leaves: nodes with no directed outgoing edges
	    List<CDGNode_old> leaves = new ArrayList<>();
	    for (CDGNode_old n : cdg.vertexSet()) {
	        if (cdg.outgoingEdgesOf(n).isEmpty()) {
	            leaves.add(n);
	        }
	    }

	    // 3) For each leaf, reverse-collect only condition controllers (outer->inner)
	    List<List<String>> allChains = new ArrayList<>();
	    for (CDGNode_old leaf : leaves) {
	        List<List<String>> chainsForLeaf = new ArrayList<>();
	        reverseCollectConditions(entry, leaf, new ArrayList<>(), chainsForLeaf, new HashSet<>());
	        // Each chainsForLeaf contains 0 or more chains (each already ordered entry->...->leaf)
	        // If no condition controllers were found, produce a single minimal chain with just the leaf.
	        if (chainsForLeaf.isEmpty()) {
	            List<String> single = new ArrayList<>();
	            single.add(leaf.toString());
	            allChains.add(single);
	        } else {
	            allChains.addAll(chainsForLeaf);
	        }
	    }

	    // 4) Print/write chains
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
	        writer.write("\n========================= Branch Chains (from CDG): " + unitFunName + " =========================\n");
	        int idx = 1;
	        for (List<String> chain : allChains) {
	            writer.write("Chain " + idx++ + ": ");
	            for (int i = 0; i < chain.size(); i++) {
	                writer.write(chain.get(i));
	                if (i < chain.size() - 1) writer.write(" → ");
	            }
	            writer.write("\n");
	        }
	        writer.write("\n====================================================================\n\n");
	    } catch (IOException e) {
	        System.err.println("Error writing CDG file: " + e.getMessage());
	    }
	}

	/**
	 * Reverse-collect condition controllers for `current` (initially a leaf).
	 *
	 * - entry: the CDG entry node (stop / top).
	 * - current: node we're climbing from (leaf or a controller).
	 * - acc: accumulated controllers (closest-to-leaf first while recursing).
	 * - chainsOut: when a top is reached, adds a forward-ordered chain (outer->...->inner->leaf).
	 * - visited: cycle guard.
	 */
	private void reverseCollectConditions(CDGNode_old entry,
	                                      CDGNode_old current,
	                                      List<String> acc,
	                                      List<List<String>> chainsOut,
	                                      Set<CDGNode_old> visited) {
	    if (visited.contains(current)) return;
	    visited.add(current);

	    // incoming edges: controllers -> current
	    Set<CDGEdge> inEdges = cdg.incomingEdgesOf(current);

	    // Filter to only controllers that "look like conditions"
	    List<CDGEdge> condEdges = new ArrayList<>();
	    for (CDGEdge e : inEdges) {
	        CDGNode_old src = cdg.getEdgeSource(e);
	        if (looksLikeCondition(src)) {
	            condEdges.add(e);
	        }
	    }

	    // If we found no condition controllers, stop and build a forward chain from acc + leaf
	    if (condEdges.isEmpty()) {
	        // acc currently has controllers closest-to-leaf first.
	        // Build forward order: reverse acc, then append the leaf (current originally leaf)
	        List<String> forward = new ArrayList<>();
	        for (int i = acc.size() - 1; i >= 0; --i) {
	            forward.add(acc.get(i));
	        }
	        // Append the final leaf/current
	        forward.add(current.toString());
	        // Optionally, include Entry at front if desired:
	        if (entry != null && (forward.isEmpty() || !forward.get(0).equals(entry.toString()))) {
	            // Only add explicit entry if you want it displayed:
	            // forward.add(0, entry.toString());
	        }
	        // Only add non-empty meaningful chains
	        if (!forward.isEmpty()) chainsOut.add(forward);
	        visited.remove(current);
	        return;
	    }

	    // Otherwise, for every condition controller, recurse upward.
	    // This will produce multiple chains (if there are multiple condition controllers).
	    for (CDGEdge e : condEdges) {
	        CDGNode_old src = cdg.getEdgeSource(e);
	        String label = e.getLabel();
	        String piece = src.toString() + (label != null && !label.isEmpty() ? " [" + label + "]" : "");
	        acc.add(piece); // add closest-to-leaf first
	        reverseCollectConditions(entry, src, acc, chainsOut, visited);
	        acc.remove(acc.size() - 1); // backtrack
	    }

	    visited.remove(current);
	}

	/**
	 * Heuristic: detect if a node string looks like a condition (if/boolean expression).
	 * Adjust tokens depending on your CDG node naming format.
	 */
	private boolean looksLikeCondition(CDGNode_old n) {
	    if (n == null) return false;
	    String s = n.toString().toLowerCase();
	    return s.contains("if") || s.contains(">") || s.contains("<") ||
	           s.contains("==") || s.contains("!=") || s.contains("&&") ||
	           s.contains("||") || s.contains("condition");
	}


	/*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
	// Main method: generate and print filtered branch chains & pairs using the CDG field
	public void generateBranchChainsFromCDG_old() {
	    if (cdg == null) {
	        System.out.println("CDG is null - nothing to process.");
	        return;
	    }

	    CDGNode_old entry = cdg.getStart();
	    if (entry == null) {
	        for (CDGNode_old n : cdg.vertexSet()) {
	            if (cdg.incomingEdgesOf(n).isEmpty()) {
	                entry = n;
	                break;
	            }
	        }
	    }

	    if (entry == null) {
	        System.out.println("CDG entry node not found.");
	        return;
	    }

	    List<List<String>> branchChains = new ArrayList<>();
	    dfsCollectChains(entry, new ArrayList<>(), branchChains);

	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
	        writer.write("\n========================= Branch Chains (from CDG): " + unitFunName + " =========================\n");
	        int idx = 1;
	        for (List<String> chain : branchChains) {
	            writer.write("Chain " + idx++ + ": ");
	            for (int i = 0; i < chain.size(); i++) {
	                writer.write(chain.get(i));
	                if (i < chain.size() - 1) writer.write(" → ");
	            }
	            writer.write("\n");
	        }
	        writer.write("\n====================================================================\n\n");
	    } catch (IOException e) {
	        System.err.println("Error writing CDG file: " + e.getMessage());
	    }
	}
	private void dfsCollectChains(CDGNode_old current, List<String> path, List<List<String>> allPaths) {
	    Set<CDGEdge> outEdges = cdg.outgoingEdgesOf(current);

	    // If no outgoing edges — it's a LEAF node
	    if (outEdges.isEmpty()) {
	        List<String> completed = new ArrayList<>(path);
	        completed.add(current.toString());
	        allPaths.add(completed);
	        return;
	    }

	    // Otherwise, follow each branch
	    for (CDGEdge edge : outEdges) {
	        CDGNode_old target = cdg.getEdgeTarget(edge);
	        String label = edge.getLabel();

	        List<String> newPath = new ArrayList<>(path);
	        // include both current node + branch label for clarity
	        if (!looksLikeEntry(current) && !looksLikeStart(current))
	            newPath.add(current + (label != null && !label.isEmpty() ? " [" + label + "]" : ""));

	        dfsCollectChains(target, newPath, allPaths);
	    }
	}

	/*public void generateBranchChainsFromCDG() {
		if (cdg == null) {
			System.out.println("CDG is null - nothing to process.");
			return;
		}

		// Find CDG entry node
		CDGNode entry = cdg.getStart();
		if (entry == null) {
			// fallback: pick a node with no incoming edges
			for (CDGNode n : cdg.vertexSet()) {
				if (cdg.incomingEdgesOf(n).isEmpty()) {
					entry = n;
					break;
				}
			}
		}

		if (entry == null) {
			System.out.println("CDG entry node not found.");
			return;
		}

		// Collect all root->leaf chains (as lists of CDGNode)
		List<List<CDGNode>> rawChains = new ArrayList<>();
		dfsCollectChains(entry, new ArrayList<>(), rawChains, new HashSet<>());

		// Filter chains by removing leading Entry/Start and trailing End nodes
		List<List<CDGNode>> filteredChains = new ArrayList<>();
		for (List<CDGNode> rc : rawChains) {
			List<CDGNode> chain = new ArrayList<>(rc);

			// remove first node if it's CDG Entry (by toString content)
			if (!chain.isEmpty() && looksLikeEntry(chain.get(0))) {
				chain.remove(0);
			}
			// remove second node if it's Start (after removal above we must recheck)
			if (!chain.isEmpty() && looksLikeStart(chain.get(0))) {
				chain.remove(0);
			}

			// remove last node if it's End
			if (!chain.isEmpty() && looksLikeEnd(chain.get(chain.size() - 1))) {
				chain.remove(chain.size() - 1);
			}

			// add only non-empty chains (you requested at least first->last)
			if (!chain.isEmpty()) filteredChains.add(chain);
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
			// Print filtered chains
			writer.write("\n========================= Branch Chains: " + unitFunName + " =========================\n");
			int chainIdx = 1;
			for (List<CDGNode> chain : filteredChains) {
				writer.write("Chain " + chainIdx++ + ": ");
				for (int i = 0; i < chain.size(); i++) {
					CDGNode n = chain.get(i);
					writer.write(n.toString());
					if (i < chain.size() - 1) writer.write(" -> ");
				}
				writer.write("\n");
			}

			// Print pairs (first -> last) using same filtered chains
			/*writer.write("\n========================= Branch Pairs: " + unitFunName + " =========================\n");
			int pairIdx = 1;
			for (List<CDGNode> chain : filteredChains) {
				if (chain.size() >= 1) {
					CDGNode start = chain.get(0);
					CDGNode end = chain.get(chain.size() - 1);
					String sLabel = extractLeadingIdOrString(start.toString());
					String eLabel = extractLeadingIdOrString(end.toString());
					writer.write("\nPair " + pairIdx++ + ": " + sLabel + " -> " + eLabel);
				}
			}*/
			/*writer.write("\n====================================================================");
			writer.write("\n====================================================================");
			writer.write("\n====================================================================");
		} catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
	}*/

	// DFS helper collecting all root->leaf chains in the CDG (works with CDGNode/CDGEdge)
	private void dfsCollectChains(CDGNode_old current,
			List<CDGNode_old> path,
			List<List<CDGNode_old>> allChains,
			Set<CDGNode_old> visiting) {
		if (visiting.contains(current)) return; // avoid cycles
		visiting.add(current);
		path.add(current);

		Set<CDGEdge> out = cdg.outgoingEdgesOf(current);
		if (out == null || out.isEmpty()) {
			// reached a leaf: record the chain
			allChains.add(new ArrayList<>(path));
		} else {
			for (CDGEdge e : out) {
				CDGNode_old tgt = cdg.getEdgeTarget(e);
				if (!path.contains(tgt)) { // avoid simple cycles in path
					dfsCollectChains(tgt, path, allChains, visiting);
				}
			}
		}

		// backtrack
		path.remove(path.size() - 1);
		visiting.remove(current);
	}

	// Try to extract ID like "N3" from strings that start with "3: ..." ; otherwise return trimmed string
	private String extractLeadingIdOrString(String nodeToString) {
		if (nodeToString == null) return "null";
		String s = nodeToString.trim();
		int colon = s.indexOf(':');
		if (colon > 0) {
			String possibleNum = s.substring(0, colon).trim();
			if (possibleNum.matches("\\d+")) {
				return "N" + possibleNum;
			}
		}
		// fallback shorten
		if (s.length() > 80) return s.substring(0, 77) + "...";
		return s;
	}

	// Heuristics on toString() to detect Entry / Start / End nodes
	private boolean looksLikeEntry(CDGNode_old n) {
		if (n == null) return false;
		String s = n.toString().toLowerCase();
		return s.contains("cdg entry") || s.equals("entry") || s.contains("augmentedentry");
	}
	private boolean looksLikeStart(CDGNode_old n) {
		if (n == null) return false;
		String s = n.toString().toLowerCase();
		return s.equals("start") || s.contains("start") || s.contains("augmentedentry");
	}
	private boolean looksLikeEnd(CDGNode_old n) {
		if (n == null) return false;
		String s = n.toString().toLowerCase();
		return s.equals("end") || s.contains("end") || s.contains("augmentedexit");
	}
	/*public void printBranchChainsWithIds() {
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


	} catch (IOException e) {
		System.err.println("Error writing CDG file: " + e.getMessage());
	}

}

public void generateBranchChainsFromCDG() {
    if (cdg == null || cdg.getGraph() == null) {
        System.out.println("CDG graph is not initialized.");
        return;
    }

    DirectedGraph<CDGNode, DefaultEdge> graph = cdg.getGraph();

    // Find the CDG Entry node
    CDGNode entryNode = cdg.getStart();
    if (entryNode == null) {
        for (CDGNode node : graph.vertexSet()) {
            if (node.toString().toLowerCase().contains("entry")) {
                entryNode = node;
                break;
            }
        }
    }

    if (entryNode == null) {
        System.out.println("CDG Entry node not found.");
        return;
    }

    System.out.println("\n========================= Branch Chains =========================");

    List<List<CDGNode>> allChains = new ArrayList<>();
    dfsCollectChains(graph, entryNode, new ArrayList<>(), allChains);

    // Filter out Entry/Start/End nodes
    List<List<CDGNode>> filteredChains = new ArrayList<>();
    for (List<CDGNode> chain : allChains) {
        List<CDGNode> cleanedChain = new ArrayList<>();
        for (CDGNode node : chain) {
            String label = node.toString().toLowerCase();
            if (!(label.contains("entry") || label.contains("start") || label.contains("end"))) {
                cleanedChain.add(node);
            }
        }
        if (!cleanedChain.isEmpty()) {
            filteredChains.add(cleanedChain);
        }
    }

    // Print chains
    int chainCount = 1;
    for (List<CDGNode> chain : filteredChains) {
        System.out.print("Chain " + chainCount++ + ": ");
        for (int i = 0; i < chain.size(); i++) {
            CDGNode node = chain.get(i);
            System.out.print(node.getId() + "(" + node.toString() + ")");
            if (i < chain.size() - 1) System.out.print(" -> ");
        }
        System.out.println();
    }

    // Print pairs (first → last)
    System.out.println("\n========================= Branch Pairs =========================");
    int pairCount = 1;
    for (List<CDGNode> chain : filteredChains) {
        if (chain.size() >= 2) {
            CDGNode first = chain.get(0);
            CDGNode last = chain.get(chain.size() - 1);
            System.out.println("Pair " + pairCount++ + ": N" + first.getId() + " -> N" + last.getId());
        }
    }
}

//Depth-first search helper to collect all possible chains from entry to leaf nodes.

private void dfsCollectChains(DirectedGraph<CDGNode, DefaultEdge> graph,
                              CDGNode current,
                              List<CDGNode> path,
                              List<List<CDGNode>> allChains) {
    path.add(current);

    Set<DefaultEdge> outgoing = graph.outgoingEdgesOf(current);
    if (outgoing.isEmpty()) {
        allChains.add(new ArrayList<>(path)); // reached leaf node
    } else {
        for (DefaultEdge edge : outgoing) {
            CDGNode target = graph.getEdgeTarget(edge);
            if (!path.contains(target)) { // avoid cycles
                dfsCollectChains(graph, target, path, allChains);
            }
        }
    }

    path.remove(path.size() - 1); // backtrack
}

private void dfsCollectChains(CDGNode current, List<CDGNode> path, List<List<CDGNode>> allChains) {
    path.add(current);

    Set<CDGEdge> outgoing = cdg.outgoingEdgesOf(current);
    if (outgoing.isEmpty()) {
        allChains.add(new ArrayList<>(path));
    } else {
        for (CDGEdge edge : outgoing) {
            CDGNode target = cdg.getEdgeTarget(edge);
            if (!path.contains(target)) {
                dfsCollectChains(target, path, allChains);
            }
        }
    }

    path.remove(path.size() - 1);
}
private String getNodeLabel(Object node) {
    if (node == null) return "null";
    try {
        java.lang.reflect.Method m = node.getClass().getMethod("getLabel");
        Object val = m.invoke(node);
        if (val != null) return val.toString();
    } catch (Exception ignored) {}

    try {
        java.lang.reflect.Method m = node.getClass().getMethod("getId");
        Object val = m.invoke(node);
        if (val != null) return val.toString();
    } catch (Exception ignored) {}

    return node.toString();
}

private String getNodeIdSafe(Object node) {
    if (node == null) return "null";
    try {
        // Try to call getId() if it exists
        java.lang.reflect.Method m = node.getClass().getMethod("getId");
        Object id = m.invoke(node);
        return id != null ? id.toString() : "?";
    } catch (Exception e1) {
        try {
            // Try getLabel() as a fallback
            java.lang.reflect.Method m = node.getClass().getMethod("getLabel");
            Object label = m.invoke(node);
            return label != null ? label.toString() : "?";
        } catch (Exception e2) {
            return node.toString();
        }
    }
}

private List<List<CDGNode>> collectChainsStartingFromStart() {
    List<List<CDGNode>> chains = new ArrayList<>();
    if (cdg == null) return chains;

    CDGNode cdgEntry = cdg.getStart();

    CDGNode startNode = null;
    if (cdgEntry != null) {
        for (CDGEdge e : cdg.outgoingEdgesOf(cdgEntry)) {
            CDGNode tgt = cdg.getEdgeTarget(e);
            if (looksLikeStart(tgt)) { startNode = tgt; break; }
        }
    }
    if (startNode == null) {
        for (CDGNode n : cdg.vertexSet()) {
            if (looksLikeStart(n)) { startNode = n; break; }
        }
    }
    if (startNode == null) {
        for (CDGNode n : cdg.vertexSet()) {
            if (cdg.incomingEdgesOf(n).isEmpty() && !looksLikeEntry(n)) { startNode = n; break; }
        }
    }
    if (startNode == null) return chains;

    dfsCollectFromStart(startNode, new ArrayList<>(), chains, new HashSet<>());
    return chains;
}

private void dfsCollectFromStart(CDGNode current,
                                 List<CDGNode> path,
                                 List<List<CDGNode>> chains,
                                 Set<CDGNode> visiting) {
    if (visiting.contains(current)) return;
    visiting.add(current);
    path.add(current);

    Set<CDGEdge> outgoing = cdg.outgoingEdgesOf(current);
    if (outgoing == null || outgoing.isEmpty()) {
        chains.add(new ArrayList<>(path));
    } else {
        for (CDGEdge e : outgoing) {
            CDGNode tgt = cdg.getEdgeTarget(e);
            if (looksLikeEntry(tgt)) continue;
            if (!path.contains(tgt)) dfsCollectFromStart(tgt, path, chains, visiting);
        }
    }

    path.remove(path.size() - 1);
    visiting.remove(current);
}

private boolean looksLikeEntry(CDGNode n) {
    if (n == null) return false;
    String s = n.toString().toLowerCase();
    return s.contains("cdg entry") || s.equals("entry") || s.contains("augmentedentry");
}

private boolean looksLikeStart(CDGNode n) {
    if (n == null) return false;
    String s = n.toString().toLowerCase();
    return s.equals("start") || s.contains(" start") || s.contains(": start") || s.contains("augmentedentry");
}

private boolean looksLikeEnd(CDGNode n) {
    if (n == null) return false;
    String s = n.toString().toLowerCase();
    return s.equals("end") || s.contains(" end") || s.contains(": end") || s.contains("augmentedexit");
}
	 */
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Call this to generate and print chains and pairs from the CDG field
	/*public void generateBranchChainsFromCDG() {
	    if (cdg == null) {
	        System.out.println("CDG is null - nothing to process.");
	        return;
	    }

	    CDGNode entry = cdg.getStart();
	    if (entry == null) {
	        // fallback: try to find a node with no incoming edges
	        for (CDGNode n : cdg.vertexSet()) {
	            if (cdg.incomingEdgesOf(n).isEmpty()) {
	                entry = n;
	                break;
	            }
	        }
	    }

	    if (entry == null) {
	        System.out.println("CDG entry node not found.");
	        return;
	    }

	    // Collect chains (each chain is a list of CDGNode)
	    List<List<CDGNode>> allChains = new ArrayList<>();
	    dfsCollectChains(entry, new ArrayList<>(), allChains, new HashSet<>());
	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
	    // Print chains
	    	writer.write("\n========================= Branch Chains: " + unitFunName + " =========================\n");
	    int chainIdx = 1;
	    for (List<CDGNode> chain : allChains) {
	    	writer.write("Chain " + chainIdx++ + ": ");
	        for (int i = 0; i < chain.size(); i++) {
	            CDGNode n = chain.get(i);
	            writer.write(n.toString());
	            if (i < chain.size() - 1) writer.write(" -> ");
	        }
	        writer.write("\n");
	    }

	    // Print start->end pairs (first and last of each chain)
	    writer.write("\n========================= Branch Pairs: " + unitFunName + " =========================\n");
	    int pairIdx = 1;
	    for (List<CDGNode> chain : allChains) {
	        if (chain.size() >= 1) {
	            CDGNode start = chain.get(0);
	            CDGNode end = chain.get(chain.size() - 1);

	            // Try to extract a concise ID like "N0" from toString() if possible
	            String startLabel = extractLeadingIdOrString(start.toString());
	            String endLabel = extractLeadingIdOrString(end.toString());

	            writer.write("\nPair " + pairIdx++ + ": " + startLabel + " -> " + endLabel);
	        }
	    }
	    writer.write("\n==========================================================================\n");
		writer.write("\n==============================================================================\n");
		writer.write("\n******************************************************************************\n");
		writer.write("\n==============================================================================\n");

	    } catch (IOException e) {
			System.err.println("Error writing CDG file: " + e.getMessage());
		}
	}

	// DFS helper: collects all root-to-leaf chains of CDGNodes
	private void dfsCollectChains(CDGNode current,
	                              List<CDGNode> path,
	                              List<List<CDGNode>> allChains,
	                              Set<CDGNode> visiting) {
	    // avoid cycles
	    if (visiting.contains(current)) return;

	    visiting.add(current);
	    path.add(current);

	    Set<CDGEdge> out = cdg.outgoingEdgesOf(current);
	    if (out == null || out.isEmpty()) {
	        // leaf -> record chain
	        allChains.add(new ArrayList<>(path));
	    } else {
	        for (CDGEdge e : out) {
	            CDGNode tgt = cdg.getEdgeTarget(e);
	            // Continue DFS only if not already in current path (prevents simple cycles)
	            if (!path.contains(tgt)) {
	                dfsCollectChains(tgt, path, allChains, visiting);
	            }
	        }
	    }

	    // backtrack
	    path.remove(path.size() - 1);
	    visiting.remove(current);
	}

	// Utility: try to extract a leading numeric id "N<id>" from a node.toString() like "3: sum = a + b"
	// If not present, returns the original string (trimmed).
	private String extractLeadingIdOrString(String nodeToString) {
	    if (nodeToString == null) return "null";
	    String s = nodeToString.trim();
	    // Pattern: optional number at start followed by ':' (e.g. "3: ...")
	    // We'll return "N3" if matches, otherwise full string
	    try {
	        int colon = s.indexOf(':');
	        if (colon > 0) {
	            String possibleNum = s.substring(0, colon).trim();
	            if (possibleNum.matches("\\d+")) {
	                return "N" + possibleNum;
	            }
	        }
	    } catch (Exception ignored) {}
	    // fallback: shorten long strings for readability
	    if (s.length() > 80) return s.substring(0, 77) + "...";
	    return s;
	}*/


}
