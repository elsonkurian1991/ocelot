package it.unisa.ocelot.runnable.runners;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.cfg.CFGWindow;
import it.unisa.ocelot.c.types.CTypeHandler;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.BranchManager;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PCPairObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairsManager;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.GenericCoverageCalculator;
import it.unisa.ocelot.suites.TestSuiteGenerationException;
import it.unisa.ocelot.suites.benchmarks.BenchmarkCalculator;
import it.unisa.ocelot.suites.benchmarks.EvaluationBenchmarkCalculator;
import it.unisa.ocelot.suites.benchmarks.TimeBenchmarkCalculator;
import it.unisa.ocelot.suites.generators.TestSuiteGenerator;
import it.unisa.ocelot.suites.generators.TestSuiteGeneratorHandler;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizer;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizerHandler;
import it.unisa.ocelot.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.output.TeeOutputStream;
import org.eclipse.core.runtime.CoreException;

public class ExecuteWholeCoverage {
	public void run() throws IOException, CoreException, TestSuiteGenerationException {
		ConfigManager config = ConfigManager.getInstance();

		// Sets up the output file
		File outputDirectory = new File(config.getOutputFolder());
		outputDirectory.mkdirs();
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		String formattedDateTime = now.format(formatter);
		FileOutputStream fos = new FileOutputStream(config.getOutputFolder() + "exp_res_" + formattedDateTime+".txt");
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

		if (config.getUI()) {
			showUI(cfg);
		}

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

		// Builds the output folder
		String folderPath = config.getResultsFolder() + "/" + config.getTestFunction() + "/";
		File folder = new File(folderPath);
		folder.mkdirs();

		System.out.println("Running generator: " + config.getTestSuiteGenerator());
		TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, objectives);

		TestSuiteMinimizer<?> minimizer = null;
		if (config.isExperimentMinimization()) {
			GenericCoverageCalculator coverageCalculator = new GenericCoverageCalculator(objectives);
			minimizer = TestSuiteMinimizerHandler.getInstance(config, coverageCalculator);
		}

		// Makes all the benchmarks for test suite generation
		BenchmarkCalculator<?> timeBenchmark = new TimeBenchmarkCalculator();
		//BenchmarkCalculator<?> coverageBenchmark = new BranchCoverageBenchmarkCalculator(cfg);
		//BenchmarkCalculator<?> sizeBenchmark = new TestSuiteSizeBenchmarkCalculator(cfg);
		BenchmarkCalculator<?> evaluationsBenchmark = new EvaluationBenchmarkCalculator();

		generator.addBenchmark(timeBenchmark);
		//generator.addBenchmark(coverageBenchmark);
		//generator.addBenchmark(sizeBenchmark);
		generator.addBenchmark(evaluationsBenchmark);

		ArrayList<BenchmarkCalculator<?>> benchmarks = new ArrayList<>();
		benchmarks.add(timeBenchmark);
		//benchmarks.add(coverageBenchmark);
		//benchmarks.add(sizeBenchmark);
		benchmarks.add(evaluationsBenchmark);

		// Generates the test suite
		Set<TestCase> suite = generator.generateTestSuite();
		
		// Shuts down the C bridge
		CBridge.shutdown();

		// Possibly minimizes the test suite
		Set<TestCase> minimizedSuite = (config.isExperimentMinimization() ? minimizer.minimize(suite) : suite);

		// Exports the benchmarks to files
		String[] parts = config.getTestFilename().split("[./]");
		String preFilename = parts[parts.length - 2]
				+ "_" + config.getTestFunction() 
				+ "_" + config.getTestSuiteGenerator();
		Utils.exportBenchmarksCSV(folderPath + preFilename, benchmarks);

		if (config.getPrintResults()) {
			preFilename = config.getTestFunction() + "_" + config.getTestSuiteGenerator();

			String cumulativeResult = "";
			cumulativeResult += timeBenchmark.getPrintableCumulativeResults() + "\n";
			//cumulativeResult += coverageBenchmark.getPrintableCumulativeResults() + "\n";
			//cumulativeResult += sizeBenchmark.getPrintableCumulativeResults() + "\n";
			cumulativeResult += evaluationsBenchmark.getPrintableCumulativeResults() + "\n";

			Utils.writeFile(folderPath + "/" + preFilename + "_cumulative.txt", cumulativeResult);

			String result = "";
			result += timeBenchmark.getPrintableResults() + "\n";
			//result += coverageBenchmark.getPrintableResults() + "\n";
			//result += sizeBenchmark.getPrintableResults() + "\n";
			result += evaluationsBenchmark.getPrintableResults() + "\n";
			Utils.writeFile(folderPath + "/" + preFilename + "_normal.txt", result);
		}

		// Prints the benchmarks
		System.out.println(timeBenchmark.getPrintableCumulativeResults());
		//System.out.println(coverageBenchmark.getPrintableCumulativeResults());
		//System.out.println(sizeBenchmark.getPrintableCumulativeResults());
		System.out.println(evaluationsBenchmark.getPrintableCumulativeResults());

		// Evaluates the coverage
		GenericCoverageCalculator coverageCalculatorEvaluate = new GenericCoverageCalculator(objectivesToEvaluate);
		coverageCalculatorEvaluate.updateCoverage(minimizedSuite);

		// Prints the test suite stats
		System.out.println("-------------------------------------------------------");
		System.out.println("Generated test cases: " + suite.size());
		System.out.println("Minimized test cases: " + minimizedSuite.size());
		System.out.println("Objective coverage achieved: " + coverageCalculatorEvaluate.getCoverage());
		System.out.print("Objectives still uncovered: ");			
		System.out.println(coverageCalculatorEvaluate.getUncovered());
		System.out.println("-------------------------------------------------------");
		System.out.println((config.isExperimentMinimization() ? "Minimized t" : "T") + "est suite:");
		for (TestCase tc : minimizedSuite) {
			System.out.println("######### Test Case #########");
			System.out.print("Covered objectives: ");
			System.out.println(tc.getCoveredObjectives());
			System.out.println("Test arguments: ");
			System.out.println(Utils.printArguments(tc.getArguments()));
		}
		System.out.println("-----------------------------------------------------");

		System.out.println();
		System.out.println("Operation completed!");
	}

	public void showUI(final CFG pCFG) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				CFGWindow window = new CFGWindow(pCFG);
				window.setVisible(true);
			}
		});
	}
}
