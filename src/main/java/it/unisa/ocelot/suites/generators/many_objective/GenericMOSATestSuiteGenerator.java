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

public class GenericMOSATestSuiteGenerator extends TestSuiteGenerator implements CascadeableGenerator {

	private boolean satisfied;
	private int evaluations;
	private List<GenericObjective> objectives;

	// -------------------------------------------------------------------------
	// New fields — start here
	// -------------------------------------------------------------------------

	/**
	 * Seed population from the previous iteration, provided by PopulationStore.
	 * If set before generateTestSuite() is called, it is passed into MOSA so
	 * the search continues from existing solutions rather than starting randomly.
	 * Null means no seeding — MOSA initialises fully randomly (original behaviour).
	 */
	private SolutionSet seedPopulation;

	/**
	 * Reference to the MOSA_Generic algorithm instance from the last run.
	 * Retained so GenAndWrite can call getFinalPopulation() after the run
	 * to store the population for the next iteration's seeding.
	 */
	private SolutionSet lastFinalPopulation;

	// -------------------------------------------------------------------------
	// New fields — end here
	// -------------------------------------------------------------------------

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
		return suite;
	}

	private double calculateCoverage() {
		double covered = 0;
		int total = 0;
		for (GenericObjective objective : objectives) {
			if (objective.isCovered())
				covered++;
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

		MOSAGenericCoverageExperiment mosaExperiment = new MOSAGenericCoverageExperiment(
				cfg, pTargets, config, cfg.getParameterTypes());

		this.setupBudgetManager(1);
		try {
			this.budgetManager = this.budgetManager.changeTo(BasicBudgetManager.class);
		} catch (InstantiationException e) {
			throw new TestSuiteGenerationException(e.getMessage());
		}

		// -------------------------------------------------------------------------
		// Pass seed population into the experiment before initialising — start here
		// -------------------------------------------------------------------------
		// If a seed population is available from the previous iteration, pass it
		// to the experiment so MOSA_Generic can start from known-good solutions
		// instead of a fully random population. This preserves search progress
		// across iterations and avoids re-discovering the same easy regions.
		if (this.seedPopulation != null && this.seedPopulation.size() > 0) {
			mosaExperiment.setSeedPopulation(this.seedPopulation);
			System.out.println("[GenericMOSATestSuiteGenerator] Passing "
					+ this.seedPopulation.size() + " seed solutions to MOSA.");
		}
		// -------------------------------------------------------------------------
		// Pass seed population into the experiment before initialising — end here
		// -------------------------------------------------------------------------

		mosaExperiment.initExperiment(this.budgetManager);

		try {
			archiveSolutions = mosaExperiment.multiObjectiveRun();
			System.out.println(mosaExperiment.getAlgorithmStats().getLog());
			this.evaluations = mosaExperiment.getNumberOfEvaluation();
		} catch (JMException | ClassNotFoundException e) {
			e.printStackTrace();
			throw new TestSuiteGenerationException(e.getMessage());
		}

		// -------------------------------------------------------------------------
		// Store final population after MOSA run completes — start here
		// -------------------------------------------------------------------------
		// Retrieve and store the final population from MOSA_Generic so GenAndWrite
		// can pass it to PopulationStore for seeding the next iteration.
		this.lastFinalPopulation = mosaExperiment.getFinalPopulation();
		// -------------------------------------------------------------------------
		// Store final population after MOSA run completes — end here
		// -------------------------------------------------------------------------

		Solution currentSolution;
		List<Integer> numberOfEvaluations = mosaExperiment.getNumberOfEvaluations();
		for (int i = 0; i < archiveSolutions.size(); i++) {
			currentSolution = archiveSolutions.get(i);
			if (currentSolution.getFitness() == 0) {
				VariableTranslator translator = new VariableTranslator(currentSolution);
				Object[][][] numericParams = translator.translateArray(cfg.getParameterTypes());
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

	// -------------------------------------------------------------------------
	// New methods — start here
	// -------------------------------------------------------------------------

	/**
	 * Sets the seed population to use when MOSA initialises its population.
	 * Must be called BEFORE generateTestSuite() for seeding to take effect.
	 * Called by GenAndWrite using solutions from PopulationStore.
	 *
	 * @param seedPopulation solutions from the previous iteration's final
	 *                       population, limited to populationSize / 2
	 */
	public void setSeedPopulation(SolutionSet seedPopulation) {
		this.seedPopulation = seedPopulation;
	}

	/**
	 * Returns the final population from the last completed MOSA run.
	 * Called by GenAndWrite after generateTestSuite() returns, to store
	 * the population in PopulationStore for the next iteration's seeding.
	 *
	 * @return final SolutionSet from MOSA_Generic, or null if not yet run
	 */
	public SolutionSet getFinalPopulation() {
		return this.lastFinalPopulation;
	}

	// -------------------------------------------------------------------------
	// New methods — end here
	// -------------------------------------------------------------------------
}