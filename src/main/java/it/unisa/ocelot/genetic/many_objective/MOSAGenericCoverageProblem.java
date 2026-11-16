package it.unisa.ocelot.genetic.many_objective;

import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.objectives.BranchDistancesReader;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.simulator.SimulatorListener;
import jmetal.core.Solution;

/**
 * Problem: Cover all (externally-specified, generic) objectives in a program, as a multi-objective problem.
 */
public final class MOSAGenericCoverageProblem extends StandardProblem {
	private static final long serialVersionUID = 1L;

	private final Set<? extends GenericObjective> objectives;

	public MOSAGenericCoverageProblem(boolean pDebug, CFG pCfg, int pArraySize,
			Range<Double>[] pRanges, Set<? extends GenericObjective> pObjectives) {
		super(MOSAGenericCoverageProblem.class.getName(), pObjectives.size(), pDebug, pCfg, pArraySize, pRanges);
		this.objectives = pObjectives;
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return new SimulatorListener[0]; //No listeners for us
	}
	
	@Override
	protected void completeEvaluation(Solution solution) {
		BranchDistancesReader.read();
		for (GenericObjective objective : this.objectives) {
			objective.evaluate(solution);
		}
	}
	
	@Override
	public double getLastBranchDistance() {
		return 0; //this method is not used by MOSA_Generic
	}

	//methods that can possibly be deleted(see also MOSA)

	//TODO do we really need to get the targets from HERE???????
	/**
	 * Returns the set of objectives representing the targets 
	 * of our coverage problem
	 * 
	 * @return a List of Labeled objectives
	 */
	public Set<? extends GenericObjective> getTargets() {
		return this.objectives;
	}
}
