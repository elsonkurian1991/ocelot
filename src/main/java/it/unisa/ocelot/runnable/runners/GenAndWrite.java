package it.unisa.ocelot.runnable.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.output.TeeOutputStream;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.types.CTypeHandler;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.BranchManager;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PCPairObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairsManager;
import it.unisa.ocelot.simulator.CBridge;
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
			String formattedDateTime = now.format(formatter);
			FileOutputStream fos = new FileOutputStream(config.getOutputFolder() + "exp_res_" + formattedDateTime + ".txt");
			TeeOutputStream myOut = new TeeOutputStream(System.out, fos);
			PrintStream ps = new PrintStream(myOut);
			System.setOut(ps);
	
			// Builds the CFG and sets the target
			CFG cfg = CFGBuilder.build(config.getTestFilename(), config.getTestFunction());

			// Emits some stats on the CFG
			System.out.println();
			System.out.println("===== CFG features =====");
			int mcCabePaths = cfg.edgeSet().size() - cfg.vertexSet().size() + 1;
			System.out.println("Cyclomatic complexity: " + mcCabePaths);
			System.out.println("Number of edges: " + cfg.edgeSet().size());
			System.out.println("Number of vertices: " + cfg.vertexSet().size());
			System.out.println("========================");
			System.out.println();
	
			// Initializes the C bridge
			CTypeHandler typeHandler = new CTypeHandler(cfg.getParameterTypes());
			CBridge.initialize(
					typeHandler.getValues().size(), 
					typeHandler.getPointers().size(),
					typeHandler.getPointers().size());
				
			// Loads the objectives to optimize on
			Set<? extends GenericObjective> objectives;
			if (config.getOptimizeFor().equals("Pairs")) {
				objectives = PC_PairsManager.loadObjectives();	
			} else if (config.getOptimizeFor().equals("Branches")) {
				objectives = BranchManager.loadObjectives();
			} else {
				throw new RuntimeException("Don't know what you are optimizing for");
			}
			
			// Loads the objectives to evaluate on
			Predicate<GenericObjective> notSynthetic = Predicate.not(obj -> (obj instanceof PCPairObjective && ((PCPairObjective) obj).isSynthetic));
			Set<? extends GenericObjective> objectivesToEvaluate;
			if (config.getEvaluateOn().equals("Pairs")) {
				objectivesToEvaluate = PC_PairsManager.loadObjectives().stream().filter(notSynthetic).collect(Collectors.toSet());	
			} else if (config.getEvaluateOn().equals("Branches")) {
				objectivesToEvaluate = BranchManager.loadObjectives().stream().filter(notSynthetic).collect(Collectors.toSet());
			} else {
				throw new RuntimeException("Don't know what you are evaluating for");
			}
			
			System.out.println("Running generator: " + config.getTestSuiteGenerator());
			TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, objectives);

			TestSuiteMinimizer<?> minimizer = null;
			if (config.isExperimentMinimization()) {
				GenericCoverageCalculator coverageCalculator = new GenericCoverageCalculator(objectives);
				minimizer = TestSuiteMinimizerHandler.getInstance(config, coverageCalculator);
			}

			// Generates the test suite
			Set<TestCase> suite = generator.generateTestSuite();
	
			// Possibly minimizes the test suite
			Set<TestCase> minimizedSuite = (config.isExperimentMinimization() ? minimizer.minimize(suite) : suite);

			GenericCoverageCalculator coverageCalculatorEvaluate = new GenericCoverageCalculator(objectivesToEvaluate);		
			coverageCalculatorEvaluate.updateCoverage(minimizedSuite);
	
			System.out.println("-------------------------------------------------------");
			System.out.println("Generated test cases: " + suite.size());
			System.out.println("Minimized test cases: " + minimizedSuite.size());
			System.out.println("Objective coverage achieved: " + coverageCalculatorEvaluate.getCoverage());
			System.out.print("Objectives still uncovered: ");			
			System.out.println(coverageCalculatorEvaluate.getUncovered());
			System.out.println("-------------------------------------------------------");
			
			//Emits the test suite
			String formattedFilename = config.getTestFilename().replaceAll("[^A-Za-z0-9]", "_");
			String filename = config.getOutputFolder() + "/" + "_Test_" + config.getTestFunction() + "_" + formattedFilename + ".c";
			System.out.println("Writing test suite on " + filename + "...");
			TestFramework framework = new TestFramework(new CheckFactory());
			String content = framework.writeTestSuite(minimizedSuite, cfg, config);
			Utils.writeFile(filename, content);
			
			System.out.println();
			System.out.println("Operation completed!");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
