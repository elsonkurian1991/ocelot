package it.unisa.ocelot.genetic.nodes;

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
 * Listener that calculates branch distance and approach level. The targets are the nodes.
 *  
 * @author simone
 */
public final class NodeDistanceListener implements SimulatorListener {
	private final CFG cfg;
	private final CFGNode target;
	private final Set<CFGNode> dominators;

	private final List<ExecutionEvent> nearestEvents;
	private CFGNode nearest;
	
	public NodeDistanceListener(CFG pCFG, CFGNode pTarget, Set<CFGNode> pDominators) {
		this.cfg = pCFG;
		this.target = pTarget;
		this.dominators = new HashSet<>(pDominators);
		this.nearestEvents = new ArrayList<>();
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
		if (this.dominators.contains(pNode)) {
			this.dominators.remove(pNode);
			this.nearest = pNode;
		}
	}
	
	/**
	 * Calculates the approach level.
	 * 
	 * @return an approach level {@code int} value.
	 */
	private int getApproachLevel() {
		return this.dominators.size();
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
				
		//TODO is this big conditional statement ok? I've hacked it from  Ocelot code, but I'm not sure I've understood everything
		if (this.nearestEvents.size() == 1) {
			//we are in an "if" statement
			//TODO the comment below ("One of the two branches has distance 0") might mean the opposite: check whether we are in a "switch" statement
			IfExecutionEvent event = (IfExecutionEvent) this.nearestEvents.get(0);
			
			//One of the two branches has distance 0, the other is our branch distance, because it is the
			//distance from the nearest node.
			return Math.max(event.getDistanceThen(), event.getDistanceElse());
		} else {
			//we are in a switch statement
			//TODO or not? Look the previous TODO.
			
			//TODO this seems a crappy way of finding the nearest edge. Does it work?
			List<LabeledEdge> path = findPathBetween(this.cfg, this.nearest, this.target);
			LabeledEdge nearestEdge = path.get(0);
			
			for (ExecutionEvent event : this.nearestEvents) {
				if (event.getEdge().equals(nearestEdge)) {
					return ((CaseExecutionEvent) event).getDistance();
				}
			}
			
			assert false: "An error occurred in NodeDistanceListener...";
			return 10000.0;
		}
	}
	
	/**
	 * Return the value of the node distance.
	 * 
	 * @return a {@code double} value of normalized branch distance.
	 */
	public double getDistance() {
		return Utils.safeDouble(getApproachLevel() + Utils.normalize(getBranchDistance()));
	}
}
