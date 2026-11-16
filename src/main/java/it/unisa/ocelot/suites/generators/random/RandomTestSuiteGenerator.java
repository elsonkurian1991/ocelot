package it.unisa.ocelot.suites.generators.random;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.types.CTypeDouble;
import it.unisa.ocelot.c.types.CTypeInteger;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.simulator.CoverageCalculatorEdges;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import jmetal.core.Solution;
import jmetal.core.Variable;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

/**
 * Randomly generates test cases, keeping only the ones that improve coverage. The maximum number of generations
 * is set in the config file.
 * @author simone
 *
 */
public final class RandomTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	private final static int SECONDS_TO_MILLIS = 1000;
	
	private Random random;
	private Range<Double>[] ranges;
	
	public RandomTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorEdges(pCfg), pCfg);
		this.random = new Random();
		this.ranges = this.configManager.getTestRanges();
	}
	
	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		generateRandomSuite(suite);
		
		return suite;
	}
	
	private void generateRandomSuite(Set<TestCase> suite) throws TestSuiteGenerationException {
		long time = 0;
		long timeout = System.currentTimeMillis() + this.configManager.getRandomTimeLimit() * SECONDS_TO_MILLIS;
		if (this.configManager.getRandomTimeLimit() < 0) {
			timeout = Long.MAX_VALUE;
		}
		
		int sizeout = this.configManager.getRandomSizeLimit();
		if (sizeout < 0) {
			sizeout = Integer.MAX_VALUE;
		}
		
		double lastCoverage = 0.0;
		while (this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage() &&
				suite.size() <= sizeout &&
				time < timeout) {
			coverRandom(suite);
			
			// Updates current coverage 
			this.coverageCalculator.updateCoverage(suite);
			if (this.coverageCalculator.getCoverage() > lastCoverage) {
				measureBenchmarks("Random", suite, 0);
				print("Current coverage: ");
				print(this.coverageCalculator.getCoverage());
				print(", current suite size: ");
				println(suite.size());
			}
			
			time = System.currentTimeMillis();
			println("Remaining time: " + (timeout - time));
		}
	}
	
	private void coverRandom(Set<TestCase> suite) throws TestSuiteGenerationException {
		for (int i = 0; i < this.configManager.getRandomGranularity(); i++) {
			Solution solution = random(this.cfg.getParameterTypes());
			TestCase testCase = new TestCase(suite.size(), solution, this.cfg.getParameterTypes());
			suite.add(testCase);
		}
	}
	
	//TODO Make this method work. Now it doesn't, due to the major change of parameters
	private Solution random(CType[] pParamTypes) {
		Solution solution = new Solution(pParamTypes.length);
		
		Object[][][] parameters = new Object[pParamTypes.length][][];
		parameters[0] = new Object[1][];
		parameters[0][0] = new Object[pParamTypes.length];
		
		for (int i = 0; i < pParamTypes.length; i++) {
			double currentMinimum = this.ranges[i].getMinimum();
			double currentRange = this.ranges[i].getMaximum() - currentMinimum;
			if (pParamTypes[i] instanceof CTypeDouble) {
				double param = this.random.nextDouble() * currentRange;
				param += currentMinimum;
				parameters[0][0][i] = param;
			} else if (pParamTypes[i] instanceof CTypeInteger) {
				int param = this.random.nextInt((int) currentRange);
				param += currentMinimum;
				parameters[0][0][i] = param;
			} else {
				double param = this.random.nextDouble() * currentRange;
				param += currentMinimum;
				parameters[0][0][i] = param;
			}
		}
		
		return parameters; 
	}
}
