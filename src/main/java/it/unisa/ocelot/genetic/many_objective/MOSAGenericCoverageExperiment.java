package it.unisa.ocelot.genetic.many_objective;

import java.util.List;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.settings.MOSASettingsGeneric;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * A Branch Coverage experiment performed with MOSA algorithm proposed by
 * Panichella et al. in
 * "Reformulating Branch Coverage as a Many-Objective Optimization Problem" ICST
 * 2015
 * 
 * @author giograno
 *
 */
public class MOSAGenericCoverageExperiment extends OcelotExperiment {
	private CType[] parametersTypes;
	private CFG cfg;
	private ConfigManager config;
	private List<GenericObjective> objectives;
	private SolutionSet seedPopulation;
	private SolutionSet finalPopulation;
	private MOSAGenericCoverageProblem problem;

	public MOSAGenericCoverageExperiment(CFG cfg, List<GenericObjective> objectives, ConfigManager configManager, CType[] types) {
		super(configManager.getResultsFolder(), configManager.getExperimentRuns());

		this.config = configManager;
		this.parametersTypes = types;
		this.objectives = objectives;
		this.cfg = cfg;

		this.algorithmNameList_ = new String[] { "MOSA" };
	}

	@Override
	public void algorithmSettings(Algorithm[] algorithm) {
		try {
			Range<Double>[] ranges = config.getTestRanges();

			if (ranges != null) {
				problem = new MOSAGenericCoverageProblem(this.cfg, this.parametersTypes, config.getTestArraysSize(), 
						ranges, objectives);
				if (seedPopulation != null) {
					mangleSeedPopulation();
				}
			} else
				throw new RuntimeException("Error: please, set the ranges for the parameters for MOSA algorithm");
			
			if (config.getAlgorithm().equals(SettingsFactory.AVM)) {
				System.err.println("Warning: MOSA will run with its own algorithm (AVM ignored)!");
			}

			MOSASettingsGeneric settings = new MOSASettingsGeneric(problem, config);
			if (config.isMetaMutatorEnabled())
				settings.useMetaMutator();
			settings.setNumericConstants(this.cfg.getConstantNumbers());
			problem.setDebug(config.getDebug());
			algorithm[0] = settings.configure();
		} catch (Exception e) {
			System.err.println("An error occurred while instantiating problem: " + e.getMessage());
			return;
		}
	}

	public SolutionSet multiObjectiveRun() throws ClassNotFoundException, jmetal.util.JMException {
		this.algorithmSettings(this.problemList_[0], 0, new Algorithm[1]);
		if (this.algorithm instanceof MOSA_Generic) {
			((MOSA_Generic) this.algorithm).setSeedPopulation(seedPopulation);
		}

		SolutionSet solutionSet = this.algorithm.execute();
		this.finalPopulation = solutionSet;
		
		this.budgetManager.reportConsumedBudget(this, this.algorithm.getStats().getEvaluations());
		return solutionSet;
	}

	public List<Integer> getNumberOfEvaluations() {
		return ((MOSA_Generic) this.algorithm).getEvaluations();
	}

	/**
	 * Sets the seed population to use when MOSA initialises its population.
	 * Must be called BEFORE multiObjectiveRun() for seeding to take effect.
	 *
	 * @param seedPopulation solutions from the previous iteration's final
	 *                       population, limited to populationSize / 2
	 */
	public void setSeedPopulation(SolutionSet seedPopulation) {
		this.seedPopulation = seedPopulation;
		if (problem != null) {
			mangleSeedPopulation();
		}
	}

	/**
	 * Returns the final population from the last completed MOSA run.
	 * Called after multiObjectiveRun() returns, to store the population
	 * for the next iteration's seeding.
	 *
	 * @return final SolutionSet from MOSA_Generic, or null if not yet run
	 */
	public SolutionSet getFinalPopulation() {
		return this.finalPopulation;
	}
	
	/**
	 * Mangles the seed population to ensure that it is compatible with the current problem instance.
	 */
	private void mangleSeedPopulation() {
		SolutionSet mangledPopulation = new SolutionSet(seedPopulation.size());
		for (int i = 0; i < seedPopulation.size(); i++) {
			GenericSolution seedOld = (GenericSolution) seedPopulation.get(i);
			GenericSolution seedNew = new GenericSolution(problem, seedOld.getDecisionVariables());
			mangledPopulation.add(seedNew);
		}
		this.seedPopulation = mangledPopulation;
	}
}
