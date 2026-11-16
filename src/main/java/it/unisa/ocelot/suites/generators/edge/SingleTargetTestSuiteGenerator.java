package it.unisa.ocelot.suites.generators.edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.EdgeComparator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.edges.PlainEdgeCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Test suite generators which selects, in turn, each single branch and tries to cover it.
 * 
 * @author simone
 */
public final class SingleTargetTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public SingleTargetTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg);
		this.evaluations = 0;
	}
	
	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);		
		coverTargets(suite);
		
		return suite;
	}
	
	private void coverTargets(Set<TestCase> suite) throws TestSuiteGenerationException {		
		List<LabeledEdge> targets = new ArrayList<>(this.cfg.edgeSet()); //TODO these are all edges, not branches! Is it ok?
		Collections.sort(targets, new EdgeComparator(this.cfg));
		
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
			PlainEdgeCoverageExperiment exp = new PlainEdgeCoverageExperiment(this.configManager, this.budgetManager, this.cfg, target);			
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
			
			// Updates the total number of evaluations
			this.evaluations += exp.getStats().getEvaluations();
			
			// Possibly adds a new test case to the suite
			addTestCaseSingleObjective(exp, solution, suite);
			
			// Gives feedback on current coverage
			print("Current coverage: ");
			println(this.coverageCalculator.getCoverage()); //TODO this is edge coverage! Is it ok?
			
			// Gets next target
			target = (targets.size() == 0 ? null : targets.remove(0));
		}
	}
}
