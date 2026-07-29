package it.unisa.ocelot.suites;

import it.unisa.ocelot.genetic.objectives.GenericObjective;

import java.util.List;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Loads the next subset of objectives for a MOSA iteration.
 *
 * <p>Enforces the rule that subset size is always half of the configured
 * MOSA population size, so the number of objectives never exceeds what MOSA
 * can handle effectively (avoids the fitness dilution problem seen when 130
 * objectives compete for a population of 100 solutions).
 *
 * <p>Delegates actual objective selection and ordering to {@link MABController},
 * which uses UCB1 to balance exploration and exploitation across iterations.
 */
public class ObjSubsetLoader {

    // Half the MOSA population size — enforced as the maximum subset size
    private final int subsetSize;

    // MAB controller that scores and selects objectives
    private final MABController mab;

    /**
     * @param populationSize the MOSA population size from config
     *                       (subset will be populationSize / 2)
     * @param mab            the shared MABController instance
     */
    public ObjSubsetLoader(int populationSize, MABController mab) {
        // Subset size = half population, minimum 1 to avoid empty subsets
        this.subsetSize = Math.max(1, populationSize / 2);
        this.mab        = mab;
        System.out.println("[ObjSubsetLoader] Subset size fixed at: " + this.subsetSize
                + " (populationSize=" + populationSize + " / 2)");
    }

    /**
     * Returns the next subset of objectives to pass to MOSA.
     *
     * <p>On the first iteration, all objectives are passed directly (no MAB
     * scoring yet — we need at least one run to gather statistics). From
     * iteration 2 onwards, MAB selects the best subset.
     *
     * @param uncoveredObjectives remaining uncovered objectives
     * @param iterationNumber     current iteration (1-based)
     * @return selected subset, ordered by MAB priority (highest first)
     */
    public List<GenericObjective> loadSubset(
            List<GenericObjective> uncoveredObjectives, int iterationNumber) {

        System.out.println("[ObjSubsetLoader] Iteration " + iterationNumber
                + ": " + uncoveredObjectives.size()
                + " uncovered objectives, selecting up to " + subsetSize);

        // First iteration: let MAB select (all objectives are "never attempted"
        // so they all get MAX_VALUE score and are returned in original order
        // up to subsetSize). This also populates the initial attempt records.
        List<GenericObjective> subset =
                mab.selectSubset(uncoveredObjectives, subsetSize);

        System.out.println("[ObjSubsetLoader] Selected " + subset.size()
                + " objectives for this iteration.");
        return subset;
    }

    /** Returns the enforced subset size for logging/debugging. */
    public int getSubsetSize() {
        return subsetSize;
    }
}