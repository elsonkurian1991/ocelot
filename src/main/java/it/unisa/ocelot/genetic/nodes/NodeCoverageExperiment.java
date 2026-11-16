package it.unisa.ocelot.genetic.nodes;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.Algorithm;
import jmetal.util.JMException;

public final class NodeCoverageExperiment extends OcelotExperiment<CFGNode> {
	private final CFG cfg;
	private final CFGNode target;

	public NodeCoverageExperiment(CFG pCfg, ConfigManager pConfigManager, BudgetManager pBudgetManager, CFGNode pTarget) {
		super(pConfigManager, pBudgetManager);

		this.algorithmNameList_ = new String[] { pConfigManager.getAlgorithm() }; 

		this.cfg = pCfg;
		this.target = pTarget;
		
		//TODO is it necessary to set the algorithmNameList_ field? Why change it from PGGA (set in the superclass)? What about other jmetal parameters defined in superclass constructor?
	}
	
	@Override
	public Class<CFGNode> getTargetClass() {
		return CFGNode.class;
	}

	@Override
	public void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {		
		//gets input ranges
		Range<Double>[] ranges = this.configManager.getTestRanges();

		//makes the problem
		ProblemTargetSingleNode problem = new ProblemTargetSingleNode(this.configManager.getDebug(), this.cfg, this.configManager.getTestArraysSize(), ranges, this.target);

		//makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric()); //TODO what about non-numeric constants?
		this.algorithmSettings_[0] = settings; //TODO who should set this.algorithmSettings_ ?

		//configures the algorithm
		algorithm[0] = settings.configure();
	}
}
