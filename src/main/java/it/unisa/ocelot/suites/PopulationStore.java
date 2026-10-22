package it.unisa.ocelot.suites;

import it.unisa.ocelot.genetic.solutions.GenericSolution;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jmetal.core.SolutionSet;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Stores the final population produced by a MOSA run so that the next
 * iteration can be seeded with it instead of starting from random.
 *
 * <p>Seeding preserves the search progress made in the previous iteration —
 * without this, every MOSA instance re-discovers the same easy regions,
 * wasting budget that could be spent on harder objectives.</p>
 */
public class PopulationStore {

    // Fixed maximum capacity of the pool — set once at construction
    private final int capacity;
 
    // Ordered buffer: front = best score (covering solutions first), back = worst.
    // LinkedList used for efficient add-front and remove-back operations.
    private final LinkedList<GenericSolution> pool;
 
    /**
     * @param populationSize the MOSA population size read from config.
     *                       Pool capacity is set to populationSize / 2.
     */
    public PopulationStore(int populationSize) {
        // Pool holds half the population size — matches the seed budget in GenAndWrite
        this.capacity = Math.max(1, populationSize / 2);
        this.pool     = new LinkedList<>();
        System.out.println("[PopulationStore] Initialised with capacity: "
                + this.capacity + " (populationSize=" + populationSize + " / 2)");
    }
 
    /**
     * Stores the final population from a completed MOSA run into the pool.
     * Merges the new population with the pool, ranks by score 
     * (best objective value, 0.0 = covers an objective) and 
     * keeps the best 'capacity' solutions. 
     * Newer solutions win ties.
     */
    public void store(SolutionSet population) {
        if (population == null || population.size() == 0) return;

        // New solutions first, then the existing pool. List.sort is stable, so
        // on equal scores the newest solutions win.
        List<GenericSolution> merged = new ArrayList<>(population.size() + pool.size());
        for (int i = 0; i < population.size(); i++) {
            merged.add(new GenericSolution((GenericSolution) population.get(i))); // deep copy
        }
        merged.addAll(pool);
        merged.sort(Comparator.comparingDouble(PopulationStore::score));

        pool.clear();
        for (int i = 0; i < Math.min(capacity, merged.size()); i++) {
            pool.add(merged.get(i));
        }

        System.out.println("[PopulationStore] Offered " + population.size()
                + " solutions. Pool size: " + pool.size() + "/" + capacity
                + ", best score: " + score(pool.getFirst()));
    }
    
    
    /**
     * Quality score of a solution: the best (lowest) objective value in its vector.
     * 0.0 means the solution covers at least one objective. Uses only vector
     * positions, so it does not depend on objective IDs (which are remapped per run).
     */
    private static double score(GenericSolution s) {
        double best = Double.MAX_VALUE;
        for (int i = 0; i < s.getNumberOfObjectives(); i++) {
            double v = s.getObjective(i);
            if (!Double.isNaN(v)) {
                best = Math.min(best, v);
            }
        }
        return best;
    }
    /**
     * Returns true if at least one seed solution is available.
     */
    public boolean hasSeedPopulation() {
        return !pool.isEmpty();
    }
 
    /**
     * Returns up to {@code maxSize} solutions from the pool for seeding MOSA.
     * Best-scoring solutions are returned first, as they are
     * most relevant to the current iteration's objectives.
     *
     * @param maxSize maximum number of seeds to return
     * @return SolutionSet of seed solutions (deep copies)
     */
    public SolutionSet getSeedPopulation(int maxSize) {
        int count  = Math.min(maxSize, pool.size());
        SolutionSet seeds = new SolutionSet(count);
 
        int i = 0;
        for (GenericSolution solution : pool) {
            if (i >= count) break;
            seeds.add(new GenericSolution(solution)); // deep copy — do not mutate stored pool
            i++;
        }
 
        System.out.println("[PopulationStore] Providing " + seeds.size()
                + " seed solutions (pool size: " + pool.size() + ")");
        return seeds;
    }
 
    /**
     * Returns the current number of solutions held in the pool.
     */
    public int size() {
        return pool.size();
    }
 
    /**
     * Returns the fixed maximum capacity of the pool.
     */
    public int getCapacity() {
        return capacity;
    }
 
    /**
     * Clears the entire pool. Call between independent test generation runs.
     */
    public void clear() {
        pool.clear();
    }
 
    
}