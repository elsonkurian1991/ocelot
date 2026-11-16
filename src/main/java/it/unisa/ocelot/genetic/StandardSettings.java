package it.unisa.ocelot.genetic;

import it.unisa.ocelot.conf.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jmetal.core.Algorithm;
import jmetal.core.Operator;
import jmetal.experiments.Settings;
import jmetal.operators.crossover.SBXGenericCrossover;
import jmetal.operators.mutation.GenericPolynomialMutation;
import jmetal.operators.mutation.PolynomialMutation;
import jmetal.operators.selection.SelectionFactory;
import jmetal.util.JMException;

/**
 * The standard settings for an {@link OcelotAlgorithm}.
 * It gathers all the additional settings that all the
 * {@link OcelotAlgorithm} have w.r.t. a general
 * {@link Algorithm}. The values to these settings 
 * are read from a {@link ConfigManager}.
 */
public abstract class StandardSettings extends Settings {
    protected final List<Double> constantsNumeric; //TODO what about other non-numeric constants?
    protected final ConfigManager configManager;

    /**
     * Constructor.
     * 
     * @param pProblem a {@link StandardProblem}.
     * @param pConstantsNumeric the numeric constants, 
     *        a {@link List}{@code <}{@link Double}{@code >}. 
     * @param pConfigManager a {@link ConfigManager}.
     */
	public StandardSettings(StandardProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		this.problem_ = pProblem;
		this.constantsNumeric = new ArrayList<>(pConstantsNumeric); //safety copy
		this.configManager = pConfigManager;
	}

	@Override
	public final OcelotAlgorithm configure() throws JMException {
		// Delegate to subclass
		OcelotAlgorithm algorithm = doConfigure(); 
		
		// The parameters
	    double crossoverProbability;
	    double mutationProbability;
	    boolean useMetaMutator;
	    int populationSize;
	    int maxEvaluations;
	    boolean debug; //TODO move it where it is used
		
		// Read all the parameters from this.configManager
		try {
			crossoverProbability = this.configManager.getCrossoverProbability();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			crossoverProbability = 0.9;
		}
		
		if (this.configManager.isDynamicMutation()) { 
			//TODO what shall we do now? Check whether zero probability is ok.
			mutationProbability = 0d; //a silly default
		} else {
			//calculates the default mutation probability (NOT IN CONFIG MANAGER)
			//TODO can't this be done in the ConfigManager???
			double mutationProbabilityDefault = 1.0 / this.problem_.getNumberOfVariables();
			try {
				if (this.configManager.getMutationProbability() >= 0) {
					mutationProbability = this.configManager.getMutationProbability();
				} else {
					//fall back on default (NOT IN CONFIG MANAGER)
					mutationProbability = mutationProbabilityDefault;
				}
			} catch (NumberFormatException e) {
				//fall back on default (NOT IN CONFIG MANAGER)
				mutationProbability = mutationProbabilityDefault;
			}
		}
		
		useMetaMutator = this.configManager.isMetaMutatorEnabled();
		
		try {
			populationSize = this.configManager.getPopulationSize();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			populationSize = 100;
		}
		
		try {
			maxEvaluations = this.configManager.getEvaluationsMax();
		} catch (NumberFormatException e) {
			//fall back on default from ConfigManager
			maxEvaluations = 0;
		}

		debug = this.configManager.getDebug(); //TODO move it where it is used		

        // Selection (not externally configurable)
        Operator selection = SelectionFactory.getSelectionOperator("BinaryTournament", null);
        /* TODO this was the original MOSA selection operator from MOSABranchCoverageSettings:
        HashMap<String, Object> parameters = new HashMap<>();
		Set<LabeledEdge> targetsSafetyCopy = new HashSet<>(this.targets);
		parameters.put("comparator", new MOSADominanceComparator(targetsSafetyCopy));
		Operator selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);
		*/

        // Crossover
        HashMap<String, Object> parametersCrossover = new HashMap<>();
        parametersCrossover.put("probability", crossoverProbability);
        Operator crossover = new SBXGenericCrossover(parametersCrossover);
        /* TODO this was the original MOSA crossover operator from MOSABranchCoverageSettings:
        HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("probability", crossoverProbability);
		Operator crossover = CrossoverFactory.getCrossoverOperator("SBXCrossover", parameters);
		We cannot pass from the CrossoverFactory because SBXGenericCrossover is a custom class of ours.
		*/

        // Mutation
        HashMap<String, Object> parametersMutation = new HashMap<>();
        parametersMutation.put("probability", mutationProbability);
        Operator mutation;
        if (useMetaMutator) {
        	parametersMutation.put("realOperator", new PolynomialMutation(parametersMutation));
        	parametersMutation.put("metaMutationProbability", 0.001);
            parametersMutation.put("mutationElements", this.constantsNumeric);
            mutation = new GenericPolynomialMutation(parametersMutation);
        } else {
	        mutation = new PolynomialMutation(parametersMutation);
        }
        /* TODO this was the original MOSA mutation operator from MOSABranchCoverageSettings:
        HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("probability", mutationProbability);
		parameters.put("realOperator", MutationFactory.getMutationOperator("PolynomialMutation", parameters));
		parameters.put("metaMutationProbability", this.mutationProbability / 4);
		List<Double> mutationElements = new ArrayList<>(this.numericConstants); //safety copy
		parameters.put("mutationElements", mutationElements);
		Operator mutation = new ConstantMetaMutation(parameters);
		*/

        // Add the operators to the algorithm
        algorithm.addOperator("crossover", crossover);
        algorithm.addOperator("mutation", mutation);
        algorithm.addOperator("selection", selection);
        
		// Add the numeric constants to the algorithm
		//TODO is it really used by all the subclasses? I don't think so.
		algorithm.setInputParameter("constantsNumeric", this.constantsNumeric);
		
		// Add the scalar input parameters
        algorithm.setInputParameter("populationSize", populationSize);
        algorithm.setInputParameter("maxEvaluations", maxEvaluations);

        return algorithm;
	}
	
	protected abstract OcelotAlgorithm doConfigure() throws JMException;
}
