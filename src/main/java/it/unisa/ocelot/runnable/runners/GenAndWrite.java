package it.unisa.ocelot.runnable.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

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
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGeneratorHandler;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizer;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizerHandler;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.writer.TestFramework;
import it.unisa.ocelot.writer.check.CheckFactory;

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
			FileOutputStream fos = new FileOutputStream(config.getOutputFolder() + "exp_res_"+formatedDateTime+".txt");
			TeeOutputStream myOut = new TeeOutputStream(System.out, fos);
			PrintStream ps = new PrintStream(myOut);
			System.setOut(ps);
	
			// Builds the CFG and sets the target
			CFG cfg = CFGBuilder.build(config.getTestFilename(), config.getTestFunction());
	
			
			CTypeHandler typeHandler = new CTypeHandler(cfg.getParameterTypes());
			CBridge.initialize(
					typeHandler.getValues().size(), 
					typeHandler.getPointers().size(),
					typeHandler.getPointers().size());
	
			
			int mcCabePaths = cfg.edgeSet().size() - cfg.vertexSet().size() + 1;
			System.out.println("Cyclomatic complexity: " + mcCabePaths);
	
			  // Load the full objective list once
			 List<GenericObjective> allObjectives = BranchChainManager.loadObjectives();
	            // currentObjectives shrinks each iteration as objectives get covered
	            List<GenericObjective> currentObjectives = allObjectives;
	            
	            // Accumulator: test cases produced across ALL iterations
	            Set<TestCase> suite = new HashSet<>();
	 
	            // Helper that checks coverage and ranks remaining objectives
	            CoverageVerifier verifier = new CoverageVerifier();
	 
	            System.out.println("Starting iterative generation. "
	                    + "Total objectives: " + allObjectives.size()
	                    + ", max iterations: " + MAX_ITERATIONS);
	            
	            int iteration = 0;
	            
	            while (iteration < MAX_ITERATIONS) {
	                iteration++;
	                System.out.println("\n=== Iteration " + iteration
	                        + "/" + MAX_ITERATIONS + " ===");
	                System.out.println("Objectives in this round: "
	                        + currentObjectives.size());
	                
	                /*for (GenericObjective obj : currentObjectives) {
	                	obj.setActive(true);
	                    obj.bestFitness = Double.MAX_VALUE;
	                    obj.counter = 0;
	                    if (obj.TriggeredPair != null && !obj.TriggeredPair.isCovered()) {
	                        obj.TriggeredPair.setActive(true);
	                    }
	                }*/
	                Map<GenericObjective, Integer> savedIds =
	                        remapObjectiveIds(currentObjectives);
	            
	                // Clear stale fitness cache from previous iteration so C instrumentation
	             // writes fresh values — prevents false non-zero fitness scores blocking coverage
	               // BranchChainManager.newFitnessHashMap.clear();
	                
	                // 3b. Build a fresh generator for this iteration's objectives.
	                TestSuiteGenerator generator =
	                        TestSuiteGeneratorHandler.getInstance(
	                                config, cfg, currentObjectives);
	 
	                if (generator == null) {
	                    restoreObjectiveIds(savedIds);          // clean up before exit
	                    System.err.println("No generator found for config: "
	                            + config.getTestSuiteGenerator());
	                    break;
	                }
	 
	                System.out.println("Generator: "
	                        + generator.getClass().getSimpleName());
	 
	                // 3c. Run generation — MOSA runs here, completely unmodified.
	                //     Wrapped in try/finally so original IDs are ALWAYS restored
	                //     even if the generator throws mid-run.
	                Set<TestCase> iterationSuite;
	                try {
	                    iterationSuite = generator.generateTestSuite();
	                } finally {
	                    // 3d. Restore original IDs before ANY further use of
	                    //     allObjectives (coverage reporting, verifier, logging).
	                    restoreObjectiveIds(savedIds);
	                }
	 
	                // 3e. Merge new test cases into the global accumulator
	                suite.addAll(iterationSuite);
	 
	                System.out.println(verifier.summarise(allObjectives));
	 
	                // 3f. Early exit: all objectives covered
	                if (verifier.isFullyCovered(allObjectives)) {
	                    System.out.println("All objectives covered — stopping early "
	                            + "after " + iteration + " iteration(s).");
	                    break;
	                }
	 
	                // 3g. Identify uncovered objectives, sorted easy → hard.
	                //     bestFitness was set by MOSA using the remapped IDs but the
	                //     values themselves are fitness distances, not IDs, so they
	                //     are unaffected by the remap/restore.
	                List<GenericObjective> uncovered =
	                        verifier.getUncoveredObjectivesSorted(allObjectives, suite);
	 
	                if (uncovered.isEmpty()) {
	                    System.out.println("No uncovered objectives remain.");
	                    break;
	                }
	 
	                System.out.println("Uncovered objectives after iteration "
	                        + iteration + ": " + uncovered.size()
	                        + " (sorted easy → hard by bestFitness)");
	 
	               /* for (int i = 0; i < uncovered.size(); i++) {
	                    GenericObjective obj = uncovered.get(i);
	                    System.out.printf("  [%2d] ObjectiveID=%-4d  bestFitness=%.4f%n",
	                            i + 1,
	                            obj.getObjectiveID(),
	                            obj.getBestFitness());
	                }*/
	 
	                // Next iteration works on the uncovered subset only
	                currentObjectives = uncovered;
	            }
	 
	            if (iteration == MAX_ITERATIONS
	                    && !verifier.isFullyCovered(allObjectives)) {
	                System.out.println("\nBudget exhausted after "
	                        + MAX_ITERATIONS + " iteration(s). "
	                        + verifier.summarise(allObjectives));
	            }
	            
	            
	            //  Final coverage report (unchanged from original)
			//Set<TestCase> minimizedSuite = minimizer.minimize(suite);
			Set<TestCase> minimizedSuite = suite;

			
			GenericCoverageCalculator calculator = new GenericCoverageCalculator(cfg, allObjectives);
			
			calculator.calculateCoverage(minimizedSuite);
			
			// Print and write uncovered branch-chain objectives to file (include coverage counts)
			printUncoveredBCobjectives(calculator, allObjectives, minimizedSuite);
			
			System.out.println("Size of objectivesToEvaluate: "+allObjectives.size());
			System.out.println("-------------------------------------------------------");
			System.out.println("Minimized test cases: " + minimizedSuite.size());
			System.out.println("Objective coverage achieved: " + calculator.getObjectiveCoverage());
			//System.out.println("Branch coverage achieved: " + calculator.getBranchCoverage());
			//System.out.println("Statement coverage achieved: " + calculator.getBlockCoverage());
			System.out.println("-------------------------------------------------------");
			
			//TODO last enable the following to print the test cases.
			/*String formattedFilename = config.getTestFilename();
			formattedFilename = formattedFilename.replaceAll("[^A-Za-z0-9]", "_");
			String filename = "_Test_" + config.getTestFunction() + "_" + formattedFilename + ".c";
			System.out.println("Writing test suite on " + filename + "...");
			
			TestFramework framework = new TestFramework(new CheckFactory());
			
			String content = framework.writeTestSuite(minimizedSuite, cfg, config);
			Utils.writeFile(filename, content);*/
			
			System.out.println("Operation completed!");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	// ------------------------------------------------------------------
    // Helper: re-index a list of objectives to contiguous IDs 0 … N-1.
    //
    // Returns a map of  objective → originalId  so the caller can restore
    // them after the generator finishes.  The objectives themselves are
    // mutated in-place (no copies), so MOSA sees the updated IDs when it
    // calls  solution.setObjective(objective.getObjectiveID(), fitness).
    // ------------------------------------------------------------------
    private Map<GenericObjective, Integer> remapObjectiveIds(
            List<GenericObjective> objectives) {
 
        Map<GenericObjective, Integer> saved = new HashMap<>(objectives.size());
        for (int i = 0; i < objectives.size(); i++) {
            GenericObjective obj = objectives.get(i);
            saved.put(obj, obj.getObjectiveID());   // save original
            obj.setObjectiveID(i);                  // assign 0-based index
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
	// Helper: prints uncovered branch-chain objectives (human readable) and writes them to uncoveredBCobjectives.txt
	// Also computes how many times each branch chain is covered across the provided test suite
	private void printUncoveredBCobjectives(GenericCoverageCalculator calculator, List<GenericObjective> branchChainObjectives, Set<TestCase> suite) {
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
		// Re-evaluate objectives for each test and increment counts when objective is covered
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
					String summary = idLine + " | " + bc1.getLabel() + " (covered " + count1 + " times)  <->  " + bc2.getLabel() + " (covered " + count2 + " times)";
					//System.out.println(summary);
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