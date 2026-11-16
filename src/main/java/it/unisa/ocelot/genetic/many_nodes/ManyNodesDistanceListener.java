package it.unisa.ocelot.genetic.many_nodes;

import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

//TODO DUPLICATES!!! This class and ManyNodesDistanceListenerShortestPath and ManyEdgesDistanceListener and DominatorListener are almost identical
public final class ManyNodesDistanceListener implements SimulatorListener {
	private final CFG cfg;
	private final List<CFGNode> targets;
	private final Map<CFGNode, Integer> fixedApproachLevels;
	private final Map<CFGNode, Set<CFGNode>> dominators;
	
	private int currentTargetIndex;
	private CFGNode currentTarget;
	
	private final List<ExecutionEvent> nearestEvents;
	private CFGNode nearest;
	
	public ManyNodesDistanceListener(
			CFG pCfg, List<CFGNode> pTargets, 
			Map<CFGNode, Integer> pFixedApproachLevels, Map<CFGNode, Set<CFGNode>> pDominators) {
		this.cfg = pCfg;
		this.targets = pTargets;
		this.fixedApproachLevels = pFixedApproachLevels;
		this.dominators = pDominators;
		
		this.currentTargetIndex = 0;
		this.currentTarget = this.targets.get(this.currentTargetIndex);

		this.nearestEvents = new ArrayList<>();
		this.nearest = null;
}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		//nothing to do
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		CFGNode node = this.cfg.getEdgeSource(pEdge);

		if (node.equals(this.nearest)) {
			this.nearestEvents.clear();
			this.nearestEvents.add(pEvent);
		}
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		CFGNode node = this.cfg.getEdgeSource(pEdge);

		if (node.equals(this.nearest)) {
			this.nearestEvents.clear();
			this.nearestEvents.addAll(pCases);
		}
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		if (pNode.equals(this.currentTarget)) {
			this.currentTargetIndex++;
			try {
				this.currentTarget = this.targets.get(this.currentTargetIndex);
			} catch (IndexOutOfBoundsException e) {
				this.currentTarget = null;
			}
		}
		
		if (this.currentTarget == null) {
			return;
		}
		
		Set<CFGNode> dominators = this.dominators.get(this.currentTarget);
		if (dominators.contains(pNode)) {
			dominators.remove(pNode);
			this.nearest = pNode;
		}
	}
	
	/**
	 * Calculates the approach level
	 * 
	 * @return an approach level {@code int} value.
	 */
	//TODO fix here
	public int getApproachLevel() {
		//If the last target node is reached, return 0
		if (this.currentTarget == null) {
			return 0;
		}
		
		int approachLevel = this.dominators.get(this.currentTarget).size();
		int additionalApproachLevel = this.fixedApproachLevels.get(this.currentTarget);
		return approachLevel + additionalApproachLevel;
	}
	
	/**
	 * Calculates the branch distance.
	 * 
	 * @return a {@code double} value of branch distance.
	 */
	//TODO we want it private, but unfortunately this method is used outside this class...
	public double getBranchDistance() {
		if (getApproachLevel() == 0) {
			return 0d;
		}
		
		// approach level more than 0
		
		List<LabeledEdge> path = findPathBetween(this.cfg, this.nearest, this.currentTarget);
		LabeledEdge nearestEdge = path.get(0);

		double distance = 0;
		if (this.nearestEvents.size() == 1) {
			// single condition
			//TODO is this cast correct? Possibly this is a CaseExecutionEvent
			IfExecutionEvent event = (IfExecutionEvent) this.nearestEvents.get(0);
			distance = Math.max(event.getDistanceThen(), event.getDistanceElse());
		} else {
			for (ExecutionEvent event : this.nearestEvents) {
				if (event.getEdge().equals(nearestEdge)) {
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
	 * Return the value of the many-nodes distance.
	 * 
	 * @return a {@code double} value of many-nodes distance.
	 */
	public double getDistance() {
		return getApproachLevel() + Utils.normalize(getBranchDistance());
	}
}
