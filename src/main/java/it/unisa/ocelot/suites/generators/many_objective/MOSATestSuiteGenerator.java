package it.unisa.ocelot.suites.generators.many_objective;

import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.many_objective.MOSABranchCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.budget.BudgetManagerHandler;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * Many-Objective test suite generator. Approach as described in Panichella et al. (Reformulating Branch Coverage as a 
 * Many-Objective Optimization Problem).
 * @author simone
 *
 */
//TODO decide whether to make AbstractTestSuiteGeneratorCFG a class that also supports multi-objective generation, and make this class one of its subclasses; or just factor common stuff with GenericMOSATestSuiteGenerator
//TODO is it *really* cascadeable?
//TODO make it final.
public class MOSATestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public MOSATestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		//TODO put back here the code of the protected constructor
		this(pConfigManager, pCfg, pCfg.edgeSet());
	}
	
	//TODO remove!
	protected MOSATestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg, Set<LabeledEdge> pTargets) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg);
		
		if (!BudgetManagerHandler.BASIC_MANAGER.equals(this.configManager.getBudgetManager())) {
			//overrides setting
			System.err.println("Warning: A MOSA test suite generator will always run with a BasicManager budget manager: Your suite.generator.algorithm property value will be overridden");
			this.configManager.setBudgetManager(BudgetManagerHandler.BASIC_MANAGER);
		}
	}

	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverMultiObjectiveBranches(suite);

		return suite;
	}
	
	private void coverMultiObjectiveBranches(Set<TestCase> suite) throws TestSuiteGenerationException {
		// Sets the total budget
		setupBudgetManager(1);
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}
		
		// Makes the experiment
		MOSABranchCoverageExperiment exp = new MOSABranchCoverageExperiment(
				this.cfg, this.configManager, this.budgetManager, this.coverageCalculator.getUncovered());
		exp.initExperiment();
		
		SolutionSet solutions = null;
		try {
			solutions = exp.multiObjectiveRun();
		} catch (JMException | ClassNotFoundException e) {
			throw new TestSuiteGenerationException(e.getMessage());
		}

		addTestSuiteMultiObjective(exp, solutions, suite);
	}
}