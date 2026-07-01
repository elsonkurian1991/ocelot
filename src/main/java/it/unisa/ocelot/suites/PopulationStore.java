package it.unisa.ocelot.suites;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;

/**
 * Stores the final population produced by a MOSA run so that the next
 * iteration can be seeded with it instead of starting from random.
 *
 * <p>Seeding preserves the search progress made in the previous iteration —
 * without this, every MOSA instance re-discovers the same easy regions,
 * wasting budget that could be spent on harder objectives.</p>
 */
public class PopulationStore {

    // The full population from the last completed MOSA run
    private SolutionSet lastPopulation;

    public PopulationStore() {
        this.lastPopulation = null;
    }

    /**
     * Stores the final population after a MOSA run completes.
     * Called by GenAndWrite immediately after generateTestSuite() returns.
     *
     * @param population the final population from MOSA_Generic
     */
    public void store(SolutionSet population) {
        // Deep-copy each solution so future runs do not mutate stored state
        this.lastPopulation = new SolutionSet(population.size());
        for (int i = 0; i < population.size(); i++) {
            this.lastPopulation.add(new Solution(population.get(i)));
        }
    }

    /**
     * Returns true if a population from a previous run is available for seeding.
     */
    public boolean hasSeedPopulation() {
        return lastPopulation != null && lastPopulation.size() > 0;
    }

    /**
     * Returns up to {@code maxSize} solutions from the stored population to
     * use as seeds for the next MOSA run.
     *
     * <p>Only half the requested population size is seeded — the other half
     * will be random new solutions inside MOSA. This keeps diversity while
     * preserving previous progress.</p>
     *
     * @param maxSize maximum number of seed solutions to return
     *                (typically populationSize / 2)
     * @return a SolutionSet of seed solutions, or empty set if none stored
     */
    public SolutionSet getSeedPopulation(int maxSize) {
        if (!hasSeedPopulation()) {
            return new SolutionSet(0);
        }

        int seedCount = Math.min(maxSize, lastPopulation.size());
        SolutionSet seeds = new SolutionSet(seedCount);

        // Take the first seedCount solutions from the stored population.
        // MOSA_Generic will have sorted these by fitness during its last run,
        // so the front of the list contains the best solutions.
        for (int i = 0; i < seedCount; i++) {
            seeds.add(new Solution(lastPopulation.get(i)));
        }
        return seeds;
    }

    /** Clears stored population — call between independent test generation runs. */
    public void clear() {
        this.lastPopulation = null;
    }
}