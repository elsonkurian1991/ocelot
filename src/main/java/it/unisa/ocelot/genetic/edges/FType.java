package it.unisa.ocelot.genetic.edges;

import java.util.Objects;

public class FType {
	private double fitnessValue;
	private boolean testCovered;
	private boolean testGenerated;
	private boolean first;
	
	public FType(double fitnessValue,boolean testCovered, boolean testGenerated, boolean first) {
		// TODO Auto-generated constructor stub
		this.fitnessValue = fitnessValue;
		this.testCovered = testCovered;
		this.testGenerated = testGenerated;
		this.first = first;
	}
	
	public boolean isFirst() {
		return this.first;
	}

	public boolean isTestGenerated() {
		return this.testGenerated;
	}
	
	@Override
	public String toString() {
		return "FType [fitnessValue=" + fitnessValue + ", testCovered=" + testCovered + ", testGenerated="
				+ testGenerated + ", first=" + first + "]" + System.lineSeparator();
	}
	
	public double getFitnessValue() {
		return this.fitnessValue;
	}
	
	public boolean isTestCovered() {
		return this.testCovered;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.fitnessValue, this.testCovered, this.testGenerated, this.first);
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
		FType other = (FType) obj;
		return this.first == other.first
				&& Double.compare(this.fitnessValue, other.fitnessValue) == 0
				&& this.testCovered == other.testCovered && this.testGenerated == other.testGenerated;
	}

}
