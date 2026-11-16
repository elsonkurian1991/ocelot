//  ArrayReal.java
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

package jmetal.encodings.variable;

import static jmetal.util.PseudoRandom.randDouble;

import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Variable;
import jmetal.util.Configuration;
import jmetal.util.JMException;

/**
 * Class implementing a decision encodings.variable representing an array of
 * real values. The real values of the array have their own bounds.
 */
public final class ArrayParameters extends ArrayReal {
	private static final long serialVersionUID = 5439330919895497946L;
	
	private final int id;
	private final StandardProblem standardProblem; //same as the inherited this._problem, but with more precise type

	/**
	 * Constructor.
	 * 
	 * @param pId an {@code int} id for this object.
	 * @param pSize size of the array.
	 * @param pStandardProblem a {@link StandardProblem}.
	 */
	public ArrayParameters(int pId, int pSize, StandardProblem pStandardProblem) {
		super(pSize, pStandardProblem);
		this.id = pId;
		this.standardProblem = pStandardProblem;		

		// Initializes the array with (pseudo)random values
		// within the limits
		for (int i = 0; i < pSize; i++) {
			this.array_[i] = randDouble()
					* (this.standardProblem.getUpperLimit(i) - this.standardProblem.getLowerLimit(i))
					+ this.standardProblem.getLowerLimit(i);
		}
	}

	/**
	 * Copy constructor.
	 *
	 * @param other the ArrayParameters to copy.
	 */
	private ArrayParameters(ArrayParameters other) {
		this(other.id, other.array_.length, other.standardProblem.setLimits(other.id));
		System.arraycopy(other.array_, 0, this.array_, 0, other.array_.length);
	}

	@Override
	public Variable deepCopy() {
		return new ArrayParameters(this);
	}

	/**
	 * Gets the lower bound of a value.
	 *
	 * @param index The index of the value
	 * @return the lower bound
	 */
	@Override
	public double getLowerBound(int index) throws JMException {
		int size = getLength();
		if ((index >= 0) && (index < size)) {
			return this.standardProblem.getLowerBound(this.id, index);
		} else {
			String message = ArrayParameters.class
					+ ".getLowerBound(): index value (" + index
					+ ") invalid";
			Configuration.logger_.severe(message);
			throw new JMException(message);
		}
	}

	/**
	 * Gets the upper bound of a value.
	 *
	 * @param index the index of the value.
	 * @return the upper bound
	 */
	@Override
	public double getUpperBound(int index) throws JMException {
		int size = getLength();
		if (index >= 0 && index < size) {
			return this.standardProblem.getUpperBound(this.id, index);
		} else {
			String message = ArrayParameters.class
					+ ".getUpperBound(): index value (" + index
					+ ") invalid";
			Configuration.logger_.severe(message);
			throw new JMException(message);
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		int size = getLength();
		for (int i = 0; i < size - 1; i++) {
			result.append(this.array_[i].toString());
			result.append(' ');
		}
		result.append(this.array_[size - 1].toString());
		return result.toString();
	}
}
