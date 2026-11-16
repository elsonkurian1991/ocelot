package it.unisa.ocelot.genetic.edges;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.Algorithm;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * An (abstract) edge coverage experiment. This experiment 
 * aims to cover a single {@link LabeledEdge}.
 */
public abstract class EdgeCoverageExperiment extends OcelotExperiment<LabeledEdge> {
	protected final CFG cfg;
	
	private final LabeledEdge target;
	
	protected EdgeCoverageExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager, SolutionSet pSeed, int pKeepOfSeed, CFG pCfg, LabeledEdge pTarget) {
		super(pConfigManager, pBudgetManager, pSeed, pKeepOfSeed);
		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_EDGE_COVERAGE_SINGLE };
		
		this.cfg = pCfg;
		this.target = pTarget;
	}
	
	protected EdgeCoverageExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager, CFG pCfg, LabeledEdge pTarget) {
		super(pConfigManager, pBudgetManager);
		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_EDGE_COVERAGE_SINGLE };
		
		this.cfg = pCfg;
		this.target = pTarget;
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}
	
	@Override
	protected final void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {		
		// Makes the problem
		ProblemTargetSingleEdge problem = new ProblemTargetSingleEdge(this.configManager.getDebug(), this.cfg, this.configManager.getTestRanges(), this.configManager.getTestArraysSize(), this.target);
		//TODO see this.problemList_ : isn't it done automatically in some way?

		// Makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric());
		this.algorithmSettings_[0] = settings;

		// Makes the algorithm
		algorithm[0] = settings.configure();
	}
}
