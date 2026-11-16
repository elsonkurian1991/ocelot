package it.unisa.ocelot.genetic.edges;

import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.SerendipitousProblem;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.runnable.Run;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

import jmetal.core.Solution;

import org.apache.commons.lang3.Range;

/**
 * Problem: Cover a single CFG edge.
 */
public final class ProblemTargetSingleEdge extends StandardProblem implements SerendipitousProblem<LabeledEdge> {
	private static final long serialVersionUID = 1930014794768729268L;

	private final LabeledEdge target;
	private final Set<CFGNode> dominators;
	private final Set<LabeledEdge> serendipitousPotentials;
	private final EdgeDistanceListener edgeDistanceListener;
	private final SimulatorListener[] listeners;
	
	private Set<LabeledEdge> serendipitousCovered;

	public ProblemTargetSingleEdge(boolean pDebug, CFG pCfg, Range<Double>[] pRanges, int pArraySize, LabeledEdge pTarget) {
		super(ProblemTargetSingleEdge.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);
		
		this.target = pTarget;
		DominanceCalculatorVertices<CFGNode, LabeledEdge> dominators = new DominanceCalculatorVertices<>(this.cfg, this.cfg.getEntry());
		CFGNode source = this.cfg.getEdgeSource(pTarget);
		this.dominators = dominators.getStrictDominators(source);
		this.serendipitousPotentials = new HashSet<>();
		this.edgeDistanceListener = new EdgeDistanceListener(this.cfg, this.target, this.dominators, this.serendipitousPotentials);
		this.listeners = new SimulatorListener[] { this.edgeDistanceListener };
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	public void setSerendipitousPotentials(Set<LabeledEdge> pSerendipitousPotentials) {
		this.serendipitousPotentials.clear();
		this.serendipitousPotentials.addAll(pSerendipitousPotentials);		
		this.serendipitousPotentials.remove(this.target);
	}
	
	@Override
	public Set<LabeledEdge> getSerendipitousCovered() {
		return this.serendipitousCovered;
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	public void completeEvaluation(Solution solution) {
		// Update serendipity
		this.serendipitousCovered = this.edgeDistanceListener.getSerendipitousCovered();
		this.serendipitousPotentials.removeAll(this.serendipitousCovered);
		
		// Evaluate solution
		double edgeDistance = this.edgeDistanceListener.getDistance();
		double fitnessPC;
		if (Run.isExpWithEvalFun) {
			// Here we need to read the fitness values to objectives
			// from the files that we wrote before.
			Object[][][] arguments = getArguments(solution);
			fitnessPC = CalculateFitnessFromEvalPC4.calculateFitness(arguments);
		} else {
			fitnessPC = 0d;
		}
		double objective = Utils.safeDouble(fitnessPC + edgeDistance); 
		solution.setObjective(0, objective);
	}
	
	@Override
	public double getLastBranchDistance() {
		return this.edgeDistanceListener.getBranchDistance();
	}
}
