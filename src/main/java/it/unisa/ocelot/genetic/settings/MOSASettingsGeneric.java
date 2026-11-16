package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;

import jmetal.util.JMException;

/**
 * Settings for {@link MOSA_Generic} algorithm.
 */
public final class MOSASettingsGeneric extends StandardSettings {
	public MOSASettingsGeneric(MOSAGenericCoverageProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);
	}
	
	@Override
	protected MOSA_Generic doConfigure() throws JMException {
		// The parameters
		double maxCoverage;
		int maxGenerations;
		int maxTime;
		boolean isRandomRun;
		double addRandom;
		boolean isDynamicObjectives;
		String optimizeFor;

		// Read all the parameters from this.configManager
		try {
			maxCoverage = this.configManager.getRequiredCoverage();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			maxCoverage = 1.0;
		}

		try {
			maxGenerations = this.configManager.getAlgorithmGenerations();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			maxGenerations = 0;
		}
		
		try {
			maxTime = this.configManager.getAlgorithmTime();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			maxTime = 0;
		}
		
		isRandomRun = this.configManager.isRandomRun();
		
		addRandom = isRandomRun ? 1.0 : 0.1;

		isDynamicObjectives = this.configManager.isDynamicObjectives();

		optimizeFor = this.configManager.getOptimizeFor();
		
		// Create the algorithm
		MOSA_Generic algorithm = new MOSA_Generic((MOSAGenericCoverageProblem) this.problem_);
		
		// Add simple parameters
		algorithm.setInputParameter("maxCoverage", maxCoverage);
		algorithm.setInputParameter("maxGenerations", maxGenerations);
		algorithm.setInputParameter("maxTime", maxTime);
		algorithm.setInputParameter("isRandomRun", isRandomRun);
		algorithm.setInputParameter("addRandom", addRandom);
		algorithm.setInputParameter("isDynamicObjectives", isDynamicObjectives);
		algorithm.setInputParameter("optimizeFor", optimizeFor);
		
		return algorithm;
    }
}
