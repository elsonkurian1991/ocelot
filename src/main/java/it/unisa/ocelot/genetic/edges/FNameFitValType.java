package it.unisa.ocelot.genetic.edges;

import java.util.Objects;

public class FNameFitValType {
    
	private String fName;
	private Double fitnessVal;
	public FNameFitValType() {
		
	}
	public FNameFitValType(String fName,Double fitnessVal) {
		// TODO Auto-generated constructor stub
		this.fName=fName;
		this.fitnessVal=fitnessVal;
	}
	public String getfName() {
		return fName;
	}
	public void setfName(String fName) {
		this.fName = fName;
	}
	public Double getFitnessVal() {
		return fitnessVal;
	}
	public void setFitnessVal(Double fitnessVal) {
		this.fitnessVal = fitnessVal;
	}
	@Override
	public int hashCode() {
		return Objects.hash(fName, fitnessVal);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FNameFitValType other = (FNameFitValType) obj;
		return Objects.equals(fName, other.fName) && Objects.equals(fitnessVal, other.fitnessVal);
	}
	@Override
	public String toString() {
		return " [fName=" + fName + ", fitnessVal=" + fitnessVal + "]";
	}

}
