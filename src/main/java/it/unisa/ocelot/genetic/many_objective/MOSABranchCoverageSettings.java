package it.unisa.ocelot.genetic.many_objective;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.algorithms.MOSA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.experiments.Settings;
import jmetal.operators.crossover.CrossoverFactory;
import jmetal.operators.mutation.ConstantMetaMutation;
import jmetal.operators.mutation.MutationFactory;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;
import jmetal.util.MOSADominanceComparator;

/**
 * Settings for the Branch Coverage problem solved using the MOSA algorithm
 * proposed by Panichella et al in
 * "Reformulating Branch Coverage as a Many-Objective Optimization Problem"
 * 
 * @author giograno
 *
 */
//obsoleted by MOSASettings
@Deprecated
public final class MOSABranchCoverageSettings extends Settings {
	private Set<LabeledEdge> targets; //necessary for creating the genetic operators
	private List<Double> numericConstants; //necessary for creating the genetic operators
	private double crossoverProbability;
	private double mutationProbability;
	private int populationSize;
	private int maxEvaluations;
	private int maxGenerations;
	private double maxCoverage;
	private int maxTime;
	

	/**
	 * Constructor with specified parameters
	 * 
	 * @param pProblem MOSABranchCoverageProblem to configure
	 * @param pConstantsNumeric
	 * @param pConfigManager configuration taken by config file
	 */
	public MOSABranchCoverageSettings(MOSABranchCoverageProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		this.problem_ = pProblem;
		
		this.targets = new HashSet<>(pProblem.getTargets()); //safety copy
		
		this.numericConstants = new ArrayList<>(pConstantsNumeric); //safety copy

		try {
			this.crossoverProbability = pConfigManager.getCrossoverProbability();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.crossoverProbability = 0.9;
		}
			
		double mutationProbabilityDefault = 1.0 / pProblem.getNumberOfVariables();
		try {
			this.mutationProbability = pConfigManager.getMutationProbability();
			if (this.mutationProbability < 0) {
				//fall back on default (not calculated by config manager!)
				this.mutationProbability = mutationProbabilityDefault;
			}
		} catch (NumberFormatException e) {
			//fall back on default (not calculated by config manager!)
			this.mutationProbability = mutationProbabilityDefault;
		}
		
		try {
			this.populationSize = pConfigManager.getPopulationSize();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.populationSize = 100;
		}
		
		try {
			this.maxEvaluations = pConfigManager.getEvaluationsMax();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.maxEvaluations = 0;
		}
		
		try {
			this.maxGenerations = pConfigManager.getAlgorithmGenerations();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.maxGenerations = 0;
		}
		
		try {
			this.maxCoverage = pConfigManager.getRequiredCoverage();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.maxCoverage = 1.0;
		}
		
		try {
			this.maxTime = pConfigManager.getAlgorithmTime();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			this.maxTime = 0;
		}
		
		//TODO this.threads = 1;
	}

	@Override
	public Algorithm configure() throws JMException {
		// Create the algorithm
		Algorithm algorithm = new MOSA((MOSABranchCoverageProblem) this.problem_);
		
		// Parameters for operators creation
		HashMap<String, Object> parameters;
		
		// Selection Operator
		parameters = new HashMap<>();
		Set<LabeledEdge> targetsSafetyCopy = new HashSet<>(this.targets);
		parameters.put("comparator", new MOSADominanceComparator(targetsSafetyCopy));
		Operator selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

		// Crossover operator
		parameters = new HashMap<>();
		parameters.put("probability", this.crossoverProbability);
		Operator crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);

		// Mutation operator
		parameters = new HashMap<>();
		parameters.put("probability", mutationProbability);
		parameters.put("realOperator", MutationFactory.getMutationOperator("PolynomialMutation", parameters));
		parameters.put("metaMutationProbability", this.mutationProbability / 4);
		List<Double> mutationElements = new ArrayList<>(this.numericConstants); //safety copy
		parameters.put("mutationElements", mutationElements);
		Operator mutation = new ConstantMetaMutation(parameters);

		// Add the operators to the algorithm
		algorithm.addOperator("selection", selection);
		algorithm.addOperator("crossover", crossover);
		algorithm.addOperator("mutation", mutation);

		// Add numeric parameters
		algorithm.setInputParameter("populationSize", this.populationSize);
		algorithm.setInputParameter("maxEvaluations", this.maxEvaluations);
		algorithm.setInputParameter("maxGenerations", this.maxGenerations);
		algorithm.setInputParameter("maxCoverage", this.maxCoverage);
		algorithm.setInputParameter("maxTime", this.maxTime);
		
		return algorithm;
	}
}
