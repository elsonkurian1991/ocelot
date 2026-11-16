package it.unisa.ocelot.suites.generators.many_objective;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageExperiment;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.simulator.GenericCoverageCalculator;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.budget.BudgetManagerHandler;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGenerator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * Many-Objective test suite generator. Approach as described in Panichella et
 * al. (Reformulating Branch Coverage as a Many-Objective Optimization Problem).
 * 
 * @author simone
 *
 */
//TODO decide whether to make AbstractTestSuiteGeneratorCFG a class that also supports multi-objective generation, and make this class one of its subclasses; or just factor common stuff with MOSATestSuiteGenerator
//TODO is it *really* cascadeable?
public final class GenericMOSATestSuiteGenerator extends AbstractTestSuiteGenerator<GenericObjective> implements CascadeableTestSuiteGenerator {
	private final CFG cfg;
	
	public GenericMOSATestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg, Set<? extends GenericObjective> pObjectives) {
		super(pConfigManager, new GenericCoverageCalculator(pObjectives));
		this.cfg = pCfg;
		
		if (!BudgetManagerHandler.BASIC_MANAGER.equals(this.configManager.getBudgetManager())) {
			//overrides setting
			System.err.println("Warning: A generic MOSA test suite generator will always run with a BasicManager budget manager: Your suite.generator.algorithm property value will be overridden");
			this.configManager.setBudgetManager(BudgetManagerHandler.BASIC_MANAGER);
		}
	}

	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverMultiObjective(suite);
		
		return suite;
	}

	private void coverMultiObjective(Set<TestCase> suite) throws TestSuiteGenerationException {
		// Sets the total budget
		setupBudgetManager(1);
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}

		// Makes the experiment
		MOSAGenericCoverageExperiment exp = new MOSAGenericCoverageExperiment(this.cfg, this.coverageCalculator.getUncovered(), 
				this.configManager, this.budgetManager);
		exp.initExperiment();
		
		// Runs the experiments
		SolutionSet solutions = new SolutionSet();
		try {
			print("Running... ");
			solutions = exp.multiObjectiveRun();
			println("Done!");
		} catch (JMException | ClassNotFoundException e) {
			throw new TestSuiteGenerationException(e.getMessage());
		}

		// Adds new test cases to the suite
		List<Integer> evaluationsHistory = exp.getNumberOfEvaluations();
		for (int i = 0; i < solutions.size(); i++) {
			measureBenchmarks("Target", suite, evaluationsHistory.get(i));
			Solution currentSolution = solutions.get(i);
			TestCase testCase = new TestCase(suite.size(), currentSolution, this.cfg.getParameterTypes());
			suite.add(testCase);
		}

		// Gets the total number of evaluations
		this.evaluations = exp.getStats().getEvaluations();
	}
}