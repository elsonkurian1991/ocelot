package it.unisa.ocelot.c.cdg;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.*;

public class BranchChainExtractor {
	private final CDG cdg;
	private List<BranchChain> branchChains;
	private final String unitComponentName;
	private final Map<IASTExpression, Integer> branchChainsMap;

	// Internal state to ensure unique IDs per decision point
	private final Map<CDGNode, Integer> localNodeIdMap = new HashMap<>();
	private int idCounter = 0;

	// FIX-1: Inlined branch index map.
	// Built once at the start of extractBranchChains() by scanning all CDG
	// nodes that own at least one TRUE/FALSE outgoing edge.
	// Sorted by CDGNode.getId() for deterministic branch0, branch1, ... labels.
	// Completely self-contained — no changes required in CDG.java.
	private final Map<CDGNode, Integer> branchIndexMap = new LinkedHashMap<>();

	public BranchChainExtractor(CDG cdg, String unitComponentName, Map<IASTExpression, Integer> branchChainsMap) {
		this.cdg = cdg;
		this.branchChains = new ArrayList<>();
		this.unitComponentName = unitComponentName;
		this.branchChainsMap = branchChainsMap;
	}

	public List<BranchChain> extractBranchChains() throws Exception {
		this.branchChains.clear();
		this.localNodeIdMap.clear();
		this.idCounter = 0;

		// FIX-1: Build the branch index map before any path traversal so that
		// every predicate node has a stable, unique index for this run.
		buildBranchIndexMap();

		// 1. Process standard leaf nodes
		List<CDGNode> leafNodes = cdg.vertexSet().stream()
				.filter(node -> node.isLeafNode(cdg))
				.collect(Collectors.toList());

		for (CDGNode leaf : leafNodes) {
			//findPathsToTarget(cdg.getEntryNode(), leaf, new ArrayList<>(), new HashSet<>());
			findPathsFromSrcToTarget(
				    cdg.getEntryNode(),
				    leaf,
				    new ArrayList<PathStep>(),
				    new HashSet<CDGNode>());
		}

		// 2. Process Loop Exit (FALSE) paths as virtual leaves
		//
		// FIX-2: Original isLoopHeader() checked the CDGNode's AST type for
		// IASTForStatement/IASTWhileStatement/IASTDoStatement.  This always
		// failed because loop *condition* CDG nodes carry the condition
		// expression (e.g. "k < LOOP_MAX"), not the loop statement wrapper.
		// isLoopHeader() now uses a structural self-loop check instead.
		for (CDGNode node : cdg.vertexSet()) {
			if (isLoopHeader(node)) {
				for (ControlDependenceEdge edge : cdg.outgoingEdgesOf(node)) {
					if (edge.toString().toUpperCase().contains("FALSE")) {
						CDGNode exitTarget = cdg.getEdgeTarget(edge);
						findPathsToTarget(cdg.getEntryNode(), exitTarget, new ArrayList<>(), new HashSet<>());
					}
				}
			}
		}

		// 3. Prune redundant paths while preserving target-specific outcomes
		filterBranchChains();

		// 4. Assign global chain labels
		for (int i = 0; i < branchChains.size(); i++) {
			branchChains.get(i).setLabel(unitComponentName, i + 1);
		}

		return branchChains;
	}

	/**
	 * Finds all CFG-structural paths from {@code entryNode} to {@code leaf},
	 * walking {@link CDGNode#successors} (control-flow order) instead of CDG
	 * outgoing edges.
	 *
	 * <p>Purpose: {@link #findPathsToTarget} only traverses CDG dependency edges,
	 * so it misses nodes that are CFG-reachable but carry no outgoing CDG edge
	 * (e.g. straight-line statements between two branch points, or nodes whose
	 * only incoming CDG edge was from the virtual entry).  This method closes
	 * that gap by following the original control-flow successor lists stored in
	 * each {@link CDGNode}.
	 *
	 * <p>For each CFG edge (current → successor) encountered during traversal,
	 * the method looks up whether a real {@link ControlDependenceEdge} exists
	 * between those two CDG nodes.  If one exists it is used directly, so branch
	 * labels ("TRUE"/"FALSE"/case) are preserved exactly as in
	 * {@link #findPathsToTarget}.  If no CDG edge exists the step is recorded
	 * with a synthetic FLOW edge (label "FLOW"), which {@link PathStep#hasBranchCondition}
	 * will correctly mark as non-branching.
	 *
	 * <p>The cycle guard, spurious-flow filter, branch-condition labelling, and
	 * {@link #hasLogicalBranch} acceptance check are all identical to
	 * {@link #findPathsToTarget}.
	 *
	 * @param current      node currently being visited (starts as entry)
	 * @param target       leaf node we are searching for
	 * @param currentPath  path accumulated so far (mutable, backtracked on return)
	 * @param pathVisited  set of nodes on the current DFS stack (cycle guard)
	 */
	@SuppressWarnings("unchecked")
	private void findPathsFromSrcToTarget(
	        CDGNode                  current,
	        CDGNode                  target,
	        List<PathStep>           currentPath,
	        Set<CDGNode>             pathVisited) throws Exception {

	    // ------------------------------------------------------------------ //
	    // Base case: reached the target leaf                                   //
	    // ------------------------------------------------------------------ //
	    if (current.equals(target)) {
	        // Only record paths that contain at least one real branch decision.
	        // This mirrors the guard in findPathsToTarget and avoids recording
	        // pure sequential (FLOW-only) paths as branch chains.
	        if (!currentPath.isEmpty() && hasLogicalBranch(currentPath)) {
	            branchChains.add(
	                new BranchChain(target,
	                                new ArrayList<>(currentPath),
	                                unitComponentName,
	                                0));
	        }
	        return;
	    }

	    // ------------------------------------------------------------------ //
	    // Cycle guard: do not re-visit a node already on the current DFS path //
	    // ------------------------------------------------------------------ //
	    if (pathVisited.contains(current)) return;
	    pathVisited.add(current);

	    // ------------------------------------------------------------------ //
	    // Iterate over CFG successors (structural flow, not CDG edges)         //
	    // ------------------------------------------------------------------ //
	    for (int successorId : current.successors) {

	        CDGNode successor = cdg.getNode(successorId);
	        if (successor == null) continue;   // safety: unknown node id

	        // -------------------------------------------------------------- //
	        // Look up the best ControlDependenceEdge for (current → successor) //
	        //                                                                  //
	        // Priority:                                                        //
	        //   1. A real CDG outgoing edge from `current` whose target is     //
	        //      `successor`  →  preserves TRUE/FALSE/case labels exactly.   //
	        //   2. A real CDG incoming edge of `successor` whose source is     //
	        //      `current`    →  same information, reverse lookup.           //
	        //   3. A synthetic FLOW edge                →  marks the step as   //
	        //      non-branching (hasBranchCondition = false).                 //
	        // -------------------------------------------------------------- //
	        ControlDependenceEdge edge = findCdgEdgeBetween(current, successor);

	        // -------------------------------------------------------------- //
	        // Spurious-flow guard (mirrors findPathsToTarget)                  //
	        // Skip a FLOW edge FROM the entry node when the successor has      //
	        // more than one incoming CDG edge – these are structural flows     //
	        // that would create duplicate / misleading branch chains.          //
	        // -------------------------------------------------------------- //
	        if (current.equals(cdg.getEntryNode())
	                && isSpuriousFlow(edge, successor)) {
	            continue;
	        }

	        // -------------------------------------------------------------- //
	        // Build the PathStep and optionally attach a branch-condition label //
	        // -------------------------------------------------------------- //
	        PathStep step = new PathStep(current, successor, edge);

	        if (step.hasBranchCondition()) {
	            int    branchId = resolveBranchId(current);
	            // Normalise the edge label to a URL-safe token
	            // (mirrors the label normalisation in findPathsToTarget)
	            String outcome  = edge.toString()
	                                  .replace("'",  "")
	                                  .replace(':',  '_')
	                                  .replace('-',  '_')
	                                  .trim()
	                                  .toLowerCase();
	            step.setBranchConditionLabel(
	                unitComponentName + ":branch" + branchId + "-" + outcome);
	        }

	        // -------------------------------------------------------------- //
	        // Recurse (DFS)                                                    //
	        // -------------------------------------------------------------- //
	        currentPath.add(step);
	        findPathsFromSrcToTarget(successor, target, currentPath, pathVisited);
	        currentPath.remove(currentPath.size() - 1);   // backtrack
	    }

	    // ------------------------------------------------------------------ //
	    // Remove current from the visited set on the way back (backtracking)  //
	    // ------------------------------------------------------------------ //
	    pathVisited.remove(current);
	}

	/**
	 * Returns the {@link ControlDependenceEdge} that connects {@code from} to
	 * {@code to} in the CDG, or a synthetic FLOW edge if no such CDG edge exists.
	 *
	 * <p>Lookup strategy (fastest first):
	 * <ol>
	 *   <li>Scan {@code cdg.outgoingEdgesOf(from)} for an edge whose target is
	 *       {@code to}.  This is O(out-degree of {@code from}) and covers the
	 *       common case.</li>
	 *   <li>Scan {@code cdg.incomingEdgesOf(to)} for an edge whose source is
	 *       {@code from}.  Useful when the CDG edge was registered in the
	 *       reverse direction (defensive).</li>
	 *   <li>Synthesise a FLOW edge with no backing {@link LabeledEdge}.</li>
	 * </ol>
	 *
	 * @param from source CDGNode
	 * @param to   target CDGNode
	 * @return a non-null {@link ControlDependenceEdge}
	 */
	private ControlDependenceEdge findCdgEdgeBetween(CDGNode from, CDGNode to) {
	    // 1. Forward lookup: outgoing edges of `from`
	    for (ControlDependenceEdge e : cdg.outgoingEdgesOf(from)) {
	        CDGNode edgeTgt = cdg.getEdgeTarget(e);
	        if (edgeTgt != null && edgeTgt.equals(to)) {
	            return e;
	        }
	    }

	    // 2. Reverse lookup: incoming edges of `to`
	    for (ControlDependenceEdge e : cdg.incomingEdgesOf(to)) {
	        CDGNode edgeSrc = cdg.getEdgeSource(e);
	        if (edgeSrc != null && edgeSrc.equals(from)) {
	            return e;
	        }
	    }

	    // 3. Synthesise a FLOW edge – no CDG dependency exists for this CFG step
	    return new ControlDependenceEdge(to, from, "FLOW");
	}


	/**
	 * Scans every CDGNode in the graph. Any node that has at least one
	 * outgoing TRUE or FALSE edge is a predicate (branch) node and receives
	 * a unique, sequential index: 0, 1, 2, ...
	 *
	 * Nodes are sorted by CDGNode.getId() before indexing so the assignment
	 * is deterministic across runs and matches the CDG dump order.
	 *
	 * Called once at the top of extractBranchChains(), before any traversal.
	 */
	private void buildBranchIndexMap() {
		branchIndexMap.clear();

		List<CDGNode> predicates = new ArrayList<>();
		for (CDGNode node : cdg.vertexSet()) {
			if (hasTrueFalseEdge(node)) {
				predicates.add(node);
			}
		}

		// Sort by node ID for stable, deterministic index assignment
		predicates.sort(Comparator.comparingInt(CDGNode::getId));

		int index = 0;
		for (CDGNode node : predicates) {
			branchIndexMap.put(node, index++);
		}
	}

	/** Returns true if the node has at least one TRUE or FALSE outgoing edge. */
	private boolean hasTrueFalseEdge(CDGNode node) {
		for (ControlDependenceEdge e : cdg.outgoingEdgesOf(node)) {
			String lbl = e.toString().toUpperCase();
			if (lbl.contains("TRUE") || lbl.contains("FALSE")) return true;
		}
		
		return false;
	}

	// -------------------------------------------------------------------------
	// FIX-1: resolveBranchId — uses inlined branchIndexMap as priority source
	// -------------------------------------------------------------------------

	/**
	 * Returns a stable, unique branch index for the given CDGNode.
	 *
	 * Priority order:
	 *  1. branchIndexMap (built at start of run) — unique per predicate node,
	 *     never collides.
	 *  2. branchChainsMap lookup by AST raw signature — legacy path, kept for
	 *     backward compatibility.
	 *  3. idCounter fallback — last resort only.
	 *
	 * localNodeIdMap caches the result so the same node always returns the
	 * same ID within one extraction run no matter how many times it is visited.
	 */
	private int resolveBranchId(CDGNode node) {
		// Cache hit — same node seen before in this run
		if (localNodeIdMap.containsKey(node)) return localNodeIdMap.get(node);

		int assignedId = -1;

		// Priority 1: inlined branch index map (always correct, always unique)
		if (branchIndexMap.containsKey(node)) {
			assignedId = branchIndexMap.get(node);
		}

		// Priority 2: legacy branchChainsMap lookup by AST raw signature
		if (assignedId == -1) {
			IASTExpression expr = extractExpression(node);
			if (expr != null && branchChainsMap != null) {
				String rawSig = expr.getRawSignature();
				for (Map.Entry<IASTExpression, Integer> entry : branchChainsMap.entrySet()) {
					if (entry.getKey().getRawSignature().equals(rawSig)) {
						assignedId = entry.getValue();
						break;
					}
				}
			}
		}

		// Priority 3: fallback counter (last resort)
		if (assignedId == -1) assignedId = idCounter++;

		localNodeIdMap.put(node, assignedId);
		return assignedId;
	}

	// -------------------------------------------------------------------------
	// FIX-2: isLoopHeader — structural self-loop detection
	// -------------------------------------------------------------------------

	/**
	 * Returns true if the given CDGNode is a loop condition node.
	 *
	 * Original implementation checked whether the CDGNode's first AST child
	 * was an IASTForStatement/IASTWhileStatement/IASTDoStatement — always
	 * false for loop condition nodes which carry the condition expression.
	 *
	 * Correct detection: a loop condition node has a self-loop TRUE edge in
	 * the CDG (outgoing TRUE edge whose target is the node itself).
	 * This structural invariant is always present for for/while/do-while loops.
	 */
	private boolean isLoopHeader(CDGNode node) {
		for (ControlDependenceEdge edge : cdg.outgoingEdgesOf(node)) {
			if (cdg.getEdgeTarget(edge).equals(node)
					&& edge.toString().toUpperCase().contains("TRUE")) {
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// Unchanged methods below — not modified
	// -------------------------------------------------------------------------

	private void findPathsToTarget(CDGNode current, CDGNode target,
			List<PathStep> currentPath, Set<CDGNode> pathVisited) throws Exception {
		if (current.equals(target)) {
			if (!currentPath.isEmpty() && hasLogicalBranch(currentPath)) {
				branchChains.add(new BranchChain(target, new ArrayList<>(currentPath), unitComponentName, 0));
			}
			return;
		}

		if (pathVisited.contains(current)) return;
		pathVisited.add(current);

		for (ControlDependenceEdge edge : cdg.outgoingEdgesOf(current)) {
			CDGNode successor = cdg.getEdgeTarget(edge);

			if (current.equals(cdg.getEntryNode()) && isSpuriousFlow(edge, successor)) continue;

			PathStep step = new PathStep(current, successor, edge);

			if (step.hasBranchCondition()) {
				int branchId = resolveBranchId(current);
				String outcome = edge.toString().replace("'", "").replace(':', '_').replace('-', '_').trim().toLowerCase();
				step.setBranchConditionLabel(unitComponentName + ":branch" + branchId + "-" + outcome);
			}

			currentPath.add(step);
			findPathsToTarget(successor, target, currentPath, pathVisited);
			currentPath.remove(currentPath.size() - 1);
		}
		pathVisited.remove(current);
	}

	public String extractBranchChainsText() {
		StringBuilder sb = new StringBuilder();
		sb.append("=========================Control Dependence Graph: ").append(unitComponentName).append("=====================\n");
		sb.append("=== BRANCH-CHAIN ANALYSIS ===\n");
		sb.append("Total chains: ").append(branchChains.size()).append("\n\n");

		for (BranchChain chain : branchChains) {
			sb.append("Label: ").append(chain.getLabel()).append("\n");
			sb.append("  Path: ");

			List<PathStep> steps = chain.getPath();
			for (PathStep step : steps) {
				sb.append("Node[").append(step.getFrom().getId()).append("]");
				sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
			}
			sb.append("(LEAF)\n");
			sb.append("  Leaf content: ").append(chain.getLeafNode().toString()).append("\n\n");
		}

		sb.append("Extracted ").append(branchChains.size()).append(" branch-chains:\n");
		for (BranchChain chain : branchChains) {
			sb.append("  - ").append(chain.getLabel())
			.append(" (to leaf node ").append(chain.getLeafNode().getId()).append(")\n");
		}

		return sb.toString();
	}

	private void filterBranchChains() {
		if (branchChains.isEmpty()) return;

		Map<String, BranchChain> uniquePaths = new HashMap<>();
		for (BranchChain chain : branchChains) {
			String logicSig = chain.getPath().stream()
					.filter(PathStep::hasBranchCondition)
					.map(PathStep::getBranchLabel)
					.collect(Collectors.joining("->"));

			String fullSig = logicSig + "|Target:" + chain.getLeafNode().getId();

			if (!uniquePaths.containsKey(fullSig) ||
					chain.getPath().size() > uniquePaths.get(fullSig).getPath().size()) {
				uniquePaths.put(fullSig, chain);
			}
		}

		List<BranchChain> filtered = new ArrayList<>(uniquePaths.values());
		filtered.sort((a, b) -> Integer.compare(b.getPath().size(), a.getPath().size()));

		List<BranchChain> finalChains = new ArrayList<>();
		for (BranchChain candidate : filtered) {
			boolean redundant = false;
			for (BranchChain existing : finalChains) {
				if (candidate.getLeafNode().getId() == existing.getLeafNode().getId() &&
						isLogicalPrefix(candidate, existing)) {
					redundant = true;
					break;
				}
			}
			if (!redundant) finalChains.add(candidate);
		}
		this.branchChains = finalChains;
	}

	private boolean isLogicalPrefix(BranchChain small, BranchChain large) {
		List<String> s = small.getPath().stream().filter(PathStep::hasBranchCondition).map(PathStep::getBranchLabel).collect(Collectors.toList());
		List<String> l = large.getPath().stream().filter(PathStep::hasBranchCondition).map(PathStep::getBranchLabel).collect(Collectors.toList());
		if (s.size() >= l.size()) return false;
		for (int i = 0; i < s.size(); i++) {
			if (!s.get(i).equals(l.get(i))) return false;
		}
		return true;
	}

	private boolean isSpuriousFlow(ControlDependenceEdge edge, CDGNode successor) {
		return edge.toString().toUpperCase().contains("FLOW") &&
				cdg.incomingEdgesOf(successor).size() > 1;
	}

	private boolean hasLogicalBranch(List<PathStep> path) {
		return path.stream().anyMatch(PathStep::hasBranchCondition);
	}

	private IASTExpression extractExpression(CDGNode node) {
		List<IASTNode> asts = node.getASTNodes();
		if (asts == null || asts.isEmpty()) return null;
		IASTNode n = asts.get(0);
		if (n instanceof IASTExpression) return (IASTExpression) n;
		if (n instanceof IASTIfStatement) return ((IASTIfStatement) n).getConditionExpression();
		if (n instanceof IASTWhileStatement) return ((IASTWhileStatement) n).getCondition();
		if (n instanceof IASTForStatement) return ((IASTForStatement) n).getConditionExpression();
		return null;
	}
}