package it.unisa.ocelot.genetic.many_nodes;

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

//TODO DUPLICATES!!! This class and ManyNodesDistanceListener and ManyEdgesDistanceListener and DominatorListener are almost identical
@Deprecated
public final class ManyNodesDistanceListenerShortestPath implements SimulatorListener {
	private final CFG cfg;
	private final List<CFGNode> targets;
	private final Map<CFGNode, Integer> fixedApproachLevels;

	private int currentTargetIndex;
	private CFGNode currentTarget;
	
	private final List<ExecutionEvent> nearestEvents;
	private LabeledEdge nearestEdge;
	private CFGNode nearestNode;
	
	private int shortestPathLength;
	
	@Deprecated
	public ManyNodesDistanceListenerShortestPath(CFG pCfg, List<CFGNode> pTargets, Map<CFGNode, Integer> pFixedApproachLevels) {
		this.cfg = pCfg;
		this.targets = pTargets;
		this.fixedApproachLevels = pFixedApproachLevels; //TODO calculate it as in the other two classes?
		
		this.currentTargetIndex = 0;
		this.currentTarget = this.targets.get(this.currentTargetIndex);
		
		this.nearestEvents = new ArrayList<>();
		this.nearestEdge = null;
		this.nearestNode = null;

		this.shortestPathLength = Integer.MAX_VALUE;
	}

	@Deprecated
	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		//nothing to do
	}

	@Deprecated
	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		CFGNode node = this.cfg.getEdgeSource(pEdge);

		if (node.equals(this.nearestNode)) {
			this.nearestEvents.clear();
			this.nearestEvents.add(pEvent);
		}
	}

	@Deprecated
	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		CFGNode node = this.cfg.getEdgeSource(pEdge);

		if (node.equals(this.nearestNode)) {
			this.nearestEvents.clear();
			this.nearestEvents.addAll(pCases);
		}
	}

	@Deprecated
	@Override
	public void onNodeVisit(CFGNode pNode) {
		if (pNode.equals(this.currentTarget)) {
			this.currentTargetIndex++;
			try {
				this.currentTarget = this.targets.get(this.currentTargetIndex);
			} catch (IndexOutOfBoundsException e) {
				this.currentTarget = null;
			}
			
			this.shortestPathLength = Integer.MAX_VALUE;
		}
		
		if (this.currentTarget == null) {
			return;
		}
		
		List<LabeledEdge> path = findPathBetween(this.cfg, pNode, this.currentTarget);
		if (path != null && path.size() < this.shortestPathLength) {
			this.nearestNode = pNode;
			if (path.size() > 0) {
				this.nearestEdge = path.get(0);
			} else {
				this.nearestEdge = null;
			}
			this.shortestPathLength = path.size();
		}
	}
	
	//TODO fix here
	@Deprecated
	public int getApproachLevel() {
		//If the last target node is reached, return 0
		if (this.currentTarget == null) {
			return 0;
		}
		
		int approachLevel = this.shortestPathLength;
		int additionalApproachLevel = this.fixedApproachLevels.get(this.currentTarget);
		return approachLevel + additionalApproachLevel;
	}
	
	@Deprecated
	public double getBranchDistance() {
		if (getApproachLevel() == 0) {
			return 0d;
		}

		// approach level more than 0
		
		double distance = 0;
		if (this.nearestEvents.size() == 1) {
			// single condition
			//TODO is this cast correct? Possibly this is a CaseExecutionEvent
			IfExecutionEvent event = (IfExecutionEvent) this.nearestEvents.get(0);
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
	
	@Deprecated
	public double getDistance() {
		return Utils.safeDouble(getApproachLevel() + Utils.normalize(getBranchDistance()));
	}
}
