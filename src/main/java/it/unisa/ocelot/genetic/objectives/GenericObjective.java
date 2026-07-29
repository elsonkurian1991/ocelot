package it.unisa.ocelot.genetic.objectives;

import java.util.ArrayList;
import java.util.List;

import it.unisa.ocelot.genetic.solutions.GenericSolution;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public abstract class GenericObjective {
    private int objectiveID;
    private double bestFitness;

    /**
     * Snapshot history of bestFitness recorded at fixed evaluation intervals
     * during a MOSA run. Each entry is a (evaluationCount, bestFitness) pair.
     * Used by MABController to compute how fast this objective is improving.
     */
    private List<double[]> fitnessSnapshots;

    /**
     * Velocity of fitness improvement for this objective, computed after each
     * MOSA run by MABController.
     * velocity = (prevBestFitness - currentBestFitness) / evaluationsDelta
     * Higher value means the objective is improving faster (easier to cover).
     * Initialised to 0.0 — unknown velocity before first run.
     */
    private double velocity;

    public GenericObjective(int objectiveID) {
        super();
        this.objectiveID  = objectiveID;
        this.bestFitness  = Double.MAX_VALUE;

        this.fitnessSnapshots = new ArrayList<>();
        this.velocity         = 0.0;
    }
    
    public void reset() {
    	bestFitness = Double.MAX_VALUE;
    	clearSnapshots();
    }

    public boolean isCovered() {
        return this.bestFitness == 0.0;
    }

    public int getObjectiveID() {
        return objectiveID;
    }

    public void setObjectiveID(int objectiveID) {
        this.objectiveID = objectiveID;
    }

    /** Returns the velocity of fitness improvement computed by MABController. */
    public double getVelocity() {
        return velocity;
    }
    
    public void setVelocity(double velocity) {
    	this.velocity = velocity;
    }

    /** Returns the best fitness value seen so far for this objective. */
    public double getBestFitness() {
        return bestFitness;
    }
    
    public void updateBestFitness(double bestFitness) {
    	if (bestFitness < this.bestFitness) {
    		this.bestFitness = bestFitness;
    	}
    }
    
    /**
     * Records a fitness snapshot at the given generation and evaluation count.
     * Called by MOSA_Generic once per generation for every objective.
     *
     * @param generation      MOSA generation number (x-axis for convergence plot)
     * @param evaluationCount cumulative evaluations at this generation
     * @param fitness         best fitness seen for this objective so far
     */
    public void recordSnapshot(int generation, int evaluationCount, double fitness) {
        // Stores [generation, evaluationCount, fitness]
        fitnessSnapshots.add(new double[]{generation, evaluationCount, fitness});
    }

    /**
     * Clears snapshot history before a new MOSA run so stale data from the
     * previous iteration does not pollute velocity calculations.
     */
    public void clearSnapshots() {
        fitnessSnapshots.clear();
    }
    
    public List<double[]> getSnapshots() {
    	return fitnessSnapshots;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + objectiveID;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        GenericObjective other = (GenericObjective) obj;
        return objectiveID == other.objectiveID;
    }

    @Override
    public String toString() {
        return "GenericObjective [bestFitness=" + bestFitness
                + ", objectiveID=" + objectiveID + "]";
    }

    public abstract double getFitness(GenericSolution solution);
}