package it.unisa.ocelot.genetic.objectives;

import java.util.HashMap;
import jmetal.core.Solution;

public class BranchObjective extends GenericObjective{
	public String testObj;
	public Solution DiscovererTestCase;
	
	public BranchObjective(boolean isCovered, int objectiveID, String testObj) {
		super(isCovered, objectiveID);
		this.testObj = testObj;
	}
	
	@Override
	public double getFitness(Object[][][] arguments) {
		return calculateFitness(arguments);
	}

	private double calculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		HashMap<String, Double> branchDistances = BranchDistanceCache.getBranchDistances();

		
		if (branchDistances.containsKey(testObj))
			fitness =  branchDistances.get(testObj);
		else
			fitness =  1;

		if (fitness == Double.POSITIVE_INFINITY) {
			fitness = Double.MAX_VALUE;
		}
		return fitness;
	}

	@Override
	public String toString() {
		return "BranchObjective [objective="+ testObj + ", isCovered()=" + isCovered() + ", getObjectiveID()=" + getObjectiveID()
				+ ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + ", getClass()=" + getClass()
				+ "]";
	}

}
