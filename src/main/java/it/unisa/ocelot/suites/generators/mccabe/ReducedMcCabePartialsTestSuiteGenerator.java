package it.unisa.ocelot.suites.generators.mccabe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.McCabeCalculator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.paths.PathSearchSimplex;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.many_edges.ManyEdgesCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Generates statically the smallest set of path that can achieve the full branch coverage and tries to cover each of
 * them. Configures the problem of covering a specific path as a multi-node coverage problem instead of a path coverage 
 * problem.
 * 
 * @author simone
 */
//TODO this class is embarassingly similar to McCabeTestSuiteGenerator and ReducedMcCabeTestSuiteGenerator; unify them!
//TODO is it really cascadeable???
public final class ReducedMcCabePartialsTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	private static final int MAX_IMPROVEMENTS = 3;

	public ReducedMcCabePartialsTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg);
	}

	@Override
	protected Set<TestCase> doGenerateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		HashSet<TestCase> suite = new HashSet<>(pSuite);
		coverMcCabePaths(suite);

		return suite;
	}

	private void coverMcCabePaths(Set<TestCase> suite) throws TestSuiteGenerationException {
		//TODO this method is especially critical, because the targets of genetic search (paths) are *not* the members of the coverage domain (branches)!
		
		// Makes the targets calculator
		McCabeCalculator mcCabeCalculator = new McCabeCalculator(cfg);
		
		// Gets the targets for the first iteration
		PathSearchSimplex pathSearch = new PathSearchSimplex(
				this.cfg, 
				mcCabeCalculator.getMcCabeVectorPaths(), 
				this.configManager.getReducedMcCabeCoverageTimes());
		pathSearch.solve(this.coverageCalculator.getUncovered());
		List<List<LabeledEdge>> targets = pathSearch.getChosenPaths();
		
		// Sets the total budget
		setupBudgetManager(targets.size() * MAX_IMPROVEMENTS);
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}
		
		boolean coverageImproved = true;
		while (coverageImproved && this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage()) {
			println("### Improvement iteration ###");
			double previousCoverage = this.coverageCalculator.getCoverage(); //TODO this is edge coverage! Is it ok?
			for (List<LabeledEdge> target : targets) {
				// Prints current target
				printSeparator();
				print("Current target: ");
				println(target);

				// Shortens the target path by removing the 
				// longest covered suffix 
				Set<LabeledEdge> uncoveredEdges = this.coverageCalculatorCFG.getUncoveredEdges();
				for (int i = target.size() - 1; i >= 0; i--) {
					if (uncoveredEdges.contains(target.get(i))) {
						break;
					}
					target.remove(i);
				}
				
				// Makes the experiment
				ManyEdgesCoverageExperiment exp = new ManyEdgesCoverageExperiment(this.cfg, this.configManager, this.budgetManager, target);
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

				// Gives feedback on coverage
				print("Current coverage: ");
				println(this.coverageCalculator.getCoverage());

				// Updates the total number of evaluations
				this.evaluations += exp.getStats().getEvaluations();
			}
			
			// Gets the targets for the next iteration (if performed)
			double currentCoverage = this.coverageCalculator.getCoverage();
			coverageImproved = previousCoverage < currentCoverage;
			if (coverageImproved) {
				pathSearch.solve(this.coverageCalculator.getUncovered());
				targets = pathSearch.getChosenPaths();
			}
		}
	}
}
