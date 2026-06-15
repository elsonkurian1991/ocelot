package it.unisa.ocelot.c.cdg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFunctionCallExpression;

import com.jgraph.algebra.JGraphFibonacciHeap.Node;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.CaseEdge;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Control Dependence Graph (CDG) implementation.
 *
 * <p>
 * Converts a CFG ({@link CFG}) into a CDG by computing:
 * <ol>
 * <li>Pre-Dominators (PED)</li>
 * <li>Immediate Pre-Dominators (IPED)</li>
 * <li>Reverse Pre-Domination (rPED)</li>
 * <li>Reverse Immediate Pre-Domination (rIPED)</li>
 * <li>Pre-Dominance Frontier (PEF)</li>
 * <li>Post-Dominators (POD)</li>
 * <li>Immediate Post-Dominators (IPOD)</li>
 * <li>Reverse Post-Domination (rPOD)</li>
 * <li>Reverse Immediate Post-Domination (rIPOD)</li>
 * <li>Post-Dominance Frontier (POF)</li>
 * <li>Control Dependencies (CD)</li>
 * </ol>
 *
 * <p>
 * CDG edges are {@link ControlDependenceEdge} instances stored in an adjacency
 * map: conditionNode → list of (dependentNode, edge) pairs. This matches the
 * JGraphT-style API expected by {@link CDGNode#isLeafNode},
 * {@link CDGNode#isBranchNode}, and {@link BranchChainExtractor}.
 */
public class CDG {

	// -----------------------------------------------------------------------
	// CFG-derived node map and structural ids
	// -----------------------------------------------------------------------

	/** CDGNodes keyed by their auto-assigned CDGNode id. */
	private final Map<Integer, CDGNode> nodes;

	/** Entry node id (maps to cfg.getStart()). */
	private final int entryId;

	/** Exit node id (maps to cfg.getEnd()). */
	private final int exitId;

	// -----------------------------------------------------------------------
	// CDG edge structures (built during buildCDG)
	// -----------------------------------------------------------------------

	/**
	 * Adjacency list for the CDG, keyed by <em>source</em> (condition) CDGNode id.
	 * Each value is the list of outgoing {@link ControlDependenceEdge} objects,
	 * together with the target (dependent) CDGNode they point to.
	 *
	 * <p>
	 * We keep both maps so we can answer "who does node X control?" and "which
	 * edges leave node X?" in O(degree) time.
	 */
	private final Map<Integer, List<ControlDependenceEdge>> outgoingEdges;
	private final Map<Integer, List<ControlDependenceEdge>> incomingEdges;
	private final Map<ControlDependenceEdge, Integer> edgeSource;
	private final Map<ControlDependenceEdge, Integer> edgeTarget;

	/** Original CFG (null for map-based test constructor). */
	private final CFG originalCFG;
	/** CFG node → CDG node (CFG constructor only). */
	private final Map<CFGNode, CDGNode> cfgToCdgMap;
	/** Case-edge labels: (conditionId, succId) → label string. */
	private final Map<Long, String> caseEdgeLabels;

	/** Nodes that may receive ENTRY→FLOW (computed for CFG-based CDG). */
	private Set<Integer> entryFlowNodeIds;

	// -----------------------------------------------------------------------
	// Intermediate analysis results (package-private for testing if needed)
	// -----------------------------------------------------------------------

	/** PED[n] – pre-dominators */
	private Map<Integer, Set<Integer>> ped;
	/** IPED[n] – immediate pre-dominators */
	private Map<Integer, Set<Integer>> iped;
	/** rPED[n] – reverse pre-domination */
	private Map<Integer, Set<Integer>> rPed;
	/** rIPED[n] – reverse immediate pre-domination */
	private Map<Integer, Set<Integer>> rIped;
	/** PEF[n] – pre-dominance frontier */
	private Map<Integer, Set<Integer>> pef;

	/** POD[n] – post-dominators */
	private Map<Integer, Set<Integer>> pod;
	/** IPOD[n] – immediate post-dominators */
	private Map<Integer, Set<Integer>> ipod;
	/** rPOD[n] – reverse post-domination */
	private Map<Integer, Set<Integer>> rPod;
	/** rIPOD[n] – reverse immediate post-domination */
	private Map<Integer, Set<Integer>> rIpod;
	/** POF[n] – post-dominance frontier */
	private Map<Integer, Set<Integer>> pof;

	/**
	 * CD[n] – raw control dependencies as (conditionId, label) pairs. Used
	 * internally; the public surface uses {@link ControlDependenceEdge}.
	 */
	private Map<Integer, Set<CDGEdge>> cd;

	// -----------------------------------------------------------------------
	// Constructor: Map-based (original, kept for the Builder / tests)
	// -----------------------------------------------------------------------

	/**
	 * Constructs a CDG from an already-built node map (used by the inner
	 * {@link Builder} and unit tests).
	 *
	 * @param nodes   CDGNode map keyed by node id
	 * @param entryId entry node id
	 * @param exitId  exit node id
	 */
	public CDG(Map<Integer, CDGNode> nodes, int entryId, int exitId) {
		this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
		this.entryId = entryId;
		this.exitId = exitId;
		this.originalCFG = null;
		this.cfgToCdgMap = Collections.emptyMap();
		this.caseEdgeLabels = new HashMap<>();
		this.outgoingEdges = new HashMap<>();
		this.incomingEdges = new HashMap<>();
		this.edgeSource = new LinkedHashMap<>();
		this.edgeTarget = new LinkedHashMap<>();
	}

	// -----------------------------------------------------------------------
	// Constructor: CFG-based (main production entry point)
	// -----------------------------------------------------------------------

	/**
	 * Converts a {@link CFG} into a CDG.
	 *
	 * <p>
	 * Steps:
	 * <ol>
	 * <li>Map every {@link CFGNode} to a {@link CDGNode}, preserving the JGraphT
	 * edge structure ({@link TrueEdge}/{@link FalseEdge}/
	 * {@link CaseEdge}/{@link FlowEdge}).</li>
	 * <li>Run all dominance / frontier analyses.</li>
	 * <li>Materialise {@link ControlDependenceEdge} objects from the raw CD result
	 * so callers can use the graph API.</li>
	 * </ol>
	 *
	 * @param cfg a fully built {@link CFG}
	 */
	public CDG(CFG cfg) {
		this.originalCFG = cfg;
		this.cfgToCdgMap = new LinkedHashMap<>();
		this.caseEdgeLabels = new HashMap<>();
		this.outgoingEdges = new HashMap<>();
		this.incomingEdges = new HashMap<>();
		this.edgeSource = new LinkedHashMap<>();
		this.edgeTarget = new LinkedHashMap<>();

		Map<Integer, CDGNode> builtNodes = new LinkedHashMap<>();

		for (CFGNode cfgNode : cfg.vertexSet()) {
			CDGNode cdgNode = new CDGNode(cfgNode);
			cfgToCdgMap.put(cfgNode, cdgNode);
			builtNodes.put(cdgNode.id, cdgNode);
		}

		for (CFGNode cfgNode : cfg.vertexSet()) {
			CDGNode srcCdg = cfgToCdgMap.get(cfgNode);
			for (LabeledEdge edge : cfg.outgoingEdgesOf(cfgNode)) {
				CFGNode cfgTarget = cfg.getEdgeTarget(edge);
				CDGNode tgtCdg = cfgToCdgMap.get(cfgTarget);
				System.out.println("Processing CFG edge: " + cfgNode.getId() + " --[" + edge.toString() + "]--> "
						+ cfgTarget.getId());
				System.out.println("Mapped to CDG edge: " + srcCdg.id + " --[" + edge.toString() + "]--> "
						+ (tgtCdg != null ? tgtCdg.id : "null"));
				if (tgtCdg == null)
					continue;

				if (!srcCdg.successors.contains(tgtCdg.id)) {
					srcCdg.successors.add(tgtCdg.id);
				}
				if (!tgtCdg.predecessors.contains(srcCdg.id)) {
					tgtCdg.predecessors.add(srcCdg.id);
				}

				if (edge instanceof TrueEdge) {
					srcCdg.isCondition = true;
					srcCdg.trueSuccessor = tgtCdg.id;
					// Fix: enrich label from CFG node if currently blank

					enrichConditionLabel(srcCdg, cfgNode);

				} else if (edge instanceof FalseEdge) {
					srcCdg.isCondition = true;
					srcCdg.falseSuccessor = tgtCdg.id;
					// Fix: enrich label from CFG node if currently blank
					enrichConditionLabel(srcCdg, cfgNode);

				} else if (edge instanceof CaseEdge) {
					srcCdg.isCondition = true;
					Object lbl = edge.getLabel();
					if (lbl != null) {
						caseEdgeLabels.put(packPair(srcCdg.id, tgtCdg.id), lbl.toString());
					}
					// Fix: enrich label from CFG node if currently blank
					enrichConditionLabel(srcCdg, cfgNode);
				}

			}
		}

		CFGNode cfgStart = cfg.getStart();
		CFGNode cfgEnd = cfg.getEnd();
		CDGNode entryNode = cfgToCdgMap.get(cfgStart);
		CDGNode exitNode = cfgToCdgMap.get(cfgEnd);

		if (entryNode == null || exitNode == null) {
			throw new IllegalArgumentException("CFG start/end node not found in vertex set. "
					+ "Ensure cfg.getStart() and cfg.getEnd() are part of cfg.vertexSet().");
		}

		this.nodes = Collections.unmodifiableMap(builtNodes);
		this.entryId = entryNode.id;
		this.exitId = exitNode.id;

	}

	/** Resets the CDGNode id counter (call between compilation units). */
	public static void resetNodeIds() {
		CDGNode.resetIdCounter();
	}

	// -----------------------------------------------------------------------
	// Core analysis pipeline
	// -----------------------------------------------------------------------

	void buildCDG() {
		// PED to PEF are not important
		ped = computePreDominators();
		iped = computeImmediatePreDominators(ped);
		rPed = computeReversePreDomination(ped);
		rIped = computeReverseImmediatePreDomination(iped);
		pef = computePreDominanceFrontier(iped, rIped);
		// POD to POF is important
		pod = computePostDominators();
		ipod = computeImmediatePostDominators(pod);
		rPod = computeReversePostDomination(pod);
		rIpod = computeReverseImmediatePostDomination(ipod);
		pof = computePostDominanceFrontier(ipod, rIpod);

		cd = computeControlDependencies(pod, pof);

		materialiseEdges();
		// attachEntryFlowEdges();
		/*
		 * if (originalCFG != null) { computeEntryFlowNodeIds(); attachEntryFlowEdges();
		 * } else { attachFlowToNodesWithoutIncoming(); }
		 */
	}

	private void attachEntryFlowEdges() {
		// Mark entry node as a condition node so CDGBranchPathCollector
		// dispatches it through handleBranchNode
		nodes.get(entryId).isCondition = true; // Mark entry as condition to prevent it being treated as leaf
		for (int id : nodes.keySet()) {
			if (id == entryId)
				continue;
			List<ControlDependenceEdge> incoming = incomingEdges.getOrDefault(id, Collections.emptyList());

			// Exclude self-loops: a node whose only incoming edges are from itself
			// is effectively disconnected (e.g. for-loop condition self-back-edge)
			boolean hasExternalIncoming = incoming.stream().anyMatch(e -> !Objects.equals(edgeSource.get(e), id));

			if (!hasExternalIncoming) {
				addFlowEdge(entryId, id);
			}
		}

	}

	private void attachEntryFlowEdges_notworking() {
		// First compute reachability from entry via existing edges
		Set<Integer> reachable = new HashSet<>();
		Deque<Integer> queue = new ArrayDeque<>();
		queue.add(entryId);
		reachable.add(entryId);
		while (!queue.isEmpty()) {
			int current = queue.poll();
			for (ControlDependenceEdge e : outgoingEdges.getOrDefault(current, Collections.emptyList())) {
				Integer target = edgeTarget.get(e);
				if (target != null && reachable.add(target)) {
					queue.add(target);
				}
			}
		}

		// Only add FLOW from entry to nodes that:
		// 1. Are not reachable from entry
		// 2. Have NO incoming TRUE/FALSE/case edges from any condition node
		for (int id : nodes.keySet()) {
			if (id == entryId)
				continue;
			if (reachable.contains(id))
				continue;

			// Skip if this node already has a real incoming branch edge
			boolean hasRealBranchIncoming = incomingEdges.getOrDefault(id, Collections.emptyList()).stream()
					.anyMatch(e -> {
						String lbl = e.toString();
						return "TRUE".equals(lbl) || "FALSE".equals(lbl) || (!lbl.equals("FLOW") && !lbl.isBlank());
					});

			if (!hasRealBranchIncoming) {
				addFlowEdge(entryId, id);
			}
		}
	}

	private void attachEntryFlowEdges_old() {
		// Find all nodes reachable from entry via existing edges
		Set<Integer> reachable = new HashSet<>();
		Deque<Integer> queue = new ArrayDeque<>();
		queue.add(entryId);
		reachable.add(entryId);
		while (!queue.isEmpty()) {
			int current = queue.poll();
			for (ControlDependenceEdge e : outgoingEdges.getOrDefault(current, Collections.emptyList())) {
				Integer target = edgeTarget.get(e);
				if (target != null && reachable.add(target)) {
					queue.add(target);
				}
			}
		}

		// Any node not reachable from entry gets a FLOW edge from entry
		for (int id : nodes.keySet()) {
			if (id == entryId)
				continue;
			if (!reachable.contains(id)) {
				addFlowEdge(entryId, id);
			}
		}
		/*
		 * V2 for (int id : nodes.keySet()) { if (id == entryId) continue;
		 * List<ControlDependenceEdge> incoming = incomingEdges.getOrDefault(id,
		 * Collections.emptyList());
		 * 
		 * // * treat self-loop-only nodes as having no real incoming edge *** boolean
		 * hasRealIncoming = incoming.stream() .anyMatch(e ->
		 * !Objects.equals(edgeSource.get(e), id));
		 * 
		 * if (!hasRealIncoming) { addFlowEdge(entryId, id); } }
		 */
		/*
		 * V1 for (int id : nodes.keySet()) { if (id == entryId) continue; // Any node
		 * with no incoming CDG edges gets a FLOW edge from entry if
		 * (incomingEdges.getOrDefault(id, Collections.emptyList()).isEmpty()) {
		 * addFlowEdge(entryId, id); } }
		 */
	}

	private void addFlowEdge(int sourceId, int targetId) {
		if (hasEdge(sourceId, targetId))
			return;
		ControlDependenceEdge cde = new ControlDependenceEdge(nodes.get(targetId), nodes.get(sourceId), "FLOW");
		registerEdge(sourceId, targetId, cde);
	}

	private boolean hasEdge(int sourceId, int targetId) {
		for (ControlDependenceEdge e : outgoingEdges.getOrDefault(sourceId, Collections.emptyList())) {
			if (Objects.equals(edgeTarget.get(e), targetId))
				return true;
		}
		return false;
	}
	// -----------------------------------------------------------------------
	// Materialise ControlDependenceEdge objects from raw CD map
	// -----------------------------------------------------------------------

	/**
	 * Converts the raw {@code cd} map (conditionId, label pairs) into
	 * {@link ControlDependenceEdge} instances stored in the adjacency maps
	 * ({@link #outgoingEdges}, {@link #edgeSource}, {@link #edgeTarget}).
	 *
	 * <p>
	 * Direction of a CDG edge: condition node → dependent node. "Node X is
	 * control-dependent on condition Y with label L" becomes an edge Y --L--> X.
	 */
	private void materialiseEdges() {// all node must have atleast one edge associated to it, otherwise it will be
										// treated as a leaf node and not traversed by the BranchChainExtractor
		for (int id : nodes.keySet()) {
			outgoingEdges.put(id, new ArrayList<>());
			incomingEdges.put(id, new ArrayList<>());
		}

		for (Map.Entry<Integer, Set<CDGEdge>> entry : cd.entrySet()) {
			int dependentId = entry.getKey(); // X is the DEPENDENT node

			for (CDGEdge raw : entry.getValue()) {
				int conditionId = raw.conditionNodeId; // Y is the CONDITION node
				String rawLabel = raw.label;

				ControlDependenceEdge cde = new ControlDependenceEdge(nodes.get(dependentId), nodes.get(conditionId),
						rawLabel);
				// FIX: condition is the SOURCE, dependent is the TARGET
				registerEdge(conditionId, dependentId, cde);

				// LabeledEdge originalCfgEdge = resolveCfgEdge(conditionId, dependentId,
				// rawLabel);
				// String normLabel = normaliseBranchLabel(rawLabel, originalCfgEdge);
				// ControlDependenceEdge cde = new ControlDependenceEdge(normLabel,
				// originalCfgEdge);

			}
		}
		// add this extra:Mark entry as condition since it now has outgoing TRUE/FLOW
		// edges
		//nodes.get(entryId).isCondition = true;
	}

	private void materialiseEdges_notfullyworking() {
		for (int id : nodes.keySet()) {
			outgoingEdges.put(id, new ArrayList<>());
			incomingEdges.put(id, new ArrayList<>());
		}

		// First pass: add all TRUE/FALSE/case edges from real condition nodes
		// Priority: loop conditions take precedence over inner if-conditions
		Map<Integer, CDGEdge> bestDep = new HashMap<>(); // targetId -> best CDGEdge

		for (Map.Entry<Integer, Set<CDGEdge>> entry : cd.entrySet()) {
			int dependentId = entry.getKey();
			for (CDGEdge raw : entry.getValue()) {
				int conditionId = raw.conditionNodeId;
				String label = raw.label;

				// Skip FLOW/entry edges for now
				if ("TRUE".equals(label) && conditionId == entryId)
					continue;

				CDGEdge existing = bestDep.get(dependentId);
				if (existing == null) {
					bestDep.put(dependentId, raw);
				} else {
					// Prefer the condition that is a loop node (has self-loop)
					// over inner if-conditions (break scenario)
					boolean newIsLoop = hasOutgoingSelfLoop(conditionId);
					boolean existingIsLoop = hasOutgoingSelfLoop(existing.conditionNodeId);
					if (newIsLoop && !existingIsLoop) {
						bestDep.put(dependentId, raw);
					}
					// If both or neither are loops, keep both (legitimate multi-deps)
					else if (!newIsLoop && !existingIsLoop) {
						// register both — handled below
						bestDep.put(dependentId, null); // sentinel: keep all
					}
				}
			}
		}

		for (Map.Entry<Integer, Set<CDGEdge>> entry : cd.entrySet()) {
			int dependentId = entry.getKey();
			for (CDGEdge raw : entry.getValue()) {
				int conditionId = raw.conditionNodeId;
				String rawLabel = raw.label;

				CDGEdge best = bestDep.get(dependentId);
				// Skip if a better (loop) condition was found for this dependent
				if (best != null && best.conditionNodeId != conditionId && hasOutgoingSelfLoop(best.conditionNodeId)) {
					continue; // suppress spurious break dependency
				}

				ControlDependenceEdge cde = new ControlDependenceEdge(nodes.get(dependentId), nodes.get(conditionId),
						rawLabel);
				registerEdge(conditionId, dependentId, cde);
			}
		}

		// Second pass: add entry TRUE edges for nodes with no incoming
		for (int id : nodes.keySet()) {
			if (id == entryId)
				continue;
			boolean hasExternalIncoming = incomingEdges.getOrDefault(id, Collections.emptyList()).stream()
					.anyMatch(e -> !Objects.equals(edgeSource.get(e), id));
			if (!hasExternalIncoming) {
				ControlDependenceEdge cde = new ControlDependenceEdge(nodes.get(id), nodes.get(entryId), "TRUE");
				registerEdge(entryId, id, cde);
				nodes.get(entryId).isCondition = true;
			}
		}
	}

	private boolean hasOutgoingSelfLoop(int nodeId) {
		CDGNode node = nodes.get(nodeId);
		if (node == null)
			return false;
		return node.successors.contains(nodeId);
	}

	private void registerEdge(int sourceId, int targetId, ControlDependenceEdge cde) {
		outgoingEdges.get(sourceId).add(cde);
		incomingEdges.get(targetId).add(cde);
		edgeSource.put(cde, sourceId);
		edgeTarget.put(cde, targetId);

	}

	private void enrichConditionLabel(CDGNode cdgNode, CFGNode cfgNode) {

		String currentLabel = cdgNode.label == null ? "" : cdgNode.label.trim();
		boolean isEffectivelyBlank = currentLabel.isBlank() || currentLabel.matches("\\d+:\\s*");

		if (!isEffectivelyBlank)
			return;

		// Walk children of the leading node to find condition expression
		IASTNode leading = cfgNode.getLeadingNode();
		if (leading == null)
			return;

		String extracted = extractConditionFromChildren(leading);

		// Fallback: try parent if children yield nothing
		if (extracted == null || extracted.isBlank()) {
			IASTNode parent = leading.getParent();
			if (parent instanceof IASTIfStatement) {
				extracted = ((IASTIfStatement) parent).getConditionExpression().getRawSignature();
			} else if (parent instanceof IASTWhileStatement) {
				extracted = ((IASTWhileStatement) parent).getCondition().getRawSignature();
			} else if (parent instanceof IASTForStatement) {
				extracted = ((IASTForStatement) parent).getConditionExpression().getRawSignature();
			} else if (parent instanceof IASTSwitchStatement) {
				// switch (x) → extract controller expression
				IASTExpression cond = ((IASTSwitchStatement) parent).getControllerExpression();
				if (cond != null)
					extracted = "switch(" + cond.getRawSignature() + ")";
			}
		}

		if (extracted != null && !extracted.isBlank()) {
			cdgNode.label = cfgNode.getId() + ": " + extracted.trim();
			System.out.println("Label fixed to: '" + cdgNode.label + "'");
		}
	}

	private String extractConditionFromChildren(IASTNode node) {
		for (IASTNode child : node.getChildren()) {
			// CASTBinaryExpression carries the condition e.g. "x > 5", "i < 3"
			if (child instanceof IASTBinaryExpression) {
				String raw = child.getRawSignature();
				if (raw != null && !raw.isBlank())
					return raw;
			}
			// CASTIdExpression for simple boolean variable conditions
			if (child instanceof IASTIdExpression) {
				String raw = child.getRawSignature();
				if (raw != null && !raw.isBlank())
					return raw;
			}
		}
		return null;
	}

	/*
	 * private void addFlowEdge(int sourceId, int targetId) { if (hasEdge(sourceId,
	 * targetId, "FLOW")) return; registerEdge(sourceId, targetId, new
	 * ControlDependenceEdge("FLOW", null)); }
	 */

	/*
	 * private boolean hasEdge(int sourceId, int targetId, String label) { for
	 * (ControlDependenceEdge e : outgoingEdges.getOrDefault(sourceId,
	 * Collections.emptyList())) { if (edgeTarget.get(e) == targetId &&
	 * label.equals(e.toString())) { return true; } } return false; }
	 */
	/**
	 * Tries to locate the original {@link LabeledEdge} in the CFG that corresponds
	 * to the transition from the condition node to the branch that leads
	 * (eventually) to {@code dependentId}.
	 *
	 * <p>
	 * We look at the immediate successors of the condition node and pick the one
	 * whose label matches {@code rawLabel}. If the original CFG is not available
	 * (Map-based constructor) this returns {@code null}.
	 */
	/*
	 * private LabeledEdge resolveCfgEdge(int conditionId, int dependentId, String
	 * rawLabel) { if (originalCFG == null) return null; CDGNode condNode =
	 * nodes.get(conditionId); if (condNode == null) return null; CFGNode cfgCond =
	 * condNode.getOriginalCFGNode(); if (cfgCond == null) return null;
	 * 
	 * if ("T".equals(rawLabel) && condNode.trueSuccessor >= 0) { CFGNode t =
	 * nodes.get(condNode.trueSuccessor).getOriginalCFGNode(); return
	 * findCfgEdge(cfgCond, t); } if ("F".equals(rawLabel) &&
	 * condNode.falseSuccessor >= 0) { CFGNode f =
	 * nodes.get(condNode.falseSuccessor).getOriginalCFGNode(); return
	 * findCfgEdge(cfgCond, f); } for (LabeledEdge e :
	 * originalCFG.outgoingEdgesOf(cfgCond)) { if (e instanceof CaseEdge) { CFGNode
	 * tgt = originalCFG.getEdgeTarget(e); CDGNode cdgTgt = cfgToCdgMap.get(tgt); if
	 * (cdgTgt != null && cdgTgt.id == dependentId) { return e; } } } return null; }
	 */
	/*
	 * private LabeledEdge findCfgEdge(CFGNode src, CFGNode tgt) { if (src == null
	 * || tgt == null) return null; return originalCFG.getEdge(src, tgt); }
	 */
	private static long packPair(int a, int b) {
		return (((long) a) << 32) | (b & 0xffffffffL);
	}

	/**
	 * Public helper to let external classes query case labels for a specific step.
	 */
	public String getSwitchCaseLabel(int conditionId, int successorId) {
		long key = (((long) conditionId) << 32) | (successorId & 0xffffffffL); // packPair
		return this.caseEdgeLabels.get(key);
	}
	/**
	 * Converts an internal "T"/"F"/case label to a {@link ControlDependenceEdge}
	 * display label ("TRUE", "FALSE", or the case string).
	 */
	/*
	 * private static String normaliseBranchLabel(String rawLabel, LabeledEdge
	 * cfgEdge) { if ("T".equals(rawLabel)) return "TRUE"; if ("F".equals(rawLabel))
	 * return "FALSE"; // For case edges the raw label is the case value string
	 * already return rawLabel != null ? rawLabel : "FLOW"; }
	 */

	// -----------------------------------------------------------------------
	// Pre-Dominators (PED)
	// -----------------------------------------------------------------------

	/**
	 * PED[n] = {n} ∪ (∩ PED[p] for all p ∈ prec(n)) Entry is initialised to
	 * {entry}; all others start as the full set.
	 */
	private Map<Integer, Set<Integer>> computePreDominators() {
		Set<Integer> allIds = nodes.keySet();

		Map<Integer, Set<Integer>> result = new HashMap<>();
		result.put(entryId, new HashSet<>(Collections.singleton(entryId)));
		for (int id : allIds) {
			if (id != entryId)
				result.put(id, new HashSet<>(allIds));
		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (int nId : allIds) {
				if (nId == entryId)
					continue;
				CDGNode n = nodes.get(nId);

				Set<Integer> inter = null;
				for (int predId : n.predecessors) {
					Set<Integer> predSet = result.get(predId);
					if (predSet == null)
						continue;
					inter = (inter == null) ? new HashSet<>(predSet) : retainAll(inter, predSet);
				}
				if (inter == null)
					inter = new HashSet<>();
				inter.add(nId);

				if (!inter.equals(result.get(nId))) {
					result.put(nId, inter);
					changed = true;
				}
			}
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Immediate Pre-Dominators (IPED)
	// -----------------------------------------------------------------------

	/**
	 * IPED[n] = closest pre-dominator of n. Computed as: (PED[n] \ {n}) minus any d
	 * that is dominated by another dominator in PED[n].
	 */
	private Map<Integer, Set<Integer>> computeImmediatePreDominators(Map<Integer, Set<Integer>> ped) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int nId : nodes.keySet()) {
			Set<Integer> candidates = new HashSet<>(ped.get(nId));
			candidates.remove(nId);

			Set<Integer> toRemove = new HashSet<>();
			for (int d : candidates) {
				for (int o : candidates) {
					if (d != o && ped.get(o).contains(d)) {
						toRemove.add(d);
						break;
					}
				}
			}
			candidates.removeAll(toRemove);
			result.put(nId, candidates);
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Reverse Pre-Domination (rPED)
	// -----------------------------------------------------------------------

	/** rPED[n] = { d | n ∈ PED[d] } */
	private Map<Integer, Set<Integer>> computeReversePreDomination(Map<Integer, Set<Integer>> ped) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());
		for (Map.Entry<Integer, Set<Integer>> e : ped.entrySet()) {
			for (int nId : e.getValue())
				result.get(nId).add(e.getKey());
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Reverse Immediate Pre-Domination (rIPED)
	// -----------------------------------------------------------------------

	/** rIPED[n] = { d | n ∈ IPED[d] } */
	private Map<Integer, Set<Integer>> computeReverseImmediatePreDomination(Map<Integer, Set<Integer>> iped) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());
		for (Map.Entry<Integer, Set<Integer>> e : iped.entrySet()) {
			for (int nId : e.getValue())
				result.get(nId).add(e.getKey());
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Pre-Dominance Frontier (PEF)
	// -----------------------------------------------------------------------

	/**
	 * PEF[X] = { Y | Y ∈ succ(X) ∧ X ∉ IPED[Y] } ∪ { Y | Z ∈ rIPED[X], Y ∈ PEF[Z],
	 * X ∉ IPED[Y] }
	 */
	private Map<Integer, Set<Integer>> computePreDominanceFrontier(Map<Integer, Set<Integer>> iped,
			Map<Integer, Set<Integer>> rIped) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());

		boolean changed = true;
		while (changed) {
			changed = false;
			for (int xId : nodes.keySet()) {
				CDGNode X = nodes.get(xId);
				Set<Integer> newSet = new HashSet<>();

				for (int yId : X.successors) {
					if (!iped.get(yId).contains(xId))
						newSet.add(yId);
				}
				for (int zId : rIped.get(xId)) {
					for (int yId : result.get(zId)) {
						if (!iped.get(yId).contains(xId))
							newSet.add(yId);
					}
				}

				if (!newSet.equals(result.get(xId))) {
					result.put(xId, newSet);
					changed = true;
				}
			}
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Post-Dominators (POD)
	// -----------------------------------------------------------------------

	/**
	 * POD[n] = {n} ∪ (∩ POD[s] for all s ∈ succ(n)) Exit is initialised to {exit};
	 * all others start as the full set.
	 */
	private Map<Integer, Set<Integer>> computePostDominators() {
		Set<Integer> allIds = nodes.keySet();

		Map<Integer, Set<Integer>> result = new HashMap<>();
		result.put(exitId, new HashSet<>(Collections.singleton(exitId)));
		for (int id : allIds) {
			if (id != exitId)
				result.put(id, new HashSet<>(allIds));
		}

		boolean changed = true;
		while (changed) {
			changed = false;
			for (int nId : allIds) {
				if (nId == exitId)
					continue;
				CDGNode n = nodes.get(nId);

				Set<Integer> inter = null;
				for (int succId : getPostDomSuccessors(n)) {
					Set<Integer> succSet = result.get(succId);
					if (succSet == null)
						continue;
					inter = (inter == null) ? new HashSet<>(succSet) : retainAll(inter, succSet);
				}
				if (inter == null)
					inter = new HashSet<>();
				inter.add(nId);

				if (!inter.equals(result.get(nId))) {
					result.put(nId, inter);
					changed = true;
				}
			}
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Immediate Post-Dominators (IPOD)
	// -----------------------------------------------------------------------

	/**
	 * IPOD[n] = closest post-dominator of n.
	 */
	private Map<Integer, Set<Integer>> computeImmediatePostDominators(Map<Integer, Set<Integer>> pod) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int nId : nodes.keySet()) {
			Set<Integer> candidates = new HashSet<>(pod.get(nId));
			candidates.remove(nId);

			Set<Integer> toRemove = new HashSet<>();
			for (int d : candidates) {
				for (int o : candidates) {
					if (d != o && pod.get(o).contains(d)) {
						toRemove.add(d);
						break;
					}
				}
			}
			candidates.removeAll(toRemove);
			result.put(nId, candidates);
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Reverse Post-Domination (rPOD)
	// -----------------------------------------------------------------------

	/** rPOD[n] = { d | n ∈ POD[d] } */
	private Map<Integer, Set<Integer>> computeReversePostDomination(Map<Integer, Set<Integer>> pod) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());
		for (Map.Entry<Integer, Set<Integer>> e : pod.entrySet()) {
			for (int nId : e.getValue())
				result.get(nId).add(e.getKey());
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Reverse Immediate Post-Domination (rIPOD)
	// -----------------------------------------------------------------------

	/** rIPOD[n] = { d | n ∈ IPOD[d] } */
	private Map<Integer, Set<Integer>> computeReverseImmediatePostDomination(Map<Integer, Set<Integer>> ipod) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());
		for (Map.Entry<Integer, Set<Integer>> e : ipod.entrySet()) {
			for (int nId : e.getValue())
				result.get(nId).add(e.getKey());
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Post-Dominance Frontier (POF)
	// -----------------------------------------------------------------------

	/**
	 * POF[X] = { Y | Y ∈ prec(X) ∧ X ∉ IPOD[Y] } ∪ { Y | Z ∈ rIPOD[X], Y ∈ POF[Z],
	 * X ∉ IPOD[Y] }
	 */
	private Map<Integer, Set<Integer>> computePostDominanceFrontier(Map<Integer, Set<Integer>> ipod,
			Map<Integer, Set<Integer>> rIpod) {

		Map<Integer, Set<Integer>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());

		boolean changed = true;
		while (changed) {
			changed = false;
			for (int xId : nodes.keySet()) {
				CDGNode X = nodes.get(xId);
				Set<Integer> newSet = new HashSet<>();

				for (int yId : X.predecessors) {
					if (!ipod.get(yId).contains(xId))
						newSet.add(yId);
				}
				for (int zId : rIpod.get(xId)) {
					for (int yId : result.get(zId)) {
						if (!ipod.get(yId).contains(xId))
							newSet.add(yId);
					}
				}

				if (!newSet.equals(result.get(xId))) {
					result.put(xId, newSet);
					changed = true;
				}
			}
		}
		return result;
	}

	// -----------------------------------------------------------------------
	// Control Dependencies (CD)
	// -----------------------------------------------------------------------

	/**
	 * CD[X] = { (Y, label(Y,S)) | Y ∈ POF[X], S ∈ succ(Y), X ∈ POD[S] }
	 *
	 * <p>
	 * label is "T" when S is Y's true-branch successor, "F" otherwise. For
	 * switch/case nodes the label carries the case value.
	 */
	private Map<Integer, Set<CDGEdge>> computeControlDependencies(Map<Integer, Set<Integer>> pod,
			Map<Integer, Set<Integer>> pof) {

		Map<Integer, Set<CDGEdge>> result = new HashMap<>();
		for (int id : nodes.keySet())
			result.put(id, new HashSet<>());

		for (int xId : nodes.keySet()) {
			Set<CDGEdge> deps = result.get(xId);

			for (int yId : pof.get(xId)) {
				CDGNode Y = nodes.get(yId);
				for (int sId : Y.successors) {
					Set<Integer> podS = pod.get(sId);
					if (podS != null && podS.contains(xId)) {
						// deps.add(new CDGEdge(yId, getBranchLabel(Y, sId)));
						// Check this: suppress spurious break-induced dependencies ***
						//if (isBreakInducedDependency(Y, sId, xId, pod, pof))
							//continue;
						deps.add(new CDGEdge(yId, getBranchLabel(Y, sId)));
					}
				}
			}
		}
		// Augment: connect entry to all nodes not reachable
	    // via any condition in the standard CDG.
	    // This handles: top-level statements, loop conditions
	    // with only self-loop incoming, and any node made
	    // unreachable due to break/continue/return paths.
	    augmentWithEntryEdges(result);
		// Only add entry dependency for nodes with truly empty deps
		// (not reachable via any condition in POF computation)
		// AND that are not loop-body nodes (which have self-loop conditions)
	    //STILL THIS PART IS NOT WORKING FOR BREAK
		/*for (int xId : nodes.keySet()) {
			if (xId == entryId || xId == exitId)
				continue;
			Set<CDGEdge> deps = result.get(xId);
			CDGNode X = nodes.get(xId);
			if (deps.isEmpty()) {
				System.err.println("[First] Adding synthetic FLOW edge: " + entryId + " -> " + xId);
				deps.add(new CDGEdge(entryId, "FLOW"));
			}
			else if(X.isCondition && !X.successors.contains(xId)) {
				// loop condition node (has self-loop) — 
	            // always needs FLOW from entry so traversal can reach it ***
				 System.err.println("LOOP NODE " + xId + " deps: " + deps);
	            boolean hasExternalIncoming = deps.stream()
	                .anyMatch(e -> e.conditionNodeId != xId);
	            System.err.println("hasExternalIncoming=" + hasExternalIncoming);
	            if (!hasExternalIncoming) {
	            	System.err.println("[Next] Adding synthetic FLOW edge: " + entryId + " -> " + xId);
	                deps.add(new CDGEdge(entryId, "FLOW"));
	            }
			}
		}*/
		return result;
	}
	/**
	 * Universal entry augmentation.
	 *
	 * A node needs a direct edge from entry if and only if
	 * it is not reachable from entry through the CDG edges
	 * computed so far — excluding self-loops.
	 *
	 * This correctly handles ALL cases:
	 * - Top-level statements (no deps at all)
	 * - Loop conditions with self-loop only (fun3, fun8)
	 * - Nodes made unreachable by break/continue (fun7, fun10)
	 * - Nested structures of any depth
	 */
	private void augmentWithEntryEdges(Map<Integer, Set<CDGEdge>> cd) {

	    // Step 1: build a reachability set from entry
	    // using the cd map (condition->dependent direction)
	    // A node is reachable if there exists a path from
	    // entryId to it through cd edges, ignoring self-loops.
	    Set<Integer> reachable = new HashSet<>();
	    Deque<Integer> queue = new ArrayDeque<>();
	    reachable.add(entryId);
	    queue.add(entryId);

	    // Build reverse map: conditionId -> set of dependentIds
	    Map<Integer, Set<Integer>> condToDeps = new HashMap<>();
	    for (int id : nodes.keySet())
	        condToDeps.put(id, new HashSet<>());

	    for (Map.Entry<Integer, Set<CDGEdge>> entry : cd.entrySet()) {
	        int depId = entry.getKey();
	        for (CDGEdge e : entry.getValue()) {
	            if (e.conditionNodeId != depId) { // exclude self-loops
	                condToDeps.get(e.conditionNodeId).add(depId);
	            }
	        }
	    }

	    // BFS from entry through condition->dependent edges
	    while (!queue.isEmpty()) {
	        int current = queue.poll();
	        for (int dep : condToDeps.get(current)) {
	            if (reachable.add(dep)) {
	                queue.add(dep);
	            }
	        }
	    }

	    // Step 2: any node not reachable from entry gets
	    // a direct FLOW edge from entry
	    for (int xId : nodes.keySet()) {
	        if (xId == entryId || xId == exitId) continue;
	        if (!reachable.contains(xId)) {
	            cd.get(xId).add(new CDGEdge(entryId, "FLOW"));
	            // Now this node is reachable — BFS its dependents too
	            // (handles chains of unreachable nodes)
	            reachable.add(xId);
	            queue.add(xId);
	            while (!queue.isEmpty()) {
	                int current = queue.poll();
	                for (int dep : condToDeps.get(current)) {
	                    if (reachable.add(dep)) {
	                        queue.add(dep);
	                    }
	                }
	            }
	        }
	    }
	}

	/**
	 * Returns true if the dependency (Y controls xId via successor sId) is spurious
	 * because it was induced by a break statement.
	 *
	 * A dependency is break-induced when ALL of these hold: 1. Y is a condition
	 * node (if-inside-loop) 2. sId is Y's TRUE successor 3. sId is NOT
	 * post-dominated by xId (TRUE exits past xId — break path) 4. xId IS
	 * post-dominated by Y's FALSE successor (FALSE stays in scope of xId) 5. There
	 * exists a loop condition L such that: - L has a self-loop (L is a loop head) -
	 * xId is in POD of L's TRUE successor (xId is inside the loop body) - Y is also
	 * inside that loop (Y is in POD of L's TRUE successor)
	 */
	private boolean isBreakInducedDependency(CDGNode Y, int sId, int xId, Map<Integer, Set<Integer>> pod,
			Map<Integer, Set<Integer>> pof) {

		if (!Y.isCondition)
			return false;
		// sId must be TRUE successor of Y
		if (Y.trueSuccessor != sId)
			return false;
		// FALSE successor must exist
		if (Y.falseSuccessor < 0)
			return false;

		Set<Integer> podTrueSucc = pod.get(sId);
		Set<Integer> podFalseSucc = pod.get(Y.falseSuccessor);
		if (podTrueSucc == null || podFalseSucc == null)
			return false;

		// TRUE path must NOT post-dominate xId (exits past it)
		if (podTrueSucc.contains(xId))
			return false;
		// FALSE path must post-dominate xId (stays in scope)
		if (!podFalseSucc.contains(xId))
			return false;

		// Now verify xId and Y are both inside a common loop
		for (int loopId : nodes.keySet()) {
			CDGNode loop = nodes.get(loopId);
			// Loop condition has a self-loop successor
			if (!loop.successors.contains(loopId))
				continue;
			if (loop.trueSuccessor < 0)
				continue;

			Set<Integer> podLoopTrue = pod.get(loop.trueSuccessor);
			if (podLoopTrue == null)
				continue;

			// Both xId and Y must be inside the loop body
			if (podLoopTrue.contains(xId) && podLoopTrue.contains(Y.id)) {
				return true; // confirmed break-induced dependency
			}
		}

		return false;
	}

	private boolean isBreakEdge(CDGNode condNode, int succId) {
		if (condNode.trueSuccessor != succId)
			return false;
		int falseSucc = condNode.falseSuccessor;
		if (falseSucc < 0)
			return false;
		Set<Integer> podTrue = pod.get(succId);
		Set<Integer> podFalse = pod.get(falseSucc);
		if (podTrue == null || podFalse == null)
			return false;
		// true-path post-dominates everything false-path does → true is the exit
		return podTrue.containsAll(podFalse);
	}

	/**
	 * Returns "T" if {@code succId} is the true-branch successor of {@code cond},
	 * "F" for the false-branch, or the case-label string for switch edges. Falls
	 * back to "F" for any unrecognised edge (e.g. loop exits, flow edges from a
	 * condition that has no explicit false successor).
	 */
	private String getBranchLabel(CDGNode cond, int succId) {
		if (cond.trueSuccessor == succId)
			return "TRUE";
		if (cond.falseSuccessor == succId)
			return "FALSE";
		String caseLbl = caseEdgeLabels.get(packPair(cond.id, succId));
		if (caseLbl != null)
			return caseLbl;
		// Check if the condition node is actually a switch statement
		List<IASTNode> asts = cond.getASTNodes();
		if (asts != null && !asts.isEmpty() && asts.get(0).getParent() instanceof IASTSwitchStatement) {
			return "DEFAULT";
		}
		return "FALSE"; // consistent fallback — was "F"
	}

	// -----------------------------------------------------------------------
	// Graph-API methods used by CDGNode, BranchChainExtractor, etc.
	// -----------------------------------------------------------------------

	/**
	 * Returns the list of {@link ControlDependenceEdge}s that originate at
	 * {@code cdgNode} (i.e. the nodes that {@code cdgNode} controls).
	 *
	 * <p>
	 * Used by {@link CDGNode#isLeafNode} and {@link CDGNode#isBranchNode}.
	 *
	 * @param cdgNode the condition (source) node
	 * @return outgoing CDG edges from {@code cdgNode}; never {@code null}
	 */
	public List<ControlDependenceEdge> outgoingEdgesOf(CDGNode cdgNode) {
		List<ControlDependenceEdge> edges = outgoingEdges.get(cdgNode.id);
		return (edges != null) ? Collections.unmodifiableList(edges) : Collections.emptyList();
	}

	/**
	 * Returns CDG edges whose target is {@code successor} (incoming control
	 * dependence).
	 */
	public List<ControlDependenceEdge> incomingEdgesOf(CDGNode successor) {
		if (successor == null)
			return Collections.emptyList();
		List<ControlDependenceEdge> edges = incomingEdges.get(successor.id);
		return (edges != null) ? Collections.unmodifiableList(edges) : Collections.emptyList();
	}

	/** Entry node (CFG start). */
	public CDGNode getEntryNode() {
		return nodes.get(entryId);
	}

	/** Lookup CDG node for a CFG vertex (CFG-based CDG only). */
	public CDGNode getCDGNode(CFGNode cfgNode) {
		return cfgToCdgMap.get(cfgNode);
	}

	public CFG getOriginalCFG() {
		return originalCFG;
	}

	/**
	 * Returns the source (condition) node of a {@link ControlDependenceEdge}.
	 *
	 * @param ce a CDG edge previously returned by this graph
	 * @return the condition {@link CDGNode}, or {@code null} if the edge is unknown
	 */
	public CDGNode getEdgeSource(ControlDependenceEdge ce) {
		Integer srcId = edgeSource.get(ce);
		return (srcId != null) ? nodes.get(srcId) : null;
	}

	/**
	 * Returns the target (dependent) node of a {@link ControlDependenceEdge}.
	 *
	 * @param ce a CDG edge previously returned by this graph
	 * @return the dependent {@link CDGNode}, or {@code null} if the edge is unknown
	 */
	public CDGNode getEdgeTarget(ControlDependenceEdge ce) {
		Integer tgtId = edgeTarget.get(ce);
		return (tgtId != null) ? nodes.get(tgtId) : null;
	}

	/**
	 * Returns the complete set of {@link ControlDependenceEdge}s in the CDG.
	 *
	 * <p>
	 * Mirrors the JGraphT {@code edgeSet()} contract.
	 *
	 * @return unmodifiable set of all CDG edges
	 */
	public Set<ControlDependenceEdge> edgeSet() {
		// edgeSource.keySet() contains exactly the edges that were materialised
		return Collections.unmodifiableSet(edgeSource.keySet());
	}

	/**
	 * Returns the complete set of {@link CDGNode}s in the CDG.
	 *
	 * <p>
	 * Mirrors the JGraphT {@code vertexSet()} contract.
	 *
	 * @return unmodifiable set of all CDG nodes
	 */
	public Set<CDGNode> vertexSet() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(nodes.values()));
	}

	// -----------------------------------------------------------------------
	// Additional public accessors
	// -----------------------------------------------------------------------

	public int getEntryId() {
		return entryId;
	}

	public int getExitId() {
		return exitId;
	}

	public Map<Integer, CDGNode> getNodes() {
		return nodes;
	}

	public CDGNode getNode(int id) {
		return nodes.get(id);
	}

	public Set<CDGEdge> getControlDependencies(int nodeId) {
		return Collections.unmodifiableSet(cd.getOrDefault(nodeId, Collections.emptySet()));
	}

	public Map<Integer, Set<CDGEdge>> getAllControlDependencies() {
		return Collections.unmodifiableMap(cd);
	}

	public Set<Integer> getPreDominators(int n) {
		return unmod(ped, n);
	}

	public Set<Integer> getImmediatePreDominators(int n) {
		return unmod(iped, n);
	}

	public Set<Integer> getReversePreDomination(int n) {
		return unmod(rPed, n);
	}

	public Set<Integer> getReverseImmediatePreDomination(int n) {
		return unmod(rIped, n);
	}

	public Set<Integer> getPreDominanceFrontier(int n) {
		return unmod(pef, n);
	}

	public Set<Integer> getPostDominators(int n) {
		return unmod(pod, n);
	}

	public Set<Integer> getImmediatePostDominators(int n) {
		return unmod(ipod, n);
	}

	public Set<Integer> getReversePostDomination(int n) {
		return unmod(rPod, n);
	}

	public Set<Integer> getReverseImmediatePostDomination(int n) {
		return unmod(rIpod, n);
	}

	public Set<Integer> getPostDominanceFrontier(int n) {
		return unmod(pof, n);
	}

	public Set<Integer> getLeafNodes() {
		Set<Integer> leaves = new HashSet<>();
		for (CDGNode n : nodes.values()) {
			if (n.id != exitId && outgoingEdges.getOrDefault(n.id, Collections.emptyList()).isEmpty())
				leaves.add(n.id);
		}
		return leaves;
	}

	public Set<Integer> getConditionNodes() {
		Set<Integer> conds = new HashSet<>();
		for (CDGNode n : nodes.values()) {
			if (n.isCondition)
				conds.add(n.id);
		}
		return conds;
	}

	// -----------------------------------------------------------------------
	// Utility helpers
	// -----------------------------------------------------------------------

	/**
	 * In-place intersection returning the modified set (avoids extra allocation).
	 */
	private static Set<Integer> retainAll(Set<Integer> a, Set<Integer> b) {
		a.retainAll(b);
		return a;
	}

	private Set<Integer> unmod(Map<Integer, Set<Integer>> map, int id) {
		return Collections.unmodifiableSet(map.getOrDefault(id, Collections.emptySet()));
	}

	// -----------------------------------------------------------------------
	// Debug / summary
	// -----------------------------------------------------------------------

	public StringBuilder printSummary() {
	    StringBuilder sb = new StringBuilder();
	    List<Integer> sorted = new ArrayList<>(nodes.keySet());
	    Collections.sort(sorted);

	    sb.append("=== CDG Summary ===\n");
	    sb.append(String.format("Entry: N%d  Exit: N%d%n%n", entryId, exitId));

	    sb.append("--- Control Dependencies (CD) ---\n");
	    for (int id : sorted) {
	        sb.append(String.format("  CD[N%d] = %s%n", id, formatCDSet(cd.get(id))));
	    }

	    sb.append(String.format("%n--- Post-Dominance Frontier (POF) ---%n"));
	    for (int id : sorted) {
	        sb.append(String.format("  POF[N%d] = %s%n", id, pof.get(id)));
	    }

	    sb.append(String.format("%n--- Post-Dominators (POD) ---%n"));
	    for (int id : sorted) {
	        sb.append(String.format("  POD[N%d] = %s%n", id, pod.get(id)));
	    }

	    sb.append(String.format("%n--- Immediate Post-Dominators (IPOD) ---%n"));
	    for (int id : sorted) {
	        sb.append(String.format("  IPOD[N%d] = %s%n", id, ipod.get(id)));
	    }

	    sb.append(String.format("%n--- CDG Edges ---%n"));
	    for (ControlDependenceEdge e : edgeSet()) {
	        CDGNode src = getEdgeSource(e);
	        CDGNode tgt = getEdgeTarget(e);
	        sb.append(String.format("  N%d --[%s]--> N%d%n", (src != null ? src.id : -1), e.toString(), (tgt != null ? tgt.id : -1)));
	    }
	    sb.append(String.format("%n"));

	    return sb;
	}


	private String formatCDSet(Set<CDGEdge> edges) {
		if (edges == null || edges.isEmpty())
			return "∅";
		StringBuilder sb = new StringBuilder("{ ");
		for (CDGEdge e : edges)
			sb.append(String.format("(N%d, %s) ", e.conditionNodeId, e.label));
		sb.append("}");
		return sb.toString();
	}

	// -----------------------------------------------------------------------
	// Post-dominator successors & ENTRY FLOW
	// -----------------------------------------------------------------------

	/** CFG successors; leaf nodes (except exit) virtually reach exit. */
	private List<Integer> getPostDomSuccessors(CDGNode n) {
		if (!n.successors.isEmpty()) {
			return n.successors;
		}
		if (n.id != exitId) {
			return Collections.singletonList(exitId);
		}
		return Collections.emptyList();
	}

	/**
	 * Nodes eligible for ENTRY→FLOW: forward-reachable, outside loop bodies and
	 * if-branch interiors (if-no-else chains excluded from branch scope).
	 */
	private void computeEntryFlowNodeIds() {
		Set<CFGNode> loopBody = new HashSet<>();
		Set<CFGNode> loopInits = new HashSet<>();
		Set<CFGNode> branchScoped = new HashSet<>();

		for (CFGNode a : originalCFG.vertexSet()) {
			CFGNode ts = getCfgTrueSucc(a);
			CFGNode fs = getCfgFalseSucc(a);
			if (ts == null || fs == null)
				continue;

			if (isLoopHead(a, ts, fs)) {
				loopBody.addAll(collectCfgRegion(a, fs, ts));
				loopInits.addAll(collectLoopInits(a));
			} else if (!isIfNoElse(a)) {
				branchScoped.addAll(collectCfgRegion(a, fs, ts));
				branchScoped.addAll(collectCfgRegion(a, ts, fs));
			}
		}

		Set<CFGNode> reachable = forwardReachableFrom(originalCFG.getStart());
		entryFlowNodeIds = new HashSet<>();
		CFGNode end = originalCFG.getEnd();

		for (CFGNode n : reachable) {
			if (loopBody.contains(n) || loopInits.contains(n) || branchScoped.contains(n))
				continue;
			CDGNode cdg = cfgToCdgMap.get(n);
			if (cdg != null)
				entryFlowNodeIds.add(cdg.id);
		}
		if (end != null) {
			CDGNode cdgEnd = cfgToCdgMap.get(end);
			if (cdgEnd != null)
				entryFlowNodeIds.add(cdgEnd.id);
		}
		CDGNode cdgEntry = cfgToCdgMap.get(originalCFG.getStart());
		if (cdgEntry != null)
			entryFlowNodeIds.add(cdgEntry.id);
	}

	/*
	 * private void attachEntryFlowEdges() { if (entryFlowNodeIds == null) return;
	 * for (int targetId : entryFlowNodeIds) { if (targetId == entryId) continue;
	 * boolean suppress = false; for (ControlDependenceEdge inE :
	 * incomingEdges.getOrDefault(targetId, Collections.emptyList())) { int srcId =
	 * edgeSource.get(inE); if (srcId == entryId) continue; String lbl =
	 * inE.toString(); if ("FLOW".equals(lbl)) continue; if ("TRUE".equals(lbl) ||
	 * "FALSE".equals(lbl)) continue; suppress = true; break; } if (!suppress) {
	 * addFlowEdge(entryId, targetId); } } }
	 */

	/*
	 * private void attachFlowToNodesWithoutIncoming() { for (int id :
	 * nodes.keySet()) { if (id == entryId) continue; if
	 * (incomingEdges.getOrDefault(id, Collections.emptyList()).isEmpty()) {
	 * addFlowEdge(entryId, id); } } }
	 */

	private Set<CFGNode> forwardReachableFrom(CFGNode start) {
		Set<CFGNode> visited = new HashSet<>();
		Deque<CFGNode> q = new ArrayDeque<>();
		if (start != null) {
			q.add(start);
			visited.add(start);
		}
		while (!q.isEmpty()) {
			CFGNode cur = q.poll();
			for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
				CFGNode s = originalCFG.getEdgeTarget(e);
				if (s != null && visited.add(s))
					q.add(s);
			}
		}
		return visited;
	}

	private Set<CFGNode> collectCfgRegion(CFGNode head, CFGNode stop, CFGNode from) {
		Set<CFGNode> visited = new HashSet<>();
		Deque<CFGNode> stack = new ArrayDeque<>();
		stack.push(from);
		while (!stack.isEmpty()) {
			CFGNode cur = stack.pop();
			if (cur.equals(head) || cur.equals(stop))
				continue;
			if (!visited.add(cur))
				continue;
			for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
				CFGNode t = originalCFG.getEdgeTarget(e);
				if (t != null)
					stack.push(t);
			}
		}
		return visited;
	}

	private boolean isLoopHead(CFGNode head, CFGNode trueSucc, CFGNode falseSucc) {
		Set<CFGNode> body = collectCfgRegion(head, falseSucc, trueSucc);
		for (CFGNode n : body) {
			for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
				if (originalCFG.getEdgeTarget(e).equals(head))
					return true;
			}
		}
		return false;
	}

	private Set<CFGNode> collectLoopInits(CFGNode loopHead) {
		Set<CFGNode> inits = new HashSet<>();
		for (LabeledEdge e : originalCFG.incomingEdgesOf(loopHead)) {
			if (!(e instanceof FlowEdge))
				continue;
			CFGNode pred = originalCFG.getEdgeSource(e);
			if (pred != null && !pred.equals(originalCFG.getStart())) {
				inits.add(pred);
			}
		}
		return inits;
	}

	private boolean isIfNoElse(CFGNode a) {
		CFGNode merge = getCfgFalseSucc(a);
		CFGNode trueEntry = getCfgTrueSucc(a);
		if (merge == null || trueEntry == null)
			return false;
		for (LabeledEdge e : originalCFG.incomingEdgesOf(merge)) {
			CFGNode pred = originalCFG.getEdgeSource(e);
			if (pred.equals(a))
				continue;
			if (reachableInArm(a, trueEntry, pred, merge))
				return true;
		}
		return false;
	}

	private boolean reachableInArm(CFGNode head, CFGNode from, CFGNode target, CFGNode merge) {
		Set<CFGNode> vis = new HashSet<>();
		Deque<CFGNode> q = new ArrayDeque<>();
		q.add(from);
		vis.add(from);
		while (!q.isEmpty()) {
			CFGNode cur = q.poll();
			if (cur.equals(target))
				return true;
			for (LabeledEdge e : originalCFG.outgoingEdgesOf(cur)) {
				CFGNode t = originalCFG.getEdgeTarget(e);
				if (t == null || t.equals(head) || t.equals(merge))
					continue;
				if (vis.add(t))
					q.add(t);
			}
		}
		return false;
	}

	private CFGNode getCfgTrueSucc(CFGNode n) {
		for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
			if (e instanceof TrueEdge)
				return originalCFG.getEdgeTarget(e);
		}
		return null;
	}

	private CFGNode getCfgFalseSucc(CFGNode n) {
		for (LabeledEdge e : originalCFG.outgoingEdgesOf(n)) {
			if (e instanceof FalseEdge)
				return originalCFG.getEdgeTarget(e);
		}
		return null;
	}
}