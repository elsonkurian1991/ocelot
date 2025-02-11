package it.unisa.ocelot.suites.generators.mccabe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.DynamicMcCabeCalculator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.VariableTranslator;
import it.unisa.ocelot.genetic.edges.CalculateFitnessFromEvalPC2;
import it.unisa.ocelot.genetic.edges.CalculateFitnessFromEvalPC3;
import it.unisa.ocelot.genetic.edges.DMCExperiment;
import it.unisa.ocelot.genetic.edges.FitType;
import it.unisa.ocelot.genetic.edges.ReadEFLfilesforPairCombination;
import it.unisa.ocelot.runnable.Run;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.generators.CascadeableGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.util.Utils;
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
public class DynamicMcCabeTestSuiteGenerator extends TestSuiteGenerator implements CascadeableGenerator {
	private boolean satisfied;

	private int evaluations;

	public DynamicMcCabeTestSuiteGenerator(ConfigManager pConfigManager, CFG pCFG) {
		super(pCFG);
		this.config = pConfigManager;
	}

	@Override
	public Set<TestCase> generateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {
		Set<TestCase> suite = new HashSet<TestCase>(pSuite);

		this.startBenchmarks();

		coverMcCabePaths(suite);

		calculator.calculateCoverage(suite);
		if (calculator.getBranchCoverage() >= this.config.getRequiredCoverage()) {
			this.satisfied = true;
		}

		return suite;
	}

	public boolean isSatisfied() {
		return satisfied;
	}

	private void coverMcCabePaths(Set<TestCase> suite) throws TestSuiteGenerationException {
		DynamicMcCabeCalculator mcCabeCalculator = new DynamicMcCabeCalculator(cfg);

		LabeledEdge currentTarget = cfg.getStart().navigate(cfg).goFlow().edge();

		System.out.println("Number of experiments: " + config.getMaxEvaluations() / (config.getPopulationSize() * 10));
		this.setupBudgetManager(config.getMaxEvaluations() / (config.getPopulationSize() * 10));
		// this.setupBudgetManager(mcCabeCalculator.extimateMissingTargets());

		SolutionSet seedPopulation = null;

		// while (currentTarget != null && calculator.getBranchCoverage() <
		// config.getRequiredCoverage()) {

		// here we read a file, list of all epc file names. or combination of list

		boolean finish = false;
		int i = 0;
		String tempString = "";
		if (Run.isExpWithEvalFun) {
			while (currentTarget != null && !finish)// !finish calculator.getBranchCoverage() <
													// config.getRequiredCoverage()
			{
				DMCExperiment exp = new DMCExperiment(cfg, config, cfg.getParameterTypes(), currentTarget,
						seedPopulation, this.config.getDMCSeedSize());

				exp.initExperiment(this.budgetManager);

				exp.setSerendipitousPotentials(new HashSet<>(this.getUncoveredEdges(suite)));

				CFGNode departingNode = cfg.getEdgeSource(currentTarget);
				this.printSeparator();
				this.println("Current target: branch " + currentTarget.toString() + " of node " + departingNode);
				try {
					this.print("Running... ");
					exp.basicRun(); // population and Ftype should come
					this.evaluations += exp.getNumberOfEvaluation();

					if (this.config.isDMCSeed())
						seedPopulation = exp.getLastPopulation();
					this.println("Done!");
				} catch (JMException | ClassNotFoundException e) {
					e.printStackTrace();
					throw new TestSuiteGenerationException(e.getMessage());
				}

				this.addSerendipitousTestCases(exp, suite);

				double fitnessValue = exp.getFitnessValue();
				VariableTranslator translator = new VariableTranslator(exp.getSolution());
				tempString += i + ":" + translator + "\n";
				if (config.getSerendipitousCoverage())
					for (Solution solution : exp.getSerendipitousSolutions()) {
						VariableTranslator currentTranslator = new VariableTranslator(solution);
						Object[][][] serendipitousParameters = currentTranslator
								.translateArray(cfg.getParameterTypes());
						calculator.calculateCoverage(serendipitousParameters);
						mcCabeCalculator.addPath(calculator.getCoveredPath());
					}

				Object[][][] numericParams = translator.translateArray(cfg.getParameterTypes());

				TestCase testCase = this.createTestCase(numericParams, suite.size());

				this.println("Fitness function: " + fitnessValue + ". ");
				if (fitnessValue == 0.0) {
					this.println("Target covered!");
					calculator.calculateCoverage(numericParams);
					mcCabeCalculator.addPath(calculator.getCoveredPath());
					suite.add(testCase);
					this.measureBenchmarks("McCabe target", suite, exp.getNumberOfEvaluation());

					for (Entry<String, FitType> entry : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals
							.entrySet()) {
						// String key = entry.getKey();
						// boolean isCovered= entry.getValue().isTestCovered() &&
						// entry.getValue().isFirst();
						String params = CalculateFitnessFromEvalPC3.GetArguInString(numericParams);
						boolean foundTCParams = false;
						if (entry.getValue().getArgumentList().contains(params)) {
							foundTCParams = true;
						}
						if (!entry.getValue().isTestGenerated() && foundTCParams) {
							entry.getValue().setTestCovered(false);
							entry.getValue().setTestGenerated(true);
							entry.getValue().setFirst(false);
						}
					}

					/*
					 * for(Entry<String, FType> fitnessGenVal :
					 * CalculateFitnessFromEvalPC.filesWithFitnessVals.entrySet()){ String key =
					 * fitnessGenVal.getKey(); boolean isCovered=
					 * fitnessGenVal.getValue().isTestCovered() &&
					 * fitnessGenVal.getValue().isFirst();
					 * if(!fitnessGenVal.getValue().isTestGenerated()) {
					 * CalculateFitnessFromEvalPC.filesWithFitnessVals.put(key, new
					 * FType(Double.MAX_VALUE, false, isCovered,false)); }
					 * 
					 * 
					 * }
					 */

				} else {
					this.println("Target not covered...");
					this.println("Useless test case. Discarded.");
				}

				this.println("Parameters found: " + Utils.printParameters(numericParams));

				/*
				 * currentTarget = mcCabeCalculator.getNextTarget(); if (currentTarget != null
				 * && !getUncoveredEdges(suite).contains(currentTarget)) {
				 * System.out.println(this.cfg.getEdgeSource(currentTarget));
				 * System.out.println("ERRORE"); }
				 */
				calculator.calculateCoverage(suite);
				// this.println("Partial coverage: " + calculator.getBranchCoverage());

				finish = true;
				for (FitType fitnessCovered : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.values()) {
					finish = finish && fitnessCovered.isTestGenerated();
				}
				/*
				 * for(FType fitnessCovered :
				 * CalculateFitnessFromEvalPC.filesWithFitnessVals.values()){ finish=finish &&
				 * fitnessCovered.isTestGenerated(); }
				 */
				// int maxEvaluations = this.getMaxEvaluations; ((Integer)
				// getInputParameter("maxEvaluations")).intValue();
				if (finish) {
					// hope we reach all targets from the start!
					currentTarget = mcCabeCalculator.getNextTarget();
					if (currentTarget != null && !getUncoveredEdges(suite).contains(currentTarget)) {
						System.out.println(this.cfg.getEdgeSource(currentTarget));
						System.out.println("ERRORE");
					}
					System.out.println("reached finish for next target");
				}
				i++;

				System.out.println(
						"Number of experiments: " + (((config.getMaxEvaluations() - budgetManager.getConsumedBudget())
								/ (config.getPopulationSize() * 10)) + 1));
				this.budgetManager.updateTargets(((config.getMaxEvaluations() - budgetManager.getConsumedBudget())
						/ (config.getPopulationSize() * 10)) + 1);

				int no = budgetManager.getConsumedBudget();
				int nos = budgetManager.getExperimentBudget(exp);
				System.out.println("ALLOCATED:" + nos + " --------- CONSUMED:" + no);
				// this.setupBudgetManager(--nosExperiments);//200
				// System.out.println(nosExperiments+"of nosExperiments
				// CONSUMED");//---------OUT OF:"+nos);//nosExperiments=nosExperiments-10;
				System.out.println("Remaining budget: " + budgetManager.getBudget());
				if (budgetManager.getBudget() <= 0) {
					System.out.println("Budget finshed.");
					finish = true;// to stop the execution if not able to find the fitness.
				}
			}
		} else {
			while (currentTarget != null && calculator.getBranchCoverage() < config.getRequiredCoverage()) {
				DMCExperiment exp = new DMCExperiment(cfg, config, cfg.getParameterTypes(), currentTarget,
						seedPopulation, this.config.getDMCSeedSize());

				exp.initExperiment(this.budgetManager);

				exp.setSerendipitousPotentials(new HashSet<>(this.getUncoveredEdges(suite)));

				CFGNode departingNode = cfg.getEdgeSource(currentTarget);
				this.printSeparator();
				this.println("Current target: branch " + currentTarget.toString() + " of node " + departingNode);
				try {
					this.print("Running... ");
					exp.basicRun();
					this.evaluations += exp.getNumberOfEvaluation();

					if (this.config.isDMCSeed())
						seedPopulation = exp.getLastPopulation();
					this.println("Done!");
				} catch (JMException | ClassNotFoundException e) {
					e.printStackTrace();
					throw new TestSuiteGenerationException(e.getMessage());
				}

				this.addSerendipitousTestCases(exp, suite);

				double fitnessValue = exp.getFitnessValue();
				VariableTranslator translator = new VariableTranslator(exp.getSolution());

				if (config.getSerendipitousCoverage())
					for (Solution solution : exp.getSerendipitousSolutions()) {
						VariableTranslator currentTranslator = new VariableTranslator(solution);
						Object[][][] serendipitousParameters = currentTranslator
								.translateArray(cfg.getParameterTypes());
						calculator.calculateCoverage(serendipitousParameters);
						mcCabeCalculator.addPath(calculator.getCoveredPath());
					}

				Object[][][] numericParams = translator.translateArray(cfg.getParameterTypes());

				TestCase testCase = this.createTestCase(numericParams, suite.size());

				this.println("Fitness function: " + fitnessValue + ". ");
				if (fitnessValue == 0.0) {
					this.println("Target covered!");
					calculator.calculateCoverage(numericParams);
					mcCabeCalculator.addPath(calculator.getCoveredPath());
					suite.add(testCase);
					this.measureBenchmarks("McCabe target", suite, exp.getNumberOfEvaluation());
				} else {
					this.println("Target not covered...");
					this.println("Useless test case. Discarded.");
				}

				this.println("Parameters found: " + Utils.printParameters(numericParams));

				currentTarget = mcCabeCalculator.getNextTarget();
				if (currentTarget != null && !getUncoveredEdges(suite).contains(currentTarget)) {
					System.out.println(this.cfg.getEdgeSource(currentTarget));
					System.out.println("ERRORE");
				}

				calculator.calculateCoverage(suite);
				this.println("Partial coverage: " + calculator.getBranchCoverage());
				this.budgetManager.updateTargets(mcCabeCalculator.extimateMissingTargets());
			}
		}

		// System.out.println(tempString);
	}

	@Override
	public int getNumberOfEvaluations() {
		return this.evaluations;
	}
}
