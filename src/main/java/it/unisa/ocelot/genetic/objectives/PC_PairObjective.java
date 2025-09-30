package it.unisa.ocelot.genetic.objectives;

import java.util.ArrayList;
import java.util.HashMap;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import jmetal.core.Solution;

public class PC_PairObjective extends GenericObjective {

	public TestObjStateMachine sm;
	public Integer budget;
	public String direction;
	public Solution DiscovererTestCase;
	public int indirectionLevel;
	
	public boolean isSynthetic;
	
	

	
	public PC_PairObjective(boolean isCovered, int objectiveID, TestObjStateMachine SM, String direction, int indirectionLevel) {
		super(isCovered, objectiveID);
		sm = SM;
		budget = 0;
		this.direction = direction;
		DiscovererTestCase = null;
		this.indirectionLevel = indirectionLevel;
		isSynthetic = false;
	}
	


	@Override
	public double getFitness(Object[][][] arguments) {
		return calculateFitness(arguments);
	}

	private double calculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		HashMap<String, Double> branchDistances = BranchDistanceCache.getBranchDistances();
		double fitValOne = 1.0;
		double fitValTwo = 1.0;

		FunBranchNameAndFitness infoFromLinebr1;
		Double testObj1 = branchDistances.get(sm.getTestObjOne());
		if (testObj1 != null) {
			infoFromLinebr1 = new FunBranchNameAndFitness(sm.getTestObjOne(), testObj1);
			fitValOne = branchDistances.get(sm.getTestObjOne());
		}
		else
			infoFromLinebr1 = new FunBranchNameAndFitness(sm.getTestObjOne(), 1);
		sm.transition(infoFromLinebr1);

		FunBranchNameAndFitness infoFromLinebr2;
		Double testObj2 = branchDistances.get(sm.getTestObjTwo());
		if (testObj2 != null) {
			infoFromLinebr2 = new FunBranchNameAndFitness(sm.getTestObjTwo(), testObj2);
			fitValTwo = branchDistances.get(sm.getTestObjTwo());
		}
		else
			infoFromLinebr2 = new FunBranchNameAndFitness(sm.getTestObjTwo(), 1);
		sm.transition(infoFromLinebr2);

		fitness = (fitValOne + fitValTwo)/2;
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
	
	public Integer getBudgetUsed() {
		return this.budget;
	}
	
}