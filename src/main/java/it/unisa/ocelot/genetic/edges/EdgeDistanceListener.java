package it.unisa.ocelot.genetic.edges;

import static org.jgrapht.alg.DijkstraShortestPath.findPathBetween;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

/**
 * A listener class used to calculate distances about edges
 * 
 * @author giograno
 */
public final class EdgeDistanceListener implements SimulatorListener {
	private final CFG cfg;
	private final LabeledEdge targetEdge; // the target edge (the one we want to cover)
	private final CFGNode targetSource; // the source node of targetEdge
	private final HashSet<CFGNode> dominators;
	private final HashSet<LabeledEdge> serendipitousPotentials;
	
	private CFGNode nearestNode;
	private List<ExecutionEvent> nearestEvents;
	private boolean forceCovered;
	private Set<LabeledEdge> serendipitousCovered;
	private Set<List<ExecutionEvent>> pairEventSets;

	/**
	 * Constructor.
	 * 
	 * @param pCfg the {@link CFG}.
	 * @param pTarget the {@link LabeledEdge} of {@code pCfg} that 
	 *        we want to cover.
	 * @param pDominators the {@link Set}{@code <}{@link CFGNode}{@code >}
	 *        containing all the dominators of 
	 */
	public EdgeDistanceListener(CFG pCfg, LabeledEdge pTarget, Set<CFGNode> pDominators, Set<LabeledEdge> pSerendipitousPotentials) {
		this.cfg = pCfg;
		this.targetEdge = pTarget;
		this.targetSource = this.cfg.getEdgeSource(this.targetEdge);
		this.dominators = new HashSet<>(pDominators);
		this.serendipitousPotentials = new HashSet<>(pSerendipitousPotentials);
		
		this.nearestNode = null;
		this.nearestEvents = new ArrayList<>();
		this.forceCovered = false;
		this.serendipitousCovered = new HashSet<>();
		this.pairEventSets = new HashSet<>();
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		if (pEdge.equals(this.targetEdge)) {
			this.forceCovered = true;
		}
		
		if (this.serendipitousPotentials.contains(pEdge)) {
			this.serendipitousCovered.add(pEdge);
		}
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		onEdgeVisit(pEdge);
		
		if (this.forceCovered) {
			return;
		}
		
		CFGNode node = this.cfg.getEdgeSource(pEdge);
		if (node.equals(this.nearestNode)) {
			this.nearestEvents.clear();
			this.nearestEvents.add(pEvent);
			this.pairEventSets.add(new ArrayList<>(this.nearestEvents)); //TODO are we sure we want to put in pairEventSets an alias to the *mutable* this.nearestEvents List? Shouldn't we make a copy? 
		}
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		onEdgeVisit(pEdge);
		
		if (this.forceCovered) {
			return;
		}

		CFGNode node = this.cfg.getEdgeSource(pEdge);
		if (node.equals(this.nearestNode)) {
			this.nearestEvents.clear();
			this.nearestEvents.addAll(pCases);
			this.pairEventSets.add(new ArrayList<>(this.nearestEvents)); //TODO are we sure we want to put in pairEventSets an alias to the *mutable* this.nearestEvents List? Shouldn't we make a copy?
		}
	}

	@Override
	public void onNodeVisit(CFGNode node) {
		if (this.dominators.contains(node)) {
			this.dominators.remove(node);
			this.nearestNode = node;
			this.pairEventSets.clear();
		}
	}

	/**
	 * Calculates the approach level
	 * 
	 * @return an approach level {@code int} value.
	 */
	private int getApproachLevel() {
		if (this.forceCovered) {
			return 0;
		}
		return this.dominators.size();
	}
	
	/**
	 * Calculates the branch distance.
	 * 
	 * @return a {@code double} value of branch distance.
	 */
	//TODO we want it private, but unfortunately this method is used outside this class...
	public double getBranchDistance() {
		if (this.forceCovered) {
			return 0.0d;
		}
		
		//TODO tHis is surely WRONG!!!!!
		double minDistance = Double.MAX_VALUE;
		if (getApproachLevel() == 0) {
			for (List<ExecutionEvent> nearestEvents : this.pairEventSets) {
				if (nearestEvents.size() == 1) {
					// Execution has reached the source node of the target edge
					ExecutionEvent event = nearestEvents.get(0);
					
					if (event.getEdge().equals(this.targetEdge)) {
						return 0;
					} else {
						//TODO is this cast ok?
						IfExecutionEvent eventIf = (IfExecutionEvent) event;
						double distance = Math.max(eventIf.getDistanceThen(), eventIf.getDistanceElse()); 
						if (distance < minDistance) {
							minDistance = distance;
						}
					}
				} else {
					for (ExecutionEvent event : nearestEvents) {
						if (event.getEdge().equals(this.targetEdge)) {
							//TODO is this cast ok?
							CaseExecutionEvent eventCase = (CaseExecutionEvent) event;
							double distance = eventCase.getDistance(); 
							if (distance < minDistance) {
								minDistance = distance;
							}
						}
					}
				}
			}
		}
		
		//TODO shouldn't go an "else" here? (for approach level > 0)?

		for (List<ExecutionEvent> nearestEvents : this.pairEventSets) {
			// approach level more than 0
			if (nearestEvents.size() == 1) {
				// (as before)
				ExecutionEvent event = nearestEvents.get(0);
				
				//TODO is this cast ok?
				IfExecutionEvent eventIf = (IfExecutionEvent) event;
				double distance = Math.max(eventIf.getDistanceThen(), eventIf.getDistanceElse()); 
				if (distance < minDistance) {
					minDistance = distance;
				}
			} else {
				List<LabeledEdge> pathShortestNearestToTarget = findPathBetween(this.cfg, this.nearestNode, this.targetSource);

				LabeledEdge nearestEdge;
				if (pathShortestNearestToTarget.size() > 0) {
					nearestEdge = pathShortestNearestToTarget.get(0);
				} else {
					// Zero-length path: this.nearestNode is this.targetSource!
					nearestEdge = this.targetEdge;
				}
				
				for (ExecutionEvent event : nearestEvents) {
					if (event.getEdge().equals(nearestEdge)) {
						//TODO is this cast ok?
						CaseExecutionEvent eventCase = (CaseExecutionEvent) event;
						double distance = eventCase.getDistance();
						if (distance < minDistance) {
							minDistance = distance;
						}
					}
				}
			}
		}
		
		return minDistance;
	}

	/**
	 * Return the value of the edge distance.
	 * 
	 * @return a {@code double} value of edge distance.
	 */
	public double getDistance() {
		return getApproachLevel() + Utils.normalize(getBranchDistance());
	}

	/**
	 * Returns the serendipitous potentials that 
	 * have been covered.
	 * 
	 * @return a {@link Set}{@code <}{@link LabeledEdge}{@code >},
	 *         subset of the set of serendipitous potentials 
	 *         passed upon creation.
	 */
	public Set<LabeledEdge> getSerendipitousCovered() {
		return this.serendipitousCovered;
	}
}
