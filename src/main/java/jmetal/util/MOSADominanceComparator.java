package jmetal.util;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import jmetal.core.Solution;

/**
 * This class implements a {@link Comparator}{@code <}{@link Solution}{@code >}
 * based on the concept of Pareto Dominance, as proposed
 * by Panichella et al. in "Reformulating Branch Coverage as a Many-Objective
 * Optimization Problem", considering only the non-dominance relation for the
 * uncovered branches in a {@code MOSABranchCoverageProblem}.
 * 
 * @author giograno
 */
public class MOSADominanceComparator implements Comparator<Solution> {
	private final Set<LabeledEdge> targets;

	/**
	 * Constructor.
	 * 
	 * @param targets
	 */
	public MOSADominanceComparator(Set<LabeledEdge> targets) {
		this.targets = new HashSet<>(targets);
	}

	/**
	 * Compares two solutions.
	 * 
	 * @param solution1 the first {@link Solution}.
	 * @param solution2 the second {@link Solution}.
	 * @return -1, or 0, or 1 if solution1 dominates solution2, 
	 *         both are non-dominated, 
	 *         or solution1 is dominated by solution2,
	 *         respectively.
	 */
	public int compare(Solution solution1, Solution solution2) {
		if (solution1 == null) {
			return 1;
		} else if (solution2 == null) {
			return -1;
		}

		// dominate1 indicates if some objective of solution1
        // dominates the same objective in solution2. dominate2
		// is the complementary of dominate1.
		int dominate1 = 0; 
		int dominate2 = 0;
		for (LabeledEdge target : this.targets) {
			if (target.isCovered()) {
				continue; //branch covered
			}
			
			double objective1 = solution1.getObjective(target.getObjectiveId());
			double objective2 = solution2.getObjective(target.getObjectiveId());
			int flag; // stores the result of the comparison
			if (objective1 < objective2) {
				flag = -1;
			} else if (objective1 > objective2) {
				flag = 1;
			} else {
				flag = 0;
			}
			
			if (flag == -1) {
				dominate1 = 1;
			}
			if (flag == 1) {
				dominate2 = 1;
			}
		}

		if (dominate1 == dominate2) {
			return 0; // No one dominates the other
		}
		if (dominate1 == 1) {
			return -1; // solution1 dominate
		}
		return 1; // solution2 dominate
	}
}
