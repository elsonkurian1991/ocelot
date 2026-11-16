package it.unisa.ocelot.genetic.edges;

import java.io.Serializable;
import java.util.Objects;

enum State {
	NONE_COVERED,
	ONE_COVERED,
	TWO_COVERED
}

public class TestObjStateMachine implements Serializable {
	private static final long serialVersionUID = 1L;
	
	String testObjOne;
	String testObjTwo;
	
	double bdOne;
	double bdTwo;
	State currState;
	boolean isGenerated;
	String argumentList;
	
	public TestObjStateMachine(String testObjOne, String testObjTwo) {
		this.testObjOne = testObjOne;
		this.testObjTwo = testObjTwo;
		
		this.bdOne = 1;
		this.bdTwo = 1;
		this.currState = State.NONE_COVERED;
		this.isGenerated = false;
		this.argumentList = "null";
	}

	public String getArgumentList() {
		return this.argumentList;
	}
	
	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}
	
	public boolean isGenerated() {
		return this.isGenerated;
	}
	
	public void setGenerated() {
		this.isGenerated = true;
	}
	
	public String getSMPairName() {
		return this.testObjOne + "," + this.testObjTwo + "\n";
	}
	
	public String getTestObjOne() {
		return this.testObjOne;
	}
	
	public void setTestObjOne(String testObjOne) {
		this.testObjOne = testObjOne;
	}
	
	public String getTestObjTwo() {
		return this.testObjTwo;
	}
	
	public void setTestObjTwo(String testObjTwo) {
		this.testObjTwo = testObjTwo;
	}
	
	public double getBdOne() {
		return this.bdOne;
	}
	
	public void setBdOne(double bdOne) {
		this.bdOne = bdOne;
	}
	
	public double getBdTwo() {
		return this.bdTwo;
	}
	
	public void setBdTwo(double bdTwo) {
		this.bdTwo = bdTwo;
	}
	
	public State getCurrState() {
		return this.currState;
	}
	
	public void setCurrState(State currState) {
		this.currState = currState;
	}
	
	@Override
	public String toString() {
		return "TestObjStateMachine [testObjOne=" + this.testObjOne + ", bdOne=" + this.bdOne + ", testObjTwo="
				+ this.testObjTwo + ", bdTwo=" + this.bdTwo + ", currState=" + this.currState + ", isGenerated=" + this.isGenerated
				+ ", argumentList=" + this.argumentList + "]";
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
	public void transition(FunBranchNameAndDistance infoFromLinebr) {  
		if (getTestObjOne().contentEquals(infoFromLinebr.getFunBranchName())) {
			if (infoFromLinebr.getBranchDistance() == 0.0) {
				setCurrState(State.ONE_COVERED);
			} else {
				setCurrState(State.NONE_COVERED);
				setBdTwo(1);
			}
			setBdOne(infoFromLinebr.getBranchDistance());
		}
		if(this.getTestObjTwo().contentEquals(infoFromLinebr.getFunBranchName())) {

			if (this.currState == State.ONE_COVERED) {
				if (infoFromLinebr.getBranchDistance() == 0.0) {
					setCurrState(State.TWO_COVERED);				
				} else {
					setCurrState(State.ONE_COVERED);
				}	
				setBdTwo(infoFromLinebr.getBranchDistance());
			}
		}
	}
	
	public boolean isCovered() {
		return (this.currState == State.TWO_COVERED);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.currState, this.bdOne, this.bdTwo, this.testObjOne, this.testObjTwo);
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
		TestObjStateMachine other = (TestObjStateMachine) obj;
		return this.currState == other.currState
				&& Double.doubleToLongBits(this.bdOne) == Double.doubleToLongBits(other.bdOne)
				&& Double.doubleToLongBits(this.bdTwo) == Double.doubleToLongBits(other.bdTwo)
				&& Objects.equals(this.testObjOne, other.testObjOne) 
				&& Objects.equals(this.testObjTwo, other.testObjTwo);
	}

}


