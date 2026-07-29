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
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.CaseEdge;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
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
	/** CDGNodes keyed by their auto-assigned CDGNode id. */
	private final Map<Integer, CDGNode> nodes;
	/** Entry node id (maps to cfg.getStart()). */
	private final int entryId;
	/** Exit node id (maps to cfg.getEnd()). */
	private final int exitId;
	
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
					enrichConditionLabel(srcCdg, cfgNode);
					
				} else if (edge instanceof FalseEdge) {
					srcCdg.isCondition = true;
					srcCdg.falseSuccessor = tgtCdg.id;
					enrichConditionLabel(srcCdg, cfgNode);

				} else if (edge instanceof CaseEdge) {
					srcCdg.isCondition = true;
					Object lbl = edge.getLabel();
					if (lbl != null) {
						caseEdgeLabels.put(packPair(srcCdg.id, tgtCdg.id), lbl.toString());
					}
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

	public void buildCDG() {
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
	}

	/**
	 * Converts the raw {@code cd} map (conditionId, label pairs) into
	 * {@link ControlDependenceEdge} instances stored in the adjacency maps
	 * ({@link #outgoingEdges}, {@link #edgeSource}, {@link #edgeTarget}).
	 *
	 * <p>
	 * Direction of a CDG edge: condition node → dependent node. "Node X is
	 * control-dependent on condition Y with label L" becomes an edge Y --L--> X.
	 */
	private void materialiseEdges() {
		
		for (int id : nodes.keySet()) {
			outgoingEdges.put(id, new ArrayList<>());
			incomingEdges.put(id, new ArrayList<>());
		}

		for (Map.Entry<Integer, Set<CDGEdge>> entry : cd.entrySet()) {
			int dependentId = entry.getKey(); // X is the DEPENDENT node

			for (CDGEdge raw : entry.getValue()) {
				int conditionId = raw.conditionNodeId; // Y is the CONDITION node
				String rawLabel = raw.label;

				ControlDependenceEdge cde = new ControlDependenceEdge(rawLabel);
				registerEdge(conditionId, dependentId, cde);
			}
		}
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

	
	private static long packPair(int a, int b) {
		return (((long) a) << 32) | (b & 0xffffffffL);
	}


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
						deps.add(new CDGEdge(yId, getBranchLabel(Y, sId)));
					}
				}
			}
		}
	    augmentWithEntryEdges(result);
		
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
}