package it.unisa.ocelot.suites.generators.paths;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.TooManyFailedPathsException;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.paths.DynamicPathFinder;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.paths.PathCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Do not use. NP-Hard problem, not quite easy to solve.
 * 
 * @author simone
 */
//TODO notice that the budget *per iteration* is bounded, but the *total* budget is unlimited.
@Deprecated
public final class DynamicPathsTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> {
	@Deprecated
	public DynamicPathsTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg); //TODO our choice of branch coverage is quite arbitrary. Look at old Ocelot code and align the objective of genetic search.
	}
	
	@Override
	@Deprecated
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {		
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverAllPaths(suite);

		return suite;
	}
	
	private void coverAllPaths(Set<TestCase> suite) throws TestSuiteGenerationException {
		// Makes the targets calculator
		DynamicPathFinder pathFinder = new DynamicPathFinder(this.cfg);
		
		// Sets the initial budget
		setupBudgetManager(1);
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}

		List<LabeledEdge> target = pathFinder.getMaxCoveragePath();
		while (target != null && this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage()) {
			// Prints current target
			printSeparator();
			print("Current target: ");
			println(target);
			
			// Makes the experiment
			PathCoverageExperiment exp = new PathCoverageExperiment(this.cfg, this.configManager, this.budgetManager, target);
			exp.initExperiment();

			// Runs the experiment
			Solution solution = null;
			try {
				print("Running... ");
				solution = exp.basicRun();
				println("Done!");
			} catch (JMException | ClassNotFoundException e) {
				throw new TestSuiteGenerationException(e.getMessage());
			}
	
			// Possibly adds a new test case to the suite
			addTestCaseSingleObjective(exp, solution, suite);
			
			// Gives feedback on coverage
			print("Current coverage: ");
			println(this.coverageCalculator.getCoverage());
			
			// Further updates on coverage
			if (solution.getObjective(0) == 0.0) {
				pathFinder.markPathAsCovered(target);
			} else {
				try {
					pathFinder.notifyFailedPath();
				} catch (TooManyFailedPathsException e) {
					break;
				}
			}

			// Updates the total number of evaluations
			this.evaluations += exp.getStats().getEvaluations();
			
			// Replenishes the budget manager for the next iteration
			setupBudgetManager(1);
			
			// Gets next target
			target = pathFinder.getMaxCoveragePath();
		}
	}
}
