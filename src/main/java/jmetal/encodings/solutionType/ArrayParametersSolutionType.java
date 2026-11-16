package jmetal.encodings.solutionType;

import jmetal.core.SolutionType;
import jmetal.core.Variable;
import jmetal.encodings.variable.ArrayParameters;

import it.unisa.ocelot.genetic.StandardProblem;

/**
 * The {@link SolutionType} for {@link StandardProblem}s.
 */
public class ArrayParametersSolutionType extends SolutionType {
	private final StandardProblem standardProblem;
	
	public ArrayParametersSolutionType(StandardProblem pProblem) {
		super(pProblem);
		this.standardProblem = pProblem;
	}
	
	@Override
	public Variable[] createVariables() {
		Variable[] variables;

		//gets data
		int numberOfVariables = this.standardProblem.getNumberOfVariables();
		int numberOfArrayVariables = this.standardProblem.getNumberOfArrayVariables();
		int arraySize = this.standardProblem.getArraySize();
		
		//makes variables
		variables = new Variable[2 + numberOfArrayVariables];
		variables[0] = new ArrayParameters(0, numberOfVariables - numberOfArrayVariables - 1, this.standardProblem.setLimits(0));
		for (int i = 1; i <= numberOfArrayVariables; i++) {
			variables[i] = new ArrayParameters(i, arraySize, this.standardProblem.setLimits(i));
		}
		variables[numberOfArrayVariables + 1] = new ArrayParameters(numberOfArrayVariables + 1, numberOfArrayVariables, this.standardProblem.setLimits(numberOfArrayVariables + 1));

		return variables ;
	}
	
	@Override
	public Variable[] copyVariables(Variable[] vars) {
		Variable[] retVal = new Variable[vars.length];
		
		for (int i = 0; i < vars.length; i++) {
			retVal[i] = vars[i].deepCopy();
		}
		
		return retVal;
	} // copyVariables
} // ArrayRealSolutionType
