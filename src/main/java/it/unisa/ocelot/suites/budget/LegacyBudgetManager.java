package it.unisa.ocelot.suites.budget;

import it.unisa.ocelot.genetic.OcelotExperiment;
import jmetal.core.Solution;

/**
 * A legacy budget manager. Gives a same (fixed) 
 * budget to all the experiments asking for it.
 * It does not grant more budget.
 * 
 * @author simone
 */

public final class LegacyBudgetManager extends BudgetManager {
	private final int budgetPerExperiment;
	
	public LegacyBudgetManager(int pTotalBudget, int pNumberOfExperiments) {
		super(pTotalBudget, pNumberOfExperiments);
		this.budgetPerExperiment = this.initialBudget / this.initialNumberOfExperiments;
		
		System.err.println("Warning: you are using a Legacy Budget Manager. It allows only one experiment. Currently the total number of experiments is " + pNumberOfExperiments);
		System.err.println("Legacy Budget Manager: Started with " + this.currentBudget + " for " + this.currentNumberOfExperiments);
	}
	
	public LegacyBudgetManager(BudgetManager pOther) {
		super(pOther);
		this.budgetPerExperiment = this.initialBudget / this.initialNumberOfExperiments;
	}
	
	@Override
	public int getExperimentBudget(OcelotExperiment<?> pExperiment) {
		int result = Math.min(this.currentBudget, this.budgetPerExperiment) ; //caps on remaining budget
		System.err.println("Legacy Budget Manager: Granted " + result + " as budget to " + (pExperiment == null ? "cascaded test generator" : ("experiment " + pExperiment.experimentName_)));
		return result;
	}
	
	@Override
	public int askForMoreBudget(OcelotExperiment<?> pExperiment, Solution pSolution) {
		System.err.println("Legacy Budget Manager: Ehi! You greedy... Sorry, no more budget granted");
		return 0;
		//TODO perhaps grant some additional budget in case some earlier experiment gave back a part of its own budget
	}
	
	@Override
	public void reportConsumedBudget(OcelotExperiment<?> pExperiment, int pConsumedBudget) {
		this.currentBudget -= pConsumedBudget;
		System.err.println("Legacy Budget Manager: " + (pExperiment == null ? "Cascaded test generator" : ("Experiment " + pExperiment.experimentName_)) + " reported a total budget consumption of " + pConsumedBudget + ". Current status: budget "+ this.currentBudget + " still available for " + this.currentNumberOfExperiments + " remaining experiments");
	};
	
	@Override
	public void reportConclusion(OcelotExperiment<?> pExperiment) {
		this.currentNumberOfExperiments--;
		System.err.println("Legacy Budget Manager: " + (pExperiment == null ? "Cascaded test generator" : ("Experiment " + pExperiment.experimentName_)) + " concluded");
	}
	
	@Override
	public void setTotalBudget(int pTotalBudget) {
		this.currentBudget = pTotalBudget;	
		System.err.println("Basic Budget Manager: The currently available budget now is " + this.currentBudget);
	}
	
	@Override
	public void setNumberOfExperiments(int pNumberOfExperiments) {	
		this.currentNumberOfExperiments = pNumberOfExperiments;
		System.err.println("Legacy Budget Manager: The number of remaining experiments now is " + this.currentNumberOfExperiments);
	}
}
