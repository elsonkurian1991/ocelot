package it.unisa.ocelot.genetic.many_edges;

import java.util.List;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.settings.SettingsFactory;
import jmetal.core.Algorithm;

public class ManyEdgesCoverageExperiment extends OcelotExperiment {
	private CType[] parametersTypes;
	private CFG cfg;
	private ConfigManager config;
	private List<LabeledEdge> targetEdges;

	public ManyEdgesCoverageExperiment(CFG pCfg, ConfigManager pConfig, CType[] pTypes, List<LabeledEdge> targetEdges) {
		super(pConfig.getResultsFolder(), 1);
		
		this.cfg = pCfg;
		this.config = pConfig;
		this.parametersTypes = pTypes;
		this.targetEdges = targetEdges;
	}

	@Override
	public void algorithmSettings(Algorithm[] algorithm) {
		try {
			Range<Double>[] ranges = config.getTestRanges();

			ManyEdgesCoverageProblem problem;
			if (ranges != null)
				problem = new ManyEdgesCoverageProblem(this.cfg, this.parametersTypes, ranges, this.config.getTestArraysSize());
			else
				problem = new ManyEdgesCoverageProblem(this.cfg, this.parametersTypes, this.config.getTestArraysSize());

			problem.setDebug(config.getDebug());
			problem.setTarget(this.targetEdges);

			StandardSettings settings = SettingsFactory.getSettings(config.getAlgorithm(), problem, config);
			if (config.isMetaMutatorEnabled())
				settings.useMetaMutator();
			
			settings.setNumericConstants(this.cfg.getConstantNumbers());
			algorithm[0] = settings.configure();
		} catch (Exception e) {
			System.err.println("An error occurred while instantiating problem: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	}
}
