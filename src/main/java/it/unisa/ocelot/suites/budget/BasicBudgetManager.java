package it.unisa.ocelot.suites.budget;

import it.unisa.ocelot.genetic.OcelotExperiment;
import jmetal.core.Solution;

/**
 * A basic budget manager. Gives to an experiment a 
 * fraction of its current budget, as it were splitting
 * it evenly between all the experiments that are still 
 * running. It does not grant more budget.
 * 
 * @author simone
 */
public final class BasicBudgetManager extends BudgetManager {
	public BasicBudgetManager(int pTotalBudget, int pNumberOfExperiments) {
		super(pTotalBudget, pNumberOfExperiments);
		System.err.println("Basic Budget Manager: Started with " + this.currentBudget + " for " + this.currentNumberOfExperiments);
	}
	
	public BasicBudgetManager(BudgetManager pOther) {
		super(pOther);
	}
	
	@Override
	public int getExperimentBudget(OcelotExperiment<?> pExperiment) {
		int result = this.currentBudget / this.currentNumberOfExperiments;
		System.err.println("Basic Budget Manager: Granted " + result + " as budget to " + (pExperiment == null ? "cascaded test generator" : ("experiment " + pExperiment.experimentName_)));
		return result; 
	};
	
	@Override
	public int askForMoreBudget(OcelotExperiment<?> pExperiment, Solution pSolution) {
		System.err.println("Basic Budget Manager: Ehi! You greedy... Sorry, no more budget granted");
		return 0;
	}
	
	@Override
	public void reportConsumedBudget(OcelotExperiment<?> pExperiment, int pConsumedBudget) {
		this.currentBudget -= pConsumedBudget;
		System.err.println("Basic Budget Manager: " + (pExperiment == null ? "Cascaded test generator" : ("Experiment " + pExperiment.experimentName_)) + " reported a budget consumption of " + pConsumedBudget + ". Current status: budget "+ this.currentBudget + " still available");
	}
	
	@Override
	public void reportConclusion(OcelotExperiment<?> pExperiment) {
		this.currentNumberOfExperiments--;
		System.err.println("Basic Budget Manager: " + (pExperiment == null ? "Cascaded test generator" : ("Experiment " + pExperiment.experimentName_)) + " concluded");
	}
	
	@Override
	public void setTotalBudget(int pTotalBudget) {
		this.currentBudget = pTotalBudget;	
		System.err.println("Basic Budget Manager: The currently available budget now is " + this.currentBudget);
	}
	
	@Override
	public void setNumberOfExperiments(int pNumberOfExperiments) {
		this.currentNumberOfExperiments = pNumberOfExperiments;
		System.err.println("Basic Budget Manager: The number of remaining experiments now is " + this.currentNumberOfExperiments);
	}
}
