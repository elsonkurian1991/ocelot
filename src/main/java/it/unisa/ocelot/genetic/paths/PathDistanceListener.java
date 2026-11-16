package it.unisa.ocelot.genetic.paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

public final class PathDistanceListener implements SimulatorListener {
	@SuppressWarnings("unused")
	private final CFG cfg;
	private final List<LabeledEdge> target;
	
	private List<LabeledEdge> executionPath;
	private List<ExecutionEvent> events;
	private List<List<CaseExecutionEvent>> caseEvents;
	private HashMap<LabeledEdge, Boolean> edgeVisited;
	
	private double branchDistance;
	private int current; //The index of the target path that we are analysing
	private boolean onPath;
	private boolean terminated;
	
	public PathDistanceListener(CFG pCfg, List<LabeledEdge> pTarget) {
		this.cfg = pCfg;
		this.target = pTarget;
		
		this.executionPath = new ArrayList<>();
		this.events = new ArrayList<>();
		this.caseEvents = new ArrayList<>();
		this.edgeVisited = new HashMap<>();
		for (LabeledEdge edge : pTarget) {
			this.edgeVisited.put(edge, false);
		}
		this.branchDistance = 0;
		this.current = 0;
		this.onPath = true;
		this.terminated = false;
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		this.executionPath.add(pEdge);
		if (this.target.contains(pEdge)) {
			this.edgeVisited.put(pEdge, true);
		}
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		this.executionPath.add(pEdge);
		if (this.target.contains(pEdge)) {
			this.edgeVisited.put(pEdge, true);
		}
		this.events.add(pEvent);
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCaseEvents) {
		this.executionPath.add(pEdge);
		if (this.target.contains(pEdge)) {
			this.edgeVisited.put(pEdge, true);
		}
		this.events.add(pEvent);
		this.caseEvents.add(pCaseEvents);
		
		if (this.terminated) {
			return;
		}
		
		if (this.onPath) {
			// If the current index is out of bound, stops the procedure
			if (this.current >= this.target.size()) {
				this.terminated = true;
				return;
			}
			
			LabeledEdge edgeCurrent = this.target.get(this.current);
			if (pEdge.equals(edgeCurrent)) {
				this.edgeVisited.put(pEdge, true);
			} else {
				this.onPath = false;
				if (pCaseEvents == null) {
					this.branchDistance += pEvent.getDistance();
				} else {
					for (CaseExecutionEvent event : pCaseEvents) {
						if (edgeCurrent.matchesEvent(event)) {
							this.branchDistance += ((CaseExecutionEvent) event).getDistance();
						}
					}
				}
			}
		} else if (this.target.contains(pEdge)) {
			this.current = this.target.indexOf(pEdge);
			this.onPath = true;
			this.edgeVisited.put(pEdge, true);
		}
		
		this.current++;
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		//nothing to do here
	}
	
	/**
	 * Calculates the path distance.
	 * 
	 * @return an {@code int} value of path distance.
	 */
	private int getPathDistance() {
		int distance = 0;
		for (Entry<LabeledEdge, Boolean> entry : this.edgeVisited.entrySet()) {
			if (!entry.getValue()) {
				distance++;
			}
		}
		
		return distance;
	}
	
	/**
	 * Gets the branch distance.
	 * 
	 * @return a {@code double} value of branch distance.
	 */
	public double getBranchDistance() {
		return this.branchDistance;
	}
		
	/**
	 * Return the value of the total distance.
	 * 
	 * @return a {@code double} value of total distance.
	 */
	public double getDistance() {
		return getPathDistance() + Utils.normalize(getBranchDistance());
	}
}
