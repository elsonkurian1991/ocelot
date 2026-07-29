//  SBXCrossover.java
//
//  Author:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//       Juan J. Durillo <durillo@lcc.uma.es>
//
//  Copyright (c) 2011 Antonio J. Nebro, Juan J. Durillo
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.operators.crossover;

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
 * This class allows to apply a SBX crossover operator using two parent
 * solutions. It differs from SBXGenericCrossover in that it works on 
 * GenericSolutions.
 */
@SuppressWarnings("serial")
public class GenericSBXGenericCrossover extends Crossover {
	/**
	 * EPS defines the minimum difference allowed between real values
	 */
	@SuppressWarnings("unused")
	private static final double EPS = 1.0e-14;

	private static final double ETA_C_DEFAULT_ = 20.0;
	private Double crossoverProbability_ = 0.9;
	@SuppressWarnings("unused")
	private double distributionIndex_ = ETA_C_DEFAULT_;
	private GenericSBXCrossoverParams crossover;

	/**
	 * Valid solution types to apply this operator
	 */
	@SuppressWarnings("rawtypes")
	private static final List VALID_TYPES = Arrays.asList(
			RealSolutionType.class, ArrayRealSolutionType.class, ArrayParametersSolutionType.class);

	/**
	 * Constructor Create a new SBX crossover operator whit a default index
	 * given by <code>DEFAULT_INDEX_CROSSOVER</code>
	 */
	public GenericSBXGenericCrossover(HashMap<String, Object> parameters) {
		super(parameters);

		if (parameters.get("probability") != null)
			crossoverProbability_ = (Double) parameters.get("probability");
		if (parameters.get("distributionIndex") != null)
			distributionIndex_ = (Double) parameters.get("distributionIndex");
		
		this.crossover = new GenericSBXCrossoverParams(parameters);
		
	} // SBXCrossover

	/**
	 * Perform the crossover operation.
	 * 
	 * @param probability
	 *            Crossover probability
	 * @param parent1
	 *            The first parent
	 * @param parent2
	 *            The second parent
	 * @return An array containing the two offsprings
	 */
	public GenericSolution[] doCrossover(double probability, GenericSolution parent1,
			GenericSolution parent2) throws JMException {
		int numberOfVariables = parent1.getDecisionVariables().length;
		
		Variable[] decisionVariables1 = new Variable[numberOfVariables];
		Variable[] decisionVariables2 = new Variable[numberOfVariables];
		
		for (int i = 0; i < numberOfVariables; i++) {
			GenericSolution realParent1 = new GenericSolution(parent1);
			GenericSolution realParent2 = new GenericSolution(parent2);
			
			realParent1.setDecisionVariables(new Variable[] {parent1.getDecisionVariables()[i]});
			realParent2.setDecisionVariables(new Variable[] {parent2.getDecisionVariables()[i]});
			
			GenericSolution[] solutions = crossover.doCrossover(probability, realParent1, realParent2);
			decisionVariables1[i] = solutions[0].getDecisionVariables()[0];
			decisionVariables2[i] = solutions[1].getDecisionVariables()[0];
		}
		
		GenericSolution resultSolution1 = new GenericSolution(parent1);
		GenericSolution resultSolution2 = new GenericSolution(parent2);
		
		resultSolution1.setDecisionVariables(decisionVariables1);
		resultSolution2.setDecisionVariables(decisionVariables2);
		
		return new GenericSolution[] {resultSolution1, resultSolution2};
	} // doCrossover

	/**
	 * Executes the operation
	 * 
	 * @param object
	 *            An object containing an array of two parents
	 * @return An object containing the offSprings
	 */
	@SuppressWarnings("rawtypes")
	public Object execute(Object object) throws JMException {
		GenericSolution[] parents = (GenericSolution[]) object;

		if (parents.length != 2) {
			Configuration.logger_
					.severe("GenericSBXGenericCrossover.execute: operator needs two "
							+ "parents");
			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		if (!(VALID_TYPES.contains(parents[0].getType().getClass()) && VALID_TYPES
				.contains(parents[1].getType().getClass()))) {
			Configuration.logger_.severe("GenericSBXGenericCrossover.execute: the solutions "
					+ "type " + parents[0].getType()
					+ " is not allowed with this operator");

			Class cls = java.lang.String.class;
			String name = cls.getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		GenericSolution[] offSpring;
		offSpring = doCrossover(crossoverProbability_, parents[0], parents[1]);

		// for (int i = 0; i < offSpring.length; i++)
		// {
		// offSpring[i].setCrowdingDistance(0.0);
		// offSpring[i].setRank(0);
		// }
		return offSpring;
	} // execute
} // SBXCrossover
