package it.unisa.ocelot.suites.generators.mccabe;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.LIPSCalculator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.edges.CalculateFitnessFromEvalPC4;
import it.unisa.ocelot.genetic.edges.DynamicMcCabeExperiment;
import it.unisa.ocelot.genetic.edges.FitType;
import it.unisa.ocelot.genetic.edges.ReadEFLFilesForPairCombination;
import it.unisa.ocelot.genetic.edges.ReadEFLFilesForPairCombinationV2;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import it.unisa.ocelot.runnable.Run;
import it.unisa.ocelot.simulator.CoverageCalculatorBranches;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.suites.generators.AbstractTestSuiteGeneratorCFG;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * LIPS calculator. Select the first test case randomly. In a second phase it
 * selects as a target the alternative branch of the last condition in the stack
 * of covered conditions. TODO: update the name in LIPSCalculator
 * 
 * @author simone
 *
 */
//TODO this DOES NOT WORK!!!!! Needs to be fixed, and compared against McCabeTestSuiteGenerator
//TODO notice that the budget *per iteration* is bounded, but the *total* budget is unlimited.
public final class DynamicMcCabeTestSuiteGenerator extends AbstractTestSuiteGeneratorCFG<LabeledEdge> implements CascadeableTestSuiteGenerator {
	public DynamicMcCabeTestSuiteGenerator(ConfigManager pConfigManager, CFG pCfg) {
		super(pConfigManager, new CoverageCalculatorBranches(pCfg), pCfg); //TODO is it *really* branch coverage our metrics? Well, it is the same metrics as for all the McCabe generators.
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
		LIPSCalculator mcCabeCalculator = new LIPSCalculator(this.cfg, this.coverageCalculator.getCovered());
		
		// Sets the initial budget
		setupBudgetManager(1);
		if (insufficientBudget()) {
			println("Insufficient budget, please adjust configuration");
			return;
		}

		// Gets the initial seed
		SolutionSet seedPopulation = null; //TODO should we set it in some way?
		
		LabeledEdge target = this.cfg.getEntry().navigate(this.cfg).goFlow().edge();
		boolean shallContinue = Run.isExpWithEvalFun ? true : this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage();
		while (target != null && shallContinue) {
			// Prints current target
			printSeparator();
			print("Current target: ");
			print(target); 
			print(" (departing from node ");
			CFGNode departingNode = this.cfg.getEdgeSource(target);
			print(departingNode);
			print(")");

			// Makes the experiment
			DynamicMcCabeExperiment exp = new DynamicMcCabeExperiment(this.configManager, this.budgetManager, seedPopulation, this.configManager.getDMCSeedSize(), this.cfg, target);
			exp.initExperiment();

			// Inits serendipity
			if (this.configManager.isSerendipitous()) {
				exp.setSerendipitousPotentials(mcCabeCalculator.getCurrentlyMissingTargets());
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

			// Adds new serendipitous test cases to the suite
			addSerendipitousTestCases(exp, suite);

			// Gives feedback on coverage
			print("Current coverage: ");
			println(this.coverageCalculator.getCoverage());

			// Updates the total number of evaluations
			this.evaluations += exp.getStats().getEvaluations();

			// Updates seed
			if (this.configManager.isDMCSeed()) {
				seedPopulation = exp.getLastPopulation();
			}

			// Replenishes the budget manager for the next iteration
			setupBudgetManager(1);

			// Gets next target
			target = mcCabeCalculator.getNextTarget();

			// Calculates whether it shall continue or finish looping
			if (Run.isExpWithEvalFun) {
				String argumentsString = CalculateFitnessFromEvalPC4.argumentsToString(Utils.getArguments(solution, this.cfg.getParameterTypes()));
				
				//TODO what is the purpose of the next block of code??? Seems useless...
				if (solution.getObjective(0) == 0.0) {
					for (Entry<String, FitType> entry : ReadEFLFilesForPairCombination.filesPCPairComFitnessVals.entrySet()) {
						boolean foundArguments = entry.getValue().getArgumentList().contains(argumentsString);
						if (!entry.getValue().isTestGenerated() && foundArguments) {
							entry.getValue().setTestCovered(false);
							entry.getValue().setTestGenerated(true);
							entry.getValue().setFirst(false);
						}
					}
				}
				
				shallContinue = false;
				for (List<TestObjStateMachine> smList : ReadEFLFilesForPairCombinationV2.listOfSMs) { //TODO why this is V2 and the previous one has no version indication?
					for (TestObjStateMachine sm : smList) {
						boolean foundArguments = sm.getArgumentList().contains(argumentsString);
						if (foundArguments) {
							sm.setGenerated();
						}
						shallContinue = shallContinue || !sm.isGenerated();
					}
				}
			} else {
				shallContinue = this.coverageCalculator.getCoverage() < this.configManager.getRequiredCoverage();
			}
		}
		
		// Does a final check
		if (Run.isExpWithEvalFun && shallContinue) {
			// at the end of the while loop we hope to have reached 
			// all targets (edges) from the start!
			target = mcCabeCalculator.getNextTarget();
			if (target != null && this.coverageCalculator.getUncovered().contains(target)) {
				print("ERROR! McCabe path calculator emitted a target edge after the end that is not yet covered; edge: ");
				println(target.toString());
			}
		}
	}
}
