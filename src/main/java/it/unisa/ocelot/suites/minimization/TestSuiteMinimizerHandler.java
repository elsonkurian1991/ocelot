package it.unisa.ocelot.suites.minimization;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.simulator.CoverageCalculator;

public class TestSuiteMinimizerHandler {
	public static final String ADDITIONAL_GREEDY_MINIMIZER = "AdditionalGreedy";
		
	public static TestSuiteMinimizer<?> getInstance(ConfigManager pConfigManager, CoverageCalculator<?> pCoverageCalculator) {
		String name = pConfigManager.getTestSuiteMinimizer();
		if (name.equalsIgnoreCase(ADDITIONAL_GREEDY_MINIMIZER)) {
			return new AdditionalGreedyMinimizer(pConfigManager, pCoverageCalculator);
		}
		
		return null;
	}
}
