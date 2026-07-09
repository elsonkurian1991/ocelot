package it.unisa.ocelot.suites;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cdg.BranchChainManager;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.EventsHandler;
import it.unisa.ocelot.simulator.Simulator;
import it.unisa.ocelot.simulator.SimulationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final CFG cfg;

    // Dedicated CBridge instance for re-executing test cases.
    // Index 0 matches the single-bridge setup done in GenAndWrite.initialize().
    private final CBridge bridge;

    public SerendipitousCoverageChecker(CFG cfg) {
        this.cfg    = cfg;
        // Instantiate directly — same pattern StandardProblem uses when no
        // bridge exists yet for the current thread
        this.bridge = new CBridge(0);
    }

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
            Object[][][] arguments = tc.getParameters();
            if (arguments == null) continue;

            // Re-execute TC to populate newFitnessHashMap with fresh values
            boolean simulated = simulateTestCase(arguments);
            if (!simulated) continue; // C bridge error — skip this TC

            // Check each candidate objective against the fresh fitness values
            for (GenericObjective objective : candidates) {
                if (objective.isCovered()) continue; // already marked by a prior TC

                double fitness = objective.getFitness(arguments);

                if (fitness == 0.0) {
                    // TC covers this objective incidentally — mark it for free
                    objective.setCovered(true);
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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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
        for (GenericObjective obj : allObjectives) {
            if (obj.isCovered())                          continue; // already done
            if (subsetIds.contains(obj.getObjectiveID())) continue; // MOSA handled it
            candidates.add(obj);
        }
        return candidates;
    }

    /**
     * Re-executes a TC through the C bridge and simulator, then caches fresh
     * branch distances into {@code BranchChainManager.newFitnessHashMap}.
     *
     * <p>Mirrors the execution sequence in
     * {@code MOSAGenericCoverageProblem.evaluateSolution()}:
     * <pre>
     *   bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0])
     *   → new Simulator(cfg, events).simulate()
     *   → BranchChainManager.cacheFitnessValues()
     * </pre>
     *
     * @param arguments TC parameters from {@code TestCase.getParameters()}
     * @return true if simulation succeeded, false on C bridge or simulation error
     */
    private boolean simulateTestCase(Object[][][] arguments) throws SimulationException {
        try {
            EventsHandler handler = new EventsHandler();

            // Execute through C bridge — same argument layout as evaluateSolution()
            bridge.getEvents(handler,
                    arguments[0][0],  // value parameters
                    arguments[1],     // pointer parameters
                    arguments[2][0]); // pointer-to-pointer parameters

            // Build coverage path from the events produced by the C execution
            Simulator simulator = new Simulator(cfg, handler.getEvents());
            simulator.simulate();

            // Populate newFitnessHashMap with fresh branch distances so
            // objective.getFitness(arguments) reads accurate values
            BranchChainManager.cacheFitnessValues();

            return true;

        } catch (RuntimeException e) {
            System.err.println("[Serendipity] Simulation error: " + e.getMessage());
            return false;
        }
    }
}