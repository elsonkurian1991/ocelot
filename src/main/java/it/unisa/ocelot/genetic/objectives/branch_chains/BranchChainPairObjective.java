package it.unisa.ocelot.genetic.objectives.chains;

import java.io.Serializable;
import java.util.HashMap;

import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class BranchChainPairObjective extends GenericObjective implements Serializable {
	private static final long serialVersionUID = 1L;
	BranchChainPair branchChainPair;

	private static int counter=0;

	public BranchChainPairObjective(BranchChainPair branchChainPair) {
		super(counter++);
		this.branchChainPair = branchChainPair;
	}
	
	public BranchChainPairObjective(int objID, BranchChainPair branchChainPair) {
		super(objID);
		this.branchChainPair = branchChainPair;
	}
	
	
	public BranchChainPair getBranchChainPair() {
		return this.branchChainPair;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public String toString() {
		return "BranchChainPairStateMachine [branchChainPair=" + branchChainPair + "]";
	}

	@Override
	public double getFitness(GenericSolution solution) {
		@SuppressWarnings("unchecked")
		HashMap<String, Object> fitnessMap = (HashMap<String, Object>) solution.getCacheObject();
		return this.branchChainPair.calculateBranchChainPairFitness(fitnessMap);
	}
}
