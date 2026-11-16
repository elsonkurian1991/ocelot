package it.unisa.ocelot.genetic.paths;

import java.util.List;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;

/**
 * Problem: Cover a single CFG path.
 */
public final class ProblemTargetSinglePath extends StandardProblem {
	private static final long serialVersionUID = 1930014794768729268L;

	private final List<LabeledEdge> target;
	private final PathDistanceListener pathDistanceListener;
	private final SimulatorListener[] listeners;

	public ProblemTargetSinglePath(boolean pDebug, CFG pCfg, int pArraySize, Range<Double>[] pRanges, List<LabeledEdge> pTarget) {
		super(ProblemTargetSinglePath.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);
		this.target = pTarget;
		this.pathDistanceListener = new PathDistanceListener(this.cfg, this.target);
		this.listeners = new SimulatorListener[] { this.pathDistanceListener };
	}

	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		double objective = Utils.safeDouble(this.pathDistanceListener.getDistance());
		solution.setObjective(0, objective);
	}
	
	@Override
	public double getLastBranchDistance() {
		return this.pathDistanceListener.getBranchDistance();
	}
}
