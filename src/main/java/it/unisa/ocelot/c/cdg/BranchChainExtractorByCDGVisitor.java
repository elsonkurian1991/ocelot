package it.unisa.ocelot.c.cdg;

import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traverses a {@link CDG} depth-first and collects every root-to-leaf path
 * as a {@link BranchChain}.
 *
 * <h2>Edge direction — confirmed by debug</h2>
 * <pre>
 *   Storage:  dependent(source) ──► condition(target)
 *
 *   incomingEdgesOf(conditionNode)  → edges where source = dependent (child)
 *   getEdgeSource(edge)             → dependent node  (child in traversal)
 *   getEdgeTarget(edge)             → condition node  (parent in traversal)
 * </pre>
 *
 * <h2>Node classification</h2>
 * <ul>
 *   <li><b>Condition node</b>: {@code incomingEdgesOf(n)} is non-empty —
 *       it controls other nodes. Dispatch to typed visitor.</li>
 *   <li><b>True CDG leaf</b>: {@code incomingEdgesOf(n)} is empty AND the node
 *       appears as a source in at least one CDG edge (i.e. it was someone's
 *       dependent). Chain ends here — do NOT follow CFG successors.</li>
 *   <li><b>Sequential pass-through</b>: {@code incomingEdgesOf(n)} is empty AND
 *       the node never appears in any CDG edge (entry, plain statements before
 *       the first condition). Walk CFG successors to reach the next node.</li>
 * </ul>
 */
public class BranchChainExtractorByCDGVisitor {

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final CDG    cdg;
    private final String unitComponentName;
    private final Map<String, List<BranchChain>> allBranchChains;

    /**
     * Set of all node ids that appear as a SOURCE in any CDG edge.
     * Built once in the constructor. Used to distinguish true CDG leaves
     * (appear as dependent somewhere) from sequential pass-through nodes
     * (never appear in any CDG edge at all).
     */
    private final Set<Integer> cdgDependentNodeIds;

    /** Monotonically increasing counter for unique condition labels. */
    private int conditionCounter = -1;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public BranchChainExtractorByCDGVisitor(
            CDG cdg,
            String unitComponentName,
            Map<String, List<BranchChain>> allBranchChains) {

        this.cdg               = cdg;
        this.unitComponentName = unitComponentName;
        this.allBranchChains   = allBranchChains;
        
        this.allBranchChains.clear();
        //allBranchChains.putIfAbsent(unitComponentName, new ArrayList<>());

        // Pre-compute: which node ids appear as the SOURCE of a CDG edge?
        // These are the nodes that are control-dependent on some condition.
        cdgDependentNodeIds = new HashSet<>();
        for (ControlDependenceEdge e : cdg.edgeSet()) {
            CDGNode src = cdg.getEdgeSource(e);
            if (src != null) cdgDependentNodeIds.add(src.id);
        }
    }

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    public void visitor(CDG cdg) {
        CDGNode root = cdg.getEntryNode();
        if (root == null) return;

        visit(root, null, new ArrayList<>(), new ArrayDeque<>());

        List<BranchChain> chains = allBranchChains.get(unitComponentName);
        if (chains != null) {
            for (int i = 0; i < chains.size(); i++) {
                chains.get(i).setLabel(unitComponentName, i + 1);
            }
        }   
    }

   

	// -----------------------------------------------------------------------
    // Central visit
    // -----------------------------------------------------------------------

    private void visit(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        // 1. Per-branch cycle guard
        if (ancestorStack.contains(node.id)) return;

        // 2. Visit-level push
        ancestorStack.push(node.id);

        // 3. Classify node
        List<ControlDependenceEdge> dependentEdges = cdg.incomingEdgesOf(node);
        List<ControlDependenceEdge> dependentEdgesOUT = cdg.outgoingEdgesOf(node);//How to fix the starting node???
        /*for (ControlDependenceEdge e : dependentEdges) {
        	visit(e); //
        }*/
        
        if (!dependentEdges.isEmpty()) {
            // ---- CONDITION NODE: controls other nodes ----
            IASTNode astParent = resolveASTParent(node);

            if (astParent instanceof IASTIfStatement) {
                visitIf(node, incomingEdge, currentPath, ancestorStack);
            } else if (astParent instanceof IASTForStatement) {
                visitFor(node, incomingEdge, currentPath, ancestorStack);
            } else if (astParent instanceof IASTSwitchStatement) {
                visitSwitch(node, incomingEdge, currentPath, ancestorStack);
            } else if (astParent instanceof IASTWhileStatement) {
            	visitWhile(node, incomingEdge, currentPath, ancestorStack);
            } else if (astParent instanceof IASTDoStatement) {
            	visitDo(node, incomingEdge, currentPath, ancestorStack);
            } else {
                visitStandard(node, incomingEdge, currentPath, ancestorStack);
            }
        } else if (cdgDependentNodeIds.contains(node.id)) {
            // ---- TRUE CDG LEAF ----
            // This node is control-dependent on a condition (it appeared as a
            // source in a CDG edge) but controls nothing itself.
            // The chain ends here — do NOT follow CFG successors.
            PathStep leafStep = new PathStep(node, node, incomingEdge);
            currentPath.add(leafStep);
            //finaliseBranchChain(currentPath);
            //currentPath.remove(currentPath.size() - 1);
         // This node is control-dependent on a condition but controls nothing
         // itself. The chain for this CDG branch ends here, BUT sequential
         // execution may continue after this node ...
         // Collect sequential CFG successors that are pass-through nodes only
         List<Integer> seqSuccessors = new ArrayList<>();
         for (int succId : node.successors) {
             if (!ancestorStack.contains(succId)) {
                 CDGNode succ = cdg.getNode(succId);
                 if (succ != null && !cdgDependentNodeIds.contains(succId)) {
                     seqSuccessors.add(succId);
                 }
             }
         }

         if (seqSuccessors.isEmpty()) {
             finaliseBranchChain(currentPath);
         } else {
             for (int succId : seqSuccessors) {
                 CDGNode succ = cdg.getNode(succId);
                 if (succ == null) continue;
                 visit(succ, null, currentPath, ancestorStack);
             }
         }

         currentPath.remove(currentPath.size() - 1);
		
        } else {
            // ---- SEQUENTIAL PASS-THROUGH ----
            // This node never appears in any CDG edge (entry node, plain
            // statements before the first condition). Add a flow step and
            // walk CFG successors to reach the next node.
            PathStep step = new PathStep(node, node, incomingEdge);
            currentPath.add(step);

            List<Integer> eligibleSuccessors = new ArrayList<>();
            for (int succId : node.successors) {
                if (!ancestorStack.contains(succId)) {
                    eligibleSuccessors.add(succId);
                }
            }

            if (eligibleSuccessors.isEmpty()) {
                // No CFG successors either — genuine end of graph
                finaliseBranchChain(currentPath);
            } else {
                for (int succId : eligibleSuccessors) {
                    CDGNode succ = cdg.getNode(succId);
                    if (succ == null) continue;
                    visit(succ, null, currentPath, ancestorStack);
                }
            }

            currentPath.remove(currentPath.size() - 1);
        }

        // 4. Visit-level pop
        ancestorStack.pop();
    }

    private void visitDo(CDGNode node, ControlDependenceEdge incomingEdge, List<PathStep> currentPath,
			Deque<Integer> ancestorStack) {
    	throw new UnsupportedOperationException(
		        "Reached 'visitDo': Do while loops are not considered/supported in this implementation."
		    );
		
	}

	private void visitWhile(CDGNode node, ControlDependenceEdge incomingEdge, List<PathStep> currentPath,
			Deque<Integer> ancestorStack) {
		throw new UnsupportedOperationException(
		        "Reached 'visitWhile': While loops are not considered/supported in this implementation."
		    );
		
	}

	// -----------------------------------------------------------------------
    // Visit: standard (non-branching) condition node
    // -----------------------------------------------------------------------

    private void visitStandard(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        ancestorStack.push(node.id);

        List<ControlDependenceEdge> eligible =
                filterDependentNonAncestors(cdg.incomingEdgesOf(node), ancestorStack);

        for (ControlDependenceEdge childEdge : eligible) {
            CDGNode child = cdg.getEdgeSource(childEdge); // source = dependent
            if (child == null) continue;

            PathStep step = new PathStep(node, child, incomingEdge);
            currentPath.add(step);
            visit(child, childEdge, currentPath, ancestorStack);
            currentPath.remove(currentPath.size() - 1);
        }

        ancestorStack.pop();
    }

    // -----------------------------------------------------------------------
    // Visit: if-statement node
    // -----------------------------------------------------------------------

    private void visitIf(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        ancestorStack.push(node.id);
        int condId = ++conditionCounter;

        List<ControlDependenceEdge> eligible =
                filterDependentNonAncestors(cdg.incomingEdgesOf(node), ancestorStack);
        // Detect which arms are present in the CDG
        boolean hasTrue  = false;
        boolean hasFalse = false;
        for (ControlDependenceEdge e : eligible) {
            String lbl = e.toString();
            if ("TRUE".equals(lbl))  hasTrue  = true;
            if ("FALSE".equals(lbl)) hasFalse = true;
        }
        for (ControlDependenceEdge childEdge : eligible) {
            CDGNode child = cdg.getEdgeSource(childEdge); // source = dependent
            if (child == null) continue;

            // childEdge.toString() → "TRUE" or "FALSE" fun2:branch0-false
            String armLabel = unitComponentName + ":branch" + condId
                              + "-" + childEdge.toString();

            PathStep step = new PathStep(node, child, childEdge);
            step.setBranchConditionLabel(armLabel);
            currentPath.add(step);
            visit(child, childEdge, currentPath, ancestorStack);
            currentPath.remove(currentPath.size() - 1);
        }
        // Synthesise the missing arm: point it to the CDG exit node so the
        // chain still has a valid leaf representing the absent branch.
        CDGNode exitNode = cdg.getNode(cdg.getExitId());
        if (!hasTrue && exitNode != null) {
            String armLabel = unitComponentName + ":branch" + condId + "-TRUE";
            visitSyntheticArm(node, exitNode, armLabel, currentPath, ancestorStack);
        }
        if (!hasFalse && exitNode != null) {
            String armLabel = unitComponentName + ":branch" + condId + "-FALSE";
            visitSyntheticArm(node, exitNode, armLabel, currentPath, ancestorStack);
        }
        ancestorStack.pop();
    }

    // -----------------------------------------------------------------------
    // Visit: for-loop node
    // -----------------------------------------------------------------------

    private void visitFor(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        ancestorStack.push(node.id);
        int condId = ++conditionCounter;

        List<ControlDependenceEdge> eligible =
                filterDependentNonAncestors(cdg.incomingEdgesOf(node), ancestorStack);
        // Detect which arms are present in the CDG
        boolean hasTrue  = false;
        boolean hasFalse = false;
        for (ControlDependenceEdge e : eligible) {
            String lbl = e.toString();
            if ("TRUE".equals(lbl))  hasTrue  = true;
            if ("FALSE".equals(lbl)) hasFalse = true;
        }
        for (ControlDependenceEdge childEdge : eligible) { // if only true/false edges, then this is the same as visitIf
            CDGNode child = cdg.getEdgeSource(childEdge);
            if (child == null) continue;

            String armLabel = unitComponentName + ":branch" + condId
                              + "-" + childEdge.toString();

            PathStep step = new PathStep(node, child, childEdge);
            step.setBranchConditionLabel(armLabel);
            currentPath.add(step);
            visit(child, childEdge, currentPath, ancestorStack);
            currentPath.remove(currentPath.size() - 1);
        }
     // Synthesise the missing arm pointing to the CDG exit node
        CDGNode exitNode = cdg.getNode(cdg.getExitId());
        if (!hasTrue && exitNode != null) {
            String armLabel = unitComponentName + ":branch" + condId + "-TRUE";
            visitSyntheticArm(node, exitNode, armLabel, currentPath, ancestorStack);
        }
        if (!hasFalse && exitNode != null) {
            String armLabel = unitComponentName + ":branch" + condId + "-FALSE";
            visitSyntheticArm(node, exitNode, armLabel, currentPath, ancestorStack);
        }
        ancestorStack.pop();
    }

    // -----------------------------------------------------------------------
    // Visit: switch-statement node
    // -----------------------------------------------------------------------

    private void visitSwitch(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        ancestorStack.push(node.id);
        

        List<ControlDependenceEdge> eligible =
                filterDependentNonAncestors(cdg.incomingEdgesOf(node), ancestorStack);

        for (ControlDependenceEdge childEdge : eligible) {
            CDGNode child = cdg.getEdgeSource(childEdge);
            if (child == null) continue;
            int condId = ++conditionCounter;
            String caseValue = childEdge.toString();
            String armLabel  = unitComponentName + ":branch" + condId
                               + "-TRUE";

            PathStep step = new PathStep(node, child, childEdge);
            step.setBranchConditionLabel(armLabel);
            currentPath.add(step);
            visit(child, childEdge, currentPath, ancestorStack);
            currentPath.remove(currentPath.size() - 1);
        }

        ancestorStack.pop();
    }
    /* old version of switch, use the case value as part of the label, e.g. fun2:branch0-case-1
     * private void visitSwitch(
            CDGNode               node,
            ControlDependenceEdge incomingEdge,
            List<PathStep>        currentPath,
            Deque<Integer>        ancestorStack) {

        ancestorStack.push(node.id);
        int condId = ++conditionCounter;

        List<ControlDependenceEdge> eligible =
                filterDependentNonAncestors(cdg.incomingEdgesOf(node), ancestorStack);

        for (ControlDependenceEdge childEdge : eligible) {
            CDGNode child = cdg.getEdgeSource(childEdge);
            if (child == null) continue;

            String caseValue = childEdge.toString();
            String armLabel  = unitComponentName + ":branch" + condId
                               + "-case-" + caseValue;

            PathStep step = new PathStep(node, child, childEdge);
            step.setBranchConditionLabel(armLabel);
            currentPath.add(step);
            visit(child, childEdge, currentPath, ancestorStack);
            currentPath.remove(currentPath.size() - 1);
        }

        ancestorStack.pop();
    }
    */

    // -----------------------------------------------------------------------
    // Synthetic arm helper
    // -----------------------------------------------------------------------
 
    /**
     * Creates a single synthetic {@link PathStep} from {@code conditionNode}
     * to {@code target} (the CDG exit node) carrying {@code armLabel}, then
     * immediately finalises a chain.  Used when one arm of a binary condition
     * (TRUE or FALSE) has no CDG edge because the CFG analysis determined that
     * branch is not control-dependent on any other node.
     *
     * <p>The synthetic step uses a {@code null} edge (no real
     * {@link ControlDependenceEdge} exists for this arm) but carries an
     * explicit {@code branchConditionLabel} so fitness calculation can still
     * target it.
     *
     * @param conditionNode the if/for condition node
     * @param target        CDG exit node (leaf for this synthetic chain)
     * @param armLabel      label in the format {@code "<unit>:branch<n>-true/false"}
     * @param currentPath   accumulated path so far (will be snapshotted)
     * @param ancestorStack current DFS ancestor stack (exit node must not be on it)
     */
    private void visitSyntheticArm(
            CDGNode        conditionNode,
            CDGNode        target,
            String         armLabel,
            List<PathStep> currentPath,
            Deque<Integer> ancestorStack) {
 
        if (ancestorStack.contains(target.id)) return; // safety guard
 
        // Step representing the condition node's departure on this arm
        PathStep condStep = new PathStep(conditionNode, target, null);
        condStep.setBranchConditionLabel(armLabel);
        currentPath.add(condStep);
 
        // Step representing the exit node itself (leaf)
        PathStep exitStep = new PathStep(target, target, null);
        currentPath.add(exitStep);
 
        finaliseBranchChain(currentPath);
 
        currentPath.remove(currentPath.size() - 1); // remove exitStep
        currentPath.remove(currentPath.size() - 1); // remove condStep
    }
    // -----------------------------------------------------------------------
    // Leaf finalisation
    // -----------------------------------------------------------------------

    private void finaliseBranchChain(List<PathStep> currentPath) {
        if (currentPath.isEmpty()) return;

        List<PathStep> snapshot = new ArrayList<>(currentPath);
        CDGNode leaf = snapshot.get(snapshot.size() - 1).getFrom();

        BranchChain chain = new BranchChain(leaf, snapshot, unitComponentName, -1);
        allBranchChains.putIfAbsent(unitComponentName, new ArrayList<>());
        allBranchChains.get(unitComponentName).add(chain);
    }

    // -----------------------------------------------------------------------
    // Ancestor guard
    // -----------------------------------------------------------------------

    /**
     * Filters incoming CDG edges to those whose SOURCE (= dependent / child)
     * is NOT already on the current DFS branch stack.
     */
    private List<ControlDependenceEdge> filterDependentNonAncestors(
            List<ControlDependenceEdge> candidates,
            Deque<Integer>              ancestorStack) {

        List<ControlDependenceEdge> result = new ArrayList<>();
        for (ControlDependenceEdge edge : candidates) {
            CDGNode dependent = cdg.getEdgeSource(edge); // source = dependent = child
            if (dependent != null && !ancestorStack.contains(dependent.id)) {
                result.add(edge);
            }
        }
        return result;
    }

    boolean isAncestor(int candidateId, Deque<Integer> ancestorStack) {
        return ancestorStack.contains(candidateId);
    }

    // -----------------------------------------------------------------------
    // AST-parent resolution
    // -----------------------------------------------------------------------

    private IASTNode resolveASTParent(CDGNode node) {
        List<IASTNode> astNodes = node.getASTNodes();
        if (astNodes == null || astNodes.isEmpty()) return null;

        IASTNode leading = node.getLeadingASTNode();
        if (leading == null) return null;
        return leading.getParent();
    }

    // -----------------------------------------------------------------------
    // Public accessor
    // -----------------------------------------------------------------------

    public List<BranchChain> getBranchChains() {
        return allBranchChains.getOrDefault(unitComponentName, Collections.emptyList());
    }
    StringBuilder printBranchChains(String textInfo, Map<String, List<BranchChain>> BCMap) {
    	//System.err.println(allBranchChains.keySet());
		StringBuilder sb = new StringBuilder();
		sb.append("=========================Control Dependence Graph: "+ unitComponentName +"=========================\n");
		sb.append("========================="+ textInfo +" ANALYSIS =========================\n");
		BCMap.entrySet().forEach(entry -> {
		    String component = entry.getKey();
		    List<BranchChain> chainsToPrint = entry.getValue();
		    String header = String.format("Component: %s | Branch-Chains: %d%n", component, chainsToPrint.size());
		    sb.append(header);
		    chainsToPrint.forEach(chain -> {
		        //System.out.println("  - " + chain.getLabel() + " (to leaf node " + chain.getLeafNode().getId() + ")");
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