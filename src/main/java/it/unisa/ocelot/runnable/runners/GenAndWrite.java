package it.unisa.ocelot.runnable.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Spliterator.OfPrimitive;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.io.output.TeeOutputStream;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cdg.BranchChainManager;
import it.unisa.ocelot.c.cdg.BranchChainPairStateMachine;
import it.unisa.ocelot.c.cdg.BranchChain;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.types.CTypeHandler;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.BranchManager;
import it.unisa.ocelot.genetic.objectives.BranchObjective;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairsManager;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.CoverageCalculator;
import it.unisa.ocelot.simulator.GenericCoverageCalculator;
import it.unisa.ocelot.suites.CoverageVerifier;
import it.unisa.ocelot.suites.MABController;
import it.unisa.ocelot.suites.ObjSubsetLoader;
import it.unisa.ocelot.suites.ObjectiveDecomposer;
import it.unisa.ocelot.suites.PopulationStore;
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGeneratorHandler;
import it.unisa.ocelot.suites.generators.many_objective.GenericMOSATestSuiteGenerator;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizer;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizerHandler;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.writer.TestFramework;
import it.unisa.ocelot.writer.check.CheckFactory;
import jmetal.core.SolutionSet;

/**
 * GenAndWrite orchestrates iterative test suite generation using a Multi-Armed
 * Bandit (MAB) strategy for objective subset selection.
 *
 * <p>
 * <b>Algorithm overview per iteration:</b>
 * <ol>
 * <li>ObjSubsetLoader asks MABController to select the next subset of uncovered
 * objectives (size = populationSize / 2).</li>
 * <li>Reset per-objective state so MOSA starts clean.</li>
 * <li>Seed MOSA with the final population from the previous iteration (via
 * PopulationStore) so progress is not lost.</li>
 * <li>Run MOSA on the selected subset.</li>
 * <li>Store MOSA's final population in PopulationStore.</li>
 * <li>CoverageVerifier identifies newly covered and still-uncovered
 * objectives.</li>
 * <li>MABController records results (coverage gain, velocity) and updates UCB
 * scores for the next iteration's selection.</li>
 * <li>Repeat until MAX_ITERATIONS or full coverage.</li>
 * </ol>
 *
 * <p>
 * <b>Isolation guarantee:</b> MOSA and all other generators are unmodified
 * except for two small additions to MOSA_Generic (seed setter + population
 * getter) which do not affect existing behaviour when not called.
 */
public class GenAndWrite {
	// Budget: maximum number of generation iterations across all loops.
	// Adjust this constant (or load it from ConfigManager) as needed.
	private static final int MAX_ITERATIONS = 10;

	public void run() {
		try {
			ConfigManager config = ConfigManager.getInstance();

			// Sets up the output file
			File outputDirectory = new File(config.getOutputFolder());
			outputDirectory.mkdirs();
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
			String formatedDateTime = now.format(formatter);
			FileOutputStream fos = new FileOutputStream(
					config.getOutputFolder() + "exp_res_" + formatedDateTime + ".txt");
			TeeOutputStream myOut = new TeeOutputStream(System.out, fos);
			PrintStream ps = new PrintStream(myOut);
			System.setOut(ps);

			// Builds the CFG and sets the target
			CFG cfg = CFGBuilder.build(config.getTestFilename(), config.getTestFunction());

			CTypeHandler typeHandler = new CTypeHandler(cfg.getParameterTypes());
			CBridge.initialize(typeHandler.getValues().size(), typeHandler.getPointers().size(),
					typeHandler.getPointers().size());

			int mcCabePaths = cfg.edgeSet().size() - cfg.vertexSet().size() + 1;
			System.out.println("Cyclomatic complexity: " + mcCabePaths);

			// Load the full objective list once
			List<GenericObjective> allObjectives = BranchChainManager.loadObjectives();
			System.out.println("Total objectives loaded: " + allObjectives.size());

			// Initialise orchestration components
			// Tracks UCB scores and per-objective statistics across iterations
			MABController mab = new MABController();
			// Enforces subset size = populationSize / 2, delegates to MAB
			ObjSubsetLoader subsetLoader = new ObjSubsetLoader(config.getPopulationSize(), mab);

			// Holds the final MOSA population for seeding the next iteration
			PopulationStore populationStore = new PopulationStore();

			// Checks which objectives are covered after each iteration
			CoverageVerifier verifier = new CoverageVerifier();

			// Accumulates test cases produced across ALL iterations
			Set<TestCase> suite = new HashSet<>();

			// Start with all objectives uncovered
			List<GenericObjective> uncoveredObjectives = new ArrayList<>(allObjectives);

			System.out.println("Starting MAB-guided iterative generation. " + "Max iterations: " + MAX_ITERATIONS
					+ ", subset size: " + subsetLoader.getSubsetSize());
			StringBuilder objCovEachIter = new StringBuilder();
			// Main iterative generation loop
			for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
				System.out.println("\n=== Iteration " + iteration + "/" + MAX_ITERATIONS + " ===");
				System.out.println("Uncovered objectives remaining: " + uncoveredObjectives.size());

				// Exit early if nothing left to cover
				if (uncoveredObjectives.isEmpty()) {
					System.out.println("All objectives covered — stopping early.");
					break;
				}
				// MAB step 1 — select subset of objectives for this iteration
				// ObjSubsetLoader enforces size = populationSize / 2 and uses
				// UCB1 scoring to pick the most promising objectives.
				List<GenericObjective> subsetObjectives = subsetLoader.loadSubset(uncoveredObjectives, iteration);
				// Reset per-objective state before handing to MOSA.
				// isActive, bestFitness, counter must be clean so MOSA's
				// internal deactivation and preference criterion start fresh.
				// fitnessSnapshots are cleared so velocity computation for THIS
				// iteration only uses data from THIS run.
				BranchChainManager.newFitnessHashMap.clear();

				for (GenericObjective obj : subsetObjectives) {
					obj.setActive(true);
					obj.bestFitness = Double.MAX_VALUE;
					obj.counter = 0;
					obj.clearSnapshots(); // clear velocity snapshot history
					// Reset state machine for BranchChainPairStateMachine objectives
					if (obj instanceof BranchChainPairStateMachine) {
						((BranchChainPairStateMachine) obj).setCurrState(BranchChainPairStateMachine.State.zeroCover);
					}
				}
				// Re-index objective IDs to contiguous 0-based range.
				// jmetal Solution array is sized by objectives.size(), and
				// setObjective(id, v) uses id as direct array index. After
				// filtering, IDs are non-contiguous — remapping prevents
				// ArrayIndexOutOfBoundsException in MOSAGenericCoverageProblem.
				Map<GenericObjective, Integer> savedIds = remapObjectiveIds(subsetObjectives);

				// Build generator for this iteration's objective subset
				TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, subsetObjectives);

				if (generator == null) {
					restoreObjectiveIds(savedIds);
					System.err.println("No generator found for: " + config.getTestSuiteGenerator());
					break;
				}

				System.out.println("Generator: " + generator.getClass().getSimpleName());
				// Seed MOSA with the previous iteration's final population.
				// This preserves search progress — without seeding, each MOSA
				// instance re-discovers the same easy solutions from scratch.
				// Only GenericMOSATestSuiteGenerator supports seeding.
				if (generator instanceof GenericMOSATestSuiteGenerator && populationStore.hasSeedPopulation()) {
					int seedSize = config.getPopulationSize() / 2;
					SolutionSet seeds = populationStore.getSeedPopulation(seedSize);
					((GenericMOSATestSuiteGenerator) generator).setSeedPopulation(seeds);
					System.out.println("Seeded MOSA with " + seeds.size() + " solutions from previous iteration.");
				}
				// Run MOSA — the core generation step.
				// try/finally guarantees IDs are always restored even on exception.
				Set<TestCase> iterationSuite;
				try {
					iterationSuite = generator.generateTestSuite();
				} finally {
					// Restore original IDs before any coverage reporting
					restoreObjectiveIds(savedIds);
				}

				// Accumulate test cases from this iteration into global suite
				suite.addAll(iterationSuite);
				System.out.println("Test cases this iteration: " + iterationSuite.size() + " | Total accumulated: "
						+ suite.size());
				// Store MOSA's final population for seeding next iteration.
				// Retrieved from GenericMOSATestSuiteGenerator.getFinalPopulation()
				// which in turn calls MOSA_Generic.getFinalPopulation().
				if (generator instanceof GenericMOSATestSuiteGenerator) {
					SolutionSet finalPop = ((GenericMOSATestSuiteGenerator) generator).getFinalPopulation();
					if (finalPop != null && finalPop.size() > 0) {
						populationStore.store(finalPop);
						System.out.println("Stored final population of size: " + finalPop.size());
					}
				}
				// MAB step 2 — compute velocity for each objective in subset.
				// Uses fitnessSnapshots recorded by MOSA_Generic during the run.
				mab.computeVelocities(subsetObjectives);
				// Coverage verification — identify newly covered objectives
				System.out.println(verifier.summarise(allObjectives));

				// Update the uncovered list using the full allObjectives state
				uncoveredObjectives = verifier.getUncoveredObjectivesSorted(allObjectives, suite);

				System.out.println("Uncovered after iteration " + iteration + ": " + uncoveredObjectives.size());
				// MAB step 3 — record result and update UCB statistics.
				// Uses the number of evaluations MOSA spent this iteration.
				int evaluationsThisIteration = generator.getNumberOfEvaluations();
				mab.recordResult(subsetObjectives, evaluationsThisIteration);

				// Early exit if everything is covered
				if (verifier.isFullyCovered(allObjectives)) {
					System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					System.out.println("Full coverage achieved after " + iteration + " iteration(s).");
					System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
					break;
				}
				String line = "\n" + iteration + "/" + MAX_ITERATIONS + " -> " + iterationSuite.size()
						+ " Objectives covered";
				objCovEachIter.append(line);
			} // For loop end

			// Final coverage report
			if (!verifier.isFullyCovered(allObjectives)) {
				System.out.println("\nBudget exhausted after " + MAX_ITERATIONS + " iteration(s). "
						+ verifier.summarise(allObjectives));
			}

			// Final coverage report (unchanged from original)
			// Set<TestCase> minimizedSuite = minimizer.minimize(suite); //TODO later
			Set<TestCase> minimizedSuite = suite;

			GenericCoverageCalculator calculator = new GenericCoverageCalculator(cfg, allObjectives);

			calculator.calculateCoverage(minimizedSuite);

			// Print and write uncovered branch-chain objectives to file (include coverage
			// counts)
			// printUncoveredBCobjectives(calculator, allObjectives, minimizedSuite);
			System.out.println(objCovEachIter.toString());
			System.out.println("-------------------------------------------------------");
			System.out.println("Size of objectivesToEvaluate: " + allObjectives.size());
			System.out.println("-------------------------------------------------------");
			System.out.println("Minimized test cases: " + minimizedSuite.size());
			System.out.println("Objective coverage achieved: " + calculator.getObjectiveCoverage());
			System.out.println("Computed coverage for " + suite.size() + " test cases");
			System.out.println("Uncovered objectives: " + uncoveredObjectives.size());
			System.out.println("Size of all objectives: " + allObjectives.size());
			System.out.println("-------------------------------------------------------");
			System.out.println("Total test cases: " + suite.size());
			System.out.println("Objective coverage achieved: " + calculator.getObjectiveCoverage());
			System.out.println("-------------------------------------------------------");

			for (GenericObjective objective : uncoveredObjectives) {
				if (!objective.isCovered()) {
					 System.out.println(objective.toString());
				}
			}
			// System.out.println("Branch coverage achieved: " +
			// calculator.getBranchCoverage());
			// System.out.println("Statement coverage achieved: " +
			// calculator.getBlockCoverage());

			// TODO last enable the following to print the test cases.
			
			  String formattedFilename = config.getTestFilename(); formattedFilename =
			  formattedFilename.replaceAll("[^A-Za-z0-9]", "_"); String filename = "_Test_"
			  + config.getTestFunction() + "_" + formattedFilename + ".c";
			  System.out.println("Writing test suite on " + filename + "...");
			  
			  TestFramework framework = new TestFramework(new CheckFactory());
			  
			  String content = framework.writeTestSuite(minimizedSuite, cfg, config);
			  Utils.writeFile(filename, content);
			 

			System.out.println("Operation completed!");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	// ------------------------------------------------------------------
	// Helper: re-index a list of objectives to contiguous IDs 0 … N-1.
	//
	// Returns a map of objective → originalId so the caller can restore
	// them after the generator finishes. The objectives themselves are
	// mutated in-place (no copies), so MOSA sees the updated IDs when it
	// calls solution.setObjective(objective.getObjectiveID(), fitness).
	// ------------------------------------------------------------------
	private Map<GenericObjective, Integer> remapObjectiveIds(List<GenericObjective> objectives) {

		Map<GenericObjective, Integer> saved = new HashMap<>(objectives.size());
		for (int i = 0; i < objectives.size(); i++) {
			GenericObjective obj = objectives.get(i);
			saved.put(obj, obj.getObjectiveID()); // save original
			obj.setObjectiveID(i); // assign 0-based index
		}
		return saved;
	}

	// ------------------------------------------------------------------
	// Helper: restore original IDs after the generator has finished.
	// Must be called even when the generator throws, to keep the
	// allObjectives list consistent for coverage reporting.
	// ------------------------------------------------------------------
	private void restoreObjectiveIds(Map<GenericObjective, Integer> saved) {
		for (Map.Entry<GenericObjective, Integer> entry : saved.entrySet()) {
			entry.getKey().setObjectiveID(entry.getValue());
		}
	}

	// Helper: prints uncovered branch-chain objectives (human readable) and writes
	// them to uncoveredBCobjectives.txt
	// Also computes how many times each branch chain is covered across the provided
	// test suite
	private void printUncoveredBCobjectives(GenericCoverageCalculator calculator,
			List<GenericObjective> branchChainObjectives, Set<TestCase> suite) {
		List<GenericObjective> uncovered = calculator.getUncoveredObjectives();
		System.out.println("Uncovered Branch-Chain Objectives: " + uncovered.size());
		// Build counts for each branch chain label
		Map<String, Integer> coverageCounts = new HashMap<>();
		for (GenericObjective obj : branchChainObjectives) {
			if (obj instanceof BranchChainPairStateMachine) {
				BranchChainPairStateMachine bcsm = (BranchChainPairStateMachine) obj;
				BranchChain bc1 = bcsm.getBranchChainOne();
				BranchChain bc2 = bcsm.getBranchChainTwo();
				coverageCounts.putIfAbsent(bc1.getLabel(), 0);
				coverageCounts.putIfAbsent(bc2.getLabel(), 0);
			} else {
				coverageCounts.putIfAbsent(obj.toString(), 0);
			}
		}
		// Re-evaluate objectives for each test and increment counts when objective is
		// covered
		for (TestCase tc : suite) {
			Object[][][] params = tc.getParameters();
			for (GenericObjective obj : branchChainObjectives) {
				double fitness = obj.getFitness(params);
				if (fitness == 0.0) {
					if (obj instanceof BranchChainPairStateMachine) {
						BranchChainPairStateMachine bcsm = (BranchChainPairStateMachine) obj;
						String l1 = bcsm.getBranchChainOne().getLabel();
						String l2 = bcsm.getBranchChainTwo().getLabel();
						coverageCounts.put(l1, coverageCounts.getOrDefault(l1, 0) + 1);
						coverageCounts.put(l2, coverageCounts.getOrDefault(l2, 0) + 1);
					} else {
						String key = obj.toString();
						coverageCounts.put(key, coverageCounts.getOrDefault(key, 0) + 1);
					}
				}
			}
		}

		try (FileWriter fw = new FileWriter("uncoveredBCobjectives.txt")) {
			for (GenericObjective obj : uncovered) {
				if (obj instanceof BranchChainPairStateMachine) {
					BranchChainPairStateMachine bcsm = (BranchChainPairStateMachine) obj;
					BranchChain bc1 = bcsm.getBranchChainOne();
					BranchChain bc2 = bcsm.getBranchChainTwo();
					String idLine = "ObjectiveID:" + obj.getObjectiveID();
					int count1 = coverageCounts.getOrDefault(bc1.getLabel(), 0);
					int count2 = coverageCounts.getOrDefault(bc2.getLabel(), 0);
					String summary = idLine + " | " + bc1.getLabel() + " (covered " + count1 + " times)  <->  "
							+ bc2.getLabel() + " (covered " + count2 + " times)";
					// System.out.println(summary);
					fw.write(summary + "\n");
					fw.write("--- Chain 1 (text) ---\n");
					fw.write(bc1.toTextRepresentation() + "\n");
					fw.write("--- Chain 2 (text) ---\n");
					fw.write(bc2.toTextRepresentation() + "\n");
					fw.write("--------------------------------------------------\n");
				} else {
					String s = obj.toString();
					int count = coverageCounts.getOrDefault(s, 0);
					String line = s + " (covered " + count + " times)";
					System.out.println(line);
					fw.write(line + "\n");
				}
			}
		} catch (IOException e) {
			System.err.println("Unable to write uncoveredBCobjectives.txt: " + e.getMessage());
		}
	}
}