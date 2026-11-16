package it.unisa.ocelot.genetic.paths;

import java.util.List;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.Algorithm;
import jmetal.util.JMException;

public final class PathCoverageExperiment extends OcelotExperiment<LabeledEdge> {
	private final CFG cfg;
	private final List<LabeledEdge> target;

	public PathCoverageExperiment(CFG pCfg, ConfigManager pConfigManager, BudgetManager pBudgetManager, List<LabeledEdge> pTarget) {
		super(pConfigManager, pBudgetManager);

		//TODO Use these parameters instead of refusing everything and putting dummy values in them.		
		this.algorithmNameList_ = new String[] { pConfigManager.getAlgorithm() }; 
		this.problemList_ = new String[] { SettingsFactory.PROBLEM_PATH_COVERAGE_SINGLE };
		
		this.cfg = pCfg;
		this.target = pTarget;
	}
	
	@Override
	public Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	public void algorithmSettingsOcelot(Algorithm[] algorithm) throws JMException {
		//gets input ranges
		Range<Double>[] ranges = this.configManager.getTestRanges();

		//makes the problem
		ProblemTargetSinglePath problem = new ProblemTargetSinglePath(this.configManager.getDebug(), this.cfg, this.configManager.getTestArraysSize(), ranges, this.target);

		//makes the settings
		StandardSettings settings = SettingsFactory.getSettings(this.configManager, problem, this.cfg.getConstantsNumeric()); //TODO what about non-numeric constants?
		this.algorithmSettings_[0] = settings; //TODO who should set this.algorithmSettings_ ?

		//configures the algorithm
		algorithm[0] = settings.configure();
	}
}
