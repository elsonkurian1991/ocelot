package it.unisa.ocelot.suites.generators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.simulator.CoverageCalculator;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.budget.BudgetManagerHandler;

/**
 * Test suite generator which combines multiple cascading generators.
 * The generators must implement {@link CascadeableTestSuiteGenerator}.
 * 
 * @author simone
 */
public final class TestSuiteGeneratorCascade<T> extends AbstractTestSuiteGenerator<T> {
	private ArrayList<CascadeableTestSuiteGenerator> generators;
	
	public TestSuiteGeneratorCascade(ConfigManager pConfigManager, CoverageCalculator<T> pCoverageCalculator) {
		super(pConfigManager, pCoverageCalculator);
		this.generators = new ArrayList<>();
		
		if (!BudgetManagerHandler.BASIC_MANAGER.equals(this.configManager.getBudgetManager())) {
			//overrides setting
			System.err.println("Warning: A cascading test suite generator always needs a BasicManager budget manager: Your suite.generator.algorithm property value will be overridden");
			this.configManager.setBudgetManager(BudgetManagerHandler.BASIC_MANAGER);
		}
	}
	
	public final void addTestSuiteGenerator(CascadeableTestSuiteGenerator pGenerator) {
		this.generators.add(pGenerator);
	}
	
	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		// Sets the initial (total) budget
		int generatorsToReplenish = (int) this.generators.stream().filter(TestSuiteGenerator::insufficientBudget).count(); //TODO is this value ok???
		setupBudgetManager(generatorsToReplenish); 
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return pSuite;
		}
		
		Set<TestCase> suite = new HashSet<>(pSuite);		
		for (CascadeableTestSuiteGenerator generator : this.generators) {
			// Sets the initial budget of the current generator, 
			// accordingly to its needs
			if (generator.insufficientBudget()) {
				generator.setTotalBudget(this.budgetManager.getExperimentBudget());
			}
			
			// Generates the next test suite, and calculates the
			// coverage before and after generation
			double previousCoverage = this.coverageCalculator.getCoverage();
			suite = generator.generateTestSuite(suite);
			double currentCoverage = this.coverageCalculator.getCoverage();
			
			// Update the budget manager (this generator's)
			this.budgetManager.reportConsumedBudget(generator.getConsumedBudget());
			this.budgetManager.reportConclusion();
			
			// Update the benchmarks (this generator's)
			if (previousCoverage < currentCoverage) {
				println("Test suite improved");
				measureBenchmarks(generator.getClass().getSimpleName(), suite, generator.getNumberOfEvaluations());
			}
			
			// Updates the total number of evaluations
			this.evaluations += generator.getNumberOfEvaluations();

			// Stops if the total coverage is sufficient
			if (this.coverageCalculator.getCoverage() >= this.configManager.getRequiredCoverage()) {
				return suite;
			}
		}
		
		return suite;
	}
}
