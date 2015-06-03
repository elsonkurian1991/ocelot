package it.unisa.ocelot.genetic.paths;

import java.util.ArrayList;

import javax.management.JMException;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.experiments.Experiment;

public class PathCoverageExperiment extends OcelotExperiment {
	private Class<Object>[] parametersTypes;
	private CFG cfg;
	private ConfigManager config;
	private ArrayList<LabeledEdge> targetPath;

	public PathCoverageExperiment(CFG pCfg, ConfigManager pConfig,
			Class<Object>[] pTypes, ArrayList<LabeledEdge> targetPath) {
		super(pConfig.getResultsFolder(), pConfig.getExperimentRuns());
		
		this.cfg = pCfg;
		this.config = pConfig;
		this.parametersTypes = pTypes;
		this.targetPath = targetPath;
	}

	@Override
	public void algorithmSettings(Algorithm[] algorithm) {
		try {
			Range<Double>[] ranges = config.getTestRanges();

			PathCoverageProblem problem;
			if (ranges != null)
				problem = new PathCoverageProblem(this.cfg, this.parametersTypes, ranges);
			else
				problem = new PathCoverageProblem(this.cfg, this.parametersTypes);

			problem.setDebug(config.getDebug());
			problem.setTarget(this.targetPath);

			PathCoverageSettings settings = new PathCoverageSettings(problem, config);
			settings.setNumericConstants(this.cfg.getConstantNumbers());
			algorithm[0] = settings.configure();
		} catch (Exception e) {
			System.err.println("An error occurred while instantiating problem: " + e.getMessage());
			return;
		}
	}
}