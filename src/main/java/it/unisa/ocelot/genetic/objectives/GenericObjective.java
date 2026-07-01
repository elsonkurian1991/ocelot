package it.unisa.ocelot.genetic.objectives;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class GenericObjective {

    private boolean isCovered;
    private int objectiveID;

    // Used in DynaMOSA to know if we are currently optimizing for this objective
    private boolean isActive;

    public GenericObjective TriggeredPair;
    public int counter;
    public double bestFitness;

    // -------------------------------------------------------------------------
    // Velocity tracking fields — new code starts here
    // -------------------------------------------------------------------------

    /**
     * Snapshot history of bestFitness recorded at fixed evaluation intervals
     * during a MOSA run. Each entry is a (evaluationCount, bestFitness) pair.
     * Used by MABController to compute how fast this objective is improving.
     */
    public List<double[]> fitnessSnapshots;

    /**
     * Velocity of fitness improvement for this objective, computed after each
     * MOSA run by MABController.
     * velocity = (prevBestFitness - currentBestFitness) / evaluationsDelta
     * Higher value means the objective is improving faster (easier to cover).
     * Initialised to 0.0 — unknown velocity before first run.
     */
    public double velocity;

    // -------------------------------------------------------------------------
    // Velocity tracking fields — new code ends here
    // -------------------------------------------------------------------------

    public GenericObjective(boolean isCovered, int objectiveID) {
        super();
        this.isCovered    = isCovered;
        this.isActive     = true;
        this.objectiveID  = objectiveID;
        this.counter      = 0;
        this.bestFitness  = Double.MAX_VALUE;

        // ---- new code starts here ----
        this.fitnessSnapshots = new ArrayList<>();
        this.velocity         = 0.0;
        // ---- new code ends here ----
    }

    public boolean isCovered() {
        return isCovered;
    }

    public void setCovered(boolean isCovered) {
        this.isCovered = isCovered;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    public int getObjectiveID() {
        return objectiveID;
    }

    public void setObjectiveID(int objectiveID) {
        this.objectiveID = objectiveID;
    }

    // ---- new code starts here ----
    /** Returns the velocity of fitness improvement computed by MABController. */
    public double getVelocity() {
        return velocity;
    }

    /** Returns the best fitness value seen so far for this objective. */
    public double getBestFitness() {
        return bestFitness;
    }

    /**
     * Records a fitness snapshot at the given evaluation count.
     * Called by MOSA_Generic at fixed intervals so MABController can compute
     * velocity after the run.
     *
     * @param evaluationCount current evaluation number inside MOSA
     * @param fitness         best fitness seen for this objective so far
     */
    public void recordSnapshot(int evaluationCount, double fitness) {
        fitnessSnapshots.add(new double[]{evaluationCount, fitness});
    }

    /**
     * Clears snapshot history before a new MOSA run so stale data from the
     * previous iteration does not pollute velocity calculations.
     */
    public void clearSnapshots() {
        fitnessSnapshots.clear();
    }
    // ---- new code ends here ----

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
        return "GenericObjective [isCovered=" + isCovered
                + ", objectiveID=" + objectiveID + "]";
    }

    public abstract double getFitness(Object[][][] arguments);
}