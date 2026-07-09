package it.unisa.ocelot.suites;

import java.util.LinkedList;

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

    // Fixed maximum capacity of the pool — set once at construction
    private final int capacity;
 
    // Ordered buffer: front = newest, back = oldest.
    // LinkedList used for efficient add-front and remove-back operations.
    private final LinkedList<Solution> pool;
 
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
     * Adds new solutions to the front of the pool (newest first).
     * Evicts the oldest non-covering solutions from the back to stay within
     * capacity. Solutions with fitness == 0.0 are never evicted.
     *
     * <p>If the pool is completely full of covering solutions (fitness == 0.0)
     * and new non-covering solutions arrive, the new ones are discarded rather
     * than evicting valuable covering solutions.
     *
     * @param population the final population from the completed MOSA run
     */
    public void store(SolutionSet population) {
        if (population == null || population.size() == 0) return;
 
        int added = 0;
        for (int i = 0; i < population.size(); i++) {
            Solution solution = new Solution(population.get(i)); // deep copy
 
            // Make room if pool is at capacity
            if (pool.size() >= capacity) {
                if (!evictOldestNonCovering()) {
                    // Pool is full of covering solutions — discard this new one
                    // to preserve the valuable covering solutions already stored
                    continue;
                }
            }
 
            // Add to front so newest solutions are offered to MOSA first
            pool.addFirst(solution);
            added++;
        }
 
        System.out.println("[PopulationStore] Stored " + added
                + " new solutions. Pool size: " + pool.size()
                + "/" + capacity);
    }
 
    /**
     * Returns true if at least one seed solution is available.
     */
    public boolean hasSeedPopulation() {
        return !pool.isEmpty();
    }
 
    /**
     * Returns up to {@code maxSize} solutions from the pool for seeding MOSA.
     * Newest solutions are returned first (front of the pool), as they are
     * most relevant to the current iteration's objectives.
     *
     * @param maxSize maximum number of seeds to return
     * @return SolutionSet of seed solutions (deep copies)
     */
    public SolutionSet getSeedPopulation(int maxSize) {
        int count  = Math.min(maxSize, pool.size());
        SolutionSet seeds = new SolutionSet(count);
 
        int i = 0;
        for (Solution solution : pool) {
            if (i >= count) break;
            seeds.add(new Solution(solution)); // deep copy — do not mutate stored pool
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
 
    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
 
    /**
     * Evicts the oldest non-covering solution from the back of the pool.
     * Traverses from back (oldest) to front (newest), skipping any solution
     * with fitness == 0.0 (a covering solution — never evicted).
     *
     * @return true if a solution was evicted, false if all remaining solutions
     *         are covering solutions and cannot be removed
     */
    private boolean evictOldestNonCovering() {
        // Traverse from back (oldest) to find first non-covering solution
        for (int i = pool.size() - 1; i >= 0; i--) {
            Solution candidate = pool.get(i);
 
            // fitness == 0.0 means this solution covered an objective — keep it
            if (candidate.getFitness() == 0.0) {
                continue; // skip — never evict a covering solution
            }
 
            // Found an evictable (non-covering) solution — remove it
            pool.remove(i);
            return true;
        }
 
        // All solutions in the pool are covering solutions — cannot evict
        return false;
    }
}