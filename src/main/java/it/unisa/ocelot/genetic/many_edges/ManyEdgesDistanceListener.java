package it.unisa.ocelot.genetic.many_edges;

import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//TODO DUPLICATES!!! This class and ManyNodesDistanceListener and ManyNodesDistanceListenerShortestPath and DominatorListener are almost identical
public final class ManyEdgesDistanceListener implements SimulatorListener {
	private final CFG cfg;
	private final List<LabeledEdge> targets;
	private final Map<LabeledEdge, Integer> fixedApproachLevels;
	private final Map<LabeledEdge, Set<LabeledEdge>> dominators;
	
	private int currentTargetIndex;
	private LabeledEdge currentTarget;
	
	private final List<ExecutionEvent> nearestEvents;
	private LabeledEdge nearest;
	
	//TODO delete it?
	private final List<LabeledEdge> DEBUGTRACE;
	
	public ManyEdgesDistanceListener(
			CFG pCFG, List<LabeledEdge> pTargets, 
			Map<LabeledEdge, Integer> pFixedApproachLevels, Map<LabeledEdge, Set<LabeledEdge>> pDominators) {
		this.cfg = pCFG;
		this.targets = pTargets;
		this.fixedApproachLevels = pFixedApproachLevels;
		this.dominators = pDominators;
		
		this.currentTargetIndex = 0;
		this.currentTarget = this.targets.get(this.currentTargetIndex);
		
		this.nearestEvents = new ArrayList<>();
		this.nearest = null;
		
		this.DEBUGTRACE = new ArrayList<>();
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		onEdgeVisitGeneric(pEdge);
		this.DEBUGTRACE.add(pEdge);
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		onEdgeVisitGeneric(pEdge);

		if (this.nearest == null) {
			return;
		}
		
		CFGNode node = this.cfg.getEdgeTarget(this.nearest);
		if (this.nearest.equals(pEdge) || this.cfg.outgoingEdgesOf(node).contains(pEdge)) {
			this.nearestEvents.clear();
			this.nearestEvents.add(pEvent);
		}
		
		this.DEBUGTRACE.add(pEdge);		
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent,
			List<CaseExecutionEvent> pCases) {
		onEdgeVisitGeneric(pEdge);
		
		if (this.nearest == null) {
			return;
		}

		CFGNode nearestTarget = this.cfg.getEdgeTarget(this.nearest);
		if (this.nearest.equals(pEdge) || this.cfg.outgoingEdgesOf(nearestTarget).contains(pEdge)) {
			this.nearestEvents.clear();
			this.nearestEvents.addAll(pCases);
		}
	}
	
	private void onEdgeVisitGeneric(LabeledEdge pEdge) {
		if (pEdge.equals(this.currentTarget)) {
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
		
		Set<LabeledEdge> dominators = this.dominators.get(this.currentTarget);
		if (dominators.contains(pEdge)) {
			dominators.remove(pEdge);
			this.nearest = pEdge;
		}
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		//nothing to do here
	}
	
	/**
	 * Calculates the approach level.
	 * 
	 * @return an approach level {@code int} value.
	 */
	//TODO fix here
	private int getApproachLevel() {
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
		
		List<LabeledEdge> path = findPathBetween(this.cfg, this.cfg.getEdgeTarget(this.nearest), this.cfg.getEdgeTarget(this.currentTarget));
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
	 * Return the value of the many-edges distance.
	 * 
	 * @return a {@code double} value of many-edges distance.
	 */
	public double getDistance() {
		return getApproachLevel() + Utils.normalize(getBranchDistance());
	}
}
