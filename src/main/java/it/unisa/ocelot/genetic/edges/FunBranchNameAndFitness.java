package it.unisa.ocelot.genetic.edges;

import java.util.Objects;

public class FunBranchNameAndFitness {
    private String funBranchName;
    private double currFitnessVal;
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
	public void setFunBranchName(String funBranchName) {
		this.funBranchName = funBranchName;
	}
	public double getCurrFitnessVal() {
		return currFitnessVal;
	}
	public void setCurrFitnessVal(double currFitnessVal) {
		this.currFitnessVal = currFitnessVal;
	}
	public FunBranchNameAndFitness(String funBranchName, double currFitnessVal) {
		super();
		this.funBranchName = funBranchName;
		this.currFitnessVal = currFitnessVal;
	}
	public FunBranchNameAndFitness() {
		// TODO Auto-generated constructor stub
	}

}
