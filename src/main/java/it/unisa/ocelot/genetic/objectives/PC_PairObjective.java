package it.unisa.ocelot.genetic.objectives;

import java.util.HashMap;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class PC_PairObjective extends GenericObjective {

	TestObjStateMachine sm;

	public PC_PairObjective(boolean isCovered, int objectiveID, String testObj1, String testObj2) {
		super(isCovered, objectiveID);
		sm = new TestObjStateMachine(testObj1, testObj2);
	}

	@Override
	public double getFitness(Object[][][] arguments) {
		return calculateFitness(arguments);
	}

	private double calculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		HashMap<String, Double> branchDistances = BranchDistanceCache.getBranchDistances();

		FunBranchNameAndFitness infoFromLinebr1;
		if (branchDistances.containsKey(sm.getTestObjOne()))
			infoFromLinebr1 = new FunBranchNameAndFitness(sm.getTestObjOne(), branchDistances.get(sm.getTestObjOne()));
		else
			infoFromLinebr1 = new FunBranchNameAndFitness(sm.getTestObjOne(), 1);
		sm.transition(infoFromLinebr1);

		FunBranchNameAndFitness infoFromLinebr2;
		if (branchDistances.containsKey(sm.getTestObjTwo()))
			infoFromLinebr2 = new FunBranchNameAndFitness(sm.getTestObjTwo(), branchDistances.get(sm.getTestObjTwo()));
		else
			infoFromLinebr2 = new FunBranchNameAndFitness(sm.getTestObjTwo(), 1);
		sm.transition(infoFromLinebr2);

		fitness = sm.getFitValOne() + sm.getFitValTwo();
		if (fitness == Double.POSITIVE_INFINITY) {
			fitness = Double.MAX_VALUE;
		}
		return fitness;
	}

	@Override
	public String toString() {
		return "PC_PairObjective [sm=" + sm + ", isCovered()=" + isCovered() + ", getObjectiveID()=" + getObjectiveID()
				+ ", hashCode()=" + hashCode() + ", toString()=" + super.toString() + ", getClass()=" + getClass()
				+ "]";
	}
	
}