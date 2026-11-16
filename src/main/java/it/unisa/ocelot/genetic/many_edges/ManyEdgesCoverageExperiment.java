package it.unisa.ocelot.genetic.many_edges;

import java.util.List;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;
import jmetal.core.Algorithm;
import jmetal.util.JMException;

public final class ManyEdgesCoverageExperiment extends OcelotExperiment<LabeledEdge> {
	private final CFG cfg;
	private final List<LabeledEdge> targets;

	public ManyEdgesCoverageExperiment(CFG pCfg, ConfigManager pConfigManager, BudgetManager pBudgetManager, 
			List<LabeledEdge> pTargets) {
		super(pConfigManager, pBudgetManager);
		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_EDGE_COVERAGE_MULTI };
		
		this.cfg = pCfg;
		this.targets = pTargets;
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	public void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {
		// Makes the problem
		ProblemTargetManyEdges problem = new ProblemTargetManyEdges(this.configManager.getDebug(), this.cfg, this.configManager.getTestArraysSize(), this.targets, this.configManager.getTestRanges());

		// Makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric());
		this.algorithmSettings_[0] = settings;

		// Makes the algorithm
		algorithm[0] = settings.configure();
	}
}
