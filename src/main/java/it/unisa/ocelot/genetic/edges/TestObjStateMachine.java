package it.unisa.ocelot.genetic.edges;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

enum State{
	zeroCover,
	oneCover,
	twoCover
}

public class TestObjStateMachine implements Serializable {
	private static final long serialVersionUID = 1L;
	String testObjOne;
	double fitValOne;
	String testObjTwo;
	double fitValTwo;
	State currState;

	boolean isGenerated;
	String argumentList;
	
	public TestObjStateMachine(String testObjOne,double fitValOne,String testObjTwo,double fitValTwo,State currState, boolean isGenerated, String argumentList) {
		this.testObjOne=testObjOne;
		this.testObjTwo=testObjTwo;
		this.fitValOne=fitValOne;
		this.fitValTwo=fitValTwo;
		this.currState=currState;
		this.isGenerated=false;
		this.argumentList=argumentList;
	}
	public TestObjStateMachine(String testObjOne,double fitValOne,String testObjTwo,double fitValTwo) {
		this.testObjOne=testObjOne;
		this.testObjTwo=testObjTwo;
		this.fitValOne=fitValOne;
		this.fitValTwo=fitValTwo;
		this.currState=State.zeroCover;
		this.isGenerated=false;
		this.argumentList="null";
	}
	
	public TestObjStateMachine(String testObjOne,String testObjTwo) {
		this.testObjOne=testObjOne;
		this.testObjTwo=testObjTwo;
		this.fitValOne=1;
		this.fitValTwo=1;
		this.currState=State.zeroCover;
		this.isGenerated=false;
		this.argumentList="null";
	}

	public String getArgumentList() {
		return argumentList;
	}
	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}
	
	public boolean isGenerated() {
		return isGenerated;
	}
	public void setGenerated(boolean isGenerated) {
		this.isGenerated = isGenerated;
	}
	@Override
	public int hashCode() {
		return Objects.hash(currState, fitValOne, fitValTwo, testObjOne, testObjTwo);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestObjStateMachine other = (TestObjStateMachine) obj;
		return currState == other.currState
				&& Double.doubleToLongBits(fitValOne) == Double.doubleToLongBits(other.fitValOne)
				&& Double.doubleToLongBits(fitValTwo) == Double.doubleToLongBits(other.fitValTwo)
				&& Objects.equals(testObjOne, other.testObjOne) && Objects.equals(testObjTwo, other.testObjTwo);
	}
	public String getSMPairName() {
		return testObjOne+","+testObjTwo+"\n";
	}
	public String getTestObjOne() {
		return testObjOne;
	}
	public void setTestObjOne(String testObjOne) {
		this.testObjOne = testObjOne;
	}
	public double getFitValOne() {
		return fitValOne;
	}
	public void setFitValOne(double fitValOne) {
		this.fitValOne = fitValOne;
	}
	public String getTestObjTwo() {
		return testObjTwo;
	}
	public void setTestObjTwo(String testObjTwo) {
		this.testObjTwo = testObjTwo;
	}
	public double getFitValTwo() {
		return fitValTwo;
	}
	public void setFitValTwo(double fitValTwo) {
		this.fitValTwo = fitValTwo;
	}
	public State getCurrState() {
		return currState;
	}
	public void setCurrState(State currState) {
		this.currState = currState;
	}
	@Override
	public String toString() {
		return "TestObjStateMachine [testObjOne=" + testObjOne + ", fitValOne=" + fitValOne + ", testObjTwo="
				+ testObjTwo + ", fitValTwo=" + fitValTwo + ", currState=" + currState 
				+ ", isGenerated=" + isGenerated + "]";
	}
	

	/*public void transitionState() {
		switch (currState) {
		case zeroCover: currState = State.zeroCover;
		break;
		case oneCover: currState= State.oneCover;
		break;
		case twoCover: currState= State.twoCover;
		break;
		}
		
	}*/
	public  void transition(FunBranchNameAndFitness infoFromLinebr) {  

		if(this.getTestObjOne().contentEquals(infoFromLinebr.getFunBranchName())) {
			if(infoFromLinebr.getCurrFitnessVal()==0.0) {
				this.setCurrState(State.oneCover);				
			}
			else {
				this.setCurrState(State.zeroCover);
			}
			this.setFitValOne(infoFromLinebr.getCurrFitnessVal());

		}
		if(this.getTestObjTwo().contentEquals(infoFromLinebr.getFunBranchName())) {

			if (this.currState==State.oneCover) {
				if(infoFromLinebr.getCurrFitnessVal()==0.0) {
					this.setCurrState(State.twoCover);				
				}
				else {
					this.setCurrState(State.oneCover);
				}										
			}

			this.setFitValTwo(infoFromLinebr.getCurrFitnessVal());
		}

	}
	
	public  boolean isCovered () {
		return this.currState==State.twoCover;
	}


}


