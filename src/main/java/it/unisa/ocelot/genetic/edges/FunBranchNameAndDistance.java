package it.unisa.ocelot.genetic.edges;

import java.util.Objects;

public class FunBranchNameAndDistance {
    private String funBranchName;
    private double branchDistance;
    	
	public FunBranchNameAndDistance(String funBranchName, double branchDistance) {
		this.funBranchName = funBranchName;
		this.branchDistance = branchDistance;
	}
	
	public String getFunBranchName() {
		return this.funBranchName;
	}
	
	public void setFunBranchName(String pFunBranchName) {
		this.funBranchName = pFunBranchName;
	}
	
	public double getBranchDistance() {
		return this.branchDistance;
	}
	
	public void setBranchDistance(double pBranchDistance) {
		this.branchDistance = pBranchDistance;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.funBranchName, this.branchDistance);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FunBranchNameAndDistance other = (FunBranchNameAndDistance) obj;
		return Double.doubleToLongBits(this.branchDistance) == Double.doubleToLongBits(other.branchDistance)
				&& Objects.equals(this.funBranchName, other.funBranchName);
	}
	
	@Override
	public String toString() {
		return "FunBranchNameAndDistance [funBranchName=" + this.funBranchName + ", branchDistance=" + this.branchDistance + "]";
	}
}
