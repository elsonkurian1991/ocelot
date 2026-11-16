package it.unisa.ocelot.genetic.edges;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.SolutionSet;

public final class DynamicMcCabeExperiment extends EdgeCoverageExperiment {	
	public DynamicMcCabeExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager,
				SolutionSet pSeed, int pKeepOfSeed,
				CFG pCfg, LabeledEdge pTarget) {
		super(pConfigManager, pBudgetManager, pSeed, pKeepOfSeed, pCfg, pTarget);
		
		this.experimentName_ = "Dynamic McCabe experiment";
	}
}
