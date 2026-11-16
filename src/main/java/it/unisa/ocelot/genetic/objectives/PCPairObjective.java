package it.unisa.ocelot.genetic.objectives;

import java.util.HashMap;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndDistance;
import it.unisa.ocelot.genetic.edges.TestObjStateMachine;
import jmetal.core.Solution;

public class PCPairObjective extends GenericObjective {
	public TestObjStateMachine sm;
	public int indirectionLevel;
	public boolean isSynthetic;
	
	public PCPairObjective(int objectiveId, TestObjStateMachine SM, int indirectionLevel) {
		super(objectiveId);
		this.sm = SM;
		this.indirectionLevel = indirectionLevel;
		this.isSynthetic = false;
	}
	
	@Override
	protected double calculateValue(Solution pSolution) { 
		// Here we assume that the last C run's output, read by 
		// BranchDistancesReader, was that of pSolution
		//TODO check it, or rerun pSolution
		HashMap<String, Double> branchDistances = BranchDistancesReader.get();

		// Advances the state machines
		FunBranchNameAndDistance infoFromLinebr1;
		if (branchDistances.containsKey(this.sm.getTestObjOne())) {
			infoFromLinebr1 = new FunBranchNameAndDistance(this.sm.getTestObjOne(), branchDistances.get(this.sm.getTestObjOne()));
		} else {
			infoFromLinebr1 = new FunBranchNameAndDistance(this.sm.getTestObjOne(), 1);
		}
		this.sm.transition(infoFromLinebr1);

		FunBranchNameAndDistance infoFromLinebr2;
		if (branchDistances.containsKey(this.sm.getTestObjTwo())) {
			infoFromLinebr2 = new FunBranchNameAndDistance(this.sm.getTestObjTwo(), branchDistances.get(this.sm.getTestObjTwo()));
		} else {
			infoFromLinebr2 = new FunBranchNameAndDistance(this.sm.getTestObjTwo(), 1);
		}
		this.sm.transition(infoFromLinebr2);
		
		// Averages
		double value = (this.sm.getBdOne() + this.sm.getBdTwo()) / 2;

		return value;
	}

	@Override
	public String toString() {
		return "PCPairObjective [id=" + getId() + ", isCovered=" + isCovered() + ", isActive=" + isActive() + ", hasCompanion=" + hasCompanion() + ", sm=" + this.sm.toString() + ", indirectionLevel=" + this.indirectionLevel + ", isSynthetic=" + this.isSynthetic + "]";
	}
}