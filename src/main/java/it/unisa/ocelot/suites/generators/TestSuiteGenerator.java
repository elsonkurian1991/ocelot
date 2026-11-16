package it.unisa.ocelot.suites.generators;

import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.benchmarks.BenchmarkCalculator;

/**
 * Interface that must be implemented by all test suite generators.
 */
public interface TestSuiteGenerator {
	void setupBudgetManager(int pNumberOfExperiments);
	void setTotalBudget(int pTotalBudget);
	void addBenchmark(BenchmarkCalculator<?> pBenchmarkCalculator);
	Set<TestCase> generateTestSuite() throws TestSuiteGenerationException;
	Set<TestCase> generateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException;
	abstract int getNumberOfEvaluations();
	boolean insufficientBudget();
}
