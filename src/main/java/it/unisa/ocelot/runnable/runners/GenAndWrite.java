package it.unisa.ocelot.runnable.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;

import org.apache.commons.io.output.TeeOutputStream;

import it.unisa.ocelot.suites.FitnessTracker;
import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.types.CTypeHandler;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.branch_chains.BranchChainManager;
import it.unisa.ocelot.genetic.objectives.branch_chains.BranchChainPairObjective;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
import it.unisa.ocelot.runnable.Run;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.GenericCoverageCalculator;
import it.unisa.ocelot.suites.CoverageVerifier;
import it.unisa.ocelot.suites.MABController;
import it.unisa.ocelot.suites.ObjSubsetLoader;
import it.unisa.ocelot.suites.PopulationStore;
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGeneratorHandler;
import it.unisa.ocelot.suites.generators.many_objective.GenericMOSATestSuiteGenerator;
import it.unisa.ocelot.writer.TestFramework;
import it.unisa.ocelot.writer.check.CheckFactory;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import it.unisa.ocelot.suites.SerendipitousCoverageChecker;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
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
 * <li>SerendipitousCoverageChecker marks non-subset objectives covered by the new TCs.</li>
 * <li>CoverageVerifier identifies newly covered and still-uncovered objectives.</li>
 * <li>MABController records results (coverage gain, velocity) and updates UCB
 * scores for the next iteration's selection.</li>
 * <li>Repeat until MAX_ITERATIONS or full coverage.</li>
 * </ol>
 *
 * <p>
 * <b>Isolation guarantee:</b> MOSA changes: seed setter and population getter only
 */
public class GenAndWrite {
	// One-time-per-JVM guard: delete old fitness mapping output file on first call
	// to getOutputFile()

	public void run() {
		FitnessTracker fitnessTracker = null;
		PrintStream originalOut = System.out;
		FileOutputStream fos = null;
		PrintStream ps = null;
		try {
			ConfigManager config = ConfigManager.getInstance();

			// Sets up the output file
			File outputDirectory = new File(config.getOutputFolder());
			outputDirectory.mkdirs();
			String formatedDateTime = LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
			fos = new FileOutputStream(new File(outputDirectory, "exp_res_" + formatedDateTime + ".txt"));
			ps = new PrintStream(new TeeOutputStream(originalOut, fos), true);
			System.setOut(ps);

			// Builds the CFG and sets the target
			CFG cfg = CFGBuilder.build(config.getTestFilename(), config.getTestFunction());

			CTypeHandler typeHandler = new CTypeHandler(cfg.getParameterTypes());
			CBridge.initialize(typeHandler.getValues().size(), typeHandler.getPointers().size(),
					typeHandler.getPointers().size());

			int mcCabePaths = cfg.edgeSet().size() - cfg.vertexSet().size() + 1;
			System.out.println("Cyclomatic complexity: " + mcCabePaths);

			// Reset the static test case ID counter so each run starts from 0 again
			GenericMOSATestSuiteGenerator.resetTestCaseIds();
			// Load the full objective list once
			List<GenericObjective> allObjectives = BranchChainManager.loadObjectives();
			System.out.println("Total objectives loaded: " + allObjectives.size());

			// Initialise orchestration components
			// Tracks UCB scores and per-objective statistics across iterations
			MABController mab = new MABController();
			// Enforces subset size = populationSize / 2, delegates to MAB
			ObjSubsetLoader subsetLoader = new ObjSubsetLoader(config.getPopulationSize(), mab);

			// Holds the final MOSA population for seeding the next iteration
			// Pool capacity = populationSize / 2, set once and fixed for all iterations
			PopulationStore populationStore = new PopulationStore(config.getPopulationSize());

			// Checks which objectives are covered after each iteration
			CoverageVerifier verifier = new CoverageVerifier();

			// Accumulates test cases produced across ALL iterations
			Set<TestCase> suite = new HashSet<>();
			// Checks whether TCs from each iteration incidentally cover
			// objectives outside the current subset — free coverage gain
			SerendipitousCoverageChecker serendipityChecker = new SerendipitousCoverageChecker();
			// Start with all objectives uncovered
			List<GenericObjective> uncoveredObjectives = new ArrayList<>(allObjectives);

			System.out.println("Starting MAB-guided iterative generation. " + "Max iterations: " + config.getMaxIterForMosa()
			+ ", subset size: " + subsetLoader.getSubsetSize());
			StringBuilder objCovEachIter = new StringBuilder();
			// Tracks and persists fitness snapshots to CSV for post-run analysis.
			// "." writes fitness_progress.csv to the current working directory —
			// change to config.getOutputDir() or similar if you have an output path.

			boolean trackFitness = false;//TODO fix this to be configurable via config file instead of system property
			fitnessTracker = trackFitness
					? openFitnessProgressFile(config)
							: FitnessTracker.disabled();

			// Main iterative generation loop
			for (int iteration = 1; iteration <= config.getMaxIterForMosa(); iteration++) {

				System.out.println("\n=== Iteration " + iteration + "/" + config.getMaxIterForMosa() + " ===");
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
				//Print the objective IDs in the subset for debugging
				System.out.println("Selected objective IDs for this iteration: ");
				for (GenericObjective obj : subsetObjectives) {
					System.out.print(" "+obj.getObjectiveID());
				}
				System.out.println();
				// Reset per-objective state before handing to MOSA.
				// isActive, bestFitness, counter must be clean so MOSA's
				// internal deactivation and preference criterion start fresh.
				// fitnessSnapshots are cleared so velocity computation for THIS
				// iteration only uses data from THIS run.
				int uncoveredBefore = uncoveredObjectives.size();

				for (GenericObjective obj : subsetObjectives) {
					obj.reset();
				}
				// Re-index objective IDs to contiguous 0-based range.
				// jmetal Solution array is sized by objectives.size(), and
				// setObjective(id, v) uses id as direct array index. After
				// filtering, IDs are non-contiguous — remapping prevents
				// ArrayIndexOutOfBoundsException in MOSAGenericCoverageProblem.
				Map<GenericObjective, Integer> savedIds = remapObjectiveIds(subsetObjectives);

				TestSuiteGenerator generator = null;
				Set<TestCase> iterationSuite;
				try {
					generator = TestSuiteGeneratorHandler.getInstance(config, cfg, subsetObjectives);
					if (generator == null) {
						throw new IllegalStateException(
								"No generator found for: " + config.getTestSuiteGenerator());
					}
					System.out.println("Generator: " + generator.getClass().getSimpleName());

					if (generator instanceof GenericMOSATestSuiteGenerator) {
						// buildAdaptiveSeeds() returns a SolutionSet containing the best-ever solutions
						//for each objective in the current subset, 
						//plus any additional solutions from the PopulationStore to fill the rest of the population.
						// buildAdaptiveSeeds() is tried but not working as expected, so we are not using it for now.
						//SolutionSet seeds = buildAdaptiveSeeds(subsetObjectives, populationStore,subsetLoader.getSubsetSize());

						if (generator instanceof GenericMOSATestSuiteGenerator && populationStore.hasSeedPopulation()) {
							SolutionSet seeds = populationStore.getSeedPopulation(subsetLoader.getSubsetSize());
							((GenericMOSATestSuiteGenerator) generator).setSeedPopulation(seeds);
							if (seeds.size() > 0) {
								((GenericMOSATestSuiteGenerator) generator).setSeedPopulation(seeds);
								System.out.println("Seeded MOSA with " + seeds.size() + " solutions from previous iteration.");
							}
						}

					}
					iterationSuite = generator.generateTestSuite();
					SolutionSet fp = ((GenericMOSATestSuiteGenerator) generator).getFinalPopulation();
					System.out.println("[Debug] finalPopulation=" + (fp == null ? "null" : fp.size()));
				}finally {
					// Always restore original IDs, even on exception
					restoreObjectiveIds(savedIds);
				}
				//Reset branch-distance tracking files so the next iteration starts clean 
				System.out.println("Resetting BranchDistanceTracker files for next iteration...");
				Run.resetFileQuietly(BranchChainManager.BRANCH_DIST_TRACK_FILE);
				Run.resetFileQuietly(BranchChainManager.FUNCTION_NAME_MAP_FILE);


				// Accumulate test cases from this iteration into global suite
				suite.addAll(iterationSuite);
				System.out.println("Test cases this iteration: " + iterationSuite.size() + " | Total accumulated: "
						+ suite.size());
				// Store MOSA's final population for seeding next iteration.
				// Retrieved from GenericMOSATestSuiteGenerator.getFinalPopulation()
				// which in turn calls MOSA_Generic.getFinalPopulation().
				/*if (generator instanceof GenericMOSATestSuiteGenerator) {
					SolutionSet seeds = buildAdaptiveSeeds(subsetObjectives, populationStore,
							subsetLoader.getSubsetSize());
					if (seeds.size() > 0) {
						((GenericMOSATestSuiteGenerator) generator).setSeedPopulation(seeds);
						System.out.println("Seeded MOSA with " + seeds.size() + " solutions from previous iteration.");
					}
				}*/
				if (generator instanceof GenericMOSATestSuiteGenerator) {
					SolutionSet finalPop = ((GenericMOSATestSuiteGenerator) generator).getFinalPopulation();
					if (finalPop != null && finalPop.size() > 0) {
						populationStore.store(finalPop);
						System.out.println("Stored final population of size: " + finalPop.size());
					}
				}
				// ------------------------------------------------------------------
				// Serendipitous coverage check — re-execute each TC from this
				// iteration against ALL uncovered non-subset objectives.
				// Any objective whose fitness == 0.0 is marked covered for free,
				// without spending an additional MOSA iteration on it.
				// ------------------------------------------------------------------
				long t0 = System.nanoTime();
				// ... check(...) ...
				List<Solution> evaluated = new ArrayList<>();
				for (TestCase tc : iterationSuite) {
					evaluated.add(tc.getSolution());
				}
				if (generator instanceof GenericMOSATestSuiteGenerator) {
					SolutionSet fp = ((GenericMOSATestSuiteGenerator) generator).getFinalPopulation();
					// we added the final population to the population store, 
					//but we also want to check if any of those solutions serendipitously covered objectives 
					//outside the subset
					if (fp != null) {
						for (int i = 0; i < fp.size(); i++) {
							evaluated.add(fp.get(i));
						}
					}
				}
				int serendipitouslyCovered = serendipityChecker.check(iterationSuite, allObjectives, subsetObjectives);
				if(iteration%10==0) {
					dumpProximity(allObjectives, iteration);
				}
				/*int serendipitouslyCovered = serendipityChecker.check(iterationSuite, // TCs produced this iteration
						allObjectives, // full objective list to scan
						subsetObjectives); // exclude — MOSA already handled these*/
				if (serendipitouslyCovered > 0) {
					System.out.println("[Serendipity] Serendipitous coverage: " + serendipitouslyCovered
							+ " additional objective(s) covered at no extra cost.");
				}
				System.out.println("[Serendipity] took " + (System.nanoTime() - t0) / 1_000_000 + " ms");
				// MAB step 2 — compute velocity for each objective in subset.
				// Uses fitnessSnapshots recorded by MOSA_Generic during the run.
				mab.computeVelocities(subsetObjectives);

				// Flush per-generation snapshots for ALL objectives — subset ones have
				// real snapshots, non-subset ones get last-known fitness as placeholder
				// Write per-generation fitness snapshots for all objectives to the CSV
				fitnessTracker.flush(iteration, allObjectives, subsetObjectives);

				// Coverage verification — identify newly covered objectives
				System.out.println(verifier.summarise(allObjectives));

				// Update the uncovered list using the full allObjectives state
				uncoveredObjectives = verifier.getUncoveredObjectivesSorted(allObjectives, suite);
				long chainPairCount = uncoveredObjectives.stream()
						.filter(o -> o instanceof BranchChainPairObjective)
						.count();
				String lineChainPairCount = "\n" + iteration + "/" + config.getMaxIterForMosa() + " -> " + "Uncovered BranchChainPairObjectives: "
						+ "" + chainPairCount + " / " + uncoveredObjectives.size();
				objCovEachIter.append(lineChainPairCount);
				System.out.println("Uncovered BranchChainPairObjectives: " + chainPairCount + " / " + uncoveredObjectives.size());
				int newlyCovered = uncoveredBefore - uncoveredObjectives.size();
				String line = "\n" + iteration + "/" + config.getMaxIterForMosa() + " -> " + iterationSuite.size()
				+ " test cases, " + newlyCovered + " objectives newly covered";
				//String line = "\n" + iteration + "/" + MAX_ITERATIONS + " -> " + iterationSuite.size()+ " Objectives covered";
				objCovEachIter.append(line);
				//System.out.println("Uncovered after iteration " + iteration + ": " + uncoveredObjectives.size());
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

				System.out.println("[Iter " + iteration + "] "
						+ " | suite size: " + suite.size()
						+ " | populationStore pool size: " + populationStore.size()); // or equivalent getter
			} // For loop end
			System.out.println("[INFO] EvInT run finished.");
			// Close the CSV file writer cleanly after all iterations complete
			//fitnessTracker.close();

			// Report if the iteration budget ran out before full coverage
			if (!verifier.isFullyCovered(allObjectives)) {
				System.out.println("\nBudget exhausted after " + config.getMaxIterForMosa() + " iteration(s). "
						+ verifier.summarise(allObjectives));
			}

			// Final coverage calculation and report
			// Set<TestCase> minimizedSuite = minimizer.minimize(suite); //TODO No minimization yet
			Set<TestCase> minimizedSuite = suite;

			GenericCoverageCalculator calculator = new GenericCoverageCalculator(cfg, allObjectives);

			calculator.calculateCoverage(minimizedSuite);
			// ---- Check 1: replay coverage from the final suite only ----
			verifyCoverageBySuite(suite, allObjectives);
			// ---- Check 2: re-execute each TC in the final suite and compare with stored coverage ----
			verifyCoverageByReexecution(suite, allObjectives);




			// Print and write uncovered branch-chain objectives to file (include coverage
			// counts)
			// printUncoveredBCobjectives(calculator, allObjectives, minimizedSuite);

			System.out.println(objCovEachIter.toString());
			System.out.println("-------------------------------------------------------");
			System.out.println("Output Highlights:");
			System.out.println("-------------------------------------------------------");
			System.out.println("[IN] Algorithm time limit(seconds): " +  String.valueOf(config.getAlgorithmTime()));
			System.out.println("[IN] Algorithm evaluation limit: " +  String.valueOf(config.getMaxEvaluations()));			
			System.out.println("[IN] Population size: " + config.getPopulationSize());
			System.out.println("[IN] Max MAB iterations: " + config.getMaxIterForMosa());
			System.out.println("[IN] Algorithm generation limit: " + String.valueOf(config.getAlgorithmGenerations()));
			System.out.println("[IN] Total input space (array size): " + String.valueOf(config.getTestArraysSize()));
			System.out.println("-------------------------------------------------------");
			System.out.println("[OUT] Total size of objectives to evaluate: " + allObjectives.size());
			System.out.println("[OUT] Minimized test cases: " + minimizedSuite.size());
			System.out.println("[OUT] Total objectives covered: " + verifier.getFinalCoverage(allObjectives));
			//System.out.println("Objective coverage achieved: " + calculator.getObjectiveCoverage());//TODO this is not working
			//System.out.println("[OUT] Uncovered objectives: " + uncoveredObjectives.size());

			/* for (GenericObjective objective : uncoveredObjectives) { 
				 if (!objective.isCovered()) { System.out.println(objective.toString()); } }
			 System.out.println("-------------------------------------------------------");

			/* System.out.println("-------------------------------------------------------");
				System.out.println("Covered objectives: " + verifier.getFinalCoverage(allObjectives));

				 for (GenericObjective objective : allObjectives) { 
					 if (objective.isCovered()) { System.out.println(objective.toString()); } }
				 System.out.println("-------------------------------------------------------");
			 */ 
			//TODO printUncoveredBCobjectives(calculator, allObjectives, minimizedSuite);
			// System.out.println("Branch coverage achieved: " +
			// calculator.getBranchCoverage());
			// System.out.println("Statement coverage achieved: " +
			// calculator.getBlockCoverage());
			System.out.println("-------------------------------------------------------");
			System.out.println("Log file written to: " + "exp_res_" + formatedDateTime + ".txt");
			// TODO last enable the following to print the test cases.

			/*String formattedFilename = config.getTestFilename();
			formattedFilename = formattedFilename.replaceAll("[^A-Za-z0-9]", "_");
			String filename = "_Test_" + config.getTestFunction() + "_" + formattedFilename + ".c";
			System.out.println("Writing test suite on " + filename + "...");

			TestFramework framework = new TestFramework(new CheckFactory());

			String content = framework.writeTestSuite(minimizedSuite, cfg, config);
			String dir_file = config.getTestBasedir() + "/" + filename;
			PrintWriter writer = new PrintWriter(dir_file, "UTF-8");
			writer.print(content);
			writer.close();
			 */
			// Utils.writeFile(filename, content);

			System.out.println("EvInT operation completed!");

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (fitnessTracker != null) {
				fitnessTracker.close();
			}
			if (ps != null) {
				ps.flush();
			}
			System.setOut(originalOut);
			if (fos != null) {
				try { fos.close(); } catch (IOException ignored) { }
			}
		}
	}

	private FitnessTracker openFitnessProgressFile(ConfigManager config) {
		try {
			// Constructor creates the directory if needed and overwrites any old CSV
			return new FitnessTracker(config.getTestBasedir());
		} catch (IOException e) {
			throw new RuntimeException("Could not create FitnessTracker CSV", e);
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

		Map<GenericObjective, Integer> saved = new IdentityHashMap<>(objectives.size());
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

	// Debug check: recompute coverage using only the test cases in the final suite
	// and compare with the objectives flagged as covered during the run.
	private void verifyCoverageBySuite(Set<TestCase> suite, List<GenericObjective> allObjectives) {
		System.out.println("[Check1] Verifying coverage by replaying " + suite.size() + " test cases against "
				+ allObjectives.size() + " objectives.");
		HashSet<GenericObjective> coveredBySomeTest = new HashSet<>();
		for (TestCase tc : suite) {
			if (!(tc.getSolution() instanceof GenericSolution)) {
				System.out.println("[Check1] TC " + tc.getId() + " has no GenericSolution");
				continue;
			}
			GenericSolution s = (GenericSolution) tc.getSolution();
			int previouslyCovered = coveredBySomeTest.size();
			for (GenericObjective o : allObjectives) {
				if (o.getFitness(s) == 0.0) {
					coveredBySomeTest.add(o);
				}
			}
			System.out.println("[Check1] TC " + tc.getId() + " covers " + (coveredBySomeTest.size() - previouslyCovered) + " objectives");
		}

		long flagged = allObjectives.stream().filter(GenericObjective::isCovered).count();
		System.out.println("[Check1] flagged=" + flagged + " coveredBySomeTest=" + coveredBySomeTest.size());
		if (flagged != coveredBySomeTest.size()) {
			System.out.println("[Check1] MISMATCH: " + Math.abs(flagged - coveredBySomeTest.size())
			+ (flagged > coveredBySomeTest.size()
					? " objectives flagged covered without a TC in the suite (false positives)"
							: " objectives covered by the suite but not flagged"));
		}
	}

	private void verifyCoverageByReexecution(Set<TestCase> suite, List<GenericObjective> allObjectives)
			throws Exception {
		System.out.println("[Check2] Re-executing " + suite.size() + " test cases against "
				+ allObjectives.size() + " objectives.");
		Set<GenericObjective> reexec = Collections.newSetFromMap(new IdentityHashMap<>());

		for (TestCase tc : suite) {
			GenericSolution s = (GenericSolution) tc.getSolution();
			MOSAGenericCoverageProblem problem = (MOSAGenericCoverageProblem) s.getProblem();
			Object[][][] args = tc.getParameters();

			HashMap<String, Double> run1 = problem.executeForFitnessMap(args);
			HashMap<String, Double> run2 = problem.executeForFitnessMap(args);
			boolean deterministic = run1.equals(run2);

			int stored = 0, fresh = 0, differ = 0;
			for (GenericObjective o : allObjectives) {
				boolean a = o.getFitness(s) == 0.0;          // stored data (Check 1)
				boolean b = s.evaluateObjectiveOn(run1, o) == 0.0;  // fresh execution
				if (a) stored++;
				if (b) { fresh++; reexec.add(o); }
				if (a != b) differ++;
			}
			System.out.println("[Check2] TC " + tc.getId() + " stored=" + stored + " fresh=" + fresh
					+ " differ=" + differ + " deterministic=" + deterministic
					+ " mapSize=" + run1.size());
		}

		long flagged = allObjectives.stream().filter(GenericObjective::isCovered).count();
		System.out.println("[Check2] flagged=" + flagged + " reexecuted=" + reexec.size());
	}







	// Helper: prints uncovered branch-chain objectives (human readable) and writes
	// them to uncoveredBCobjectives.txt
	//TODO: Consider refactoring this method to separate printing and file writing for clarity and testability.



		private void dumpProximity(List<GenericObjective> all, int iteration) {
		Map<Double, Integer> hist = new TreeMap<>();
		int never = 0;
		for (GenericObjective o : all) {
			if (o.isCovered()) continue;
			double f = o.getBestFitness();
			if (f == Double.MAX_VALUE) { never++; continue; }
			hist.merge(Math.round(f * 100) / 100.0, 1, Integer::sum);   // bucket to 0.01
		}
		System.out.println("[Prox-dump] iter=" + iteration + " neverEvaluated=" + never
				+ " distinctFitnessValues=" + hist.size());
		hist.entrySet().stream()
		.sorted((a, b) -> b.getValue() - a.getValue()).limit(10)
		.forEach(e -> System.out.println("[Prox-dump]   fitness=" + e.getKey() + " -> " + e.getValue() + " objectives"));
	}
}