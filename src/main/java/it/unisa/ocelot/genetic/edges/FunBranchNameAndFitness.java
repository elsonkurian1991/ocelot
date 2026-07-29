package it.unisa.ocelot.genetic.edges;

import java.util.Objects;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class FunBranchNameAndFitness {
    private final String funBranchName;
    private final double currFitnessVal;
	@Override
	public int hashCode() {
		return Objects.hash(currFitnessVal, funBranchName);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunBranchNameAndFitness other = (FunBranchNameAndFitness) obj;
		return Double.doubleToLongBits(currFitnessVal) == Double.doubleToLongBits(other.currFitnessVal)
				&& Objects.equals(funBranchName, other.funBranchName);
	}
	@Override
	public String toString() {
		return "FunBranchNameAndFitness [funBranchName=" + funBranchName + ", currFitnessVal=" + currFitnessVal + "]";
	}
	public String getFunBranchName() {
		return funBranchName;
	}
	public double getCurrFitnessVal() {
		return currFitnessVal;
	}
	public FunBranchNameAndFitness(String funBranchName, double currFitnessVal) {
		super();
		this.funBranchName = funBranchName;
		this.currFitnessVal = currFitnessVal;
	}


}
