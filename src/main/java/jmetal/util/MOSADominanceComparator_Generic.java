package jmetal.util;

import it.unisa.ocelot.genetic.objectives.GenericObjective;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jmetal.core.Solution;

/**
 * This class implements a {@link Comparator}{@code <}{@link Solution}{@code >} 
 * based on the concept of Pareto Dominance, as proposed
 * by Panichella et al. in "Reformulating Branch Coverage as a Many-Objective
 * Optimization Problem", considering only the non-dominance relation for the
 * uncovered objectives.
 * 
 * @author giograno
 */
public class MOSADominanceComparator_Generic implements Comparator<Solution> {
	private final ArrayList<GenericObjective> objectives;

	/**
	 * Constructor.
	 * 
	 * @param objectives
	 */
	public MOSADominanceComparator_Generic(List<GenericObjective> objectives) {
		this.objectives = new ArrayList<>(objectives);
	}

	/**
	 * Compares two solutions.
	 * 
	 * @param solution1 the first {@link Solution}.
	 * @param solution2 the second {@link Solution}.
	 * @return -1, or 0, or 1 if {@code solution1} dominates {@code solution2}, 
	 *         neither one dominates the other, or {@code solution1} is 
	 *         dominated by {@code solution2}, respectively.
	 */
	public int compare(Solution solution1, Solution solution2) {
		if (solution1 == null) {
			return 1;
		} else if (solution2 == null) {
			return -1;
		}

		boolean dominate1 = false; // dominate1 indicates if some objective of solution1
						           // dominates the same objective in solution2. dominate2
		boolean dominate2 = false; // is the complementary of dominate1.

		int flag; // stores the result of the comparison

		// Equal number of violated constraints. Applying a dominance Test then
		double value1, value2;

		for (GenericObjective objective : this.objectives) {
			if (!objective.isActive()) {
				continue; //not relevant
			}
			
			value1 = solution1.getObjective(objective.getId());
			value2 = solution2.getObjective(objective.getId());

			if (value1 < value2) {
				flag = -1;
			} else if (value1 > value2) {
				flag = 1;
			} else {
				flag = 0;
			}

			if (flag == -1) {
				dominate1 = true;
			}

			if (flag == 1) {
				dominate2 = true;
			}
		}

		if (dominate1 == dominate2) {
			return 0; // No one dominates the other
		}
		if (dominate1) {
			return -1; // solution1 dominates
		}
		return 1; // solution2 dominates
	}
}
