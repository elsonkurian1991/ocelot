package it.unisa.ocelot.genetic.edges;

import java.util.Objects;

public class FType {
	private double fitnessValue;
	private boolean testCovered;
	private boolean testGenerated;
	private boolean first;
	
	public FType(double fitnessValue,boolean testCovered, boolean testGenerated, boolean first) {
		// TODO Auto-generated constructor stub
		this.fitnessValue =fitnessValue;
		this.testCovered =testCovered;
		this.testGenerated=testGenerated;
		this.first= first;
	}
	public boolean isFirst() {
		return first;
	}
	public void setFirst(boolean first) {
		this.first = first;
	}
	public boolean isTestGenerated() {
		return testGenerated;
	}
	public void setTestGenerated(boolean testGenerated) {
		this.testGenerated = testGenerated;
	}
	
	@Override
	public String toString() {
		return "FType [fitnessValue=" + fitnessValue + ", testCovered=" + testCovered + ", testGenerated="
				+ testGenerated + ", first=" + first + "]" + System.lineSeparator();
	}
	public double getFitnessValue() {
		return fitnessValue;
	}
	public void setFitnessValue(double fitnessValue) {
		this.fitnessValue = fitnessValue;
	}
	public boolean isTestCovered() {
		return testCovered;
	}
	public void setTestCovered(boolean firstTime) {
		this.testCovered = firstTime;
	}
	@Override
	public int hashCode() {
		return Objects.hash(first, fitnessValue, testCovered, testGenerated);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FType other = (FType) obj;
		return first == other.first
				&& Double.doubleToLongBits(fitnessValue) == Double.doubleToLongBits(other.fitnessValue)
				&& testCovered == other.testCovered && testGenerated == other.testGenerated;
	}

}
