//  PolynomialMutation.java
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

package jmetal.operators.mutation;

import static java.lang.Math.pow;
import static jmetal.util.PseudoRandom.randDouble;

import jmetal.core.Solution;
import jmetal.encodings.solutionType.ArrayParametersSolutionType;
import jmetal.util.Configuration;
import jmetal.util.JMException;
import jmetal.util.wrapper.XParam;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A polynomial mutation operator.
 */
public class PolynomialMutation extends Mutation {
	private static final long serialVersionUID = 3670094424664932307L;
	
	private static final double ETA_M_DEFAULT_ = 20.0;
	private static final double ETA_M_ = ETA_M_DEFAULT_;

	private Double mutationProbability_ = null;
	private Double distributionIndex_ = ETA_M_;

	/**
	 * Valid solution types to apply this operator
	 */
	@SuppressWarnings("rawtypes")
	private static final List VALID_TYPES = Arrays.asList(ArrayParametersSolutionType.class);

	/**
	 * Constructor.
	 */
	public PolynomialMutation(HashMap<String, Object> parameters) {
		super(parameters);
		
		if (parameters.get("probability") != null) {
			this.mutationProbability_ = (Double) parameters.get("probability");
		}
		if (parameters.get("distributionIndex") != null) {
			this.distributionIndex_ = (Double) parameters.get("distributionIndex");
		}
	}

	/**
	 * Perform the mutation operation
	 * 
	 * @param probability mutation probability.
	 * @param solution the {@link Solution} to mutate.
	 * @throws JMException
	 */
	public void doMutation(double probability, Solution solution) throws JMException {
		double rnd, delta1, delta2, mut_pow, deltaq;
		double y, yl, yu, val, xy;
		XParam x = new XParam(solution);
		for (int var = 0; var < x.getNumberOfDecisionVariables(); var++) {
			if (randDouble() <= probability) {
				y = 0;
				y = x.getValue(var);
				yl = x.getLowerBound(var);
				yu = x.getUpperBound(var);
				delta1 = (y - yl) / (yu - yl);
				delta2 = (yu - y) / (yu - yl);
				rnd = randDouble();
				mut_pow = 1.0 / (ETA_M_ + 1.0);
				if (rnd <= 0.5) {
					xy = 1.0 - delta1;
					val = 2.0 * rnd + (1.0 - 2.0 * rnd)
							* (pow(xy, (this.distributionIndex_ + 1.0)));
					deltaq = pow(val, mut_pow) - 1.0;
				} else {
					xy = 1.0 - delta2;
					val = 2.0
							* (1.0 - rnd)
							+ 2.0
							* (rnd - 0.5)
							* (pow(xy, (this.distributionIndex_ + 1.0)));
					deltaq = 1.0 - pow(val, mut_pow);
				}
				y = y + deltaq * (yu - yl);
				if (y < yl)
					y = yl;
				if (y > yu)
					y = yu;
				x.setValue(var, y);
			}
		}
	}

	/**
	 * Executes the operation
	 * 
	 * @param object
	 *            An object containing a solution
	 * @return An object containing the mutated solution
	 * @throws JMException
	 */
	public Object execute(Object object) throws JMException {
		Solution solution = (Solution) object;

		if (!VALID_TYPES.contains(solution.getType().getClass())) {
			Configuration.logger_
					.severe("PolynomialMutation.execute: the solution "
							+ "type " + solution.getType()
							+ " is not allowed with this operator");

			String name = getClass().getName();
			throw new JMException("Exception in " + name + ".execute()");
		} // if

		doMutation(mutationProbability_, solution);
		return solution;
	} // execute

} // PolynomialMutation
