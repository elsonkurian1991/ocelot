package it.unisa.ocelot.suites.generators;

import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.suites.generators.cdg.TestSuiteGeneratorCDG;
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
	
	public static TestSuiteGenerator getInstance(ConfigManager pConfigManager, CFG pCFG, Set<? extends GenericObjective> pObjectives) {
		return getInstance(pConfigManager.getTestSuiteGenerator(), pConfigManager, pCFG, pObjectives);
	}
	
	public static TestSuiteGenerator getInstance(String pName, ConfigManager pConfigManager, CFG pCFG, Set<? extends GenericObjective> pObjectives) {
		if (MCCABE_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new McCabeTestSuiteGenerator(pConfigManager, pCFG);
		} else if (ALL_EDGES_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new SingleTargetTestSuiteGenerator(pConfigManager, pCFG);
		} else if (MEMORY_EDGES_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new MemoryEdgeTestSuiteGenerator(pConfigManager, pCFG);
		} else if (REDUCED_MCCABE_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new ReducedMcCabeTestSuiteGenerator(pConfigManager, pCFG);
		} else if (REDUCED_MCCABE_PARTIALS_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new ReducedMcCabePartialsTestSuiteGenerator(pConfigManager, pCFG);
		} else if (RANDOM_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new RandomTestSuiteGenerator(pConfigManager, pCFG);
		} else if (MOSA_TEST_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new MOSATestSuiteGenerator(pConfigManager, pCFG);
		//LUCA added the following
		} else if (GENERIC_MOSA_TEST_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new GenericMOSATestSuiteGenerator(pConfigManager, pCFG, pObjectives);
		} else if (CDG_BASED_APPROACH_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new TestSuiteGeneratorCDG(pConfigManager, pCFG);
		} else if (REDUCED_MOSA_TEST_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new ReducedMOSATestSuiteGenerator(pConfigManager, pCFG);
		} else if (DYNAMIC_MCCABE_SUITE_GENERATOR.equalsIgnoreCase(pName)) {
			return new DynamicMcCabeTestSuiteGenerator(pConfigManager, pCFG);
		//TODO remove minimizer (it is not really a test suite generator!!!)
		} else if (MINIMIZER.equalsIgnoreCase(pName)) {
			return TestSuiteMinimizerHandler.getInstance(pConfigManager, null); //TODO null
		//TODO remove cascade (we do not have a coverage calculator!) 
		} else if (CASCADE_APPROACH.equalsIgnoreCase(pName)) {
			TestSuiteGeneratorCascade<?> cascadeGenerator = new TestSuiteGeneratorCascade<>(pConfigManager, null); //TODO replace null!
			
			for (String nameComponent : pConfigManager.getCascadeGenerators()) {
				if (CASCADE_APPROACH.equalsIgnoreCase(nameComponent)) {
					throw new RuntimeException("Cannot build a test suite generator that is a cascade of cascades!");
				}
				
				TestSuiteGenerator generator = TestSuiteGeneratorHandler.getInstance(nameComponent, pConfigManager, pCFG, pObjectives);
				if (generator instanceof CascadeableTestSuiteGenerator) {
					cascadeGenerator.addTestSuiteGenerator((CascadeableTestSuiteGenerator) generator);
				} else {
					throw new RuntimeException("Tried to build a cascade test generator, but one of the components is not cascadeable (" + nameComponent +")");
				}
			}
			
			return cascadeGenerator;
		}
		
		return null;
	}
}
