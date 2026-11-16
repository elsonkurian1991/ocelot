package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.MOSA;
import it.unisa.ocelot.genetic.many_objective.MOSABranchCoverageProblem;

/**
 * Settings for {@link MOSA} algorithm.
 */
//TODO look to class it.unisa.ocelot.genetic.many_objective.MOSABranchCoverageSettings, the one originally defined by the Ocelot guys
public final class MOSASettings extends StandardSettings {
	
	public MOSASettings(MOSABranchCoverageProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);		
	}
	
	@Override
	protected MOSA doConfigure() {
		// The parameters
		double maxCoverage;
		int maxGenerations;
		int maxTime;
		//plus thread (one thread)
		
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
		
		// Set to no dynamic mutation
		if (this.configManager.isDynamicMutation()) {
			System.err.println("Warning: A MOSA algorithm will always disable dynamic mutation: Your mutation.dynamic property value will be overridden");
			this.configManager.setDynamicMutation(false);
		}

		// Create the algorithm
		MOSA algorithm = new MOSA((MOSABranchCoverageProblem) this.problem_);

		// Add numeric parameters
		algorithm.setInputParameter("maxCoverage", maxCoverage);
		algorithm.setInputParameter("maxGenerations", maxGenerations);
		algorithm.setInputParameter("maxTime", maxTime);
		algorithm.setInputParameter("threads", 1); //TODO for the future...
		
		return algorithm;
    }
}
