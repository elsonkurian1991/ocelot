package it.unisa.ocelot.genetic.edges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO what's that???
public class FitType {
	private boolean testCovered;
	private boolean testGenerated;
	private boolean first;
	private String argumentList;
	private boolean fillFnameVal;
    private List<FNameFitValType> fnameVal = new ArrayList<>();
    
	public FitType(boolean testCovered, boolean testGenerated, boolean first, String argumentList, boolean fillFnameVal, List<FNameFitValType> Fname_Val) {
		this.testCovered=testCovered;
		this.testGenerated=testGenerated;
		this.first=first;
		this.argumentList=argumentList;
		this.fillFnameVal=fillFnameVal;
		this.fnameVal=Fname_Val;
		//this.testobjSM=testobjSM;
	}
	public FitType(boolean testCovered,boolean testGenerated,boolean first,String argumentList,boolean fillFnameVal) {
		this.testCovered=testCovered;
		this.testGenerated=testGenerated;
		this.first=first;
		this.argumentList=argumentList;
		this.fillFnameVal=fillFnameVal;
	}
	
	public boolean isFillFnameVal() {
		return fillFnameVal;
	}
	
	public void setFillFnameVal(boolean fillFnameVal) {
		this.fillFnameVal = fillFnameVal;
	}

	public boolean isTestCovered() {
		return testCovered;
	}

	public void setTestCovered(boolean testCovered) {
		this.testCovered = testCovered;
	}

	public boolean isTestGenerated() {
		return testGenerated;
	}

	public void setTestGenerated(boolean testGenerated) {
		this.testGenerated = testGenerated;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public String getArgumentList() {
		return this.argumentList;
	}

	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}

	public List<FNameFitValType> getFname_Val() {
		return this.fnameVal;
	}

	public void setFname_Val(List<FNameFitValType> fname_Val) {
		this.fnameVal = fname_Val;
	}
	
	
	@Override
	public String toString() {
		return "FitType [testCovered=" + this.testCovered + ", testGenerated=" + this.testGenerated + ", first=" + this.first
				+ ", argumentList=" + this.argumentList + ", fillFnameVal=" + this.fillFnameVal + ", Fname_Val=" + this.fnameVal
				+ "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.fnameVal, this.argumentList, this.fillFnameVal, this.first, this.testCovered, this.testGenerated);
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
		FitType other = (FitType) obj;
		return Objects.equals(this.fnameVal, other.fnameVal) && 
				Objects.equals(this.argumentList, other.argumentList) && 
				this.fillFnameVal == other.fillFnameVal && 
				this.first == other.first && testCovered == other.testCovered && testGenerated == other.testGenerated;
	}
}
