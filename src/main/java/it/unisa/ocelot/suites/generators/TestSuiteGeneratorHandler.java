package it.unisa.ocelot.suites.generators;

import java.util.List;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.suites.generators.cdg.CDG_BasedApproachGenerator;
import it.unisa.ocelot.suites.generators.edge.MemoryEdgeTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.edge.SingleTargetTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.many_objective.GenericMOSATestSuiteGenerator;
import it.unisa.ocelot.suites.generators.many_objective.MOSATestSuiteGenerator;
import it.unisa.ocelot.suites.generators.many_objective.ReducedMOSATestSuiteGenerator;
import it.unisa.ocelot.suites.generators.mccabe.DynamicMcCabeTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.mccabe.McCabeTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.mccabe.ReducedMcCabePartialsTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.mccabe.ReducedMcCabeTestSuiteGenerator;
import it.unisa.ocelot.suites.generators.random.RandomTestSuiteGenerator;
import it.unisa.ocelot.suites.minimization.TestSuiteMinimizerHandler;

public class TestSuiteGeneratorHandler {
	public static final String RANDOM_SUITE_GENERATOR = "Random";
	
	public static final String ALL_EDGES_SUITE_GENERATOR = "AllEdges";
	public static final String MEMORY_EDGES_SUITE_GENERATOR = "MemoryEdges";
	
	public static final String MCCABE_SUITE_GENERATOR = "McCabe";
	public static final String REDUCED_MCCABE_SUITE_GENERATOR = "ReducedMcCabe";
	public static final String REDUCED_MCCABE_PARTIALS_SUITE_GENERATOR = "ReducedMcCabePartials";
	public static final String DYNAMIC_MCCABE_SUITE_GENERATOR = "DynamicMcCabe";
	//generator for only McCabe Path without considering non covered branches
	
	public static final String MOSA_TEST_SUITE_GENERATOR = "Mosa";
	public static final String REDUCED_MOSA_TEST_SUITE_GENERATOR = "ReducedMosa";
	
	public static final String CDG_BASED_APPROACH_SUITE_GENERATOR = "Harman";
	
	public static final String MINIMIZER = "Minimizer";
	
	public static final String CASCADE_APPROACH = "Cascade";
	
	public static final String GENERIC_MOSA_TEST_SUITE_GENERATOR = "GenericMosa";
	
	public static TestSuiteGenerator getInstance(ConfigManager pConfigManager, CFG pCFG, List<GenericObjective> objectives) {
		return getInstance(pConfigManager.getTestSuiteGenerator(), pConfigManager, pCFG, objectives);
	}
	
	public static TestSuiteGenerator getInstance(String name, ConfigManager pConfigManager, CFG pCFG, List<GenericObjective> objectives) {
		if (name.equalsIgnoreCase(MCCABE_SUITE_GENERATOR))
			return new McCabeTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(ALL_EDGES_SUITE_GENERATOR))
			return new SingleTargetTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(MEMORY_EDGES_SUITE_GENERATOR))
			return new MemoryEdgeTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(REDUCED_MCCABE_SUITE_GENERATOR))
			return new ReducedMcCabeTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(REDUCED_MCCABE_PARTIALS_SUITE_GENERATOR))
			return new ReducedMcCabePartialsTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(RANDOM_SUITE_GENERATOR))
			return new RandomTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(MOSA_TEST_SUITE_GENERATOR))
			return new MOSATestSuiteGenerator(pConfigManager, pCFG);
		//LUCA added the following
		else if (name.equalsIgnoreCase(GENERIC_MOSA_TEST_SUITE_GENERATOR))
			return new GenericMOSATestSuiteGenerator(pConfigManager, pCFG, objectives);
		else if (name.equalsIgnoreCase(CDG_BASED_APPROACH_SUITE_GENERATOR))
			return new CDG_BasedApproachGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(REDUCED_MOSA_TEST_SUITE_GENERATOR))
			return new ReducedMOSATestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(DYNAMIC_MCCABE_SUITE_GENERATOR))
			return new DynamicMcCabeTestSuiteGenerator(pConfigManager, pCFG);
		else if (name.equalsIgnoreCase(MINIMIZER))
			return TestSuiteMinimizerHandler.getInstance(pConfigManager);
		else if (name.equalsIgnoreCase(CASCADE_APPROACH)) {
			CascadeTestSuiteGenerator cascadeGenerator = new CascadeTestSuiteGenerator(pConfigManager,pCFG);
			
			for (String generatorID : pConfigManager.getCascadeGenerators()) {
				if (generatorID.equalsIgnoreCase(CASCADE_APPROACH))
					throw new RuntimeException("Loop of cascades!");
				
				TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(generatorID, pConfigManager, pCFG, objectives);
				cascadeGenerator.addTestSuiteGenerator(generator);
			}
			
			return cascadeGenerator;
		}
		
		return null;
	}
}
