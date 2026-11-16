package it.unisa.ocelot.genetic.edges;

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

public final class CDGBasedExperiment extends OcelotExperiment<LabeledEdge> {
	private final CFG cfg;
	private final LabeledEdge target;
	private final List<LabeledEdge> targets;

	public CDGBasedExperiment(CFG pCfg, ConfigManager pConfigManager, BudgetManager pBudgetManager,
			LabeledEdge pTarget, List<LabeledEdge> pTargets) {
		super(pConfigManager, pBudgetManager);
		
		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.experimentName_ = "CDG-Based experiment";
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_CDG };

		this.cfg = pCfg;
		this.target = pTarget;
		this.targets = pTargets;
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	protected void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {
		// Makes the problem
		//TODO see this.problemList_ : isn't it done automatically in some way?
		ProblemTargetSingleEdgeCDG problem = new ProblemTargetSingleEdgeCDG(this.configManager.getTestRanges(), this.configManager.getTestArraysSize(), this.configManager.getDebug(), this.cfg, this.target, this.targets);

		// Makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric()); //TODO what about non-numeric constants? 
		this.algorithmSettings_[0] = settings; //TODO who should set this.algorithmSettings_ ?

		// Makes the algorithm
		algorithm[0] = settings.configure();
	}
}
