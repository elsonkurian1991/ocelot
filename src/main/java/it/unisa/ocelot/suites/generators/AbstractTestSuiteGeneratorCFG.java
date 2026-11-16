package it.unisa.ocelot.suites.generators;

import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorCFG;
import it.unisa.ocelot.util.Utils;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;

public abstract class AbstractTestSuiteGeneratorCFG<T> extends AbstractTestSuiteGenerator<T> {
	protected final CFG cfg;
	protected final CoverageCalculatorCFG<T> coverageCalculatorCFG;
	protected final CType[] parameterTypes;

	protected AbstractTestSuiteGeneratorCFG(ConfigManager pConfigManager, CoverageCalculatorCFG<T> pCoverageCalculatorCFG, CFG pCfg) {
		super(pConfigManager, pCoverageCalculatorCFG);
		this.cfg = pCfg;
		this.coverageCalculatorCFG = pCoverageCalculatorCFG;
		this.parameterTypes = this.cfg.getParameterTypes();		
		this.coverageCalculator.resetCoverage();
	}

	// Utility methods for test case generation
	
	protected final void addTestCaseSingleObjective(OcelotExperiment<LabeledEdge> exp, Solution solution, Set<TestCase> suite) {
		TestCase testCase = new TestCase(suite.size(), solution, this.cfg.getParameterTypes());
		println("Found test case, arguments: "); 
		println(Utils.printArguments(testCase.getArguments()));
		double previousCoverage = this.coverageCalculator.getCoverage();
		this.coverageCalculator.updateCoverage(testCase);
		double currentCoverage = this.coverageCalculator.getCoverage();
		if (previousCoverage < currentCoverage) {
			println("Target covered");
			suite.add(testCase);
			measureBenchmarks("Target", suite, exp.getStats().getEvaluations());
		} else {
			print("Target not covered. Objective value: ");
			print(testCase.getSolution().getObjective(0));
			println(". Test case discarded");
		}
	}
	
	protected final void addTestSuiteMultiObjective(OcelotExperiment<LabeledEdge> exp, SolutionSet solutions, Set<TestCase> suite) {
		List<Integer> evaluationsHistory = exp.getStats().getEvaluationsHistory();
		for (int i = 0; i < solutions.size(); i++) {
			TestCase testCase = new TestCase(suite.size(), solutions.get(i), this.cfg.getParameterTypes());
			suite.add(testCase);
			measureBenchmarks("Target", suite, evaluationsHistory.get(i));
		}
	}
	
	protected final void addSerendipitousTestCases(OcelotExperiment<LabeledEdge> exp, Set<TestCase> suite) {
		if (!this.configManager.isSerendipitous()) {
			return; //don't use serendipity
		}
		
		println("Adding serendipitous test cases...");
		
		Set<Solution> serendipitous = exp.getSerendipitousSolutions();
		for (Solution solution : serendipitous) {
			TestCase testCase = new TestCase(suite.size(), solution, this.parameterTypes);
			print("Serendipitous test case, arguments: "); 
			println(Utils.printArguments(testCase.getArguments()));
			double previousCoverage = this.coverageCalculator.getCoverage();
			this.coverageCalculator.updateCoverage(testCase);
			double currentCoverage = this.coverageCalculator.getCoverage();
			if (previousCoverage < currentCoverage) {
				suite.add(testCase);
				measureBenchmarks("Serendipitous", suite, exp.getStats().getEvaluations());
			} else {
				print("ERROR: this serendipitous test case does cover any serendipitous target! Objective value: ");
				print(testCase.getSolution().getObjective(0));
				println(". Test case discarded");
			}
		}
		
		println("Serendipitous test cases ended");
	}
}
