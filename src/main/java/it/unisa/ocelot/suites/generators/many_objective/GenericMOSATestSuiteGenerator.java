package it.unisa.ocelot.suites.generators.many_objective;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.VariableTranslator;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageExperiment;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.budget.BasicBudgetManager;
import it.unisa.ocelot.suites.generators.CascadeableGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

/**
 * Many-Objective test suite generator. Approach as described in Panichella et
 * al. (Reformulating Branch Coverage as a Many-Objective Optimization Problem).
 * 
 * @author simone
 *
 */
public class GenericMOSATestSuiteGenerator extends TestSuiteGenerator implements CascadeableGenerator {
	private boolean satisfied;

	private int evaluations;

	private List<GenericObjective> objectives;

	public GenericMOSATestSuiteGenerator(ConfigManager config, CFG cfg, List<GenericObjective> objectives) {
		super(cfg, objectives);
		this.config = config;
		this.objectives = objectives;
	}

	@Override
	public Set<TestCase> generateTestSuite(Set<TestCase> pSuite) throws TestSuiteGenerationException {

		Set<TestCase> suite = new HashSet<TestCase>(pSuite);
		this.startBenchmarks();

		coverMultiObjective(suite, objectives);

		double coverage = calculateCoverage();
		System.out.println("Coverage of MOSA test suite = " + coverage);

		/*
		calculator.calculateCoverage(suite);
		System.out.println("Coverage of MOSA test suite = " + calculator.getObjectiveCoverage());

		if (calculator.getObjectiveCoverage() >= this.config.getRequiredCoverage()) {
			this.satisfied = true;
		}
		*/
		
		return suite;
	}

	private double calculateCoverage() {
		double covered = 0;
		int total = 0;
		for (GenericObjective objective : objectives) {
			if (objective.isCovered())
				covered++;
			else {
			}
			total++;
		}

		double coverage = covered / total;
		System.out.println("Covered objectives: " + covered);
		System.out.println("Total objectives: " + total);
		return coverage;
	}

	public boolean isSatisfied() {
		return satisfied;
	}

	protected void coverMultiObjective(Set<TestCase> suite, List<GenericObjective> pTargets)
			throws TestSuiteGenerationException {

		SolutionSet archiveSolutions = new SolutionSet();

		MOSAGenericCoverageExperiment mosaExperiment = new MOSAGenericCoverageExperiment(cfg, pTargets, config,
				cfg.getParameterTypes());

		this.setupBudgetManager(1);
		try {
			this.budgetManager = this.budgetManager.changeTo(BasicBudgetManager.class);
		} catch (InstantiationException e) {
			throw new TestSuiteGenerationException(e.getMessage());
		}

		mosaExperiment.initExperiment(this.budgetManager);
		try {
			archiveSolutions = mosaExperiment.multiObjectiveRun();
			System.out.println(mosaExperiment.getAlgorithmStats().getLog());
			this.evaluations = mosaExperiment.getNumberOfEvaluation();
		} catch (JMException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new TestSuiteGenerationException(e.getMessage());
		}

		Solution currentSolution;
		List<Integer> numberOfEvaluations = mosaExperiment.getNumberOfEvaluations();
		for (int i = 0; i < archiveSolutions.size(); i++) {
			currentSolution = archiveSolutions.get(i);
			if (currentSolution.getFitness() == 0) {
				VariableTranslator translator = new VariableTranslator(currentSolution);

				Object[][][] numericParams = translator.translateArray(cfg.getParameterTypes());

				// System.out.println("Creating test case: " + Arrays.toString(numericParams));
				TestCase testCase = this.createTestCase(numericParams, suite.size());
				suite.add(testCase);

				this.measureBenchmarks("MOSA Target", suite, numberOfEvaluations.get(i));
			}
		}
		System.out.println("Testsuite size: " + suite.size());
	}

	@Override
	public int getNumberOfEvaluations() {
		return this.evaluations;
	}
}