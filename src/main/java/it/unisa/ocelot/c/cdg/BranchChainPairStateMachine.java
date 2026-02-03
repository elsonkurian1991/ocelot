package it.unisa.ocelot.c.cdg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;
import it.unisa.ocelot.genetic.objectives.BranchDistanceCache;
import it.unisa.ocelot.genetic.objectives.GenericObjective;

enum State{
	zeroCover,
	oneCover,
	twoCover
}
public class BranchChainPairStateMachine extends GenericObjective implements Serializable {
	private static final long serialVersionUID = 1L;
	int objectiveID;
	BranchChain branchChainOne;
	String testObjBCOne;
	double fitValBCOne;
	BranchChain branchChainTwo;
	String testObjBCTwo;
	double fitValBCTwo;
	State currState;

	static boolean isGenerated;
	String argumentList;
	static int counter=0;
	
	public BranchChainPairStateMachine(BranchChain branchChainOne, String testObjBCOne, double fitValBCOne,
			BranchChain branchChainTwo, String testObjBCTwo, double fitValBCTwo, State currState, boolean isGenerated,
			String argumentList) {
		super(isGenerated, counter);
		this.branchChainOne = branchChainOne;
		this.testObjBCOne = testObjBCOne;
		this.fitValBCOne = fitValBCOne;
		this.branchChainTwo = branchChainTwo;
		this.testObjBCTwo = testObjBCTwo;
		this.fitValBCTwo = fitValBCTwo;
		this.currState = currState;
		this.isGenerated = isGenerated;
		this.argumentList = argumentList;
	}
	
	public BranchChainPairStateMachine(int objID,BranchChain branchChainOne,BranchChain branchChainTwo) {
		super(isGenerated, objID);
		this.objectiveID=objID;
		this.branchChainOne = branchChainOne;
		this.testObjBCOne = fetchObjName(branchChainOne);
		this.fitValBCOne = fetchDefaultFitness(branchChainOne);
		this.branchChainTwo = branchChainTwo;
		this.testObjBCTwo = fetchObjName(branchChainTwo);
		this.fitValBCTwo = fetchDefaultFitness(branchChainTwo);
		this.currState = State.zeroCover;
		this.isGenerated = false;
		this.argumentList = "null";
	}
	
	
	private double fetchDefaultFitness(BranchChain branchChain) {
		double fitness=1.0;
		/*int pathLen=branchChain.getPath().size();
		if(pathLen>1) {
			return fitness/pathLen;
		}*/
		return fitness;
	}

	private String fetchObjName(BranchChain branchChain) {
		//String branchChainName;
		
		return branchChain.getLabel();
	}

	public BranchChain getBranchChainOne() {
		return branchChainOne;
	}
	public void setBranchChainOne(BranchChain branchChainOne) {
		this.branchChainOne = branchChainOne;
	}
	public String getTestObjBCOne() {
		return testObjBCOne;
	}
	public void setTestObjBCOne(String testObjBCOne) {
		this.testObjBCOne = testObjBCOne;
	}
	public double getFitValBCOne() {
		return fitValBCOne;
	}
	public void setFitValBCOne(double fitValBCOne) {
		this.fitValBCOne = fitValBCOne;
	}
	public BranchChain getBranchChainTwo() {
		return branchChainTwo;
	}
	public void setBranchChainTwo(BranchChain branchChainTwo) {
		this.branchChainTwo = branchChainTwo;
	}
	public String getTestObjBCTwo() {
		return testObjBCTwo;
	}
	public void setTestObjBCTwo(String testObjBCTwo) {
		this.testObjBCTwo = testObjBCTwo;
	}
	public double getFitValBCTwo() {
		return fitValBCTwo;
	}
	public void setFitValBCTwo(double fitValBCTwo) {
		this.fitValBCTwo = fitValBCTwo;
	}
	public State getCurrState() {
		return currState;
	}
	public void setCurrState(State currState) {
		this.currState = currState;
	}
	public boolean isGenerated() {
		return isGenerated;
	}
	public void setGenerated(boolean isGenerated) {
		this.isGenerated = isGenerated;
	}
	public String getArgumentList() {
		return argumentList;
	}
	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public String toString() {
		return "BranchChainPairStateMachine [branchChainOne=" + branchChainOne + ", testObjBCOne=" + testObjBCOne
				+ ", fitValBCOne=" + fitValBCOne + ", branchChainTwo=" + branchChainTwo + ", testObjBCTwo="
				+ testObjBCTwo + ", fitValBCTwo=" + fitValBCTwo + ", currState=" + currState + ", isGenerated="
				+ isGenerated + ", argumentList=" + argumentList + "]";
	}

	public  double getFitness(Object[][][] arguments) {
		return calculateFitnessForBranchChains(arguments);
	}

	private double calculateFitnessForBranchChains(Object[][][] arguments) {
		double fitness = 0.0;
		
		//System.out.println(BranchChainManager.newFitnessHashMap);// in new fitness hash map all the fitnewss values are stored
		//HashMap<String, Double> branchDistances = BranchDistanceCache.getBranchDistances();
		double fitValOne = 0.0;
		FunBranchNameAndFitness infoFromLinebr1;
		BranchChain bcOne = this.branchChainOne;	
		//System.out.println("BC One:"+bcOne.toString()+bcOne.getLabel());
		
		//here we need to consider the first branch chain.
		//String objOne= this.getBranchChainOne().getLabel();
		fitValOne = computeBCFitness( bcOne);
		BranchChain bcTwo = this.branchChainTwo;
		double fitValTwo = computeBCFitness( bcTwo);
		
		fitness = (fitValOne + fitValTwo)/2;
		if (fitness == Double.POSITIVE_INFINITY) {
			fitness = Double.MAX_VALUE;
		}
		
		return fitness;
	}

	private double computeBCFitness( BranchChain bc) {
		int bcPathSize=bc.getPath().size();
		double fitVal=0.0;
		int numObj=0;
		
		for(int i=0;i<bcPathSize;i++) {
			String bcLabel = bc.getPath().get(i).getBranchConditionLabel();			
			if(bcLabel!=null){
				numObj++;
				//this is branch with conditions
	
				Double testObj = BranchChainManager.newFitnessHashMap.get(bcLabel);
				if(testObj!=null) {
					//infoFromLinebr1 = new FunBranchNameAndFitness(bcLabel, testObj1);
					fitVal += testObj;
				}
				else {
					fitVal+=1.0;
					//infoFromLinebr1 = new FunBranchNameAndFitness(bcLabel, 1);
					//transition(infoFromLinebr1);
				}
				System.out.println(bcLabel+"->"+testObj);
			}			
		}
		if(numObj>0) {
			fitVal =fitVal/(double)numObj;
		}
		
		//System.out.println(bc.getLabel()+"="+fitVal);
		return fitVal;
	}
	
	public  void transition(FunBranchNameAndFitness infoFromLinebr) {  // edit this code to handle both branch at a time.

		if(this.getTestObjBCTwo().contentEquals(infoFromLinebr.getFunBranchName())) {
			if(infoFromLinebr.getCurrFitnessVal()==0.0) {
				this.setCurrState(State.oneCover);
				
			}
			else {
				this.setCurrState(State.zeroCover);
				this.setFitValBCTwo(1);
			}
			this.setFitValBCOne(infoFromLinebr.getCurrFitnessVal());
		}
		if(this.getTestObjBCTwo().contentEquals(infoFromLinebr.getFunBranchName())) {

			if (this.currState==State.oneCover) {
				if(infoFromLinebr.getCurrFitnessVal()==0.0) {
					this.setCurrState(State.twoCover);				
				}
				else {
					this.setCurrState(State.oneCover);
				}	
				this.setFitValBCTwo(infoFromLinebr.getCurrFitnessVal());
			}

			
		}

	}
	

}
