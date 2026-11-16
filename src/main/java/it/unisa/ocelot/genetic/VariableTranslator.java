package it.unisa.ocelot.genetic;

import it.unisa.ocelot.c.types.CType;
import jmetal.core.Solution;
import jmetal.core.Variable;
import jmetal.encodings.variable.ArrayParameters;

/**
 * Translates a {@link Solution} to a format
 * that is suitable to pass as arguments to
 * C program execution.
 */
public final class VariableTranslator {
	private final Solution solution;

	public VariableTranslator(Solution pSolution) {
		this.solution = pSolution;
	}

	/**
	 * Returns a three-elements array:
	 * 1) 0 -> 0 -> Object[] -> Array of values 
	 * 2) 1 -> Object[][] -> Array of arrays
	 * 3) 2 -> 0 -> Object[] -> Pointers
	 * @param pTypes
	 * @return
	 */
	public Object[][][] translateArray(CType[] pTypes) {
		int numberOfPointers = 0;
		for (int i = 0; i < pTypes.length; i++) {
			if (pTypes[i].isPointer()) {
				numberOfPointers++;
			}
		}

		// Splits the types in two partitions: value types and pointer types 
		// (keeps the original order)
		CType[] valuesTypes = new CType[pTypes.length - numberOfPointers];
		CType[] pointerTypes = new CType[numberOfPointers];
		int valueId = 0;
		int pointerId = 0;
		for (int i = 0; i < pTypes.length; i++)
			if (pTypes[i].isPointer()) {
				pointerTypes[pointerId] = pTypes[i];
				pointerId++;
			} else {
				valuesTypes[valueId] = pTypes[i];
				valueId++;
			}

		int numberOfVariables = this.solution.getDecisionVariables().length;
		
		//Sets up the three parameters return types
		Object[][][] result = new Object[3][][];
		result[0] = new Object[1][];
		result[1] = new Object[numberOfVariables - 2][];
		result[2] = new Object[1][];

		//Sets the array of values (first parameter)
		Variable variable = this.solution.getDecisionVariables()[0];
		if (variable.getVariableType().equals(ArrayParameters.class)) {
			ArrayParameters array = (ArrayParameters) variable;

			Object[] arguments = new Object[array.array_.length];
			for (int i = 0; i < array.array_.length; i++) {
				if (i < valuesTypes.length) {
					arguments[i] = valuesTypes[i].convert(array.array_[i]); //Variable values
				} else {
					throw new RuntimeException("Not enough value types given!!");
				}
			}

			result[0][0] = arguments;
		}

		//Sets the array of arrays (second parameter)
		for (int j = 1; j < numberOfVariables-1 ; j++) {
			variable = this.solution.getDecisionVariables()[j];
			if (variable.getVariableType().equals(ArrayParameters.class)) {
				ArrayParameters array = (ArrayParameters)variable;

				Object[] arguments = new Object[array.array_.length];
				for (int i = 0; i < array.array_.length; i++) {
					if (j-1 < pointerTypes.length) {
						arguments[i] = pointerTypes[j-1].convert(array.array_[i]); //Array values
					} else {
						throw new RuntimeException("Not enough pointer types given!!");
					}
				}

				result[1][j-1] = arguments;
			}
		}

		//Sets the array of pointers (third parameter)
		variable = this.solution.getDecisionVariables()[numberOfVariables-1];
		if (variable.getVariableType().equals(ArrayParameters.class)) {
			ArrayParameters array = (ArrayParameters)variable;

			Object[] arguments = new Object[array.array_.length];
			for (int i = 0; i < array.array_.length; i++) {
				arguments[i] = Integer.valueOf(array.array_[i].intValue()); //Pointer table values
			}

			result[2][0] = arguments;
		}

		return result;
	}

	@Override
	public String toString() {
		return "VariableTranslator [solution=" + solution + "]";
	}
}
