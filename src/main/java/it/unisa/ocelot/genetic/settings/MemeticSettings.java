package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.MemeticAlgorithm;

/**
 * Settings for a {@link MemeticAlgorithm}.
 */
public final class MemeticSettings extends StandardSettings {
	public MemeticSettings(StandardProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);
	}

	@Override
	protected MemeticAlgorithm doConfigure() {
		MemeticAlgorithm algorithm = new MemeticAlgorithm((StandardProblem) this.problem_);
		//TODO use parallel evaluator? How?
		return algorithm;
	}
}
