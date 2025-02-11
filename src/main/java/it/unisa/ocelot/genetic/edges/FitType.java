package it.unisa.ocelot.genetic.edges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FitType {
	private boolean testCovered;
	private boolean testGenerated;
	private boolean first;
	private String argumentList;
	private boolean fillFnameVal;
    private List<FNameFitValType> Fname_Val= new ArrayList<FNameFitValType>();
    //private TestObjStateMachine testobjSM= new TestObjStateMachine();
    
	public FitType(boolean testCovered,boolean testGenerated,boolean first,String argumentList,boolean fillFnameVal,List<FNameFitValType> Fname_Val) {
		// TODO Auto-generated constructor stub
		this.testCovered=testCovered;
		this.testGenerated=testGenerated;
		this.first=first;
		this.argumentList=argumentList;
		this.fillFnameVal=fillFnameVal;
		this.Fname_Val=Fname_Val;
		//this.testobjSM=testobjSM;
	}
	public FitType(boolean testCovered,boolean testGenerated,boolean first,String argumentList,boolean fillFnameVal) {
		// TODO Auto-generated constructor stub
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
	@Override
	public String toString() {
		return "FitType [testCovered=" + testCovered + ", testGenerated=" + testGenerated + ", first=" + first
				+ ", argumentList=" + argumentList + ", fillFnameVal=" + fillFnameVal + ", Fname_Val=" + Fname_Val
				+ "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(Fname_Val, argumentList, fillFnameVal, first, testCovered, testGenerated);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FitType other = (FitType) obj;
		return Objects.equals(Fname_Val, other.Fname_Val) && Objects.equals(argumentList, other.argumentList)
				&& fillFnameVal == other.fillFnameVal && first == other.first && testCovered == other.testCovered
				&& testGenerated == other.testGenerated;
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
		return argumentList;
	}

	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}

	public List<FNameFitValType> getFname_Val() {
		return Fname_Val;
	}

	public void setFname_Val(List<FNameFitValType> fname_Val) {
		Fname_Val = fname_Val;
	}
	



}
