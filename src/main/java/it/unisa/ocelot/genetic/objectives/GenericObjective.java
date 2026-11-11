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

	// The best solution found so far for this objective.
	private double bestFitness;
	private GenericSolution bestSolution;
	
	/**
	 * Snapshot history of bestFitness recorded at fixed evaluation intervals
	 * during a MOSA run. Each entry is a (evaluationCount, bestFitness) pair.
	 * Used by MABController to compute how fast this objective is improving.
	 */
	//TODO put it inside MABController instead of GenericObjective, because it is not used by MOSA_Generic
	private List<double[]> fitnessSnapshots;

	public GenericObjective(int objectiveID) {
		super();
		this.objectiveID  = objectiveID;
		this.bestFitness  = Double.MAX_VALUE;
		this.bestSolution  = null;

		this.fitnessSnapshots = new ArrayList<>();
	}


	public void reset() { //bestEver* fields are kept on purpose
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

	/** Returns the best fitness value seen so far for this objective. */
	public double getBestFitness() {
		return bestFitness;
	}
	
	// Returns the best solution found so far for this objective, or null if none.
	public GenericSolution getBestSolution() {
		return bestSolution;   	
	}

	public void updateBestFitness(GenericSolution currentCandidate) {
		double currentCandidateFitness = getFitness(currentCandidate); //this also works in the case the current candidate must be evaluated on an objective outside its current problem
		if (currentCandidateFitness < this.bestFitness) {
			this.bestFitness = currentCandidateFitness;
			this.bestSolution = currentCandidate;
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
	
	/**
	 * Proximity in [0,1]: 1.0 = covered, 0.0 = never evaluated, in between = 1/(1+fitness).
	 * This form works for any fitness scale, because I haven't verified the range returned
	 * by calculateBranchChainPairFitness (0 = covered, lower = closer is assumed).
	 */
	public double getProximity() {
		return bestFitness == Double.MAX_VALUE ? 0.0 : 1.0 / (1.0 + bestFitness);
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