package it.unisa.ocelot.suites;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
import it.unisa.ocelot.simulator.CBridge;
import jmetal.core.Solution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Checks whether test cases generated in one iteration incidentally cover
 * objectives that were not explicitly targeted by MOSA in that iteration.
 *
 * <p><b>Why this helps:</b> MOSA targets a subset of objectives each iteration.
 * A TC generated for objective A may execute the same code path needed for
 * objective B (not in the subset). Without this check, B would remain uncovered
 * until a future iteration — even though a TC covering it already exists.
 *
 * <p><b>Re-execution approach:</b> Each TC is re-executed through a fresh
 * {@link CBridge} instance to populate
 * {@code BranchChainManager.newFitnessHashMap} with accurate branch distances.
 * Then {@code objective.getFitness(arguments)} reads those fresh values.
 * This mirrors exactly what {@code MOSAGenericCoverageProblem.evaluateSolution()}
 * does internally.
 *
 * <p><b>Note on CBridge:</b> {@code StandardProblem.getCurrentBridge()} is
 * protected and uses an internal thread-local map — not accessible here.
 * We instantiate {@code new CBridge(0)} directly, which is safe because
 * {@code CBridge.initialize()} has already been called in {@code GenAndWrite}
 * before this class is used.
 */
public class SerendipitousCoverageChecker {
    public SerendipitousCoverageChecker() { }

    /**
     * Re-executes each TC in {@code iterationSuite} and checks whether it
     * covers any objective in {@code allObjectives} that is not yet covered
     * and was not in the subset targeted by MOSA this iteration.
     *
     * <p>Any newly covered objective is marked {@code isCovered(true)} in-place.
     * No new TCs are added — the TC is already in the suite.
     *
     * @param iterationSuite   TCs produced by MOSA in this iteration
     * @param allObjectives    the full objective list (not just the subset)
     * @param subsetObjectives objectives MOSA targeted this iteration —
     *                         excluded from the check since MOSA already
     *                         handled their coverage state
     * @return number of objectives newly covered serendipitously
     */
    public int check(Set<TestCase> iterationSuite,
                     List<GenericObjective> allObjectives,
                     List<GenericObjective> subsetObjectives) {

        // Build candidate list: uncovered objectives NOT in the subset
        List<GenericObjective> candidates =
                buildCandidateList(allObjectives, subsetObjectives);

        if (candidates.isEmpty()) {
            System.out.println("[Serendipity] No candidate objectives to check.");
            return 0;
        }

        System.out.println("[Serendipity] Checking " + iterationSuite.size()
                + " TC(s) against " + candidates.size()
                + " non-subset uncovered objectives.");

        int newlyCovered = 0;

        // One C bridge call per TC — then check all candidates against the
        // fresh fitness map produced by that single simulation
        for (TestCase tc : iterationSuite) {
			GenericSolution solution = (GenericSolution)tc.getSolution();
			if (solution == null) {
				throw new RuntimeException("Sorry, old Ocelot algorithms are not supported - yet");
			}
        	
            // Check each candidate objective against the fresh fitness values
            for (GenericObjective objective : candidates) {
                if (objective.isCovered()) continue; // already marked by a prior TC
                double fitness = Double.MAX_VALUE;
                if(objective.getObjectiveID()>= solution.getNumberOfObjectives()) {
                	continue;
                }
                 fitness = solution.getObjective(objective.getObjectiveID());

                if (fitness == 0.0) {
                    // TC covers this objective incidentally — mark it for free
                    objective.updateBestFitness(fitness);
                    newlyCovered++;
                    System.out.println("Objective "
                            + objective.getObjectiveID()
                            + " covered by TC " + tc.getId()
                            + " (serendipitous).");
                }
            }
        }

        System.out.println("[Serendipity] Result: " + newlyCovered
                + " objective(s) covered serendipitously out of "
                + candidates.size() + " candidates.");

        return newlyCovered;
    }


    /**
     * Builds the list of objectives eligible for serendipity checking.
     * Excludes already-covered objectives and objectives in the MOSA subset
     * (those are handled directly by MOSA's own archive logic).
     */
    private List<GenericObjective> buildCandidateList(
            List<GenericObjective> allObjectives,
            List<GenericObjective> subsetObjectives) {

        // Collect subset IDs for O(1) exclusion check
        Set<Integer> subsetIds = new HashSet<>();
        for (GenericObjective obj : subsetObjectives) {
            subsetIds.add(obj.getObjectiveID());
        }

        List<GenericObjective> candidates = new ArrayList<>();
        int i = 0,j=0, z=0;
        for (GenericObjective obj : allObjectives) {
            if (obj.isCovered()) {
            	++i;
            	continue; // already done
            }
            if (subsetIds.contains(obj.getObjectiveID())) {
            	++j;
            	continue; // MOSA handled it
            }
            ++z;
            candidates.add(obj);
        }
        System.out.println("i (already done) ="+i+" j (MOSA handled it) ="+j+" z (candidates) ="+z);
        return candidates;
    }
}