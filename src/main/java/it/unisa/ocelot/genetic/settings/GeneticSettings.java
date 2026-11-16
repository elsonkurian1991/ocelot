package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.GeneticAlgorithm;

/**
 * Settings for a {@link GeneticAlgorithm}.
 */
public final class GeneticSettings extends StandardSettings {
	public GeneticSettings(StandardProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);
	}

	@Override
	protected GeneticAlgorithm doConfigure() {
		GeneticAlgorithm algorithm = new GeneticAlgorithm((StandardProblem) this.problem_);
		return algorithm;
	}
}
