package it.unisa.ocelot.genetic.settings;

import java.util.HashMap;
import java.util.List;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.core.Problem;
import jmetal.operators.crossover.GenericSBXGenericCrossover;
import jmetal.operators.mutation.GenericGenericPolynomialMutation;
import jmetal.operators.mutation.GenericPolynomialMutationParams;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;
import jmetal.util.parallel.IParallelEvaluator;
import jmetal.util.parallel.MultithreadedEvaluator;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.CDG_GA;
import it.unisa.ocelot.genetic.algorithms.GeneticAlgorithm;
import it.unisa.ocelot.genetic.edges.CDG_BasedProblem;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class GenericGASettings extends StandardSettings {
	public GenericGASettings(Problem pProblem) {
		super(pProblem);
	}
	
	public GenericGASettings(Problem pProblem, ConfigManager pConfig) {
		super(pProblem, pConfig);
	}
	
	public Algorithm configure(Algorithm algorithm) throws JMException {
        Operator selection;
        Operator crossover;
        Operator mutation;
        
        HashMap<String, Object> parameters;
        
        algorithm.setInputParameter("populationSize", populationSize);
        algorithm.setInputParameter("maxEvaluations", maxEvaluations);

        // Mutation and Crossover Permutation codification
        parameters = new HashMap<String, Object>();
        parameters.put("probability", crossoverProbability);
        //crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);
        crossover = new GenericSBXGenericCrossover(parameters);

        parameters = new HashMap<String, Object>();
        parameters.put("probability", mutationProbability);
        if (!this.useMetaMutator) {
	        mutation = new GenericPolynomialMutationParams(parameters);
        } else {
            parameters.put("realOperator", new GenericPolynomialMutationParams(parameters));
            parameters.put("metaMutationProbability", 0.001);
            List<Double> mutationElements = this.numericConstants;
            parameters.put("mutationElements", mutationElements);
            mutation = new GenericGenericPolynomialMutation(parameters);
        }

        // Selection Operator 
        parameters = null;
        selection = SelectionFactory.getSelectionOperator("BinaryTournament", parameters);

        // Add the operators to the algorithm
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("mutation", mutation);
        algorithm.addOperator("selection", selection);

        return algorithm;
	}
	
	public Algorithm configure() throws JMException {
		Algorithm algorithm;
		
		IParallelEvaluator parallelEvaluator = new MultithreadedEvaluator(threads);
		
		// Creating the problem
        if (problem_ instanceof CDG_BasedProblem)
        	algorithm = new CDG_GA(problem_, parallelEvaluator);
        else 
        	algorithm = new GeneticAlgorithm(problem_);
        
		return configure(algorithm);
    }
}
