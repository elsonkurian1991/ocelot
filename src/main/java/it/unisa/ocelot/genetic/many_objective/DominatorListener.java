package it.unisa.ocelot.genetic.many_objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;

/**
 * Listener needed to calculate normalized branches distance and approach level
 * of a target edge. This listener is deprecated. The unified EdgeDistanceListener
 * is used instead.
 * 
 * @author giograno
 */
//TODO DUPLICATES!!! This class and ManyNodesDistanceListener and ManyNodesDistanceListenerShortestPath and ManyEdgesDistanceListener are almost identical
@Deprecated
public final class DominatorListener implements SimulatorListener {
	private final CFG cfg;
	private final LabeledEdge target;
	
	private final CFGNode parentNode;
	private final DominanceCalculatorVertices<CFGNode, LabeledEdge> dominatorTree;
	private final Set<CFGNode> dominators; //of parentNode
	
	private final List<ExecutionEvent> nearestEvents;
	private CFGNode nearestNode;
	private LabeledEdge nearestEdge;
	
	private int shortestPathLength;

	private boolean targetVisited;

	@SuppressWarnings("static-access")
	public DominatorListener(CFG pCfg, LabeledEdge pTarget) {
		this.cfg = pCfg;
		this.target = pTarget;
		this.parentNode = this.cfg.getEdgeSource(pTarget);
		this.dominatorTree = new DominanceCalculatorVertices<>(pCfg, pCfg.getEntry());
		this.dominators = this.dominatorTree.getStrictDominators(this.parentNode);
		this.nearestEvents = new ArrayList<>();
		this.shortestPathLength = Integer.MAX_VALUE;
		this.targetVisited = false;
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		//nothing to do here
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {

		if (!targetVisited) {
			CFGNode node = this.cfg.getEdgeSource(pEdge);

			if (node.equals(this.nearestNode)) {
				nearestEvents.clear();
				nearestEvents.add(pEvent);
			}
		}
		
		if (pEdge.equals(this.target))
			targetVisited = true;

	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		CFGNode node = this.cfg.getEdgeSource(pEdge);

		if (node.equals(this.nearestNode)) {
			nearestEvents.clear();
			nearestEvents.addAll(pCases);
		}
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		if (this.dominators.contains(pNode)) {
			this.dominators.remove(pNode);
		}

		if (this.dominators.size() < this.shortestPathLength) {
			this.nearestNode = pNode;
			this.shortestPathLength = this.dominators.size();
			if (this.dominators.size() == 0) {
				List<LabeledEdge> outgoingEdges = new ArrayList<>(this.cfg.outgoingEdgesOf(pNode));
				this.nearestEdge = outgoingEdges.get(0);
			} else {
				this.nearestEdge = this.target;
			}
		}
	}

	public int getApproachLevel() {
		if (this.dominators.isEmpty()) {
			return 0;
		}

		return this.dominators.size();
	}
	
	public double getBranchDistance() {
		double distance = 0.D;

		if (getApproachLevel() == 0) {
			// execution reach the parent node of edge target
			IfExecutionEvent event = (IfExecutionEvent) this.nearestEvents.get(0);

			if (target instanceof FalseEdge) {
				distance = event.getDistanceElse();
			} else if (target instanceof TrueEdge) {
				distance = event.getDistanceThen();
			}
			return distance;
		}

		// approach level more than 0
		
		if (this.nearestEvents.size() == 1) {
			// single condition
			//TODO is this cast correct? Possibly this is a CaseExecutionEvent
			IfExecutionEvent event = (IfExecutionEvent) nearestEvents.get(0);
			distance = Math.max(event.getDistanceThen(), event.getDistanceElse());
		} else {
			for (ExecutionEvent event : this.nearestEvents) {
				if (event.getEdge().equals(this.nearestEdge)) {
					//TODO is this cast correct? Possibly this is a IfExecutionEvent
					CaseExecutionEvent eEvent = (CaseExecutionEvent) event;
					distance = eEvent.getDistance();
					//TODO break?
				}
			}
			assert distance != 0 : "non-positive distance error!";
		}
		
		return distance;
	}

	/**
	 * Calculates the branch distance normalized as proposed by Arcuri in
	 * "It does matter how you normalize the branch distance in search based software testing"
	 * 
	 * @return a double value of normalized branch distance
	 */
	public double getNormalizedBranchDistance() {
		double branchDistance = this.getBranchDistance();
		return branchDistance / (branchDistance + 1);
	}

	/**
	 * Return the value of the fitness function, scored by branch distance plus
	 * approach level
	 * 
	 * @return the fitness value
	 */
	public double getFitness() {
		return this.getApproachLevel() + this.getNormalizedBranchDistance();
	}
}
