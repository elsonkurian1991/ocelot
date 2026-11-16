package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.algorithms.CDG;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.StandardSettings;

import jmetal.util.parallel.MultithreadedEvaluator;

public final class CGDSettings extends StandardSettings {
	public CGDSettings(StandardProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);		
	}
	
	@Override
	protected CDG doConfigure() {
		int threads;
		try {
			threads = this.configManager.getThreads();
		} catch (NumberFormatException e) {
			//fall back on default
			threads = 1;
		}
		
    	MultithreadedEvaluator evaluator = new MultithreadedEvaluator(threads);
		CDG algorithm = new CDG((StandardProblem) this.problem_, evaluator);
		return algorithm;
    }
}
