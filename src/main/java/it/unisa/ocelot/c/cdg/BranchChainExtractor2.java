package it.unisa.ocelot.c.cdg;

import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.cdt.core.dom.ast.*;

/**
 * Extracts branch chains from a CDG.
 *
 * <p><strong>Edge direction convention in this CDG implementation:</strong><br>
 * {@link ControlDependenceEdge} is stored as  dependent → condition,
 * i.e. the constructor is called as
 * {@code new ControlDependenceEdge(dependentNode, conditionNode, label)}.
 *
 * <p>As a result the CDG adjacency maps are oriented as follows:
 * <ul>
 *   <li>{@code cdg.outgoingEdgesOf(X)} – edges where X is the <b>dependent</b>
 *       (source). Returns the condition nodes that X is guarded by.</li>
 *   <li>{@code cdg.incomingEdgesOf(X)} – edges where X is the <b>condition</b>
 *       (target). Returns the dependent nodes that X controls.</li>
 *   <li>{@code cdg.getEdgeSource(e)} – the <em>dependent</em> node.</li>
 *   <li>{@code cdg.getEdgeTarget(e)} – the <em>condition</em> node.</li>
 * </ul>
 *
 * <p><strong>Key insight for CFG traversal:</strong><br>
 * When we walk a CFG edge  A → B  (A is the condition, B is a branch target),
 * the CDG stores this as  B → A  (B is dependent on A).
 * So to find the branch label for CFG step A→B we look for a CDG edge whose
 * <em>source is B</em> (dependent) and whose <em>target is A</em> (condition).
 */
public class BranchChainExtractor2 {

	private final CDG cdg;
	private List<BranchChain> branchChains;
	private final String unitComponentName;
	private final Map<IASTExpression, Integer> branchChainsMap;

	private final Map<CDGNode, Integer> localNodeIdMap = new HashMap<>();
	private int idCounter = 0;

	private final Map<CDGNode, Integer> branchIndexMap = new LinkedHashMap<>();

	public BranchChainExtractor2(CDG cdg, String unitComponentName,
			Map<IASTExpression, Integer> branchChainsMap) {
		this.cdg = cdg;
		this.branchChains = new ArrayList<>();
		this.unitComponentName = unitComponentName;
		this.branchChainsMap = branchChainsMap;
	}

	// -------------------------------------------------------------------------
	// Main entry point
	// -------------------------------------------------------------------------

	public List<BranchChain> extractBranchChains() throws Exception {
		this.branchChains.clear();
		this.localNodeIdMap.clear();
		this.idCounter = 0;

		buildBranchIndexMap();

		// Leaf nodes = nodes that control nothing = no node is dependent on them
		// = cdg.incomingEdgesOf(leaf) is empty (no dependents).
		List<CDGNode> leafNodes = cdg.vertexSet().stream()
				.filter(node -> node.isLeafNode(cdg))
				.collect(Collectors.toList());

		for (CDGNode leaf : leafNodes) {
			findPathsFromSrcToTarget(
					cdg.getEntryNode(),
					leaf,
					new ArrayList<PathStep>(),
					new HashSet<CDGNode>());
		}

		// Loop-exit (FALSE) paths as virtual leaves.
		for (CDGNode node : cdg.vertexSet()) {
			if (isLoopHeader(node)) {
				// incomingEdgesOf(condition) = its dependents.
				// The loop-exit dependent carries the FALSE label.
				for (ControlDependenceEdge edge : cdg.incomingEdgesOf(node)) {
					if (edge.toString().toUpperCase().contains("FALSE")) {
						// source = dependent = the loop-exit node
						CDGNode exitTarget = cdg.getEdgeSource(edge);
						findPathsToTarget(cdg.getEntryNode(), exitTarget,
								new ArrayList<>(), new HashSet<>());
					}
				}
			}
		}
		filter_BC_With_End_And_EndConditions();
		//branchChains =  branchChains.stream().filter(BranchChainExtractor2::hasBranchChainWithCondition).collect(Collectors.toList());
		//branchChains =  branchChains.stream().filter(BranchChainExtractor2::hasBranchChainWithEnd).collect(Collectors.toList());
		filterBranchChains();

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
	private void filter_BC_With_End_And_EndConditions() {
		List<BranchChain> filtered = new ArrayList<>();
		for (BranchChain chain : branchChains) {
			boolean leafIsEnd = chain.getLeafNode()
					.toString()
					.toUpperCase()
					.contains("END");
			if (!leafIsEnd) {
				filtered.add(chain);
			}
		}
		this.branchChains = filtered;

	}

	// -------------------------------------------------------------------------
	// Branch index map
	// -------------------------------------------------------------------------

	/**
	 * A condition node is the TARGET of CDG edges (dependent→condition).
	 * It appears in incomingEdgesOf() results.
	 * We identify condition nodes as those that have at least one dependent
	 * with a TRUE or FALSE label, i.e. at least one incoming TRUE/FALSE edge.
	 */
	private void buildBranchIndexMap() {
		branchIndexMap.clear();

		List<CDGNode> predicates = new ArrayList<>();
		for (CDGNode node : cdg.vertexSet()) {
			if (isConditionNode(node)) {
				predicates.add(node);
			}
		}

		predicates.sort(Comparator.comparingInt(CDGNode::getId));

		int index = 0;
		for (CDGNode node : predicates) {
			branchIndexMap.put(node, index++);
		}
	}

	/**
	 * A node is a condition (predicate) node if at least one other node is
	 * dependent on it with a TRUE or FALSE label.
	 *
	 * <p>In the dependent→condition convention, condition nodes appear as the
	 * TARGET (condition) of edges, so we check {@code cdg.incomingEdgesOf(node)}
	 * (the dependents of {@code node}) for TRUE/FALSE labels.
	 */
	private boolean isConditionNode(CDGNode node) {
		for (ControlDependenceEdge e : cdg.incomingEdgesOf(node)) {
			String lbl = e.toString().toUpperCase();
			if (lbl.contains("TRUE") || lbl.contains("FALSE")) return true;
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// resolveBranchId
	// -------------------------------------------------------------------------

	/**
	 * Returns a stable index for {@code conditionNode}.
	 * Always pass the condition node (CFG branch point), not the dependent.
	 */
	private int resolveBranchId(CDGNode conditionNode) {
		if (localNodeIdMap.containsKey(conditionNode))
			return localNodeIdMap.get(conditionNode);

		int assignedId = -1;

		if (branchIndexMap.containsKey(conditionNode)) {
			assignedId = branchIndexMap.get(conditionNode);
		}

		if (assignedId == -1) {
			IASTExpression expr = extractExpression(conditionNode);
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

		if (assignedId == -1) assignedId = idCounter++;

		localNodeIdMap.put(conditionNode, assignedId);
		return assignedId;
	}

	// -------------------------------------------------------------------------
	// isLoopHeader
	// -------------------------------------------------------------------------

	/**
	 * A loop condition node has a dependent that is itself — the loop body
	 * re-evaluates the condition, producing a self-referencing TRUE edge stored
	 * as:  conditionNode(dependent) → conditionNode(condition).
	 *
	 * <p>We detect this by scanning {@code cdg.incomingEdgesOf(node)} (dependents
	 * of node) for an edge whose source (dependent) is the node itself with TRUE.
	 */
	private boolean isLoopHeader(CDGNode node) {
		for (ControlDependenceEdge edge : cdg.incomingEdgesOf(node)) {
			if (cdg.getEdgeSource(edge).equals(node)
					&& edge.toString().toUpperCase().contains("TRUE")) {
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// findPathsFromSrcToTarget  (CFG-structural DFS)
	// -------------------------------------------------------------------------

	/**
	 * DFS over CFG successor lists ({@link CDGNode#successors}) from
	 * {@code current} to {@code target}.
	 *
	 * <p><strong>How branch labels are recovered for CFG edge A → B:</strong><br>
	 * The CDG stores the control dependency as  B(dependent) → A(condition).
	 * To find the label we call {@link #findCdgEdgeForCfgStep(CDGNode, CDGNode)}
	 * which looks for an edge whose <em>source is B</em> and <em>target is A</em>.
	 */
	private void findPathsFromSrcToTarget(
			CDGNode        current,
			CDGNode        target,
			List<PathStep> currentPath,
			Set<CDGNode>   pathVisited) throws Exception {

		if (current.equals(target)) {
			if (!currentPath.isEmpty() && hasLogicalBranch(currentPath)) {
				branchChains.add(new BranchChain(
						target,
						new ArrayList<>(currentPath),
						unitComponentName,
						0));
			}
			return;
		}

		if (pathVisited.contains(current)) return;
		pathVisited.add(current);

		for (int successorId : current.successors) {

			CDGNode successor = cdg.getNode(successorId);
			if (successor == null) continue;

			// CFG edge is:  current(condition) → successor(dependent)
			// CDG stores it as: successor(dependent) → current(condition)
			// So we look for the edge with source=successor, target=current.
			ControlDependenceEdge edge = findCdgEdgeForCfgStep(current, successor);

			if (current.equals(cdg.getEntryNode())
					&& isSpuriousFlow(edge, current)) {
				continue;
			}

			PathStep step = new PathStep(current, successor, edge);

			if (step.hasBranchCondition()) {
				// current is the condition node (the CFG branch point)
				int    branchId = resolveBranchId(current);
				String outcome  = edge.toString()
						.replace("'",  "")
						.replace(':',  '_')
						.replace('-',  '_')
						.trim()
						.toLowerCase();
				step.setBranchConditionLabel(
						unitComponentName + ":branch" + branchId + "-" + outcome);
			}

			currentPath.add(step);
			findPathsFromSrcToTarget(successor, target, currentPath, pathVisited);
			currentPath.remove(currentPath.size() - 1);
		}

		pathVisited.remove(current);
	}

	// -------------------------------------------------------------------------
	// findCdgEdgeForCfgStep
	// -------------------------------------------------------------------------

	/**
	 * Finds the CDG edge that encodes the branch label for CFG step
	 * {@code conditionNode} → {@code dependentNode}.
	 *
	 * <p>The CDG stores this as an edge with:
	 * <ul>
	 *   <li>source = {@code dependentNode}  (registered via {@code registerEdge}
	 *       as sourceId=dependentId)</li>
	 *   <li>target = {@code conditionNode}  (registered as targetId=conditionId)</li>
	 * </ul>
	 *
	 * <p>Lookup order:
	 * <ol>
	 *   <li>{@code cdg.outgoingEdgesOf(dependentNode)}: edges where the dependent
	 *       is the source. Check if any has {@code conditionNode} as its target.</li>
	 *   <li>{@code cdg.incomingEdgesOf(conditionNode)}: edges where the condition
	 *       is the target. Check if any has {@code dependentNode} as its source.
	 *       Defensive reverse lookup.</li>
	 *   <li>Synthesise a FLOW edge — no CDG dependency for this CFG step.</li>
	 * </ol>
	 *
	 * @param conditionNode the CFG branch node (A in A→B)
	 * @param dependentNode the CFG branch target (B in A→B)
	 * @return a non-null {@link ControlDependenceEdge}
	 */
	private ControlDependenceEdge findCdgEdgeForCfgStep(
			CDGNode conditionNode, CDGNode dependentNode) {

		// 1. outgoingEdgesOf(dependent): source=dependent, check target=condition
		for (ControlDependenceEdge e : cdg.outgoingEdgesOf(dependentNode)) {
			CDGNode target = cdg.getEdgeTarget(e);   // target = condition node
			if (target != null && target.equals(conditionNode)) {
				return e;
			}
		}

		// 2. incomingEdgesOf(condition): target=condition, check source=dependent
		for (ControlDependenceEdge e : cdg.incomingEdgesOf(conditionNode)) {
			CDGNode source = cdg.getEdgeSource(e);   // source = dependent node
			if (source != null && source.equals(dependentNode)) {
				return e;
			}
		}

		// 3. No CDG edge — sequential flow, no branch condition
		return new ControlDependenceEdge(dependentNode, conditionNode, "FLOW");
	}

	// -------------------------------------------------------------------------
	// findPathsToTarget  (CDG-edge traversal for loop-exit paths)
	// -------------------------------------------------------------------------

	/**
	 * Traverses CDG outgoing edges of {@code current} to find paths to
	 * {@code target}.
	 *
	 * <p>{@code cdg.outgoingEdgesOf(current)} returns edges where {@code current}
	 * is the dependent (source), and {@code cdg.getEdgeTarget(edge)} returns the
	 * condition node.  This traversal therefore walks from dependent nodes toward
	 * their condition guards — used specifically for loop-exit path collection.
	 */
	private void findPathsToTarget(CDGNode current, CDGNode target,
			List<PathStep> currentPath, Set<CDGNode> pathVisited) throws Exception {

		if (current.equals(target)) {
			if (!currentPath.isEmpty() && hasLogicalBranch(currentPath)) {
				branchChains.add(new BranchChain(
						target,
						new ArrayList<>(currentPath),
						unitComponentName,
						0));
			}
			return;
		}

		if (pathVisited.contains(current)) return;
		pathVisited.add(current);

		for (ControlDependenceEdge edge : cdg.outgoingEdgesOf(current)) {
			// target of edge = condition node (in dependent→condition convention)
			CDGNode conditionNode = cdg.getEdgeTarget(edge);

			if (current.equals(cdg.getEntryNode())
					&& isSpuriousFlow(edge, conditionNode)) continue;

			PathStep step = new PathStep(current, conditionNode, edge);

			if (step.hasBranchCondition()) {
				int    branchId = resolveBranchId(conditionNode);
				String outcome  = edge.toString()
						.replace("'",  "")
						.replace(':',  '_')
						.replace('-',  '_')
						.trim()
						.toLowerCase();
				step.setBranchConditionLabel(
						unitComponentName + ":branch" + branchId + "-" + outcome);
			}

			currentPath.add(step);
			findPathsToTarget(conditionNode, target, currentPath, pathVisited);
			currentPath.remove(currentPath.size() - 1);
		}

		pathVisited.remove(current);
	}

	// -------------------------------------------------------------------------
	// Text output
	// -------------------------------------------------------------------------

	public String extractBranchChainsText() {
		StringBuilder sb = new StringBuilder();
		sb.append("=========================Control Dependence Graph: ")
		.append(unitComponentName).append("=====================\n");
		sb.append("=== BRANCH-CHAIN ANALYSIS ===\n");
		sb.append("Total chains: ").append(branchChains.size()).append("\n\n");

		for (BranchChain chain : branchChains) {
			sb.append("Label: ").append(chain.getLabel()).append("\n");
			sb.append("  Path: ");
			for (PathStep step : chain.getPath()) {
				sb.append("Node[").append(step.getFrom().getId()).append("]");
				sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
			}
			sb.append("(LEAF)\n");
			sb.append("  Leaf content: ").append(chain.getLeafNode().toString())
			.append("\n\n");
		}

		sb.append("Extracted ").append(branchChains.size()).append(" branch-chains:\n");
		for (BranchChain chain : branchChains) {
			sb.append("  - ").append(chain.getLabel())
			.append(" (to leaf node ").append(chain.getLeafNode().getId())
			.append(")\n");
		}

		return sb.toString();
	}

	// -------------------------------------------------------------------------
	// Filtering
	// -------------------------------------------------------------------------

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
				if (candidate.getLeafNode().getId() == existing.getLeafNode().getId()
						&& isLogicalPrefix(candidate, existing)) {
					redundant = true;
					break;
				}
			}
			if (!redundant) finalChains.add(candidate);
		}
		this.branchChains = finalChains;
	}

	private boolean isLogicalPrefix(BranchChain small, BranchChain large) {
		List<String> s = small.getPath().stream()
				.filter(PathStep::hasBranchCondition)
				.map(PathStep::getBranchLabel)
				.collect(Collectors.toList());
		List<String> l = large.getPath().stream()
				.filter(PathStep::hasBranchCondition)
				.map(PathStep::getBranchLabel)
				.collect(Collectors.toList());
		if (s.size() >= l.size()) return false;
		for (int i = 0; i < s.size(); i++) {
			if (!s.get(i).equals(l.get(i))) return false;
		}
		return true;
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Spurious-flow guard for CFG-structural traversal.
	 *
	 * <p>A synthetic FLOW edge from the entry to a real condition node is
	 * spurious. A condition node has dependents, so
	 * {@code cdg.incomingEdgesOf(conditionNode).size() > 1} (at least the
	 * TRUE and FALSE dependents).
	 *
	 * @param edge      the edge about to be followed
	 * @param cfgSource the node we are stepping FROM (the potential condition)
	 */
	private boolean isSpuriousFlow(ControlDependenceEdge edge, CDGNode cfgSource) {
		return edge.toString().toUpperCase().contains("FLOW") &&
				cdg.incomingEdgesOf(cfgSource).size() > 1;
	}

	private boolean hasLogicalBranch(List<PathStep> path) {
		return path.stream().anyMatch(PathStep::hasBranchCondition);
	}

	private IASTExpression extractExpression(CDGNode node) {
		List<IASTNode> asts = node.getASTNodes();
		if (asts == null || asts.isEmpty()) return null;
		IASTNode n = asts.get(0);
		if (n instanceof IASTExpression)     return (IASTExpression) n;
		if (n instanceof IASTIfStatement)    return ((IASTIfStatement) n).getConditionExpression();
		if (n instanceof IASTWhileStatement) return ((IASTWhileStatement) n).getCondition();
		if (n instanceof IASTForStatement)   return ((IASTForStatement) n).getConditionExpression();
		return null;
	}
}