package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.AVM;
import it.unisa.ocelot.genetic.algorithms.CDG;
import it.unisa.ocelot.genetic.algorithms.GeneticAlgorithm;
import it.unisa.ocelot.genetic.algorithms.MOSA;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.algorithms.MemeticAlgorithm;
import it.unisa.ocelot.genetic.edges.ProblemTargetSingleEdgeCDG;
import it.unisa.ocelot.genetic.edges.ProblemTargetSingleEdge;
import it.unisa.ocelot.genetic.many_edges.ProblemTargetManyEdges;
import it.unisa.ocelot.genetic.many_nodes.ProblemTargetManyNodes;
import it.unisa.ocelot.genetic.many_objective.MOSABranchCoverageProblem;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.nodes.ProblemTargetSingleNode;
import it.unisa.ocelot.genetic.paths.ProblemTargetSinglePath;

public final class SettingsFactory {
	//algorithms
	public static final String ALGORITHM_GENETIC = GeneticAlgorithm.class.getName();
	public static final String ALGORITHN_MEMETIC = MemeticAlgorithm.class.getName();
	public static final String ALGORITHM_AVM = AVM.class.getName();
	public static final String ALGORITHM_CDG = CDG.class.getName();
	public static final String ALGORITHM_MOSA = MOSA.class.getName();
	public static final String ALGORITHM_MOSA_GENERIC = MOSA_Generic.class.getName();
	
	//problems
	public static final String PROBLEM_CDG = ProblemTargetSingleEdgeCDG.class.getName();
	public static final String PROBLEM_EDGE_COVERAGE_SINGLE = ProblemTargetSingleEdge.class.getName();
	public static final String PROBLEM_NODE_COVERAGE_SINGLE = ProblemTargetSingleNode.class.getName();
	public static final String PROBLEM_EDGE_COVERAGE_MULTI = ProblemTargetManyEdges.class.getName();
	public static final String PROBLEM_NODE_COVERAGE_MULTI = ProblemTargetManyNodes.class.getName();
	public static final String PROBLEM_PATH_COVERAGE_SINGLE = ProblemTargetSinglePath.class.getName();
	public static final String PROBLEM_BRANCHES_COVERAGE_MOSA = MOSABranchCoverageProblem.class.getName();
	public static final String PROBLEM_GENERIC_COVERAGE_MOSA = MOSAGenericCoverageProblem.class.getName();
	
	
	public static StandardSettings getSettings(ConfigManager pConfigManager, StandardProblem pProblem, List<Double> pConstantsNumeric) {
		final String algorithmName = pConfigManager.getAlgorithm();
		final String problemName = pProblem.getName();
		if (ALGORITHM_GENETIC.equalsIgnoreCase(algorithmName)) {
			return new GeneticSettings(pProblem, pConstantsNumeric, pConfigManager);
		} else if (ALGORITHN_MEMETIC.equalsIgnoreCase(algorithmName)){
			return new MemeticSettings(pProblem, pConstantsNumeric, pConfigManager);
		} else if (ALGORITHM_AVM.equalsIgnoreCase(algorithmName)) {
			return new AVMSettings(pProblem, pConstantsNumeric, pConfigManager);
		} else if (ALGORITHM_CDG.equalsIgnoreCase(algorithmName)) {
			//TODO shall we check that pProblem is a PROBLEM_CDG? Actually ALGORITHM_CDG seems to accept every StandardProblem. So what is PROBLEM_CDG for???
			return new CGDSettings(pProblem, pConstantsNumeric, pConfigManager);
		} else if (ALGORITHM_MOSA.equalsIgnoreCase(algorithmName)) {
			if (PROBLEM_BRANCHES_COVERAGE_MOSA.equals(problemName)) {
				return new MOSASettings((MOSABranchCoverageProblem) pProblem, pConstantsNumeric, pConfigManager);
				//note that the original Ocelot settings class for the MOSA algorithm were of class MOSABranchCoverageSettings! 
				//But in EvInT MOSASettings is the right class because it is conformant to EvInT customizations.
			} else {
				throw new RuntimeException("Wrong problem: the MOSA algorithm can accept only a MOSABranchCoverageProblem");
			}
		} else if (ALGORITHM_MOSA_GENERIC.equalsIgnoreCase(algorithmName)) {
			if (PROBLEM_GENERIC_COVERAGE_MOSA.equals(problemName)) {
				return new MOSASettingsGeneric((MOSAGenericCoverageProblem) pProblem, pConstantsNumeric, pConfigManager);
			} else {
				throw new RuntimeException("Wrong problem: the GenericMOSA algorithm can accept only a MOSAGenericCoverageProblem");
			}
		} else {
			//no algorithm name, or algorithm name unknown
			throw new RuntimeException("Wrong algorithm name: \"" + algorithmName + "\"");
		}
	}
}
