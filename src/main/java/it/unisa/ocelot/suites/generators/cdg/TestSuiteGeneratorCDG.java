package it.unisa.ocelot.suites.generators.cdg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.EdgeComparator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.edges.CDGBasedExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Approach described in Harman et al. (Optimizing for the Number of Tests Generated in Search Based Test Data
 * Generation with an Application to the Oracle Cost Problem).
 *  
 * @author simone
 */
public final class TestSuiteGeneratorCDG extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public TestSuiteGeneratorCDG(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg);
	}

	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverEdges(suite);

		return suite;
	}

	private void coverEdges(Set<TestCase> suite) throws TestSuiteGenerationException {
		// Gets the targets
		ArrayList<LabeledEdge> targets = new ArrayList<>(this.coverageCalculator.getUncovered()); //TODO these are edges!!!! It is WRONG!!!! It must be branches.
		Collections.sort(targets, new EdgeComparator(this.cfg));

		// Sets the total budget
		setupBudgetManager(targets.size());
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}

		LabeledEdge target = (targets.size() == 0 ? null : targets.remove(0));
		while (target != null && this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage()) { //TODO this is edge coverage! Is it ok?
			// Prints current target
			printSeparator();
			print("Current target: ");
			print(target);
			print(" (departing from node ");
			CFGNode departingNode = this.cfg.getEdgeSource(target);
			print(departingNode);
			println(")");
			
			// Makes the experiment
			CDGBasedExperiment exp = new CDGBasedExperiment(this.cfg, this.configManager, this.budgetManager, target, targets);
			exp.initExperiment();

			// Runs the experiment
			Solution solution = null;
			try {
				print("Running...");
				solution = exp.basicRun();
				println("Done!");
			} catch (JMException | ClassNotFoundException e) {
				throw new TestSuiteGenerationException(e.getMessage());
			}
			
			// Possibly adds a new test case to the suite
			addTestCaseSingleObjective(exp, solution, suite);
			
			// Updates current coverage and gives feedback 
			this.coverageCalculator.updateCoverage(suite);
			print("Current coverage: ");
			println(this.coverageCalculator.getCoverage()); //TODO this is edge coverage! Bring it to branch coverage!!!!

			// Updates the total number of evaluations
			this.evaluations += exp.getStats().getEvaluations();

			// Gets next target
			target = (targets.size() == 0 ? null : targets.remove(0));
		}
	}
}
