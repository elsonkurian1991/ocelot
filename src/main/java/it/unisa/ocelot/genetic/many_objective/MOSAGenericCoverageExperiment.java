package it.unisa.ocelot.genetic.many_objective;

import java.util.List;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.settings.MOSASettingsGeneric;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import jmetal.core.Algorithm;
import jmetal.core.SolutionSet;

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

			MOSAGenericCoverageProblem problem = null;
			if (ranges != null)
				problem = new MOSAGenericCoverageProblem(this.cfg, this.parametersTypes, config.getTestArraysSize(), 
						ranges, objectives);
			else
				throw new RuntimeException("Error: please, set the ranges for the parameters for MOSA algorithm");
			
			if (config.getAlgorithm().equals(SettingsFactory.AVM)) {
				System.err.println("Warning: MOSA will run with its own algorithm (AVM ignored)!");
			}

			StandardSettings settings = new MOSASettingsGeneric(problem, config, objectives);
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

		SolutionSet solutionSet = this.algorithm.execute();
		
		this.budgetManager.reportConsumedBudget(this, this.algorithm.getStats().getEvaluations());
		return solutionSet;
	}

	public List<Integer> getNumberOfEvaluations() {
		return ((MOSA_Generic) this.algorithm).getEvaluations();
	}
}
