//15-05-26 13:53 762,0.45

package it.unisa.ocelot.c.cdg;

import java.util.*;
import org.jgrapht.graph.DefaultDirectedGraph;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.CaseEdge;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Control Dependence Graph (CDG) construction.
 *
 * Handles:
 *   1. Standard if-else branching (with and without else).
 *   2. Normal for/while/do-while loops.
 *   3. Break-loops: loops containing absorbed break statements.
 *   4. Nested predicates inside loop bodies.
 *   5. Sequential loops (one loop followed by another).
 *   6. Switch statements with case/default labels.
 *   7. Top-level post-loop nodes receiving ENTRY FLOW correctly.
 *
 * Build pipeline:
 *  Step  1  computePostDominators()
 *  Step  2  computeStructuralSets()
 *  Step  3  buildStandardEdges()
 *  Step  4  removeAllSelfLoops()
 *  Step  5  removeSpuriousBreakEdges()
 *  Step  6  removeLoopFalseExitEdges()
 *  Step  7  addLoopBodyTrueEdges()
 *  Step  8  addLoopFalseExitEdges()        — not for break-loops / exit targets
 *  Step  8b removeSpuriousLoopFalseToExit()
 *  Step  9  removeSpuriousLoopBodyEdges()
 *  Step  9b removeSpuriousInnerIfEdgesInBreakLoops()  (after Step 11)
 *  Step  9c removeDuplicateSwitchCaseEdges()          (after Step 11)
 *  Step  9d removeSpuriousIfElseOvergoverning()       (TRUE+FALSE, twice)
 *  Step  9e removeSpuriousIfToSwitchBranches()
 *  Step 10  addBreakConditionEdges()       — not to entry-flow post-loop stmts
 *  Step 11  addMissingIfNoElseFalseEdges()
 *  Step 11b addGovernanceToNestedLoops()
 *  Step 12  attachEntryEdges()
 *
 * Key invariants:
 *  - Loop condition nodes NEVER self-loop.
 *  - Loop FALSE exit CDG edge only emitted for nested loops (false successor
 *    NOT top-level). Top-level loops have no loop FALSE CDG edge — the post-loop
 *    node receives ENTRY FLOW only.
 *  - Break-loops: no loop-FALSE / break-condition CDG edges; post-loop uses ENTRY FLOW.
 *  - Parent if nodes govern nested loop headers via addGovernanceToNestedLoops.
 *  - If-no-else FALSE edges emitted whenever TRUE exists but FALSE is missing
 *    (except when false successor is loop-governed via TRUE).
 *  - ENTRY FLOW edges go only to entryFlowNodes (unconditionally reachable,
 *    outside loop and if-branch scopes).
 */
public class CDG2 extends DefaultDirectedGraph<CDGNode, ControlDependenceEdge> {

    private static final long serialVersionUID = 1L;

    private final CFG      originalCFG;
    private final CDGNode  entryNode;
    private final Map<CFGNode, CDGNode> cfgToCdgMap;

    private Map<CFGNode, Set<CFGNode>> postDominators;

    // Structural sets — computed once in Step 2
    private Set<CFGNode> allLoopConditions;
    private Set<CFGNode> allLoopBodyNodes;
    private Set<CFGNode> allLoopInitNodes;
    private Set<CFGNode> allBreakConditions;
    private Set<CFGNode> topLevelNodes;
    private Set<CFGNode> branchScopedNodes;
    private Set<CFGNode> entryFlowNodes;
    private Map<CFGNode, Set<CFGNode>> loopBodiesByHead;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public CDG2(CFG cfg) {
        super(ControlDependenceEdge.class);
        this.originalCFG = cfg;
        this.cfgToCdgMap = new HashMap<>();

        this.entryNode = new CDGNode(cfg.getStart(), "ENTRY");
        this.addVertex(entryNode);

        for (CFGNode n : cfg.vertexSet()) {
            CDGNode cdn = new CDGNode(n);
            this.addVertex(cdn);
            cfgToCdgMap.put(n, cdn);
        }

        buildCDG();
    }

    public static void resetNodeIds() {
        CDGNode.resetIdCounter();
    }

    // =========================================================================
    // MAIN BUILD PIPELINE
    // =========================================================================

    private void buildCDG() {
        this.postDominators = computePostDominators();       // Step 1
        computeStructuralSets();                              // Step 2
        buildStandardEdges();                                 // Step 3
        removeAllSelfLoops();                                 // Step 4
        removeSpuriousBreakEdges();                           // Step 5
        removeLoopFalseExitEdges();                           // Step 6
        addLoopBodyTrueEdges();                               // Step 7
        addLoopFalseExitEdges();                              // Step 8
        removeSpuriousLoopFalseToExit();                      // Step 8b
        removeSpuriousLoopBodyEdges();                        // Step 9
        removeSpuriousIfElseOvergoverning();                   // Step 9d (early)
        addBreakConditionEdges();                             // Step 10
        addMissingIfNoElseFalseEdges();                       // Step 11
        addGovernanceToNestedLoops();                         // Step 11b
        removeSpuriousLoopBodyEdges();                        // Step 9 (2nd pass)
        removeSpuriousInnerIfEdgesInBreakLoops();              // Step 9b
        removeSpuriousIfToSwitchBranches();                    // Step 9e
        removeDuplicateSwitchCaseEdges();                      // Step 9c (after 11)
        removeSpuriousIfElseOvergoverning();                   // Step 9d (final)
        attachEntryEdges();                                   // Step 12
    }

    // =========================================================================
    // STEP 1: POST-DOMINATOR COMPUTATION
    // =========================================================================

    /**
     * Successors for post-dominator iteration only. CFG leaves with no outgoing
     * edges are treated as flowing to {@code getEnd()}, as in {@code CDGBuilder2.augmentCFG},
     * without mutating the original CFG.
     */
    private List<CFGNode> getPostDomSuccessors(CFGNode n) {
        List<CFGNode> succs = new ArrayList<>();
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
            CFGNode t = originalCFG.getEdgeTarget(e);
            if (t != null) succs.add(t);
        }
        CFGNode exit = originalCFG.getEnd();
        if (succs.isEmpty() && exit != null && !n.equals(exit)) {
            succs.add(exit);
        }
        return succs;
    }

    private Map<CFGNode, Set<CFGNode>> computePostDominators() {
        Map<CFGNode, Set<CFGNode>> postDom = new HashMap<>();
        Set<CFGNode> allNodes = originalCFG.vertexSet();
        CFGNode exitNode = originalCFG.getEnd();

        for (CFGNode n : allNodes) {
            if (n.equals(exitNode)) {
                postDom.put(n, new HashSet<>(Collections.singleton(exitNode)));
            } else {
                postDom.put(n, new HashSet<>(allNodes));
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (CFGNode n : allNodes) {
                if (n.equals(exitNode)) continue;

                Set<CFGNode> intersection = null;
                for (CFGNode succ : getPostDomSuccessors(n)) {
                    if (intersection == null) {
                        intersection = new HashSet<>(postDom.get(succ));
                    } else {
                        intersection.retainAll(postDom.get(succ));
                    }
                }

                Set<CFGNode> newPDom = new HashSet<>();
                newPDom.add(n);
                if (intersection != null) newPDom.addAll(intersection);

                if (!newPDom.equals(postDom.get(n))) {
                    postDom.put(n, newPDom);
                    changed = true;
                }
            }
        }
        return postDom;
    }

    // =========================================================================
    // STEP 2: PRE-COMPUTE STRUCTURAL SETS
    // =========================================================================

    private void computeStructuralSets() {
        allLoopConditions  = new HashSet<>();
        allLoopBodyNodes   = new HashSet<>();
        allLoopInitNodes   = new HashSet<>();
        allBreakConditions = new HashSet<>();
        loopBodiesByHead   = new HashMap<>();

        for (CFGNode a : originalCFG.vertexSet()) {
            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;

            Set<CFGNode> body = collectLoopBody(a, falseSucc, trueSucc);
            if (!loopBodyCanReachHead(body, a)) continue;

            allLoopConditions.add(a);
            loopBodiesByHead.put(a, body);
            allLoopBodyNodes.addAll(body);
            allLoopInitNodes.addAll(collectLoopInitNodes(a));

            for (CFGNode bodyNode : body) {
                if (isBreakCondition(bodyNode, body, a)) {
                    allBreakConditions.add(bodyNode);
                }
            }
        }

        topLevelNodes = computeForwardReachable();
        topLevelNodes.removeAll(allLoopBodyNodes);
        topLevelNodes.removeAll(allLoopInitNodes);

        branchScopedNodes = computeBranchScopedNodes();
        entryFlowNodes = new HashSet<>(topLevelNodes);
        entryFlowNodes.removeAll(branchScopedNodes);
        augmentEntryFlowNodes();
    }

    /** Start, End, returns, top-level if/switch headers, and non-nested loop headers. */
    private void augmentEntryFlowNodes() {
        CFGNode start = originalCFG.getStart();
        CFGNode end   = originalCFG.getEnd();
        if (start != null) entryFlowNodes.add(start);
        if (end != null) entryFlowNodes.add(end);
        for (CFGNode lc : allLoopConditions) {
            if (!branchScopedNodes.contains(lc)) entryFlowNodes.add(lc);
        }
        for (CFGNode n : originalCFG.vertexSet()) {
            if (flowsDirectlyToEnd(n)) entryFlowNodes.add(n);
            if (isTopLevelControlHeader(n)) entryFlowNodes.add(n);
        }
    }

    private boolean isTopLevelControlHeader(CFGNode n) {
        if (!topLevelNodes.contains(n) || branchScopedNodes.contains(n)) return false;
        if (isIfPredicate(n)) return true;
        return isSwitchExpressionNode(n);
    }

    private boolean flowsDirectlyToEnd(CFGNode n) {
        CFGNode end = originalCFG.getEnd();
        if (end == null || n == null) return false;
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
            CFGNode t = originalCFG.getEdgeTarget(e);
            if (end.equals(t)) return true;
        }
        return false;
    }

    /** Nodes inside if TRUE arm; if-else also includes explicit FALSE arm (not if-no-else chain). */
    private Set<CFGNode> computeBranchScopedNodes() {
        Set<CFGNode> scoped = new HashSet<>();
        for (CFGNode a : originalCFG.vertexSet()) {
            if (allLoopConditions.contains(a)) continue;
            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;
            scoped.addAll(collectLoopBody(a, falseSucc, trueSucc));
            if (!isIfNoElse(a)) {
                scoped.addAll(collectLoopBody(a, trueSucc, falseSucc));
            }
        }
        return scoped;
    }

    /** If-no-else: FALSE CFG edge goes to merge; true arm flows into that merge. */
    private boolean isIfNoElse(CFGNode a) {
        CFGNode merge = getFalseSuccessor(a);
        CFGNode trueEntry = getTrueSuccessor(a);
        if (merge == null || trueEntry == null) return false;
        for (LabeledEdge e : originalCFG.incomingEdgesOf(merge)) {
            CFGNode pred = originalCFG.getEdgeSource(e);
            if (pred.equals(a)) continue;
            if (isReachableWithout(a, trueEntry, pred, merge)) return true;
        }
        return false;
    }

    private boolean isReachableWithout(CFGNode ifHead, CFGNode from,
                                        CFGNode target, CFGNode merge) {
        Set<CFGNode> visited = new HashSet<>();
        Deque<CFGNode> queue = new ArrayDeque<>();
        queue.add(from);
        visited.add(from);
        while (!queue.isEmpty()) {
            CFGNode cur = queue.poll();
            if (cur.equals(target)) return true;
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
                CFGNode t = originalCFG.getEdgeTarget(e);
                if (t == null || t.equals(ifHead) || t.equals(merge)) continue;
                if (visited.add(t)) queue.add(t);
            }
        }
        return false;
    }

    private boolean isIfPredicate(CFGNode n) {
        if (allLoopConditions.contains(n) || allBreakConditions.contains(n)) return false;
        return getTrueSuccessor(n) != null && getFalseSuccessor(n) != null;
    }

    private boolean loopHasBreakCondition(CFGNode loopCond) {
        Set<CFGNode> body = getLoopBody(loopCond);
        for (CFGNode n : body) {
            if (allBreakConditions.contains(n)) return true;
        }
        return false;
    }

    private Set<CFGNode> computeForwardReachable() {
        Set<CFGNode>   visited  = new HashSet<>();
        Deque<CFGNode> worklist = new ArrayDeque<>();
        CFGNode start = originalCFG.getStart();
        worklist.add(start);
        visited.add(start);
        while (!worklist.isEmpty()) {
            CFGNode cur = worklist.poll();
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
                CFGNode succ = originalCFG.getEdgeTarget(e);
                if (succ != null && visited.add(succ)) {
                    worklist.add(succ);
                }
            }
        }
        return visited;
    }

    // =========================================================================
    // STEP 3: STANDARD POST-DOMINANCE CDG EDGES
    // =========================================================================

    private void buildStandardEdges() {
        for (CFGNode a : originalCFG.vertexSet()) {
            for (LabeledEdge edge : originalCFG.outgoingEdgesOf(a)) {
                CFGNode b = originalCFG.getEdgeTarget(edge);
                if (b == null) continue;
                CDGNode cdgA = cfgToCdgMap.get(a);
                if (cdgA == null) continue;
                String label = getLabel(edge);
                for (CFGNode n : findControlDependentNodes(a, b)) {
                    CDGNode cdgN = cfgToCdgMap.get(n);
                    if (cdgN != null) {
                        addEdgeIfAbsent(cdgA, cdgN, label, edge);
                    }
                }
            }
        }
    }

    /**
     * Nodes control-dependent on {@code a} with respect to CFG edge {@code a -> b}.
     * Forward CFG walk from {@code b} until the post-dominator frontier of {@code a}.
     */
    private Set<CFGNode> findControlDependentNodes(CFGNode a, CFGNode b) {
        Set<CFGNode> dependentNodes = new HashSet<>();
        Set<CFGNode> postDomA = postDominators.get(a);
        if (postDomA == null || postDomA.contains(b)) return dependentNodes;

        Set<CFGNode> visited = new HashSet<>();
        Deque<CFGNode> queue = new ArrayDeque<>();
        queue.add(b);
        visited.add(b);

        while (!queue.isEmpty()) {
            CFGNode current = queue.poll();
            if (postDomA.contains(current) && !current.equals(a)) continue;

            dependentNodes.add(current);

            for (LabeledEdge e : originalCFG.outgoingEdgesOf(current)) {
                CFGNode succ = originalCFG.getEdgeTarget(e);
                if (succ != null && visited.add(succ)) {
                    queue.add(succ);
                }
            }
        }
        return dependentNodes;
    }

    // =========================================================================
    // STEP 4: REMOVE ALL SELF-LOOPS
    // =========================================================================

    private void removeAllSelfLoops() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();
        for (ControlDependenceEdge e : this.edgeSet()) {
            if (this.getEdgeSource(e).equals(this.getEdgeTarget(e)))
                toRemove.add(e);
        }
        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 5: REMOVE SPURIOUS BREAK-CONDITION EDGES
    // =========================================================================

    private void removeSpuriousBreakEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();
        for (ControlDependenceEdge e : this.edgeSet()) {
            CFGNode src = this.getEdgeSource(e).getOriginalCFGNode();
            if (src != null && allBreakConditions.contains(src))
                toRemove.add(e);
        }
        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 6: REMOVE SPURIOUS LOOP FALSE EXIT EDGES
    // =========================================================================

    private void removeLoopFalseExitEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();
        for (ControlDependenceEdge e : this.edgeSet()) {
            if (!"FALSE".equals(e.toString())) continue;
            CFGNode src = this.getEdgeSource(e).getOriginalCFGNode();
            if (src == null || !allLoopConditions.contains(src)) continue;
            CFGNode tgt = this.getEdgeTarget(e).getOriginalCFGNode();
            if (tgt == null) continue;
            if (!allLoopBodyNodes.contains(tgt) && !allLoopInitNodes.contains(tgt))
                toRemove.add(e);
        }
        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 7: ADD MISSING LOOP BODY TRUE EDGES
    // =========================================================================

    private void addLoopBodyTrueEdges() {
        for (CFGNode a : allLoopConditions) {
            CDGNode cdgA = cfgToCdgMap.get(a);
            if (cdgA == null) continue;

            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;

            LabeledEdge trueEdge = getTrueLabeledEdge(a);
            Set<CFGNode> loopBody = collectLoopBody(a, falseSucc, trueSucc);

            Set<CFGNode> candidates = new HashSet<>(loopBody);
            for (CFGNode init : collectLoopInitNodes(a)) {
                if (isGenuineLoopInit(init)) candidates.add(init);
            }

            for (CFGNode bodyNode : candidates) {
                if (allLoopConditions.contains(bodyNode)) continue;
                CDGNode cdgBody = cfgToCdgMap.get(bodyNode);
                if (cdgBody == null || cdgBody.equals(cdgA)) continue;
                if (hasEdge(cdgA, cdgBody, "TRUE")) continue;
                if (loopBody.contains(bodyNode)
                        && isGovernedByNestedPredicate(cdgBody, cdgA, loopBody)) continue;
                addEdgeIfAbsent(cdgA, cdgBody, "TRUE", trueEdge);
            }
        }
    }

    // =========================================================================
    // STEP 8: ADD LOOP FALSE EXIT EDGES
    //
    // Loop FALSE exit CDG edge is emitted when:
    //   (a) The false successor is NOT top-level (nested loop inside if-else
    //       or another loop) — always emit.
    //   (b) The false successor IS top-level BUT the loop condition itself is
    //       governed by an outer branch node (i.e. it is inside an if-else
    //       or outer loop body) — still emit because the loop is nested even
    //       if its false successor happens to be top-level.
    //   (c) The false successor IS top-level AND the loop condition is also
    //       top-level (genuine top-level loop) — do NOT emit. Post-loop node
    //       receives ENTRY FLOW only.
    //
    // This fixes fun3 where inner loops (k<LOOP_A, j<LOOP_B, l<LOOP_MAX) are
    // nested inside r2>R2_HIGH FALSE block. Their false successors appear in
    // topLevelNodes but the loop conditions are governed by the outer if-else.
    // =========================================================================

    private void addLoopFalseExitEdges() {
        for (CFGNode loopCond : allLoopConditions) {
            if (loopHasBreakCondition(loopCond)) continue;

            CDGNode cdgLoop = cfgToCdgMap.get(loopCond);
            if (cdgLoop == null) continue;

            CFGNode falseSucc = getFalseSuccessor(loopCond);
            if (falseSucc == null) continue;
            if (flowsDirectlyToEnd(falseSucc)) continue;

            if (topLevelNodes.contains(loopCond) && topLevelNodes.contains(falseSucc)
                    && !branchScopedNodes.contains(loopCond)) continue;

            CDGNode cdgFalseSucc = cfgToCdgMap.get(falseSucc);
            if (cdgFalseSucc == null) continue;

            LabeledEdge falseEdge = getFalseLabeledEdge(loopCond);
            addEdgeIfAbsent(cdgLoop, cdgFalseSucc, "FALSE", falseEdge);
        }
    }

    private void removeSpuriousLoopFalseToExit() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();
        for (ControlDependenceEdge e : this.edgeSet()) {
            if (!"FALSE".equals(e.toString())) continue;
            CFGNode src = this.getEdgeSource(e).getOriginalCFGNode();
            if (src == null || !allLoopConditions.contains(src)) continue;
            CFGNode tgt = this.getEdgeTarget(e).getOriginalCFGNode();
            if (tgt == null) continue;
            if (loopHasBreakCondition(src)) {
                toRemove.add(e);
                continue;
            }
            if (flowsDirectlyToEnd(tgt)) toRemove.add(e);
        }
        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 9: REMOVE SPURIOUS LOOP BODY EDGES
    // =========================================================================

    private void removeSpuriousLoopBodyEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();
        for (CFGNode a : allLoopConditions) {
            CDGNode cdgA = cfgToCdgMap.get(a);
            if (cdgA == null) continue;
            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;
            Set<CFGNode> loopBody = collectLoopBody(a, falseSucc, trueSucc);

            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgA))) {
                if (!"TRUE".equals(e.toString())) continue;
                CDGNode cdgTgt = this.getEdgeTarget(e);
                if (cdgTgt.equals(cdgA)) continue;
                CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
                if (cfgTgt == null || !loopBody.contains(cfgTgt)) continue;
                if (isGovernedByNestedPredicate(cdgTgt, cdgA, loopBody))
                    toRemove.add(e);
            }
        }
        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 9b: REMOVE SPURIOUS INNER IF EDGES IN BREAK-LOOPS (fun6)
    //
    // Only if-else predicates inside a break-loop whose TRUE and FALSE CFG
    // successors both stay in the loop body. Scope is the if's own arms
    // (not the enclosing loop exit), so loop/break edges are never touched.
    // =========================================================================

    private void removeSpuriousInnerIfEdgesInBreakLoops() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (CFGNode cfgSrc : allLoopBodyNodes) {
            if (allLoopConditions.contains(cfgSrc)) continue;
            if (allBreakConditions.contains(cfgSrc)) continue;

            CFGNode trueSucc  = getTrueSuccessor(cfgSrc);
            CFGNode falseSucc = getFalseSuccessor(cfgSrc);
            if (trueSucc == null || falseSucc == null) continue;

            CFGNode enclosingLoop = findEnclosingLoop(cfgSrc);
            if (enclosingLoop == null || !loopHasBreakCondition(enclosingLoop)) continue;

            Set<CFGNode> loopBody = getLoopBody(enclosingLoop);
            if (!loopBody.contains(trueSucc) || !loopBody.contains(falseSucc)) continue;

            Set<CFGNode> validScope = new HashSet<>();
            validScope.addAll(collectLoopBody(cfgSrc, falseSucc, trueSucc));
            validScope.addAll(collectLoopBody(cfgSrc, trueSucc, falseSucc));

            CDGNode cdgSrc = cfgToCdgMap.get(cfgSrc);
            if (cdgSrc == null) continue;

            CFGNode end = originalCFG.getEnd();

            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgSrc))) {
                String lbl = e.toString();
                if (!"TRUE".equals(lbl) && !"FALSE".equals(lbl)) continue;
                CFGNode cfgTgt = this.getEdgeTarget(e).getOriginalCFGNode();
                if (cfgTgt == null) continue;
                if (allLoopConditions.contains(cfgTgt)) { toRemove.add(e); continue; }
                if (end != null && end.equals(cfgTgt)) { toRemove.add(e); continue; }
                if (flowsDirectlyToEnd(cfgTgt)) { toRemove.add(e); continue; }
                if (!validScope.contains(cfgTgt)) toRemove.add(e);
            }
        }

        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 9c: REMOVE DUPLICATE SWITCH CASE-LABEL EDGES (fun7)
    //
    // Remove switch→T only when T is NOT a direct CaseEdge target, the edge
    // label matches a case entry for that switch, and T is already governed
    // by that case entry via TRUE or FALSE.
    // =========================================================================

    private void removeDuplicateSwitchCaseEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (CFGNode cfgSwitch : originalCFG.vertexSet()) {
            boolean hasCaseEdge = false;
            Map<String, CFGNode> caseEntryByLabel = new HashMap<>();
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cfgSwitch)) {
                if (e instanceof CaseEdge) {
                    hasCaseEdge = true;
                    Object lbl = e.getLabel();
                    if (lbl != null) {
                        caseEntryByLabel.put(lbl.toString(), originalCFG.getEdgeTarget(e));
                    }
                }
            }
            if (!hasCaseEdge) continue;

            Set<CFGNode> caseEntryNodes = new HashSet<>(caseEntryByLabel.values());
            CDGNode cdgSwitch = cfgToCdgMap.get(cfgSwitch);
            if (cdgSwitch == null) continue;

            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgSwitch))) {
                String lbl = e.toString();
                if ("TRUE".equals(lbl) || "FALSE".equals(lbl) || "FLOW".equals(lbl)) continue;

                CFGNode cfgTgt = this.getEdgeTarget(e).getOriginalCFGNode();
                if (cfgTgt == null || caseEntryNodes.contains(cfgTgt)) continue;

                // Duplicate case label to a non-entry target (fun7 SW1_CASE1 → inner stmts)
                if (caseEntryByLabel.containsKey(lbl)) {
                    toRemove.add(e);
                }
            }
        }

        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 9e: IF → SWITCH DESCENDANT EDGES (fun10)
    // =========================================================================

    private void removeSpuriousIfToSwitchBranches() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (ControlDependenceEdge e : this.edgeSet()) {
            String lbl = e.toString();
            if (!"TRUE".equals(lbl) && !"FALSE".equals(lbl)) continue;

            CFGNode cfgSrc = this.getEdgeSource(e).getOriginalCFGNode();
            CFGNode cfgTgt = this.getEdgeTarget(e).getOriginalCFGNode();
            if (cfgSrc == null || cfgTgt == null) continue;
            if (allLoopConditions.contains(cfgSrc) || allBreakConditions.contains(cfgSrc)) continue;

            CFGNode trueSucc  = getTrueSuccessor(cfgSrc);
            CFGNode falseSucc = getFalseSuccessor(cfgSrc);
            if (trueSucc == null || falseSucc == null) continue;
            if (cfgTgt.equals(trueSucc) || cfgTgt.equals(falseSucc)) continue;

            if (isSwitchExpressionNode(cfgTgt) || isReachableFromSwitchHead(cfgTgt)) {
                toRemove.add(e);
            }
        }

        toRemove.forEach(this::removeEdge);
    }

    private boolean isSwitchExpressionNode(CFGNode n) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
            if (e instanceof CaseEdge) return true;
        }
        return false;
    }

    private boolean isReachableFromSwitchHead(CFGNode n) {
        for (CFGNode sw : originalCFG.vertexSet()) {
            if (!isSwitchExpressionNode(sw)) continue;
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(sw)) {
                if (!(e instanceof CaseEdge)) continue;
                CFGNode caseStart = originalCFG.getEdgeTarget(e);
                if (caseStart == null) continue;
                if (caseStart.equals(n) || collectBranchRegion(sw, caseStart).contains(n)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Nodes reachable from {@code start} without crossing {@code regionHead}. */
    private Set<CFGNode> collectBranchRegion(CFGNode regionHead, CFGNode start) {
        Set<CFGNode> visited = new HashSet<>();
        Deque<CFGNode> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            CFGNode cur = queue.poll();
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
                CFGNode t = originalCFG.getEdgeTarget(e);
                if (t == null || t.equals(regionHead) || !visited.add(t)) continue;
                queue.add(t);
            }
        }
        return visited;
    }

    // =========================================================================
    // STEP 9d: REMOVE SPURIOUS IF-ELSE FALSE EDGES THAT OVER-GOVERN NESTED NODES
    //
    // fun7 fix: the standard algorithm emits edges like:
    //   r6>R6_HIGH FALSE → sw1=h%SW1_MOD, sw1 switch, sw2=..., result>FINAL_THRESH
    // These nodes are nested INSIDE the else block and must be governed by their
    // immediate parent conditions (h>H_HIGH, h>H_LOW, switch nodes), NOT directly
    // by r6>R6_HIGH FALSE.
    //
    // Detection: edge source S has a FALSE CDG edge to target T, where S is an
    // if-else branch (has both TRUE and FALSE CFG successors), and T is reachable
    // from the FALSE successor of S BUT T already has an incoming CDG edge from
    // another non-ENTRY node that governs it more precisely.
    //
    // A node T is "over-governed" by S FALSE when:
    //   1. S is not a loop condition
    //   2. T already has an incoming TRUE or FALSE CDG edge from a node OTHER
    //      than S and ENTRY (meaning a more immediate predicate governs T)
    //   3. T is not a direct FALSE successor of S in the CFG
    // =========================================================================

    private void removeSpuriousIfElseOvergoverning() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (ControlDependenceEdge e : this.edgeSet()) {
            String edgeLbl = e.toString();
            if (!"TRUE".equals(edgeLbl) && !"FALSE".equals(edgeLbl)) continue;

            CDGNode cdgSrc = this.getEdgeSource(e);
            CFGNode cfgSrc = cdgSrc.getOriginalCFGNode();
            if (cfgSrc == null) continue;
            if (getTrueSuccessor(cfgSrc) == null || getFalseSuccessor(cfgSrc) == null) continue;
            if (allLoopConditions.contains(cfgSrc) || allBreakConditions.contains(cfgSrc)) continue;

            CDGNode cdgTgt = this.getEdgeTarget(e);
            CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
            if (cfgTgt == null) continue;

            if ("TRUE".equals(edgeLbl) && cfgTgt.equals(getTrueSuccessor(cfgSrc))) continue;
            if ("FALSE".equals(edgeLbl) && cfgTgt.equals(getFalseSuccessor(cfgSrc))) continue;

            boolean hasCloserGoverner = false;
            for (ControlDependenceEdge inE : this.incomingEdgesOf(cdgTgt)) {
                CDGNode inSrc = this.getEdgeSource(inE);
                if (inSrc.equals(cdgSrc) || inSrc.equals(entryNode)) continue;
                String inLbl = inE.toString();
                if ("TRUE".equals(inLbl) || "FALSE".equals(inLbl)) {
                    hasCloserGoverner = true;
                    break;
                }
            }
            if (hasCloserGoverner) toRemove.add(e);
        }

        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 10: BREAK-CONDITION EDGES (nested / return exit only)
    // =========================================================================

    private void addBreakConditionEdges() {
        for (CFGNode breakCond : allBreakConditions) {
            CDGNode cdgBreak = cfgToCdgMap.get(breakCond);
            if (cdgBreak == null) continue;

            CFGNode trueTarget = getTrueSuccessor(breakCond);
            if (trueTarget != null) {
                boolean toReturnExit = flowsDirectlyToEnd(trueTarget);
                boolean toEntryFlowStmt = entryFlowNodes.contains(trueTarget) && !toReturnExit;
                if (!toEntryFlowStmt) {
                    CDGNode cdgTrue = cfgToCdgMap.get(trueTarget);
                    if (cdgTrue != null) {
                        addEdgeIfAbsent(cdgBreak, cdgTrue, "TRUE",
                            getTrueLabeledEdge(breakCond));
                    }
                }
            }

            CFGNode falseTarget = getFalseSuccessor(breakCond);
            if (falseTarget != null
                    && !allLoopInitNodes.contains(falseTarget)
                    && !isGovernedByLoopCondition(falseTarget)
                    && !entryFlowNodes.contains(falseTarget)) {
                CFGNode enc = findEnclosingLoop(breakCond);
                Set<CFGNode> encBody = enc != null ? getLoopBody(enc) : Collections.emptySet();
                if (encBody.contains(falseTarget)) {
                    CDGNode cdgFalse = cfgToCdgMap.get(falseTarget);
                    if (cdgFalse != null) {
                        addEdgeIfAbsent(cdgBreak, cdgFalse, "FALSE",
                            getFalseLabeledEdge(breakCond));
                    }
                }
            }
        }
    }

    // =========================================================================
    // STEP 11: ADD MISSING IF-NO-ELSE FALSE EDGES
    //
    // For if-no-else predicates that have a TRUE CDG edge but no FALSE CDG edge,
    // add the FALSE edge to the CFG FalseEdge successor.
    //
    // Suppression: false successor already loop-governed via TRUE (fun9).
    // No topLevelNodes guard — if-chain FALSE edges must always be emitted.
    // =========================================================================

    private void addMissingIfNoElseFalseEdges() {
        for (CFGNode a : originalCFG.vertexSet()) {
            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;
            if (allLoopConditions.contains(a)) continue;
            if (allBreakConditions.contains(a)) continue;

            CDGNode cdgA = cfgToCdgMap.get(a);
            if (cdgA == null) continue;

            boolean hasTrueEdge  = false;
            boolean hasFalseEdge = false;
            for (ControlDependenceEdge e : this.outgoingEdgesOf(cdgA)) {
                if ("TRUE".equals(e.toString()))  hasTrueEdge  = true;
                if ("FALSE".equals(e.toString())) hasFalseEdge = true;
            }
            if (!hasTrueEdge || hasFalseEdge) continue;

            if (isGovernedByLoopCondition(falseSucc)) continue;
            if (flowsDirectlyToEnd(falseSucc)) continue;
            if (isIfNoElse(a) && isIfPredicate(falseSucc)) continue;
            if (!isIfNoElse(a) && isSwitchExpressionNode(falseSucc)) continue;

            CDGNode cdgFalse = cfgToCdgMap.get(falseSucc);
            if (cdgFalse == null) continue;

            addEdgeIfAbsent(cdgA, cdgFalse, "FALSE", getFalseLabeledEdge(a));
        }
    }

    // =========================================================================
    // STEP 11b: PARENT IF → NESTED LOOP HEADER (fun2, fun3, fun10)
    // =========================================================================

    private void addGovernanceToNestedLoops() {
        for (CFGNode ifNode : originalCFG.vertexSet()) {
            if (!isIfPredicate(ifNode)) continue;

            CFGNode trueSucc  = getTrueSuccessor(ifNode);
            CFGNode falseSucc = getFalseSuccessor(ifNode);
            CDGNode cdgIf = cfgToCdgMap.get(ifNode);
            if (cdgIf == null) continue;

            Set<CFGNode> trueRegion = collectLoopBody(ifNode, falseSucc, trueSucc);
            Set<CFGNode> falseRegion = isIfNoElse(ifNode)
                    ? Collections.emptySet()
                    : collectLoopBody(ifNode, trueSucc, falseSucc);

            for (CFGNode lc : allLoopConditions) {
                CDGNode cdgLc = cfgToCdgMap.get(lc);
                if (cdgLc == null) continue;
                if (trueRegion.contains(lc)) {
                    addEdgeIfAbsent(cdgIf, cdgLc, "TRUE", getTrueLabeledEdge(ifNode));
                }
                if (falseRegion.contains(lc)) {
                    addEdgeIfAbsent(cdgIf, cdgLc, "FALSE", getFalseLabeledEdge(ifNode));
                }
            }
            for (CFGNode sw : originalCFG.vertexSet()) {
                if (!isSwitchExpressionNode(sw)) continue;
                CDGNode cdgSw = cfgToCdgMap.get(sw);
                if (cdgSw == null) continue;
                if (trueRegion.contains(sw)) {
                    addEdgeIfAbsent(cdgIf, cdgSw, "TRUE", getTrueLabeledEdge(ifNode));
                }
                if (falseRegion.contains(sw)) {
                    addEdgeIfAbsent(cdgIf, cdgSw, "FALSE", getFalseLabeledEdge(ifNode));
                }
            }
        }
    }

    // =========================================================================
    // STEP 12: ATTACH ENTRY FLOW EDGES
    // =========================================================================

    private void attachEntryEdges() {
        for (CFGNode n : entryFlowNodes) {
            CDGNode cdgN = cfgToCdgMap.get(n);
            if (cdgN == null) continue;

            boolean suppressFlow = false;
            for (ControlDependenceEdge inE : this.incomingEdgesOf(cdgN)) {
                CDGNode src = this.getEdgeSource(inE);
                if (src == null || src.equals(entryNode)) continue;

                CFGNode srcCfg = src.getOriginalCFGNode();
                String  lbl    = inE.toString();
                if ("FLOW".equals(lbl)) continue;

                // If/switch/break TRUE/FALSE never block ENTRY FLOW on eligible nodes.
                if ("TRUE".equals(lbl) || "FALSE".equals(lbl)) continue;

                // Case labels from switches block ENTRY on case bodies only.
                if (!"TRUE".equals(lbl) && !"FALSE".equals(lbl) && !"FLOW".equals(lbl)) {
                    suppressFlow = true;
                    break;
                }
            }

            if (!suppressFlow) {
                addEdgeIfAbsent(entryNode, cdgN, "FLOW", null);
            }
        }
    }

    // =========================================================================
    // STRUCTURAL HELPERS
    // =========================================================================

    private Set<CFGNode> collectLoopBody(CFGNode loopHead,
                                          CFGNode loopExit,
                                          CFGNode start) {
        Set<CFGNode>   visited  = new HashSet<>();
        Deque<CFGNode> worklist = new ArrayDeque<>();
        worklist.push(start);
        while (!worklist.isEmpty()) {
            CFGNode cur = worklist.pop();
            if (cur.equals(loopHead)) continue;
            if (cur.equals(loopExit)) continue;
            if (!visited.add(cur)) continue;
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
                CFGNode t = originalCFG.getEdgeTarget(e);
                if (t != null) worklist.push(t);
            }
        }
        return visited;
    }

    private boolean loopBodyCanReachHead(Set<CFGNode> body, CFGNode head) {
        for (CFGNode n : body)
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(n))
                if (originalCFG.getEdgeTarget(e).equals(head)) return true;
        return false;
    }

    private Set<CFGNode> collectLoopInitNodes(CFGNode loopHead) {
        Set<CFGNode> inits = new HashSet<>();
        for (LabeledEdge e : originalCFG.incomingEdgesOf(loopHead)) {
            if (!(e instanceof FlowEdge)) continue;
            CFGNode pred = originalCFG.getEdgeSource(e);
            if (pred == null || pred.equals(originalCFG.getStart())) continue;
            if (isGenuineLoopInit(pred)) inits.add(pred);
        }
        return inits;
    }

    private boolean isGenuineLoopInit(CFGNode node) {
        int out = 0;
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(node)) {
            out++;
            if (e instanceof TrueEdge || e instanceof FalseEdge) return false;
        }
        if (out != 1) return false;
        for (LabeledEdge e : originalCFG.incomingEdgesOf(node))
            if (e instanceof TrueEdge || e instanceof FalseEdge) return false;
        return true;
    }

    private boolean isBreakCondition(CFGNode node,
                                      Set<CFGNode> loopBody,
                                      CFGNode loopHead) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(node)) {
            if (e instanceof TrueEdge) {
                CFGNode t = originalCFG.getEdgeTarget(e);
                if (!loopBody.contains(t) && !t.equals(loopHead)) return true;
            }
        }
        return false;
    }

    private boolean isGovernedByNestedPredicate(CDGNode cdgBody,
                                                 CDGNode cdgLoopCond,
                                                 Set<CFGNode> loopBody) {
        for (ControlDependenceEdge e : this.incomingEdgesOf(cdgBody)) {
            CDGNode src = this.getEdgeSource(e);
            if (src.equals(cdgLoopCond) || src.equals(entryNode)) continue;
            String lbl = e.toString();
            if ("TRUE".equals(lbl) || "FALSE".equals(lbl)) {
                CFGNode srcCfg = src.getOriginalCFGNode();
                if (srcCfg != null && loopBody.contains(srcCfg)) return true;
            }
        }
        return false;
    }

    private boolean isGovernedByLoopCondition(CFGNode cfgNode) {
        CDGNode cdn = cfgToCdgMap.get(cfgNode);
        if (cdn == null) return false;
        for (ControlDependenceEdge e : this.incomingEdgesOf(cdn)) {
            if (!"TRUE".equals(e.toString())) continue;
            CFGNode srcCfg = this.getEdgeSource(e).getOriginalCFGNode();
            if (srcCfg != null && allLoopConditions.contains(srcCfg)) return true;
        }
        return false;
    }

    private Set<CFGNode> getLoopBody(CFGNode loopCond) {
        Set<CFGNode> cached = loopBodiesByHead.get(loopCond);
        if (cached != null) return cached;
        CFGNode ts = getTrueSuccessor(loopCond);
        CFGNode fs = getFalseSuccessor(loopCond);
        if (ts == null || fs == null) return Collections.emptySet();
        return collectLoopBody(loopCond, fs, ts);
    }

    /** Innermost loop whose body contains {@code node} (smallest body wins). */
    private CFGNode findEnclosingLoop(CFGNode node) {
        CFGNode innermost = null;
        int smallestBody = Integer.MAX_VALUE;
        for (CFGNode loopCond : allLoopConditions) {
            Set<CFGNode> body = getLoopBody(loopCond);
            if (!body.contains(node)) continue;
            if (body.size() < smallestBody) {
                smallestBody = body.size();
                innermost = loopCond;
            }
        }
        return innermost;
    }

    // =========================================================================
    // CFG NAVIGATION HELPERS
    // =========================================================================

    private CFGNode getTrueSuccessor(CFGNode n) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n))
            if (e instanceof TrueEdge) return originalCFG.getEdgeTarget(e);
        return null;
    }

    private CFGNode getFalseSuccessor(CFGNode n) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n))
            if (e instanceof FalseEdge) return originalCFG.getEdgeTarget(e);
        return null;
    }

    private LabeledEdge getTrueLabeledEdge(CFGNode n) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n))
            if (e instanceof TrueEdge) return e;
        return null;
    }

    private LabeledEdge getFalseLabeledEdge(CFGNode n) {
        for (LabeledEdge e : originalCFG.outgoingEdgesOf(n))
            if (e instanceof FalseEdge) return e;
        return null;
    }

    private String getLabel(LabeledEdge e) {
        if (e instanceof TrueEdge)  return "TRUE";
        if (e instanceof FalseEdge) return "FALSE";
        if (e instanceof CaseEdge) {
            Object lbl = e.getLabel();
            return (lbl != null) ? lbl.toString() : "FLOW";
        }
        return "FLOW";
    }

    // =========================================================================
    // CDG EDGE HELPERS
    // =========================================================================

    private boolean hasEdge(CDGNode source, CDGNode target, String label) {
        for (ControlDependenceEdge e : this.outgoingEdgesOf(source))
            if (this.getEdgeTarget(e).equals(target) && label.equals(e.toString()))
                return true;
        return false;
    }

    private void addEdgeIfAbsent(CDGNode source, CDGNode target,
                                  String label, LabeledEdge cfgEdge) {
        if (!hasEdge(source, target, label))
            this.addEdge(source, target, new ControlDependenceEdge(label, cfgEdge));
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public CFG     getOriginalCFG()            { return originalCFG;              }
    public CDGNode getEntryNode()              { return entryNode;                }
    public CDGNode getCDGNode(CFGNode cfgNode) { return cfgToCdgMap.get(cfgNode); }
}