package jmetal.operators.mutation;

import java.util.HashMap;
import java.util.List;

import it.unisa.ocelot.genetic.solutions.GenericSolution;
import jmetal.util.JMException;
import jmetal.util.PseudoRandom;
import jmetal.util.wrapper.XParam;

/**
 * This mutation operator provides a classic (specified) mutation with a certain probability, and, instead, provides
 * a mutation in which the candidate is substituted by a constant in the program.
 * It differs from ConstantMetaMutation in that it works on GenericSolutions. 
 * @author simone
 *
 */
@SuppressWarnings("serial")
public class GenericConstantMetaMutation extends Mutation {
	private double mutationProbability_;
	private double metaMutationProbability_;
	private List<Double> mutationElements_;
	private Mutation realOperator_;
	
	@SuppressWarnings("unchecked")
	public GenericConstantMetaMutation(HashMap<String, Object> pParameters) {
		super(pParameters);
		
		if (pParameters.get("probability") != null)
	  		mutationProbability_ = (Double) pParameters.get("probability") ;  		
	  	if (pParameters.get("metaMutationProbability") != null)
	  		metaMutationProbability_ = (Double) pParameters.get("metaMutationProbability") ;
	  	if (pParameters.get("mutationElements") != null)
	  		mutationElements_ = (List<Double>) pParameters.get("mutationElements") ;
	  	if (pParameters.get("realOperator") != null)
	  		realOperator_ = (Mutation)pParameters.get("realOperator");
	  	//we do not want to support *all* the possible jmetal mutation operators with
	  	//GenericSolutions, but luckily the only real mutation operator that is injected
	  	//when using this class is the GenericPolynomialMutationParams, which supports 
	  	//GenericSolutions, so we are safe.
	}
	
	public void doMutation(double probability, GenericSolution solution) throws JMException {        
		XParam x = new XParam(solution);		
		for (int var=0; var < x.getNumberOfDecisionVariables(); var++) {
			if (PseudoRandom.randDouble() <= metaMutationProbability_) {
				@SuppressWarnings("unused")
				double y = x.getValue(var);
				if (this.mutationElements_.size() > 0) {
					int randIndex = PseudoRandom.randInt(0, this.mutationElements_.size()-1);
					double element = this.mutationElements_.get(randIndex);
					if (element < x.getUpperBound(var) && element > x.getLowerBound(var))
					x.setValue(var, element);
				}
			} else {
				if (realOperator_ instanceof PolynomialMutation)
					((PolynomialMutation)this.realOperator_).doMutation(mutationProbability_, solution);
				else
					this.realOperator_.execute(solution);
			}
		} // for

	} // doMutation


	@Override
	public Object execute(Object object) throws JMException {
		GenericSolution solution = (GenericSolution)object;

		doMutation(mutationProbability_, solution);
		return solution;
	}
	
}
