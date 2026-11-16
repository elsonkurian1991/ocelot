package it.unisa.ocelot.genetic.objectives;

import java.util.HashMap;

import jmetal.core.Solution;

public final class BranchObjective extends GenericObjective {
	public final String branchId;
	
	public BranchObjective(int objectiveId, String branchId) {
		super(objectiveId);
		this.branchId = branchId;
	}
	
	public String getBranchId() {
		return this.branchId;
	}
		
	@Override
	protected double calculateValue(Solution pSolution) {
		// Here we assume that the last C run's output, read by 
		// BranchDistancesReader, was that of pSolution
		//TODO check it, or rerun pSolution
		HashMap<String, Double> branchDistances = BranchDistancesReader.get();
		
		// The value of the objective, in this case, is just branch distance
		double value;
		if (branchDistances.containsKey(this.branchId)) {
			value = branchDistances.get(this.branchId);
		} else {
			value = 1;
		}

		return value;
	}

	@Override
	public String toString() {
		return "BranchObjective [id=" + getId() + ", isCovered=" + isCovered()+ ", isActive=" + isActive() + ", hasCompanion=" + hasCompanion() + ", branchId=" + this.branchId
				+ "]";
	}
}
