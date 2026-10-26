package it.unisa.ocelot.suites;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.solutions.GenericSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * <p>Checks whether test cases generated in one iteration incidentally cover
 * objectives that MOSA did not target in that iteration, so those objectives
 * need no iteration of their own.
 *
 * <p><b>How:</b> when MOSA evaluates a solution, {@link GenericSolution} keeps the
 * branch-distance map of that execution. Here each objective outside the subset is
 * evaluated against that stored map via {@link GenericSolution#evaluateObjective}.
 * No re-execution is needed. The objective vector cannot be used for this, because
 * it only has entries for the subset, indexed by the per-run remapped IDs.
 
 */
public class SerendipitousCoverageChecker {
	public SerendipitousCoverageChecker() { }

    /**
     * @param iterationSuite   TCs produced by MOSA in this iteration
     * @param allObjectives    the full objective list
     * @param subsetObjectives objectives MOSA targeted this iteration (excluded)
     * @return number of objectives newly covered serendipitously. Covered
     *         objectives get bestFitness 0.0 via updateBestFitness.
     */
    public int check(Set<TestCase> iterationSuite,
                     List<GenericObjective> allObjectives,
                     List<GenericObjective> subsetObjectives) {

        List<GenericObjective> candidates = buildCandidateList(allObjectives);
        if (candidates.isEmpty()) {
            System.out.println("[Serendipity] No candidate objectives to check.");
            return 0;
        }

        System.out.println("[Serendipity] Checking " + iterationSuite.size()
                + " TC(s) against " + candidates.size() + " non-subset uncovered objectives.");

        int newlyCovered = 0;
        int skippedNoData = 0;

        for (TestCase tc : iterationSuite) {
            if (!(tc.getSolution() instanceof GenericSolution)) {
                throw new RuntimeException("Sorry, old Ocelot algorithms are not supported - yet");
            }
            GenericSolution solution = (GenericSolution) tc.getSolution();
            if (!solution.hasExecutionData()) {
                skippedNoData++;
                continue;
            }

            for (GenericObjective objective : candidates) {
                if (objective.isCovered()) continue; // already covered by a prior TC
                if (solution.evaluateObjective(objective) == 0.0) {
                    objective.updateBestFitness(0.0);
                    newlyCovered++;
                    System.out.println("[Serendipity] Objective " + objective.getObjectiveID()
                            + " covered by TC " + tc.getId());
                }
            }
        }

        if (skippedNoData > 0) {
            System.out.println("[Serendipity] WARNING: " + skippedNoData
                    + " TC(s) skipped: solution carries no execution data.");
        }
        System.out.println("[Serendipity] Result: " + newlyCovered + " of "
                + candidates.size() + " candidates covered.");
        return newlyCovered;
    }

    /** Uncovered objectives that are not in the MOSA subset (compared by identity). */
    private List<GenericObjective> buildCandidateList(
            List<GenericObjective> allObjectives) {

        List<GenericObjective> candidates = new ArrayList<>();
        for (GenericObjective obj : allObjectives) {
            if (!obj.isCovered()) {
                candidates.add(obj);
            }
        }
        return candidates;
    }
}