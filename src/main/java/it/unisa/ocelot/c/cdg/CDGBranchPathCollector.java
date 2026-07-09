package it.unisa.ocelot.c.cdg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

/**
 * Traverses a {@link CDG} depth-first and collects every root-to-leaf path as a
 * {@link BranchChain}. A {@link PathStep} is appended to the current path only
 * when a branch edge (TRUE / FALSE / case label) is traversed. Per-branch cycle
 * detection is performed via an ancestor id stack so that loop back-edges do
 * not cause infinite recursion.
 */
public class CDGBranchPathCollector {

	// -----------------------------------------------------------------------
	// Fields
	// -----------------------------------------------------------------------

	private final CDG cdg;
	private final String unitComponentName;
	private final Map<String, List<BranchChain>> allBranchChains;

	private static enum BranchType {
		FLOW, IF_COND, FOR_LOOP, SWITCH_CASE, WHILE_LOOP, DO_WHILE_LOOP
	}

	/**
	 * Monotonically increasing counter used to generate unique branch-condition
	 * labels across the entire traversal, e.g. {@code fun1:condition2-TRUE}.
	 */
	private int conditionCounter = -1;

	// -----------------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------------

	public CDGBranchPathCollector(CDG cdg, String unitComponentName, Map<String, List<BranchChain>> allBranchChains) {

		this.cdg = cdg;
		this.unitComponentName = unitComponentName;
		this.allBranchChains = allBranchChains;

		this.allBranchChains.clear();
	}

	// -----------------------------------------------------------------------
	// Public entry point
	// -----------------------------------------------------------------------

	/**
	 * Starts the depth-first traversal from the CDG entry node and populates
	 * {@link #allBranchChains} with one {@link BranchChain} per root-to-leaf path.
	 * Sequential labels are assigned after collection.
	 */
	public void collect() {
		CDGNode root = cdg.getEntryNode();
		if (root == null) {
			throw new IllegalStateException("CDG entry node is null");
		}

		visit(root, null, null, new ArrayList<>(), new ArrayDeque<>(), new HashSet<>(), BranchType.FLOW, conditionCounter);

		List<BranchChain> chains = allBranchChains.get(unitComponentName);
		if (chains != null) {
			for (int i = 0; i < chains.size(); i++) {
				chains.get(i).setLabel(unitComponentName, i + 1);
			}
		}
	}

	// -----------------------------------------------------------------------
	// Core recursive visitor
	// -----------------------------------------------------------------------

	/**
	 * Visits {@code node} depth-first.
	 *
	 * <p>
	 * The {@code astContext} parameter decides which branch-handling logic runs for
	 * this node:
	 * <ul>
	 * <li>{@link IASTIfStatement} → TRUE + FALSE (synthesised if absent)</li>
	 * <li>{@link IASTForStatement} / {@link IASTWhileStatement} → TRUE (body) +
	 * FALSE (exit)</li>
	 * <li>{@link IASTSwitchStatement} → all case edges as branches</li>
	 * <li>{@code null} / other → plain flow-through</li>
	 * </ul>
	 *
	 * @param node          CDG node being visited
	 * @param incomingEdge  edge that led to this node ({@code null} at root)
	 * @param astContext    AST construct resolved at the call site ({@code null}
	 *                      for flow-through nodes and the root)
	 * @param currentPath   accumulated {@link PathStep}s from root to here
	 * @param ancestorStack ids of nodes on the current DFS stack (cycle guard)
	 * @param ancestorSet   companion HashSet for O(1) cycle detection
	 */
	private void visit(CDGNode node, ControlDependenceEdge incomingEdge, IASTNode astContext,
			List<PathStep> currentPath, Deque<Integer> ancestorStack, Set<Integer> ancestorSet, BranchType branchType, int branchCounter) {
		System.out.println("VISIT node=" + node.id + " incomingEdge=" + incomingEdge + " astContext="
				+ (astContext == null ? "NULL" : astContext.getClass().getSimpleName()) + " ancestorStack="
				+ ancestorStack);

		boolean addedStep = false;
		// 3. If we arrived via a branch edge, append a PathStep
		if (node!= cdg.getEntryNode()) { // if not a root there is always an incoming edge throw exp?
			// throw new IllegalStateException("Unexpected non-root node with null incoming
			// edge: " + node.id);
			// TODO if not a root there is always an incoming edge throw exp?

			CDGNode conditionNode = cdg.getEdgeSource(incomingEdge);
			if(conditionNode == null) { // for flow-through edges or if the source node is null for some reason, we don't label the step since it doesn't represent a real branch (e.g. the root node has no incoming edge and we synthesise a FLOW step for it, but we don't want to label it as a branch)
				conditionNode = cdg.getEntryNode(); // for the root node, which has no incoming edge, use itself as the condition source for labelling purposes (the step will be ignored anyway since it's the root)
				PathStep step = new PathStep(conditionNode, node, incomingEdge);

				//step.setBranchConditionLabel(buildCondit3ionLabel(conditionNode, incomingEdge, branchType));
				currentPath.add(step);
			}
			else {	 // skip adding a step for the root since it doesn't represent a real branch
				//conditionNode = cdg.getEntryNode(); // for the root node, which has no incoming edge, use itself as the condition source for labelling purposes (the step will be ignored anyway since it's the root)
				PathStep step = new PathStep(conditionNode, node, incomingEdge);

				step.setBranchConditionLabel(buildConditionLabel(conditionNode, incomingEdge, branchType, branchCounter));
				currentPath.add(step);
			}
			addedStep = true;
		}
		// 1. Cycle guard — stop on back-edges (loop bodies)
		if (ancestorSet.contains(node.id)) {
			finaliseBranchChain(currentPath);
			if(addedStep) currentPath.remove(currentPath.size() - 1); // remove the step we just added since we are not going to traverse this node again
			return; // hash?
		}
		// 2. Push onto ancestor stack for the duration of this branch
		ancestorStack.push(node.id);
		ancestorSet.add(node.id);
		// 4. Collect outgoing CDG edges
		List<ControlDependenceEdge> outgoingEdges = cdg.outgoingEdgesOf(node);

		// 5. Dispatch on astContext
		if (outgoingEdges.isEmpty()) {
			// ---- LEAF ----
			finaliseBranchChain(currentPath);

		} else if (node.isCondition) {
			if (isSwitchNode(node)) {
				handleSwitchNode(node, null, outgoingEdges, currentPath, ancestorStack, ancestorSet);
			} else {
				handleBranchNode(node, null, outgoingEdges, currentPath, ancestorStack, ancestorSet);
			}
		} else {
			// ---- FLOW-THROUGH ----
			handleFlowNode(node, outgoingEdges, currentPath, ancestorStack, ancestorSet);
		}

		/*
		 * else if (astContext instanceof IASTIfStatement || astContext instanceof
		 * IASTForStatement) { // ---- IF | FOR LOOP ---- handleBranchNode(node,
		 * astContext, outgoingEdges, currentPath, ancestorStack);
		 * 
		 * } else if (astContext instanceof IASTSwitchStatement) { // ---- SWITCH ----
		 * handleSwitchNode(node, (IASTSwitchStatement) astContext, outgoingEdges,
		 * currentPath, ancestorStack);
		 * 
		 * } else if (astContext instanceof IASTWhileStatement || astContext instanceof
		 * IASTDoStatement) { throw new
		 * IllegalStateException("Unexpected AST context type: " +
		 * astContext.getClass().getSimpleName()); }
		 * 
		 * else {
		 * 
		 * // ---- FLOW-THROUGH ---- handleFlowNode(node, outgoingEdges, currentPath,
		 * ancestorStack); }
		 */
		// 6. Backtrack: remove step added on entry
		/*if (node!= cdg.getEntryNode() && !currentPath.isEmpty()) {
			currentPath.remove(currentPath.size() - 1);
		}*/
		if(addedStep) currentPath.remove(currentPath.size() - 1); // remove the step we just added since we are done traversing this node

		// 7. Pop ancestor stack
		ancestorStack.pop();
		ancestorSet.remove(node.id);
	}

	// -----------------------------------------------------------------------
	// Per-construct handlers
	// -----------------------------------------------------------------------
	/**
	 * Handles if/for/while condition nodes. TRUE edge → then-branch / loop-body.
	 * FALSE edge → else-branch / loop-exit. Missing TRUE or FALSE → synthesise it.
	 */
	/**
	 * Handles an {@code if} node. Handles a {@code for} / {@code while} node. TRUE
	 * = loop body, FALSE = loop exit.
	 * <ul>
	 * <li>TRUE edge → recurse into the then-branch.</li>
	 * <li>FALSE edge → recurse into the else-branch if present; otherwise
	 * synthesise a FALSE {@link PathStep} and finalise immediately
	 * (if-with-no-else: the condition was false and the body was skipped).</li> *
	 * <li>Loop nodes: TRUE edge → loop body, FALSE edge → loop exit.</li>
	 * </ul>
	 */
	private void handleBranchNode(CDGNode node, IASTNode astContext, List<ControlDependenceEdge> outgoingEdges,
			List<PathStep> currentPath, Deque<Integer> ancestorStack, Set<Integer> ancestorSet) {
		boolean trueEdge = false;
		boolean falseEdge = false;
		int myCounter= ++conditionCounter;
		for (ControlDependenceEdge e : outgoingEdges) {
			if ("TRUE".equals(e.toString()))
				trueEdge = true;
			else if ("FALSE".equals(e.toString()))
				falseEdge = true;

			CDGNode child = cdg.getEdgeTarget(e);
			if (child == null) {
				throw new IllegalStateException(e + " edge from node " + node.id + " has null target");
			}
			if (child.id == node.id)
				continue; // skip self-loop back-edges to avoid infinite recursion and redundant paths
			// (e.g. from a loop body back to the condition) ***
			// sibling branches get the same base number in their labels (e.g.
			// condition3-TRUE and condition3-FALSE)
			// : skip self-loops (loop back-edges) — they don't produce a new path step ***

			// System.err.println("handleBranchNode: node=" + node.id + " edges=" +
			// outgoingEdges.size() + " falseEdge=" + falseEdge);
			IASTNode childAst = resolveASTParent(child);
			visit(child, e, childAst, new ArrayList<>(currentPath), copyStack(ancestorStack),
					new HashSet<>(ancestorSet), BranchType.IF_COND,myCounter);
		}

		if (!falseEdge) {
			synthesiseBranch(node, currentPath, myCounter, false);
		} else if (!trueEdge) {
			synthesiseBranch(node, currentPath, myCounter, true);
		}

	}

	/**
	 * Handles a {@code switch} node. Every outgoing edge is a taken-case branch;
	 * there is no FALSE edge.
	 */
	private void handleSwitchNode(CDGNode node, IASTSwitchStatement ast, List<ControlDependenceEdge> outgoingEdges,
			List<PathStep> currentPath, Deque<Integer> ancestorStack, Set<Integer> ancestorSet) {

		for (ControlDependenceEdge e : outgoingEdges) {

			CDGNode child = cdg.getEdgeTarget(e);
			if (child == null) {
				throw new IllegalStateException("Edge from node " + node.id + " has null target");
			}
			++conditionCounter;// we consider every case edge as a separate branch, so we need to

			IASTNode childAst = resolveASTParent(child);
			visit(child, e, childAst, new ArrayList<>(currentPath), copyStack(ancestorStack),
					new HashSet<>(ancestorSet), BranchType.SWITCH_CASE,conditionCounter);
		}
	}

	/**
	 * Handles a plain flow-through node. No {@link PathStep} is added; traversal
	 * continues into children with {@code incomingEdge = null}.
	 */
	private void handleFlowNode(CDGNode node, List<ControlDependenceEdge> outgoing, List<PathStep> currentPath,
			Deque<Integer> ancestorStack, Set<Integer> ancestorSet) {

		for (ControlDependenceEdge e : outgoing) {
			CDGNode child = cdg.getEdgeTarget(e);
			if (child == null) {
				throw new IllegalStateException("Edge from node " + node.id + " has null target");
			}
			// IASTNode childAst = resolveASTParent(child);
			// resolve astContext from the CHILD's own leading AST node directly ***
			// IASTNode childAst = child.getLeadingASTNode();
			// System.err.println("FLOW child=" + child.id + " childAst=" + (childAst ==
			// null ? "NULL" : childAst.getClass().getSimpleName()));
			visit(child, null, null, new ArrayList<>(currentPath), copyStack(ancestorStack), new HashSet<>(ancestorSet),
					BranchType.FLOW,conditionCounter);
		}
	}
	//visit(CDGNode node, ControlDependenceEdge incomingEdge, IASTNode astContext,	List<PathStep> currentPath, Deque<Integer> ancestorStack) {
	// -----------------------------------------------------------------------
	// Leaf finalisation
	// -----------------------------------------------------------------------

	/**
	 * Snapshots the current path and stores it as a new {@link BranchChain}. Chain
	 * number is set to -1 here; sequential numbering is applied after all chains
	 * are collected in {@link #collect}.
	 */
	private void finaliseBranchChain(List<PathStep> currentPath) {
		if (currentPath.isEmpty())
			return;
		// throw new IllegalStateException("Attempting to finalise branch chain with
		// empty path");
		List<PathStep> snapshot = new ArrayList<>(currentPath);
		CDGNode leaf = snapshot.get(snapshot.size() - 1).getTo();
		if (leaf == null) {
			leaf = snapshot.get(snapshot.size() - 1).getFrom();
		}

		BranchChain chain = new BranchChain(leaf, snapshot, unitComponentName, -1);
		allBranchChains.putIfAbsent(unitComponentName, new ArrayList<>());
		allBranchChains.get(unitComponentName).add(chain);
	}

	/**
	 * Synthesises a FALSE {@link PathStep} for an if-with-no-else node and
	 * immediately finalises the chain.
	 *
	 * <p>
	 * The synthesised step records that the condition evaluated to FALSE and
	 * execution fell through without entering the if-body. The if-node itself acts
	 * as both the condition source and the leaf.
	 *
	 * @param ifNode      the if-condition CDG node
	 * @param currentPath the path accumulated so far (not yet containing the FALSE
	 *                    step)
	 */
	private void synthesiseBranch(CDGNode node, List<PathStep> currentPath, int myCounter, boolean condition) {

		// Build a synthetic FALSE edge (no real ControlDependenceEdge exists)
		String conditionLabel = (condition ? "TRUE" : "FALSE");
		CDGNode exitNode = cdg.getNode(cdg.getExitId());// jsut use the exit as a dummy target for the synthesised step

		PathStep syntheticStep = new PathStep(node, exitNode, null);

		syntheticStep.setBranchConditionLabel(unitComponentName + ":branch" + myCounter + "-" + conditionLabel.toLowerCase());

		List<PathStep> snapshot = new ArrayList<>(currentPath);
		snapshot.add(syntheticStep);

		BranchChain chain = new BranchChain(node, snapshot, unitComponentName, -1);
		allBranchChains.putIfAbsent(unitComponentName, new ArrayList<>());
		allBranchChains.get(unitComponentName).add(chain);
	}
	// -----------------------------------------------------------------------
	// Switch detection
	// -----------------------------------------------------------------------

	/**
	 * Detects switch nodes reliably using the CDGNode.isCondition flag and checking
	 * if all outgoing edges are non-TRUE/FALSE (i.e. case labels).
	 */
	private boolean isSwitchNode(CDGNode node) {
		if (!node.isCondition)
			return false;
		List<ControlDependenceEdge> outgoing = cdg.outgoingEdgesOf(node);
		if (outgoing.isEmpty())
			return false;
		// A switch node has no TRUE/FALSE edges — only case-label edges
		for (ControlDependenceEdge e : outgoing) {
			String lbl = e.toString();
			if ("TRUE".equals(lbl) || "FALSE".equals(lbl) || lbl.equals("FLOW"))
				return false;
		}
		return true;
	}

	// -----------------------------------------------------------------------
	// AST parent resolution
	// -----------------------------------------------------------------------

	/**
	 * Resolves the nearest enclosing control-flow AST construct for a CDG node by
	 * walking up the AST parent chain from the node's leading {@link IASTNode}.
	 *
	 * @param node the CDG node to inspect
	 * @return the nearest {@link IASTIfStatement}, {@link IASTForStatement},
	 *         {@link IASTWhileStatement}, or {@link IASTSwitchStatement};
	 *         {@code null} if none is found
	 */
	private IASTNode resolveASTParent(CDGNode node) {
		IASTNode astNode = node.getLeadingASTNode();
		if (astNode == null)
			return null;
		/*
		 * while (astNode != null) { if (astNode instanceof IASTIfStatement || astNode
		 * instanceof IASTForStatement || astNode instanceof IASTWhileStatement ||
		 * astNode instanceof IASTSwitchStatement) { return astNode; } astNode =
		 * astNode.getParent(); }
		 */
		if (astNode instanceof IASTIfStatement || astNode instanceof IASTForStatement
				|| astNode instanceof IASTWhileStatement || astNode instanceof IASTSwitchStatement) {
			return astNode;
		}
		return null;
	}

	// -----------------------------------------------------------------------
	// Branch-condition label generation
	// -----------------------------------------------------------------------

	/**
	 * Builds a unique human-readable label for the branch direction encoded in
	 * {@code edge}, e.g. {@code "fun1:condition3-TRUE"}.
	 *
	 * @param conditionNode the CDG node whose outgoing edge carries the branch
	 * @param edge          the edge being traversed ({@code null} for synthesised
	 *                      steps)
	 * @param branchType
	 * @return a unique label string for this branch direction
	 */
	private String buildConditionLabel(CDGNode conditionNode, ControlDependenceEdge edge, BranchType branchType, int branchCounter) {
		if (branchType == BranchType.SWITCH_CASE) {
			return unitComponentName + ":branch" + branchCounter + "-" + "TRUE";
		}
		String direction = (edge != null) ? edge.toString() : "FLOW";
		return unitComponentName + ":branch" + branchCounter + "-" + direction.toLowerCase();
		//
	}

	// -----------------------------------------------------------------------
	// Utility
	// -----------------------------------------------------------------------

	public CDGBranchPathCollector(CDG cdg, String unitComponentName, Map<String, List<BranchChain>> allBranchChains,
			int conditionCounter) {
		super();
		this.cdg = cdg;
		this.unitComponentName = unitComponentName;
		this.allBranchChains = allBranchChains;
		this.conditionCounter = conditionCounter;
	}

	/**
	 * Creates an independent copy of the ancestor stack so that sibling branches
	 * each start with their own cycle guard.
	 */
	private static Deque<Integer> copyStack(Deque<Integer> original) {
		return new ArrayDeque<>(original);
	}

	StringBuilder printBranchChains(String textInfo, Map<String, List<BranchChain>> BCMap) {
		StringBuilder sb = new StringBuilder();
		sb.append("=========================Control Dependence Graph: ").append(unitComponentName)
		.append("=========================\n");
		sb.append("=========================").append(textInfo).append(" ANALYSIS =========================\n");
		BCMap.entrySet().forEach(entry -> {
			String component = entry.getKey();
			List<BranchChain> chainsToPrint = entry.getValue();
			sb.append(String.format("Component: %s | Branch-Chains: %d%n", component, chainsToPrint.size()));
			chainsToPrint.forEach(chain -> {
				sb.append("\nLabel: ").append(chain.getLabel()).append("\n");
				sb.append("  Path: ");
				for (PathStep step : chain.getPath()) {
					int fromId = (step.getFrom() != null) ? step.getFrom().getId() : -1;
					sb.append("Node[").append(fromId).append("]");
					sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
				}
				sb.append("(LEAF)\n");
				sb.append("  Leaf content: ").append(chain.getLeafNode().toString());
			});
			System.err.println(sb.toString());
		});
		return sb;
	}

	StringBuilder printBranchChains_old(String textInfo, Map<String, List<BranchChain>> BCMap) {
		// System.err.println(allBranchChains.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append("=========================Control Dependence Graph: " + unitComponentName
				+ "=========================\n");
		sb.append("=========================" + textInfo + " ANALYSIS =========================\n");
		BCMap.entrySet().forEach(entry -> {
			String component = entry.getKey();
			List<BranchChain> chainsToPrint = entry.getValue();
			String header = String.format("Component: %s | Branch-Chains: %d%n", component, chainsToPrint.size());
			sb.append(header);
			chainsToPrint.forEach(chain -> {
				// System.out.println(" - " + chain.getLabel() + " (to leaf node " +
				// chain.getLeafNode().getId() + ")");
				sb.append("\nLabel: ").append(chain.getLabel()).append("\n");
				sb.append("  Path: ");
				for (PathStep step : chain.getPath()) {
					sb.append("Node[").append(step.getFrom().getId()).append("]");
					sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
				}
				sb.append("(LEAF)\n");
				sb.append("  Leaf content: ").append(chain.getLeafNode().toString());

			});
			System.err.println(sb.toString());
		});
		return sb;

	}
}