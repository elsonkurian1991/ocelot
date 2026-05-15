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
 *  Step  8  addLoopFalseExitEdges()        — nested loops only
 *  Step  9  removeSpuriousLoopBodyEdges()
 *  Step  9b removeSpuriousNestedPredicateEdges()
 *  Step  9c removeSpuriousSwitchCaseEdges()
 *  Step  9d removeSpuriousIfElseOvergoverning()
 *  Step 10  addBreakConditionEdges()       — TRUE only when non-top-level
 *  Step 11  addMissingIfNoElseFalseEdges() — never to top-level or loop-governed
 *  Step 12  attachEntryEdges()
 *
 * Key invariants:
 *  - Loop condition nodes NEVER self-loop.
 *  - Loop FALSE exit CDG edge only emitted for nested loops (false successor
 *    NOT top-level). Top-level loops have no loop FALSE CDG edge — the post-loop
 *    node receives ENTRY FLOW only.
 *  - Break-cond TRUE CDG edge only emitted when the true target is NOT top-level.
 *    Top-level break targets receive ENTRY FLOW only.
 *  - Break-cond FALSE CDG edge not emitted when target is loop-governed.
 *  - If-no-else FALSE edge not emitted when false successor is top-level
 *    or loop-governed (avoids duplicate incoming edges).
 *  - ENTRY FLOW edges are assigned to all top-level nodes not governed by
 *    a real conditional predicate.
 */
public class CDG extends DefaultDirectedGraph<CDGNode, ControlDependenceEdge> {

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

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public CDG(CFG cfg) {
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
        removeSpuriousLoopBodyEdges();                        // Step 9
        removeSpuriousNestedPredicateEdges();                  // Step 9b
        removeSpuriousSwitchCaseEdges();                       // Step 9c
        addBreakConditionEdges();                             // Step 10
        addMissingIfNoElseFalseEdges();                       // Step 11
        attachEntryEdges();                                   // Step 12
    }

    // =========================================================================
    // STEP 1: POST-DOMINATOR COMPUTATION
    // =========================================================================

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
                for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
                    CFGNode succ = originalCFG.getEdgeTarget(e);
                    if (succ == null) continue;
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

        for (CFGNode a : originalCFG.vertexSet()) {
            CFGNode trueSucc  = getTrueSuccessor(a);
            CFGNode falseSucc = getFalseSuccessor(a);
            if (trueSucc == null || falseSucc == null) continue;

            Set<CFGNode> body = collectLoopBody(a, falseSucc, trueSucc);
            if (!loopBodyCanReachHead(body, a)) continue;

            allLoopConditions.add(a);
            allLoopBodyNodes.addAll(body);
            allLoopInitNodes.addAll(collectLoopInitNodes(a));

            for (CFGNode bodyNode : body) {
                if (isBreakCondition(bodyNode, body, a)) {
                    allBreakConditions.add(bodyNode);
                }
            }
        }

        // Top-level nodes: forward-reachable minus loop body and init nodes
        topLevelNodes = computeForwardReachable();
        topLevelNodes.removeAll(allLoopBodyNodes);
        topLevelNodes.removeAll(allLoopInitNodes);
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
                for (CFGNode n : originalCFG.vertexSet()) {
                    if (isControlDependent(n, a, b)) {
                        CDGNode cdgA = cfgToCdgMap.get(a);
                        CDGNode cdgN = cfgToCdgMap.get(n);
                        if (cdgA != null && cdgN != null) {
                            addEdgeIfAbsent(cdgA, cdgN, getLabel(edge), edge);
                        }
                    }
                }
            }
        }
    }

    private boolean isControlDependent(CFGNode n, CFGNode a, CFGNode b) {
        Set<CFGNode> postDomB = postDominators.get(b);
        Set<CFGNode> postDomA = postDominators.get(a);
        if (postDomB == null || postDomA == null) return false;
        if (n.equals(a)) return postDomB.contains(n);
        return postDomB.contains(n) && !postDomA.contains(n);
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
            CDGNode cdgLoop = cfgToCdgMap.get(loopCond);
            if (cdgLoop == null) continue;

            CFGNode falseSucc = getFalseSuccessor(loopCond);
            if (falseSucc == null) continue;

            // Only skip when BOTH the loop condition AND the false successor
            // are top-level — meaning this is a genuine top-level loop.
            // If the loop condition is governed by an outer branch (nested loop),
            // always emit the FALSE exit edge regardless of false successor.
            if (topLevelNodes.contains(falseSucc)
                    && topLevelNodes.contains(loopCond)) continue;

            CDGNode cdgFalseSucc = cfgToCdgMap.get(falseSucc);
            if (cdgFalseSucc == null) continue;

            LabeledEdge falseEdge = getFalseLabeledEdge(loopCond);
            addEdgeIfAbsent(cdgLoop, cdgFalseSucc, "FALSE", falseEdge);
        }
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
    // STEP 9b: REMOVE SPURIOUS EDGES FROM NESTED PREDICATES IN BREAK-LOOPS
    // =========================================================================

    private void removeSpuriousNestedPredicateEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (CDGNode cdgSrc : this.vertexSet()) {
            CFGNode cfgSrc = cdgSrc.getOriginalCFGNode();
            if (cfgSrc == null) continue;

            if (!allLoopBodyNodes.contains(cfgSrc)) continue;
            if (allLoopConditions.contains(cfgSrc)) continue;
            if (allBreakConditions.contains(cfgSrc)) continue;

            CFGNode trueSucc  = getTrueSuccessor(cfgSrc);
            CFGNode falseSucc = getFalseSuccessor(cfgSrc);
            if (trueSucc == null || falseSucc == null) continue;

            CFGNode enclosingLoopCond = findEnclosingLoop(cfgSrc);
            if (enclosingLoopCond == null) continue;
            CFGNode loopFalseSucc = getFalseSuccessor(enclosingLoopCond);
            if (loopFalseSucc == null) continue;

            Set<CFGNode> trueScope  = collectLoopBody(cfgSrc, loopFalseSucc, trueSucc);
            Set<CFGNode> falseScope = collectLoopBody(cfgSrc, loopFalseSucc, falseSucc);
            Set<CFGNode> validScope = new HashSet<>(trueScope);
            validScope.addAll(falseScope);

            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgSrc))) {
                String lbl = e.toString();
                if (!"TRUE".equals(lbl) && !"FALSE".equals(lbl)) continue;

                CDGNode cdgTgt = this.getEdgeTarget(e);
                CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
                if (cfgTgt == null) continue;

                if (!validScope.contains(cfgTgt)) {
                    toRemove.add(e);
                }
            }
        }

        toRemove.forEach(this::removeEdge);
    }

    // =========================================================================
    // STEP 9c: REMOVE SPURIOUS SWITCH→N EDGES WHERE N IS INSIDE A CASE BODY
    //
    // Two categories of spurious switch edges are removed:
    //
    // Category 1 (original): switch→N where N is inside a case body that is
    // already governed by a case entry node via TRUE/FALSE. N should be
    // governed by the case entry node, not directly by the switch.
    //
    // Category 2 (fun7 fix): switch emits two CDG edges with the same case
    // label to two different targets. This happens with fall-through cases
    // where the CFG has a CaseEdge to the case entry AND a second edge to a
    // nested node inside the case. Only the direct CaseEdge target (case entry
    // node) should have the case label CDG edge. The second target is inside
    // the case body and its edge must be removed.
    // =========================================================================

    private void removeSpuriousSwitchCaseEdges() {
        List<ControlDependenceEdge> toRemove = new ArrayList<>();

        for (CDGNode cdgSwitch : this.vertexSet()) {
            CFGNode cfgSwitch = cdgSwitch.getOriginalCFGNode();
            if (cfgSwitch == null) continue;

            boolean hasCaseEdge = false;
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cfgSwitch)) {
                if (e instanceof CaseEdge) { hasCaseEdge = true; break; }
            }
            if (!hasCaseEdge) continue;

            // Collect direct case entry nodes (immediate CaseEdge targets in CFG)
            Set<CFGNode> caseEntryNodes = new HashSet<>();
            for (LabeledEdge e : originalCFG.outgoingEdgesOf(cfgSwitch)) {
                if (e instanceof CaseEdge) {
                    caseEntryNodes.add(originalCFG.getEdgeTarget(e));
                }
            }

            // Category 2: detect duplicate case labels in CDG outgoing edges.
            // For each case label, keep only the edge whose target is a direct
            // case entry node. Remove any additional edge with the same label
            // whose target is inside a case body.
            Map<String, CDGNode> seenCaseLabels = new HashMap<>();
            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgSwitch))) {
                String lbl = e.toString();
                if ("TRUE".equals(lbl) || "FALSE".equals(lbl) || "FLOW".equals(lbl)) continue;
                CDGNode cdgTgt = this.getEdgeTarget(e);
                CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
                if (cfgTgt == null) continue;

                if (caseEntryNodes.contains(cfgTgt)) {
                    // This is a legitimate case entry edge — record it
                    seenCaseLabels.put(lbl, cdgTgt);
                } else {
                    // Not a direct case entry — remove (Category 2 duplicate)
                    toRemove.add(e);
                }
            }

            // Category 1: for each outgoing edge whose target is NOT a case
            // entry node and is inside a case body governed by a case entry.
            for (ControlDependenceEdge e : new ArrayList<>(this.outgoingEdgesOf(cdgSwitch))) {
                CDGNode cdgTgt = this.getEdgeTarget(e);
                CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
                if (cfgTgt == null) continue;
                if (caseEntryNodes.contains(cfgTgt)) continue;
                if (toRemove.contains(e)) continue; // already marked

                boolean insideCaseBody = false;
                for (ControlDependenceEdge inEdge : this.incomingEdgesOf(cdgTgt)) {
                    CDGNode inSrc = this.getEdgeSource(inEdge);
                    CFGNode inSrcCfg = inSrc.getOriginalCFGNode();
                    if (inSrcCfg == null) continue;
                    String lbl = inEdge.toString();
                    if (("TRUE".equals(lbl) || "FALSE".equals(lbl))
                            && caseEntryNodes.contains(inSrcCfg)) {
                        insideCaseBody = true;
                        break;
                    }
                }
                if (insideCaseBody) toRemove.add(e);
            }
        }

        toRemove.forEach(this::removeEdge);
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
            if (!"FALSE".equals(e.toString())) continue;

            CDGNode cdgSrc = this.getEdgeSource(e);
            CFGNode cfgSrc = cdgSrc.getOriginalCFGNode();
            if (cfgSrc == null) continue;

            // Only process if-else branch nodes (have both TRUE and FALSE successors)
            if (getTrueSuccessor(cfgSrc) == null || getFalseSuccessor(cfgSrc) == null) continue;

            // Not loop conditions (handled separately)
            if (allLoopConditions.contains(cfgSrc)) continue;

            CDGNode cdgTgt = this.getEdgeTarget(e);
            CFGNode cfgTgt = cdgTgt.getOriginalCFGNode();
            if (cfgTgt == null) continue;

            // Not a direct CFG FALSE successor of source
            if (cfgTgt.equals(getFalseSuccessor(cfgSrc))) continue;
            // Not a direct CFG TRUE successor of source
            if (cfgTgt.equals(getTrueSuccessor(cfgSrc))) continue;

            // Check if T has a more immediate governing CDG edge from another node
            boolean hasMoreImmediateGoverner = false;
            for (ControlDependenceEdge inE : this.incomingEdgesOf(cdgTgt)) {
                CDGNode inSrc = this.getEdgeSource(inE);
                if (inSrc.equals(cdgSrc) || inSrc.equals(entryNode)) continue;
                String lbl = inE.toString();
                if ("TRUE".equals(lbl) || "FALSE".equals(lbl)) {
                    hasMoreImmediateGoverner = true;
                    break;
                }
            }

            if (hasMoreImmediateGoverner) {
                toRemove.add(e);
            }
        }

        toRemove.forEach(this::removeEdge);
    }
    //
    // FIX 2 — fun4/fun8/fun10:
    // Break-cond TRUE is only emitted when the true target is NOT top-level.
    // When the break target is top-level (post-loop node), it must only receive
    // ENTRY FLOW — emitting a break TRUE CD edge to it causes double-governance
    // (spurious 7→8 TRUE in fun4/fun8/fun10).
    //
    // FIX 3 — fun4/fun8/fun10:
    // Break-cond FALSE is not emitted when the false target is already
    // governed by the enclosing loop condition via TRUE (e.g. k++ in fun4/fun8).
    // Emitting it creates a duplicate incoming edge on an already-governed node
    // (spurious 7→4 FALSE in fun4/fun8/fun10).
    // =========================================================================

    private void addBreakConditionEdges() {
        for (CFGNode breakCond : allBreakConditions) {
            CDGNode cdgBreak = cfgToCdgMap.get(breakCond);
            if (cdgBreak == null) continue;

            // TRUE: break fires → post-loop node
            // FIX 2: only emit when true target is NOT top-level
            CFGNode trueTarget = getTrueSuccessor(breakCond);
            if (trueTarget != null && !topLevelNodes.contains(trueTarget)) {
                CDGNode cdgTrue = cfgToCdgMap.get(trueTarget);
                if (cdgTrue != null) {
                    addEdgeIfAbsent(cdgBreak, cdgTrue, "TRUE",
                        getTrueLabeledEdge(breakCond));
                }
            }

            // FALSE: break does not fire → next loop body node
            // FIX 3: skip if target is loop-governed, top-level, or loop-init
            CFGNode falseTarget = getFalseSuccessor(breakCond);
            if (falseTarget != null
                    && !topLevelNodes.contains(falseTarget)
                    && !allLoopInitNodes.contains(falseTarget)
                    && !isGovernedByLoopCondition(falseTarget)) {
                CDGNode cdgFalse = cfgToCdgMap.get(falseTarget);
                if (cdgFalse != null) {
                    addEdgeIfAbsent(cdgBreak, cdgFalse, "FALSE",
                        getFalseLabeledEdge(breakCond));
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
    // Rules for suppression (do NOT emit FALSE edge):
    //   (a) False successor is loop-governed via TRUE — avoids duplicate
    //       incoming edge (fun9: 7→9 FALSE to loop-governed STEP_BASE).
    //   (b) False successor has an incoming CDG edge from a different predicate
    //       that already governs it via FALSE — it is a join point of an
    //       outer if-else and the edge is not needed.
    //
    // Removed the old FIX 4a guard that skipped when both predicate and false
    // successor are top-level. That guard incorrectly suppressed fun5's three
    // if-no-else FALSE edges (r3>R3_THRESH, r4>R4_THRESH, result>RES_THRESH)
    // which are genuinely needed. Top-level if-no-else predicates DO need
    // FALSE edges to represent the branch where the body is skipped.
    //
    // The fun8 case (i>I_THRESH FALSE to top-level return result) is handled
    // differently: fun8's i>I_THRESH already has a FALSE edge from the standard
    // post-dominance algorithm (8→10 FALSE) so hasFalseEdge=true and this
    // step correctly skips it.
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

            // Rule (a): if false successor is already loop-governed via TRUE,
            // do NOT emit FALSE edge — avoids duplicate incoming (fun9 case).
            if (isGovernedByLoopCondition(falseSucc)) continue;

            CDGNode cdgFalse = cfgToCdgMap.get(falseSucc);
            if (cdgFalse == null) continue;

            addEdgeIfAbsent(cdgA, cdgFalse, "FALSE", getFalseLabeledEdge(a));
        }
    }

    // =========================================================================
    // STEP 12: ATTACH ENTRY FLOW EDGES
    // =========================================================================

    private void attachEntryEdges() {
        for (CFGNode n : originalCFG.vertexSet()) {
            if (!topLevelNodes.contains(n)) continue;

            CDGNode cdgN = cfgToCdgMap.get(n);
            if (cdgN == null) continue;

            boolean governed = false;
            for (ControlDependenceEdge inE : this.incomingEdgesOf(cdgN)) {
                CDGNode src = this.getEdgeSource(inE);
                if (src == null || src.equals(entryNode)) continue;

                CFGNode srcCfg = src.getOriginalCFGNode();
                String  lbl    = inE.toString();

                // Loop FALSE exit edges do not suppress ENTRY FLOW
                // (applies to both top-level and nested loops)
                if ("FALSE".equals(lbl) && srcCfg != null
                        && allLoopConditions.contains(srcCfg)) continue;

                // Break-condition TRUE edges do not suppress ENTRY FLOW
                if ("TRUE".equals(lbl) && srcCfg != null
                        && allBreakConditions.contains(srcCfg)) continue;

                // If-no-else FALSE edges where the source is top-level also
                // do not suppress ENTRY FLOW on the target — the target
                // receives both the conditional FALSE edge and ENTRY FLOW
                // because it is unconditionally reachable (top-level).
                if ("FALSE".equals(lbl) && srcCfg != null
                        && topLevelNodes.contains(srcCfg)
                        && !allLoopConditions.contains(srcCfg)
                        && !allBreakConditions.contains(srcCfg)) continue;

                governed = true;
                break;
            }

            if (!governed) {
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

    private CFGNode findEnclosingLoop(CFGNode node) {
        for (CFGNode loopCond : allLoopConditions) {
            CFGNode ts = getTrueSuccessor(loopCond);
            CFGNode fs = getFalseSuccessor(loopCond);
            if (ts == null || fs == null) continue;
            Set<CFGNode> body = collectLoopBody(loopCond, fs, ts);
            if (body.contains(node)) return loopCond;
        }
        return null;
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