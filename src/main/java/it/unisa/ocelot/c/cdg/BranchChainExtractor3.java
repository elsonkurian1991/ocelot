package it.unisa.ocelot.c.cdg;

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.cdt.core.dom.ast.*;

/**
 * Extracts branch chains from a CDG using a pure CDG-based DFS.
 *
 * <h2>Core algorithm</h2>
 * <p>A branch chain is a sequence of (conditionNode, branchLabel) pairs that
 * describes a unique path through the program's control decisions, ending at
 * a real terminal node (return / break / goto / continue).
 *
 * <p>The algorithm performs a depth-first visit of the CDG starting from
 * every top-level condition node (a condition node that is not itself
 * controlled by any other condition).  At each step it follows the
 * condition's dependents ({@code cdg.incomingEdgesOf}), accumulates the
 * branch label, and recurses into nested conditions.  When it reaches a
 * real leaf it records the accumulated label sequence as a complete chain.
 *
 * <h2>Edge-direction convention</h2>
 * <p>Edges are stored as  <b>dependent → condition</b>, so:
 * <ul>
 *   <li>{@code cdg.outgoingEdgesOf(X)} — conditions that X is guarded by.</li>
 *   <li>{@code cdg.incomingEdgesOf(X)} — dependents that X controls.</li>
 *   <li>{@code cdg.getEdgeSource(e)}   — the dependent node.</li>
 *   <li>{@code cdg.getEdgeTarget(e)}   — the condition node.</li>
 * </ul>
 */
public class BranchChainExtractor3 {

    private final CDG    cdg;
    private final String unitComponentName;
    private final Map<IASTExpression, Integer> branchChainsMap;

    /** Chains collected during the current extraction run. */
    private List<BranchChain> branchChains;

    /**
     * Maps each condition node to a stable sequential index (branch0, branch1 …).
     * Built once at the start of each extraction run.
     */
    private final Map<CDGNode, Integer> branchIndexMap = new LinkedHashMap<>();

    /**
     * Per-run cache: condition node → assigned branch id.
     * Ensures the same node always produces the same id within one run.
     */
    private final Map<CDGNode, Integer> localNodeIdMap = new HashMap<>();

    /** Fallback counter for nodes not found in either map. */
    private int idCounter = 0;

    // -----------------------------------------------------------------------

    public BranchChainExtractor3(CDG cdg,
                                  String unitComponentName,
                                  Map<IASTExpression, Integer> branchChainsMap) {
        this.cdg                = cdg;
        this.unitComponentName  = unitComponentName;
        this.branchChainsMap    = branchChainsMap;
        this.branchChains       = new ArrayList<>();
    }

    // =========================================================================
    // Public entry point
    // =========================================================================

    /**
     * Extracts all branch chains from the CDG.
     *
     * <p>Steps:
     * <ol>
     *   <li>Build the branch-index map (stable ids for condition nodes).</li>
     *   <li>Find every top-level condition node (not controlled by any other
     *       condition in this CDG).</li>
     *   <li>Run a CDG-DFS from each top-level condition, accumulating branch
     *       labels and emitting a chain whenever a real leaf is reached.</li>
     *   <li>Remove duplicate / redundant chains.</li>
     *   <li>Assign human-readable labels.</li>
     * </ol>
     *
     * @return the final deduplicated list of branch chains
     */
    public List<BranchChain> extractBranchChains() throws Exception {
        branchChains.clear();
        localNodeIdMap.clear();
        idCounter = 0;

        buildBranchIndexMap();

        // Top-level condition nodes: condition nodes that are not themselves
        // dependent on any other condition, i.e. outgoingEdgesOf is empty.
        List<CDGNode> topLevelConditions = cdg.vertexSet().stream()
                .filter(this::isConditionNode)
                .filter(n -> cdg.outgoingEdgesOf(n).isEmpty())
                .sorted(Comparator.comparingInt(CDGNode::getId))
                .collect(Collectors.toList());

        // DFS from each top-level condition with an empty accumulated path.
        for (CDGNode root : topLevelConditions) {
            cdgDfs(root, new ArrayList<>(), new HashSet<>());
        }

        removeDuplicatesAndRedundant();

        for (int i = 0; i < branchChains.size(); i++) {
            branchChains.get(i).setLabel(unitComponentName, i + 1);
        }

        return branchChains;
    }

    // =========================================================================
    // CDG depth-first search
    // =========================================================================

    /**
     * Recursive CDG-DFS starting at {@code conditionNode}.
     *
     * For each dependent of {@code conditionNode}:
     *   - Real leaf (return/break/goto/continue): emit chain immediately.
     *   - Nested condition: recurse deeper to collect sub-branches.
     *   - Plain statement (assignment, declaration, etc.): this IS the
     *     branch outcome node — emit it as the leaf. The CDG says this
     *     node executes conditionally; it is the observable effect of
     *     taking this branch.
     *
     * This unified treatment means fun1 (return inside if) and fun2
     * (assignment inside if, return outside) both produce correct chains.
     */
    private void cdgDfs(CDGNode        conditionNode,
                        List<PathStep> currentPath,
                        Set<CDGNode>   visited) {

        if (visited.contains(conditionNode)) return;
        visited.add(conditionNode);

        for (ControlDependenceEdge edge : cdg.incomingEdgesOf(conditionNode)) {

            CDGNode dependent = cdg.getEdgeSource(edge);
            if (dependent == null) continue;

            PathStep step = buildStep(conditionNode, dependent, edge);
            currentPath.add(step);

            if (isConditionNode(dependent)) {
                // Nested condition — recurse to collect deeper branches.
                cdgDfs(dependent, currentPath, visited);

            } else {
                // Either a real terminal (return/break) OR a plain branch-
                // outcome node (assignment, declaration, etc.).
                // Both cases represent the observable effect of this branch
                // decision — emit the chain.
                // Entry and exit nodes are excluded (structural guard).
                if (dependent.id != cdg.getEntryId()
                        && dependent.id != cdg.getExitId()) {
                    branchChains.add(new BranchChain(
                            dependent,
                            new ArrayList<>(currentPath),
                            unitComponentName,
                            0));
                }
            }

            currentPath.remove(currentPath.size() - 1);
        }

        visited.remove(conditionNode);
    }

    // =========================================================================
    // PathStep construction
    // =========================================================================

    /**
     * Builds a {@link PathStep} for the CDG edge
     * {@code conditionNode} --[label]--> {@code dependent}.
     *
     * <p>If the step represents a branch condition (TRUE / FALSE / case),
     * attaches a human-readable branch-condition label such as
     * {@code "fun1:branch0-true"}.
     */
    private PathStep buildStep(CDGNode              conditionNode,
                               CDGNode              dependent,
                               ControlDependenceEdge edge) {

        PathStep step = new PathStep(conditionNode, dependent, edge);

        if (step.hasBranchCondition()) {
            int    branchId = resolveBranchId(conditionNode, dependent);
            String outcome  = resolveOutcomeLabel(conditionNode, dependent, edge);
            step.setBranchConditionLabel(
                    unitComponentName + ":branch" + branchId + "-" + outcome);
        }

        return step;
    }

    /**
     * Resolves the display outcome label for a branch step.
     *
     * <p>For switch / case edges the structural case label is preferred
     * (e.g. {@code "0"}, {@code "1"}, {@code "default"}).
     * For ordinary if/loop edges the edge label is normalised to
     * {@code "true"} or {@code "false"}.
     */
    private String resolveOutcomeLabel(CDGNode              conditionNode,
                                       CDGNode              dependent,
                                       ControlDependenceEdge edge) {
        String outcome;

        if (isSwitchNode(conditionNode)) {
            // Try the structural case label stored in the CDG first.
            String caseLabel = cdg.getSwitchCaseLabel(
                    conditionNode.getId(), dependent.getId());
            if (caseLabel != null && !caseLabel.isBlank()) {
                outcome = caseLabel;
            } else {
                String edgeStr = edge.toString().toUpperCase();
                boolean isTrueFalse = edgeStr.contains("TRUE")
                                   || edgeStr.contains("FALSE")
                                   || edgeStr.contains("FLOW");
                outcome = isTrueFalse ? "default" : edgeStr;
            }
        } else {
            outcome = edge.toString();
        }

        // Sanitise to a URL/filename-safe token.
        return outcome
                .replace("'", "")
                .replace(':', '_')
                .replace('-', '_')
                .trim()
                .toLowerCase();
    }

    // =========================================================================
    // Branch-index map
    // =========================================================================

    /**
     * Assigns a stable sequential index to every condition node.
     * Nodes are sorted by id for deterministic output across runs.
     */
    private void buildBranchIndexMap() {
        branchIndexMap.clear();

        List<CDGNode> conditions = cdg.vertexSet().stream()
                .filter(this::isConditionNode)
                .sorted(Comparator.comparingInt(CDGNode::getId))
                .collect(Collectors.toList());

        int index = 0;
        for (CDGNode node : conditions) {
            branchIndexMap.put(node, index++);
        }
    }

    /**
     * Returns a stable branch id for {@code conditionNode}.
     *
     * <p>For switch nodes the id is keyed by (condition, dependent) pair so
     * that each case arm gets its own unique id.  For ordinary if/loop nodes
     * the id is keyed by condition node alone.
     */
    private int resolveBranchId(CDGNode conditionNode, CDGNode dependent) {
        if (isSwitchNode(conditionNode)) {
            // Each case arm of a switch needs its own id.
            // We use a synthetic key stored in localNodeIdMap via a wrapper node
            // trick: we use the dependent node as the cache key for switch arms.
            if (localNodeIdMap.containsKey(dependent)) {
                return localNodeIdMap.get(dependent);
            }
            int id = branchIndexMap.getOrDefault(conditionNode, idCounter++);
            localNodeIdMap.put(dependent, id);
            return id;
        }

        if (localNodeIdMap.containsKey(conditionNode)) {
            return localNodeIdMap.get(conditionNode);
        }

        int assignedId = branchIndexMap.getOrDefault(conditionNode, -1);

        if (assignedId == -1) {
            IASTExpression expr = extractExpression(conditionNode);
            if (expr != null && branchChainsMap != null) {
                String sig = expr.getRawSignature();
                for (Map.Entry<IASTExpression, Integer> e : branchChainsMap.entrySet()) {
                    if (e.getKey().getRawSignature().equals(sig)) {
                        assignedId = e.getValue();
                        break;
                    }
                }
            }
        }

        if (assignedId == -1) assignedId = idCounter++;

        localNodeIdMap.put(conditionNode, assignedId);
        return assignedId;
    }

    // =========================================================================
    // Node classification helpers
    // =========================================================================

    /**
     * A node is a condition node if at least one other node is dependent on
     * it with a TRUE, FALSE, or case label.
     * In the dependent→condition convention, these appear as incoming edges
     * of the condition node.
     */
    private boolean isConditionNode(CDGNode node) {
        for (ControlDependenceEdge e : cdg.incomingEdgesOf(node)) {
            String lbl = e.toString().toUpperCase();
            if (lbl.contains("TRUE") || lbl.contains("FALSE")) return true;
            // Case edges are also branch conditions
            if (!lbl.contains("FLOW")) return true;
        }
        return false;
    }

    /**
     * A real leaf is a terminal statement node: return, break, goto, or
     * continue.  Plain assignments, declarations, and loop-increment nodes
     * are excluded even though they may have no incoming CDG edges.
     *
     * <p>Three-layer check:
     * <ol>
     *   <li>Structural: always exclude entry and exit nodes.</li>
     *   <li>AST: inspect the leading IASTNode type directly.</li>
     *   <li>Label fallback: used when no AST is available (test/map CDGs).</li>
     * </ol>
     */
    private boolean isRealLeaf(CDGNode node) {
        // Layer 1: structural exclusion
        if (node.id == cdg.getEntryId() || node.id == cdg.getExitId()) {
            return false;
        }

        // Layer 2: AST-based check
        List<IASTNode> asts = node.getASTNodes();
        if (asts != null && !asts.isEmpty()) {
            for (IASTNode ast : asts) {
                if (ast instanceof IASTReturnStatement)   return true;
                if (ast instanceof IASTBreakStatement)    return true;
                if (ast instanceof IASTGotoStatement)     return true;
                if (ast instanceof IASTContinueStatement) return true;
            }
            // AST present but no terminal statement → intermediate node
            return false;
        }

        // Layer 3: label fallback (no AST available)
        String label = node.label == null ? "" : node.label.toLowerCase();
        return label.contains("return")
            || label.contains("break")
            || label.contains("goto")
            || label.contains("continue");
    }

    /** Returns true if this node represents a switch statement. */
    private boolean isSwitchNode(CDGNode node) {
        List<IASTNode> asts = node.getASTNodes();
        if (asts == null || asts.isEmpty()) return false;
        IASTNode n = asts.get(0);
        return n instanceof IASTSwitchStatement
            || n.getParent() instanceof IASTSwitchStatement;
    }

    // =========================================================================
    // Deduplication
    // =========================================================================

    /**
     * Removes duplicate and redundant chains.
     *
     * <p>Two passes:
     * <ol>
     *   <li>Remove chains whose leaf is the CFG exit node (End).</li>
     *   <li>Deduplicate by logical signature: chains with identical branch
     *       label sequences are collapsed to the one with the most steps
     *       (most context).  Shorter chains that are strict logical prefixes
     *       of longer chains to the same leaf are removed.</li>
     * </ol>
     */
    private void removeDuplicatesAndRedundant() {
        // Pass 1: remove End-leaf chains
    	// Pass 1: remove exit-node chains
        branchChains.removeIf(c ->
                c.getLeafNode().id == cdg.getExitId()
             || c.getLeafNode().toString().toUpperCase().contains("END"));

        if (branchChains.isEmpty()) return;

        // Pass 2: deduplicate by logical signature (branch labels only, no FLOW)
        Map<String, BranchChain> unique = new LinkedHashMap<>();
        for (BranchChain chain : branchChains) {
            String sig = logicSignature(chain);
            if (!unique.containsKey(sig)
                    || chain.getPath().size() > unique.get(sig).getPath().size()) {
                unique.put(sig, chain);
            }
        }

        // Pass 3: remove logical prefixes
        List<BranchChain> sorted = new ArrayList<>(unique.values());
        sorted.sort((a, b) -> Integer.compare(b.getPath().size(), a.getPath().size()));

        List<BranchChain> finalChains = new ArrayList<>();
        for (BranchChain candidate : sorted) {
            boolean redundant = finalChains.stream().anyMatch(
                    existing -> isLogicalPrefix(candidate, existing));
            if (!redundant) finalChains.add(candidate);
        }

        branchChains = finalChains;
    }

    /**
     * Returns a signature string built from the branch-condition labels in
     * the path (FLOW steps are excluded).  Two chains with the same signature
     * represent the same logical decision sequence.
     */
    private String logicSignature(BranchChain chain) {
        return chain.getPath().stream()
                .filter(PathStep::hasBranchCondition)
                .map(PathStep::getBranchLabel)
                .collect(Collectors.joining("->"));
    }

    /**
     * Returns true if {@code small}'s branch-label sequence is a strict
     * prefix of {@code large}'s sequence.
     */
    private boolean isLogicalPrefix(BranchChain small, BranchChain large) {
        List<String> s = branchLabels(small);
        List<String> l = branchLabels(large);
        if (s.size() >= l.size()) return false;
        for (int i = 0; i < s.size(); i++) {
            if (!s.get(i).equals(l.get(i))) return false;
        }
        return true;
    }

    private List<String> branchLabels(BranchChain chain) {
        return chain.getPath().stream()
                .filter(PathStep::hasBranchCondition)
                .map(PathStep::getBranchLabel)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Text output
    // =========================================================================

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
            sb.append("  Leaf: ").append(chain.getLeafNode()).append("\n\n");
        }

        sb.append("Extracted ").append(branchChains.size()).append(" branch-chains:\n");
        for (BranchChain chain : branchChains) {
            sb.append("  - ").append(chain.getLabel())
              .append(" (leaf node ").append(chain.getLeafNode().getId()).append(")\n");
        }
        return sb.toString();
    }

    // =========================================================================
    // Small AST helpers
    // =========================================================================

    private IASTExpression extractExpression(CDGNode node) {
        List<IASTNode> asts = node.getASTNodes();
        if (asts == null || asts.isEmpty()) return null;
        IASTNode n = asts.get(0);
        if (n instanceof IASTExpression)     return (IASTExpression) n;
        if (n instanceof IASTIfStatement)    return ((IASTIfStatement)    n).getConditionExpression();
        if (n instanceof IASTWhileStatement) return ((IASTWhileStatement) n).getCondition();
        if (n instanceof IASTForStatement)   return ((IASTForStatement)   n).getConditionExpression();
        return null;
    }
}