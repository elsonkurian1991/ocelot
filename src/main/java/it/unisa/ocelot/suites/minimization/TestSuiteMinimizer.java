package it.unisa.ocelot.suites.minimization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.simulator.CoverageCalculator;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.benchmarks.BenchmarkCalculator;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;

//TODO factor out common stuff with AbstractTestSuiteGenerator
public abstract class TestSuiteMinimizer<T> implements CascadeableTestSuiteGenerator {
	protected final ConfigManager configManager;
	protected final CoverageCalculator<T> coverageCalculator;
	protected final List<BenchmarkCalculator<?>> benchmarkCalculators;
	
	protected TestSuiteMinimizer(ConfigManager pConfigManager, CoverageCalculator<T> pCoverageCalculator) {
		this.configManager = pConfigManager;
		this.coverageCalculator = pCoverageCalculator;
		this.benchmarkCalculators = new ArrayList<>();
	}
	
	@Override
	public final void setupBudgetManager(int pNumberOfExperiments) {
		//nothing to do, a minimizer does not
		//use budget
	}
	
	@Override
	public final void setTotalBudget(int pFixedBudget) {
		//nothing to do, a minimizer does not
		//use budget
	}
	
	@Override
	public final void addBenchmark(BenchmarkCalculator<?> pBenchmarkCalculator) {
		this.benchmarkCalculators.add(pBenchmarkCalculator);
	}
	
	@Override
	public final Set<TestCase> generateTestSuite() throws TestSuiteGenerationException {
		return generateTestSuite(new HashSet<>());
	}
	
	@Override
	public Set<TestCase> generateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		startBenchmarks(pSuite);
		Set<TestCase> suite = minimize(pSuite);
		endBenchmarks(suite);
		
		return suite;
	}
		
	public abstract Set<TestCase> minimize(Set<TestCase> pTestCases);
	
	@Override
	public final int getNumberOfEvaluations() {
		return 0; //a minimizer does not do any evaluation
	}

	@Override
	public boolean insufficientBudget() {
		//Always returns false, because a minimizer does not
		//use budget.
		return false;
	}
	
	@Override
	public int getConsumedBudget() {
		return 0; //a minimizer does not use budget
	}
	
	//TODO push up
	@Override
	public boolean isSatisfied() {
		return this.coverageCalculator.getCoverage() >= this.configManager.getRequiredCoverage();
	}
	
	// Utility methods for benchmarks
	//TODO push up
	
	private void startBenchmarks(Set<TestCase> pSuite) {
		for (BenchmarkCalculator<?> benchmarkCalculator : this.benchmarkCalculators) {
			benchmarkCalculator.start(pSuite);
		}
	}
	
	private void endBenchmarks(Set<TestCase> pSuite) {
		for (BenchmarkCalculator<?> benchmarkCalculator : this.benchmarkCalculators) {
			benchmarkCalculator.end(pSuite, getNumberOfEvaluations());
		}
	}
	
	protected final void measureBenchmarks(String pLabel, Set<TestCase> pSuite, Integer evaluations) {
		for (BenchmarkCalculator<?> benchmarkCalculator : this.benchmarkCalculators) {
			benchmarkCalculator.measure(pLabel, pSuite, evaluations);
		}
	}
}
