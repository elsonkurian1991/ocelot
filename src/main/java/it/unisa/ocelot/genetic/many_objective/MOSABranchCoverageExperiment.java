package it.unisa.ocelot.genetic.many_objective;

import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.Algorithm;
import jmetal.util.JMException;

/**
 * A Branch Coverage experiment performed with MOSA algorithm proposed by
 * Panichella et al. in "Reformulating Branch Coverage as a Many-Objective 
 * Optimization Problem" ICST 2015
 * 
 * @author giograno
 */
public final class MOSABranchCoverageExperiment extends OcelotExperiment<LabeledEdge> {
	private final CFG cfg;
	private final Set<LabeledEdge> targets; // all the branches to cover (except FlowEdges)

	public MOSABranchCoverageExperiment(CFG pCfg, ConfigManager pConfigManager, BudgetManager pBudgetManager, Set<LabeledEdge> pTargets) {
		super(pConfigManager, pBudgetManager);

		if (!pConfigManager.getAlgorithm().equals(SettingsFactory.ALGORITHM_MOSA)) {
			//overrides setting
			System.err.println("Warning: A MOSA branch coverage experiment will always run with the MOSA algorithm: Your suite.generator.algorithm property value will be overridden");
			pConfigManager.setAlgorithm(SettingsFactory.ALGORITHM_MOSA);
		}

		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.experimentName_ = "MOSA branch coverage experiment";
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_NODE_COVERAGE_MULTI };
		
		this.cfg = pCfg;
		this.targets = pTargets;
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	protected void algorithmSettingsOcelot(Algorithm[] algorithms) throws JMException {
		// Makes the problem
		MOSABranchCoverageProblem problem = new MOSABranchCoverageProblem(this.configManager.getDebug(), this.cfg, this.configManager.getTestArraysSize(), this.configManager.getTestRanges(), this.targets);

		// Makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric());
		this.algorithmSettings_[0] = settings; //TODO who should set this.algorithmSettings_ ?

		// Makes the algorithm
		algorithms[0] = settings.configure();
	}
}
