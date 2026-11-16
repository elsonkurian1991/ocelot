package it.unisa.ocelot.suites.generators;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.simulator.CoverageCalculator;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.benchmarks.BenchmarkCalculator;
import it.unisa.ocelot.suites.budget.BudgetManager;
import it.unisa.ocelot.suites.budget.BudgetManagerHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//TODO factor out common stuff with TestSuiteMinimizer
public abstract class AbstractTestSuiteGenerator<T> implements TestSuiteGenerator {
	private static final int BUDGET_MINIMUM = 5;
	
	protected final ConfigManager configManager;
	protected final CoverageCalculator<T> coverageCalculator;
	protected final List<BenchmarkCalculator<?>> benchmarkCalculators;
	protected BudgetManager budgetManager;
	protected int evaluations;
	
	protected AbstractTestSuiteGenerator(ConfigManager pConfigManager, CoverageCalculator<T> pCoverageCalculator) {
		this.configManager = pConfigManager;
		this.coverageCalculator = pCoverageCalculator;
		this.benchmarkCalculators = new ArrayList<>();
		this.budgetManager = null;
		this.evaluations = 0;
	}

	@Override
	public final void setupBudgetManager(int pNumberOfExperiments) {
		this.budgetManager = BudgetManagerHandler.getInstance(this.configManager.getBudgetManager(), this.configManager.getEvaluationsMax(), pNumberOfExperiments);
	}
	
	@Override
	public final void setTotalBudget(int pTotalBudget) {
		this.budgetManager.setTotalBudget(pTotalBudget);
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
	public final Set<TestCase> generateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		startBenchmarks(pSuite);
		this.coverageCalculator.updateCoverage(pSuite);
		Set<TestCase> suite = doGenerateTestSuite(pSuite);
		this.coverageCalculator.updateCoverage(suite);
		endBenchmarks(suite);
		
		return suite;
	}
	
	protected abstract Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException;
	
	@Override
	public final int getNumberOfEvaluations() {
		return this.evaluations;
	}
	
	// (default impl overridable)
	@Override
	public boolean insufficientBudget() {
		return (this.budgetManager.getCurrentBudget() < BUDGET_MINIMUM);
	}

	// Utility methods for cascadeable generators

	public final int getConsumedBudget() {
		return this.budgetManager.getConsumedBudget();
	}
	
	//TODO push up
	public final boolean isSatisfied() {
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
	
	protected final void measureBenchmarks(String pLabel, Set<TestCase> pSuite, Integer pEvaluations) {
		for (BenchmarkCalculator<?> benchmarkCalculator : this.benchmarkCalculators) {
			benchmarkCalculator.measure(pLabel, pSuite, pEvaluations);
		}
	}
	
	// Utility methods for printing
	//TODO remove them!
	
	protected final void printSeparator() {
		if (this.configManager.getPrintResults()) {
			System.out.println("-------------------------------------------------------------------------------");
		}
	}

	protected final void print(Object pObject) {
		if (this.configManager.getPrintResults()) {
			System.out.print(pObject);
		}
	}
	
	protected final void println(Object pObject) {
		if (this.configManager.getPrintResults()) {
			System.out.println(pObject);
		}
	}
	
	protected final void printStat(OcelotExperiment<?> experiment, String pStat) {
		if (experiment.getStats() == null) {
			System.err.println("No stats found! Please check the generator...");
		} else if (this.configManager.getPrintResults()) {
			println(experiment.getStats().getStat(pStat));
		}
	}
}
