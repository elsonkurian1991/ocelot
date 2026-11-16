package it.unisa.ocelot.c.cfg;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Produces all the linearly independent paths of a 
 * {@link CFG} by using McCabe's LIPS algorithm. 
 */
//TODO I really DO NOT think this class works; compare it with McCabeCalculator (that it's just the same thing but calculates all the paths eagerly).
@Deprecated
public final class LIPSCalculator {
	private final CFG cfg;
	private final Set<LabeledEdge> emitted;
	private final Stack<CFGNode> partiallyExploredNodes;
	private final List<List<LabeledEdge>> mcCabePaths;

	/**
	 * Constructor (with default basis path).
	 *  
	 * @param pCfg a {@link CFG}.
	 */
	@Deprecated
	public LIPSCalculator(CFG pCfg) {
		this.cfg = pCfg;
		this.emitted = new HashSet<>();
		this.partiallyExploredNodes = new Stack<>();
		this.mcCabePaths = new ArrayList<>();
	}
	
	/**
	 * Constructor (with covered edges and default basis path).
	 *  
	 * @param pCfg a {@link CFG}.
	 * @param pCovered a {@link List}{@code <}{@link LabeledEdge}{@code >}.
	 *        They must be {@link LabeledEdge#isCovered() covered}.
	 */
	@Deprecated
	public LIPSCalculator(CFG pCfg, Set<LabeledEdge> pCovered) {
		this(pCfg);
		this.emitted.addAll(pCovered);
	}
	
	/**
	 * Constructor (with covered edges and user-specified basis path).
	 * 
	 * @param pCfg a {@link CFG}.
	 * @param pCovered a {@link List}{@code <}{@link LabeledEdge}{@code >}.
	 *        They must be {@link LabeledEdge#isCovered() covered}.
	 * @param pPath a {@link List}{@code <}{@link LabeledEdge}{@code >}.
	 */
	//TODO I hacked this out of existing code, but I'm NOT AT ALL sure that it works.
	@Deprecated
	public LIPSCalculator(CFG pCfg, Set<LabeledEdge> pCovered, List<LabeledEdge> pPath) {
		this(pCfg);
		this.mcCabePaths.add(pPath);
		this.emitted.addAll(pCovered);
		this.emitted.addAll(pPath);
		for (LabeledEdge edge : pPath) {
			CFGNode origin = this.cfg.getEdgeSource(edge);
			this.partiallyExploredNodes.push(origin);
		}
	}
	
	@Deprecated
	public LabeledEdge getNextTarget() {
		try {
			CFGNode node = this.partiallyExploredNodes.peek();
			for (LabeledEdge edge : this.cfg.outgoingEdgesOf(node)) {
				if (!this.emitted.contains(edge)) {
					this.emitted.add(edge);
					return edge;
				}
			}			
			this.partiallyExploredNodes.pop();
			return getNextTarget();
		} catch (EmptyStackException e) {
			return null;
		}
	}

	@Deprecated
	public int estimateMissingTargets() {
		int extimate = 0;
		for (CFGNode node : this.cfg.vertexSet()) {
			int totalEdges = 0;
			int emittedEdges = 0;
			for (LabeledEdge edge : this.cfg.outgoingEdgesOf(node)) {
				totalEdges++;
				if (this.emitted.contains(edge)) {
					emittedEdges++;
				}
			}
			
			if (totalEdges > 1) {
				extimate += (totalEdges - emittedEdges);
			}
		}
		
		return extimate + 1;
	}
	
	@Deprecated
	public Set<LabeledEdge> getCurrentlyMissingTargets() {
		final HashSet<LabeledEdge> result = new HashSet<>();
		for (CFGNode node : this.partiallyExploredNodes) {
			for (LabeledEdge edge : this.cfg.outgoingEdgesOf(node)) {
				if (!this.emitted.contains(edge)) {
					result.add(edge);
				}
			}
		}
		return result;
	}
	
	@Deprecated
	public int currenltyMissingTargets() {
		return getCurrentlyMissingTargets().size();
	}
}
