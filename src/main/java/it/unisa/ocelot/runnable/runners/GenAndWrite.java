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
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGeneratorHandler;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizer;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizerHandler;
import it.unisa.ocelot.util.Utils;
import it.unisa.ocelot.writer.TestFramework;
import it.unisa.ocelot.writer.check.CheckFactory;

public class GenAndWrite {
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
			CFG cfgExtend = CFGBuilder.build(config.getTestFilename(), config.getTestFunction());
	
			
			CTypeHandler typeHandler = new CTypeHandler(cfg.getParameterTypes());
			CBridge.initialize(
					typeHandler.getValues().size(), 
					typeHandler.getPointers().size(),
					typeHandler.getPointers().size());
	
			
			int mcCabePaths = cfg.edgeSet().size() - cfg.vertexSet().size() + 1;
			System.out.println("Cyclomatic complexity: " + mcCabePaths);
	
			//LUCA: load list of objectives
			// Martino: decide if you want to pass pair objectives or branch objectives
			List<GenericObjective> objectives;
			
			if (config.getOptimizeFor().equals("Pairs")) {
				objectives = PC_PairsManager.loadObjectives();
				//objectives.addAll(BranchManager.loadObjectivesSynthetics(objectives.size()));
				}
			else if (config.getOptimizeFor().equals("Branches"))
				objectives = BranchManager.loadObjectives(0);
			else
				throw new Exception("Don't know what you are optimizing for");
			List<GenericObjective> objectivesToEvaluate = null;
			/*if (config.getEvaluateOn().equals("Pairs"))
				objectivesToEvaluate = PC_PairsManager.loadObjectives();	
			else if (config.getEvaluateOn().equals("Branches"))
				objectivesToEvaluate = BranchManager.loadObjectives(0);
			else
				throw new Exception("Don't know what you are Evaluate for");
			*/
			//here wwe are generating the new branch Chain Objectives
			List<GenericObjective> branchChainObjectives;
			branchChainObjectives= BranchChainManager.loadObjectives();
			
			TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, branchChainObjectives);
			//TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, objectives);
			//TestSuiteMinimizer minimizer = TestSuiteMinimizerHandler.getInstance(config);
			
			System.out.println("Generator: " + generator.getClass().getSimpleName());
			//System.out.println("Minimizer: " + minimizer.getClass().getSimpleName());
			Set<TestCase> suite = generator.generateTestSuite();
	
			//Set<TestCase> minimizedSuite = minimizer.minimize(suite);
			Set<TestCase> minimizedSuite = suite;
			List<GenericObjective> objectivesToRemove = new ArrayList<>();
			
			/*for (GenericObjective obj : objectivesToEvaluate) {
				if (obj instanceof PC_PairObjective && ((PC_PairObjective) obj).isSynthetic)
					objectivesToRemove.add(obj);
				else if (obj instanceof BranchObjective && ((BranchObjective) obj).isSynthetic)
					objectivesToRemove.add(obj);
			}
			objectivesToEvaluate.removeAll(objectivesToRemove);*/
			
			for (GenericObjective obj : branchChainObjectives) {
				if (obj instanceof PC_PairObjective && ((PC_PairObjective) obj).isSynthetic)
					objectivesToRemove.add(obj);
				else if (obj instanceof BranchObjective && ((BranchObjective) obj).isSynthetic)
					objectivesToRemove.add(obj);
			}
			branchChainObjectives.removeAll(objectivesToRemove);
			
			GenericCoverageCalculator calculator = new GenericCoverageCalculator(cfg, branchChainObjectives);
			
			calculator.calculateCoverage(minimizedSuite);
			
			// Print and write uncovered branch-chain objectives to file (include coverage counts)
			printUncoveredBCobjectives(calculator, branchChainObjectives, minimizedSuite);
			
			System.out.println("Size of objectivesToEvaluate: "+branchChainObjectives.size());
			System.out.println("-------------------------------------------------------");
			System.out.println("Minimized test cases: " + minimizedSuite.size());
			System.out.println("Objective coverage achieved: " + calculator.getObjectiveCoverage());
			//System.out.println("Branch coverage achieved: " + calculator.getBranchCoverage());
			//System.out.println("Statement coverage achieved: " + calculator.getBlockCoverage());
			System.out.println("-------------------------------------------------------");
			
			
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