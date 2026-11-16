package it.unisa.ocelot.suites.generators.edge;

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
import it.unisa.ocelot.genetic.edges.PlainEdgeCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorEdges;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Classic memory based approach. For each test case generated, all the covered edges are removed from
 * the set of targets. This approach can be run so that also serendipitous solution are added to the 
 * test suite.
 * 
 * @author simone
 */
public final class MemoryEdgeTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public MemoryEdgeTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorEdges(pCfg), pCfg); //TODO is edges the right coverage measure???
	}
	
	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverEdges(suite);
				
		return suite;
	}
	
	private void coverEdges(Set<TestCase> suite) throws TestSuiteGenerationException {
		// Gets the targets
		ArrayList<LabeledEdge> targets = new ArrayList<>(this.coverageCalculator.getUncovered());
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
			print(")");

			// Makes the experiment
			PlainEdgeCoverageExperiment exp = new PlainEdgeCoverageExperiment(this.configManager,
					this.budgetManager, this.cfg, target);
			exp.initExperiment();
			
			// Inits serendipity
			if (this.configManager.isSerendipitous()) {
				exp.setSerendipitousPotentials(this.coverageCalculator.getUncovered());
			}
			
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

			// Possibly adds new serendipitous test cases to the suite
			addSerendipitousTestCases(exp, suite);

			// Updates current coverage and gives feedback 
			this.coverageCalculator.updateCoverage(suite);
			print("Current coverage: ");
			println(this.coverageCalculator.getCoverage()); //TODO this is edge coverage! Is it ok?

			// Updates the total number of evaluations
			this.evaluations += exp.getStats().getEvaluations();

			// Gets next target
			target = (targets.size() == 0 ? null : targets.remove(0));
		}
	}
}
