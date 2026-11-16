package it.unisa.ocelot.genetic.edges;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.suites.budget.BudgetManager;

public final class PlainEdgeCoverageExperiment extends EdgeCoverageExperiment {
	public PlainEdgeCoverageExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager, CFG pCfg,
			LabeledEdge pTarget) {
		super(pConfigManager, pBudgetManager, pCfg, pTarget);

		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.experimentName_ = "Plain edge coverage experiment";
	}
}
