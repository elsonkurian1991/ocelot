package it.unisa.ocelot.suites;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.genetic.objectives.GenericObjective;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * CoverageVerifier inspects a generated test suite and identifies which
 * GenericObjectives remain uncovered.
 *
 * <p>It does NOT modify MOSA or any TestSuiteGenerator implementation.
 * It operates purely on the objective list and their {@code isCovered} /
 * {@code bestFitness} state that MOSA already maintains during a run.</p>
 *
 * <p>Sorting strategy — "easy to hard":
 * <ul>
 *   <li>Objectives whose {@code bestFitness} is lower (i.e. MOSA got closer
 *       to covering them) are considered easier and are placed first.</li>
 *   <li>Objectives that were never evaluated ({@code bestFitness == Double.MAX_VALUE})
 *       are placed last, since we have no signal about their difficulty.</li>
 * </ul>
 * This means the next MOSA iteration spends its early budget on the
 * "almost covered" objectives first, maximising the chance of new coverage
 * without wasting evaluations on the hardest targets up front.</p>
 */
public class CoverageVerifier {

    /**
     * Returns the subset of {@code allObjectives} that are still NOT covered,
     * sorted from easiest to hardest based on the best fitness MOSA recorded
     * during the previous iteration.
     *
     * <p>Assumes that MOSA (or any other generator) has already updated each
     * objective's {@code isCovered} and {@code bestFitness} fields during the
     * preceding call to {@code generateTestSuite()}.</p>
     *
     * @param allObjectives the full objective list passed into the last iteration
     * @param suite         the test suite produced by the last iteration
     *                      (not used for coverage logic here — coverage state
     *                      is read directly from each objective's {@code isCovered}
     *                      flag, which MOSA maintains; the suite is accepted as a
     *                      parameter so callers can extend this method later if
     *                      they want cross-objective coverage re-checking)
     * @return a new, mutable list of uncovered objectives sorted easy → hard
     */
    public List<GenericObjective> getUncoveredObjectivesSorted(
            List<GenericObjective> allObjectives,
            Set<TestCase> suite) {

        List<GenericObjective> uncovered = new ArrayList<>();

        for (GenericObjective objective : allObjectives) {
            if (!objective.isCovered()) {
                uncovered.add(objective);
            }
        }

        /*
         * Sort ascending by bestFitness:
         *   - Lower bestFitness  → MOSA was "close" to covering it  → easier → first
         *   - Double.MAX_VALUE   → never evaluated or no progress    → last
         *
         * Comparator.comparingDouble handles Double.MAX_VALUE naturally
         * because it is just a very large finite double.
         */
        uncovered.sort(Comparator.comparingDouble(GenericObjective::getBestFitness));

        return uncovered;
    }

    /**
     * Convenience method: returns {@code true} if every objective in the list
     * is marked as covered.
     *
     * @param objectives the objective list to check
     * @return {@code true} if all objectives are covered
     */
    public boolean isFullyCovered(List<GenericObjective> objectives) {
        for (GenericObjective objective : objectives) {
            if (!objective.isCovered()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a short human-readable summary of coverage state, useful for
     * console logging between iterations.
     *
     * @param allObjectives the full objective list
     * @return e.g. "Coverage: 7/10 objectives covered (3 remaining)"
     */
    public String summarise(List<GenericObjective> allObjectives) {
        long covered = allObjectives.stream()
                .filter(GenericObjective::isCovered)
                .count();
        int total = allObjectives.size();
        long remaining = total - covered;
        return String.format("Coverage: %d/%d objectives covered (%d remaining)",
                covered, total, remaining);
    }

	public String getFinalCoverage(List<GenericObjective> allObjectives) {
		double coveredObj = allObjectives.stream()
                .filter(GenericObjective::isCovered)
                .count();
		  int totalObj = allObjectives.size();
		// Cast one variable to double to prevent integer division truncation
		  double totalObjCovered = ((double) coveredObj / totalObj) * 100;
		// Format to 2 decimal places and append the % symbol
		  return  String.format("%.2f%%", totalObjCovered);
		
	}
}