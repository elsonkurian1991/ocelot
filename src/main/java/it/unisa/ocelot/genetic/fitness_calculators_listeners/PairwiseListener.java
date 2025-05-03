package it.unisa.ocelot.genetic.fitness_calculators_listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;

/**
 * A listener class used to calculate distances about edges
 * 
 * @author giograno
 *
 */
public class PairwiseListener implements SimulatorListener {

	private CFG cfg;

	/**
	 * Constructor of EdgeDistanceListener class
	 * 
	 * @param cfg
	 *            the control flow graph
	 * @param target
	 *            the edge target
	 */
	public PairwiseListener(CFG cfg) {
		this.cfg = cfg;
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, ExecutionEvent pEvent) {
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, ExecutionEvent pEvent,
			List<ExecutionEvent> pCases) {
	}

	@Override
	public void onNodeVisit(CFGNode node) {
	}

	/**
	 * Return the value of the fitness function, scored by branch distance plus
	 * approach level
	 * 
	 * @return the fitness value
	 */
	public double getFitness() {
		return 1.0;
	}


}
