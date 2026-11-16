package it.unisa.ocelot.genetic.many_objective;

import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;
import jmetal.core.Algorithm;
import jmetal.util.JMException;

/**
 * A Branch Coverage experiment performed with MOSA algorithm proposed by
 * Panichella et al. in
 * "Reformulating Branch Coverage as a Many-Objective Optimization Problem" ICST
 * 2015
 * 
 * @author giograno
 */
public final class MOSAGenericCoverageExperiment extends OcelotExperiment<GenericObjective> {
	private final CFG cfg;
	private final Set<? extends GenericObjective> objectives;

	public MOSAGenericCoverageExperiment(CFG pCfg, Set<? extends GenericObjective> pObjectives, ConfigManager pConfigManager, BudgetManager pBudgetManager) {
		super(pConfigManager, pBudgetManager);
		
		if (!pConfigManager.getAlgorithm().equals(SettingsFactory.ALGORITHM_MOSA_GENERIC)) {
			//overrides config manager
			System.err.println("Warning: A generic MOSA coverage experiment will always run with the generic MOSA algorithm: Your algorithm settings will be ignored");
			pConfigManager.setAlgorithm(SettingsFactory.ALGORITHM_MOSA_GENERIC); //TODO make it better
		}

		this.experimentName_ = "MOSA generic coverage experiment";
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_GENERIC_COVERAGE_MOSA };
		
		this.cfg = pCfg;
		this.objectives = pObjectives;
	}
	
	@Override
	public Class<GenericObjective> getTargetClass() {
		return GenericObjective.class;
	}

	@Override
	protected void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {
		// Makes the problem
		MOSAGenericCoverageProblem problem = new MOSAGenericCoverageProblem(this.configManager.getDebug(), this.cfg, this.configManager.getTestArraysSize(), this.configManager.getTestRanges(), this.objectives);

		// Makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric()); //TODO what about non-numeric constants?
		this.algorithmSettings_[0] = settings; //TODO who should set this.algorithmSettings_ ?

		// Makes the algorithm
		algorithm[0] = settings.configure();
	}

	//TODO do we really care?
	public List<Integer> getNumberOfEvaluations() {
		return ((MOSA_Generic) this.algorithm).getEvaluations();
	}
}
