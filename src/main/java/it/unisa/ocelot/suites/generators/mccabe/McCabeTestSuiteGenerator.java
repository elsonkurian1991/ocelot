package it.unisa.ocelot.suites.generators.mccabe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.McCabeCalculator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.paths.PathCoverageExperiment;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.util.JMException;

/**
 * Selects each linear independent path and tries to cover them in order to get full branch coverage.
 * 
 * @author simone
 */
//TODO this class is embarassingly similar to ReducedMcCabeTestSuiteGenerator and ReducedMcCabePartialsTestSuiteGenerator; unify them!
//TODO is it really cascadeable???
public final class McCabeTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public McCabeTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
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
		McCabeCalculator mcCabeCalculator = new McCabeCalculator(this.cfg);
		
		// Gets the targets
		List<List<LabeledEdge>> targets = mcCabeCalculator.getMcCabePaths();
		//TODO remove the targets that are not necessary because suite already covers the branches they aim to cover
		
		// Resets the budget manager with information on the number 
		// of targets
		setupBudgetManager(targets.size());
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}

		List<LabeledEdge> target = (targets.size() == 0 ? null : targets.remove(0));
		while (target != null && this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage()) { //TODO this is edge coverage! Is it ok?		
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

			// Gets next target
			target = (targets.size() == 0 ? null : targets.remove(0));
		}
	}
}
