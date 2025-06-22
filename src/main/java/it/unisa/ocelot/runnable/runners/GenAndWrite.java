package it.unisa.ocelot.runnable.runners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.output.TeeOutputStream;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CFGBuilder;
import it.unisa.ocelot.c.types.CTypeHandler;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.BranchManager;
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
			if (config.getOptimizeFor().equals("Pairs"))
				 objectives = PC_PairsManager.loadObjectives();	
			else if (config.getOptimizeFor().equals("Branches"))
				objectives = BranchManager.loadObjectives(0);
			else
				throw new Exception("Don't know what you are optimizing for");
			List<GenericObjective> objectivesToEvaluate;
			if (config.getEvaluateOn().equals("Pairs"))
				objectivesToEvaluate = PC_PairsManager.loadObjectives();	
			else if (config.getOptimizeFor().equals("Branches"))
				objectivesToEvaluate = BranchManager.loadObjectives(0);
			else
				throw new Exception("Don't know what you are optimizing for");
			
			
			
			TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(config, cfg, objectives);
			//TestSuiteMinimizer minimizer = TestSuiteMinimizerHandler.getInstance(config);
			
			System.out.println("Generator: " + generator.getClass().getSimpleName());
			//System.out.println("Minimizer: " + minimizer.getClass().getSimpleName());
			Set<TestCase> suite = generator.generateTestSuite();
	
			//Set<TestCase> minimizedSuite = minimizer.minimize(suite);
			Set<TestCase> minimizedSuite = suite;
			List<GenericObjective> objectivesToRemove = new ArrayList<>();
			
			for (GenericObjective obj : objectivesToEvaluate) {
				if (obj instanceof PC_PairObjective && ((PC_PairObjective) obj).isSynthetic)
					objectivesToRemove.add(obj);
			}
			objectivesToEvaluate.removeAll(objectivesToRemove);
			GenericCoverageCalculator calculator = new GenericCoverageCalculator(cfg, objectivesToEvaluate);
			
			calculator.calculateCoverage(minimizedSuite);
	
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
}
