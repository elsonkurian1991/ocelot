package it.unisa.ocelot.c.cdg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

/**
 * Traverses a {@link CDG} depth-first and collects every root-to-leaf path
 * as a {@link BranchChain}.
 *
 * <p>Rules:
 * <ul>
 *   <li>A {@link PathStep} is added to the current path only when traversing
 *       a branch edge (TRUE / FALSE / case label).</li>
 *   <li>Plain flow-through nodes (no branching, not a leaf) are visited
 *       transparently — they do not add a step.</li>
 *   <li>When a leaf is reached {@link #finaliseBranchChain} is called to
 *       snapshot the accumulated path into a {@link BranchChain}.</li>
 * </ul>
 *
 * <p>Per-branch cycle detection is performed via an ancestor stack so that
 * back-edges in loops do not cause infinite recursion.
 */
public class BranchChainExtractorByVisit {

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final CDG    cdg;
    private final String unitComponentName;
    private final Map<String, List<BranchChain>> allBranchChains;

    /**
     * Monotonically increasing counter used to generate unique
     * branch-condition labels (e.g. {@code fun1:branch0-TRUE}).
     */
    private int conditionCounter = -1;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public BranchChainExtractorByVisit(
            CDG cdg,
            String unitComponentName,
            Map<String, List<BranchChain>> allBranchChains) {

        this.cdg               = cdg;
        this.unitComponentName = unitComponentName;
        this.allBranchChains   = allBranchChains;

        this.allBranchChains.clear();
    }

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    /**
     * Starts the depth-first traversal from the CDG entry node and populates
     * {@link #allBranchChains} with one {@link BranchChain} per root-to-leaf
     * path.  After collection, sequential labels are assigned.
     *
     * @param cdg the CDG to traverse (usually the same one passed to the
     *            constructor, but accepted as a parameter for flexibility)
     */
    public void visitor(CDG cdg) {
        CDGNode root = cdg.getEntryNode();
        if (root == null) return;

        visit(root, null, new ArrayList<>(), new ArrayDeque<>());

        // Assign sequential labels to every collected chain
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
     * Visits {@code node} depth-first, extending {@code currentPath} when a
     * branch edge was taken to arrive here.
     *
     * @param node          the CDG node currently being visited
     * @param incomingEdge  the edge that led to this node (null at the root)
     * @param currentPath   accumulated {@link PathStep}s from root to here
     * @param ancestorStack ids of nodes on the current DFS stack (cycle guard)
     */
    private void visit(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        // 1. Per-branch cycle guard — back-edge in a loop: stop this branch
        if (ancestorStack.contains(node.id)) return;

        // 2. Push onto the ancestor stack for the duration of this branch
        ancestorStack.push(node.id);

        // 3. If we arrived via a branch edge, append a PathStep
        //    (incomingEdge is null only at the root)
        if (incomingEdge != null) {
            CDGNode conditionNode = cdg.getEdgeSource(incomingEdge);
            PathStep step = new PathStep(conditionNode, node, incomingEdge);
            // Attach a human-readable branch-condition label
            step.setBranchConditionLabel(buildConditionLabel(conditionNode, incomingEdge));
            currentPath.add(step);
        }

        // 4. Collect outgoing CDG edges (the nodes this node controls)
        List<ControlDependenceEdge> dependentEdges = cdg.outgoingEdgesOf(node);

        // 5. Resolve the AST parent to decide how to dispatch per branch type
        IASTNode astParent = resolveASTParent(node);

        if (dependentEdges.isEmpty()) {
            // ---- LEAF: record the path ----
            finaliseBranchChain(currentPath);

        } else if (astParent instanceof IASTIfStatement) {
            // ---- IF statement: TRUE branch + FALSE branch (may be absent) ----
            visitIfStatement(node, dependentEdges, currentPath, ancestorStack);

        } else if (astParent instanceof IASTForStatement
                || astParent instanceof IASTWhileStatement) {
            // ---- Loop: TRUE = body, FALSE = exit ----
            visitLoopStatement(node, dependentEdges, currentPath, ancestorStack);

        } else if (astParent instanceof IASTSwitchStatement) {
            // ---- Switch: each case edge treated as TRUE, no FALSE edge ----
            visitSwitchStatement(node, dependentEdges, currentPath, ancestorStack);

        } else {
            // ---- Plain flow-through node or unknown construct ----
            // Do not add a PathStep; just continue depth-first into children
            visitChildren(node, dependentEdges, currentPath, ancestorStack);
        }

        // 6. Pop: leave the ancestor stack clean for sibling branches
        ancestorStack.pop();

        // 7. Remove the step we added on entry (backtrack)
        if (incomingEdge != null && !currentPath.isEmpty()) {
            currentPath.remove(currentPath.size() - 1);
        }
    }

    // -----------------------------------------------------------------------
    // Per-construct visit helpers
    // -----------------------------------------------------------------------

    /**
     * Handles an {@code if} node.
     * TRUE and FALSE edges are dispatched independently so each becomes its
     * own chain (or sub-chain if nested further).
     */
    private void visitIfStatement(
            CDGNode                       node,
            List<ControlDependenceEdge>   edges,
            List<PathStep>                currentPath,
            Deque<Integer>                ancestorStack) {

        for (ControlDependenceEdge cdEdge : edges) {
            CDGNode child = cdg.getEdgeTarget(cdEdge);
            if (child == null) continue;
            String lbl = cdEdge.toString();

            if ("TRUE".equals(lbl) || "FALSE".equals(lbl)) {
                // Each branch gets a private copy of the path snapshot so
                // sibling branches do not interfere
                visit(child, cdEdge,
                      new ArrayList<>(currentPath),
                      copyStack(ancestorStack));
            } else {
                // Unexpected label under an if-node — treat as flow-through
                visit(child, null,
                      new ArrayList<>(currentPath),
                      copyStack(ancestorStack));
            }
        }
    }

    /**
     * Handles a {@code for} / {@code while} node.
     * TRUE = loop body, FALSE = loop exit.
     */
    private void visitLoopStatement(
            CDGNode                       node,
            List<ControlDependenceEdge>   edges,
            List<PathStep>                currentPath,
            Deque<Integer>                ancestorStack) {

        for (ControlDependenceEdge cdEdge : edges) {
            CDGNode child = cdg.getEdgeTarget(cdEdge);
            if (child == null) continue;
            String lbl = cdEdge.toString();

            if ("TRUE".equals(lbl) || "FALSE".equals(lbl)) {
                visit(child, cdEdge,
                      new ArrayList<>(currentPath),
                      copyStack(ancestorStack));
            } else {
                visit(child, null,
                      new ArrayList<>(currentPath),
                      copyStack(ancestorStack));
            }
        }
    }

    /**
     * Handles a {@code switch} node.
     * Every case edge is treated as a branch (labelled by the case value).
     * There is no separate FALSE edge.
     */
    private void visitSwitchStatement(
            CDGNode                       node,
            List<ControlDependenceEdge>   edges,
            List<PathStep>                currentPath,
            Deque<Integer>                ancestorStack) {

        for (ControlDependenceEdge cdEdge : edges) {
            CDGNode child = cdg.getEdgeTarget(cdEdge);
            if (child == null) continue;
            // Every outgoing edge from a switch is a taken-case branch
            visit(child, cdEdge,
                  new ArrayList<>(currentPath),
                  copyStack(ancestorStack));
        }
    }

    /**
     * Generic / plain flow-through: recurse into every child without adding
     * a PathStep (the incomingEdge parameter is passed as {@code null} so no
     * step is created in the recursive call).
     */
    private void visitChildren(
            CDGNode                       node,
            List<ControlDependenceEdge>   edges,
            List<PathStep>                currentPath,
            Deque<Integer>                ancestorStack) {

        for (ControlDependenceEdge cdEdge : edges) {
            CDGNode child = cdg.getEdgeTarget(cdEdge);
            if (child == null) continue;
            visit(child, null,
                  new ArrayList<>(currentPath),
                  copyStack(ancestorStack));
        }
    }

    // -----------------------------------------------------------------------
    // Leaf finalisation
    // -----------------------------------------------------------------------

    /**
     * Snapshots the current path and stores it as a new {@link BranchChain}.
     * The chain number is set to -1 here; sequential numbering is applied
     * after all chains are collected in {@link #visitor}.
     *
     * @param currentPath the root-to-leaf path accumulated so far
     */
    private void finaliseBranchChain(List<PathStep> currentPath) {
        if (currentPath.isEmpty()) return;

        List<PathStep> snapshot = new ArrayList<>(currentPath);
        CDGNode leaf = snapshot.get(snapshot.size() - 1).getTo();

        // Fallback: if getTo() is somehow null, use getFrom() of the last step
        if (leaf == null) {
            leaf = snapshot.get(snapshot.size() - 1).getFrom();
        }

        BranchChain chain = new BranchChain(leaf, snapshot, unitComponentName, -1);
        allBranchChains.putIfAbsent(unitComponentName, new ArrayList<>());
        allBranchChains.get(unitComponentName).add(chain);
    }

    // -----------------------------------------------------------------------
    // AST parent resolution
    // -----------------------------------------------------------------------

    /**
     * Resolves the dominant AST construct that a CDG node represents.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Retrieve the leading {@link IASTNode} from the CDGNode.</li>
     *   <li>Walk up the parent chain until an
     *       {@link IASTIfStatement}, {@link IASTForStatement},
     *       {@link IASTWhileStatement}, or {@link IASTSwitchStatement}
     *       is found, or until the root is reached.</li>
     * </ol>
     *
     * @param node the CDG node whose AST parent is needed
     * @return the nearest enclosing control-flow AST node, or {@code null}
     */
    private IASTNode resolveASTParent(CDGNode node) {
        // Retrieve the leading AST node attached to this CDG node
        IASTNode astNode = node.getLeadingASTNode();
        if (astNode == null) return null;

        // Walk up to find the nearest enclosing control-flow statement
        IASTNode current = astNode;
        while (current != null) {
            if (current instanceof IASTIfStatement
                    || current instanceof IASTForStatement
                    || current instanceof IASTWhileStatement
                    || current instanceof IASTSwitchStatement) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Branch-condition label generation
    // -----------------------------------------------------------------------

    /**
     * Builds a human-readable label for the branch condition encoded in
     * {@code edge}, e.g. {@code "fun1:condition2-TRUE"}.
     *
     * @param conditionNode the CDG node whose outgoing edge carries the branch
     * @param edge          the edge being traversed
     * @return a unique label string for this branch direction
     */
    private String buildConditionLabel(CDGNode conditionNode, ControlDependenceEdge edge) {
        conditionCounter++;
        String branchDirection = (edge != null) ? edge.toString() : "FLOW";
        return unitComponentName + ":condition" + conditionCounter + "-" + branchDirection;
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------

    /**
     * Creates a shallow copy of a {@link Deque} so that sibling branches
     * each start with an independent ancestor stack.
     *
     * @param original the deque to copy
     * @return a new {@link ArrayDeque} containing the same elements in the
     *         same order
     */
    private static Deque<Integer> copyStack(Deque<Integer> original) {
        // ArrayDeque copy constructor preserves iteration order (head first)
        return new ArrayDeque<>(original);
    }
}