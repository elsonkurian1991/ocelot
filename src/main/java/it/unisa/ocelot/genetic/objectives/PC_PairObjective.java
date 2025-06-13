package it.unisa.ocelot.genetic.objectives;

import java.util.ArrayList;
import java.util.HashMap;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import jmetal.core.Solution;

public class PC_PairObjective extends GenericObjective {

	TestObjStateMachine sm;
	public Integer budget;
	public String direction;
	public PC_PairObjective TriggeredPair;
	public Solution DiscovererTestCase;
	public int indirectionLevel;
	
	
	
	
	public PC_PairObjective(boolean isCovered, int objectiveID, String testObj1, String testObj2, String direction, int indirectionLevel) {
		super(isCovered, objectiveID);
		sm = new TestObjStateMachine(testObj1, testObj2);
		budget = 0;
		this.direction = direction;
		DiscovererTestCase = null;
		this.indirectionLevel = indirectionLevel;
	}
	
	public PC_PairObjective(boolean isCovered, int objectiveID, TestObjStateMachine SM, String direction, int indirectionLevel) {
		super(isCovered, objectiveID);
		sm = SM;
		budget = 0;
		this.direction = direction;
		DiscovererTestCase = null;
		this.indirectionLevel = indirectionLevel;
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

		fitness = (sm.getFitValOne() + sm.getFitValTwo())/2;
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

	public int compareTo(Integer budget2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public Integer getBudgetUsed() {
		return this.budget;
	}
	
    public static void sort(ArrayList<PC_PairObjective> list) {

        list.sort((o1, o2) -> o1.getBudgetUsed().compareTo(o2.getBudgetUsed()));
    }

	
}