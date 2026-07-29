package jmetal.operators.mutation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import it.unisa.ocelot.genetic.solutions.GenericSolution;
import jmetal.core.Variable;
import jmetal.encodings.solutionType.ArrayParametersSolutionType;
import jmetal.encodings.solutionType.ArrayRealSolutionType;
import jmetal.encodings.solutionType.RealSolutionType;
import jmetal.util.Configuration;
import jmetal.util.JMException;

/**
 * This class implements a polynomial mutation operator with metamutation.  
 * It differs from GenericPolynomialMutation in that it works on GenericSolutions.
 */
@SuppressWarnings("serial")
public class GenericGenericPolynomialMutation extends Mutation {
	private static final double ETA_M_DEFAULT_ = 20.0;
	private final double eta_m_ = ETA_M_DEFAULT_;

	private Double mutationProbability_ = null;
	@SuppressWarnings("unused")
	private Double distributionIndex_ = eta_m_;
	
	private GenericConstantMetaMutation mutation;

	/**
	 * Valid solution types to apply this operator
	 */
	@SuppressWarnings("rawtypes")
	private static final List VALID_TYPES = Arrays.asList(
			RealSolutionType.class, ArrayRealSolutionType.class, ArrayParametersSolutionType.class);

	/**
	 * Constructor Creates a new instance of the polynomial mutation operator
	 */
	public GenericGenericPolynomialMutation(HashMap<String, Object> parameters) {
		super(parameters);
		if (parameters.get("probability") != null)
			mutationProbability_ = (Double) parameters.get("probability");
		if (parameters.get("distributionIndex") != null)
			distributionIndex_ = (Double) parameters.get("distributionIndex");
		
		mutation = new GenericConstantMetaMutation(parameters);
	} // PolynomialMutation

	/**
	 * Perform the mutation operation
	 * 
	 * @param probability
	 *            Mutation probability
	 * @param solution
	 *            The solution to mutate
	 * @throws JMException
	 */
	public void doMutation(double probability, GenericSolution solution)
			throws JMException {
		int numberOfVariables = solution.getDecisionVariables().length;
		
		Variable[] decisionVariables = new Variable[numberOfVariables];
		
		for (int i = 0; i < numberOfVariables; i++) {
			GenericSolution realSolution = new GenericSolution(solution);
			
			Variable[] variables = new Variable[] {solution.getDecisionVariables()[i]};
			realSolution.setDecisionVariables(variables);
			
			mutation.doMutation(probability, realSolution);
			
			decisionVariables[i] = variables[0];
		}
				
		solution.setDecisionVariables(decisionVariables);
	} // doMutation

	/**
	 * Executes the operation
	 * 
	 * @param object
	 *            An object containing a solution
	 * @return An object containing the mutated solution
	 * @throws JMException
	 */
	@SuppressWarnings("rawtypes")
	public Object execute(Object object) throws JMException {
		GenericSolution solution = (GenericSolution) object;

		if (!VALID_TYPES.contains(solution.getType().getClass())) {
			Configuration.logger_
					.severe("GenericGenericPolynomialMutation.execute: the solution "
							+ "type " + solution.getType()
							+ " is not allowed with this operator");

			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		doMutation(mutationProbability_, solution);
		return solution;
	} // execute
}
