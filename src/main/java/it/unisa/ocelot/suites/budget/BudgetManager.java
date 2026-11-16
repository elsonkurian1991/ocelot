package it.unisa.ocelot.suites.budget;

import it.unisa.ocelot.genetic.OcelotExperiment;
import jmetal.core.Solution;

public abstract class BudgetManager {
	protected final int initialBudget;
	protected final int initialNumberOfExperiments;
	protected int currentBudget;
	protected int currentNumberOfExperiments;
	
	protected BudgetManager(int pTotalBudget, int pNumberOfExperiments) {
		this.initialBudget = pTotalBudget;
		this.initialNumberOfExperiments = pNumberOfExperiments;
		this.currentBudget = this.initialBudget;
		this.currentNumberOfExperiments = this.initialNumberOfExperiments;
	}
	
	public BudgetManager(BudgetManager pOther) {
		this.initialBudget = pOther.initialBudget;
		this.initialNumberOfExperiments = pOther.initialNumberOfExperiments;
		this.currentBudget = pOther.currentBudget;
		this.currentNumberOfExperiments = pOther.currentNumberOfExperiments;
	}
	
	/**
	 * Returns the current budget of this manager.
	 * 
	 * @return the remaining total budget that can be assigned
	 *         to the remaining experiments.
	 */
	public final int getCurrentBudget() {
		return this.currentBudget;
	}
	
	/**
	 * Returns the consumed budget of this manager.
	 * 
	 * @return the total budget that has been assigned until
	 *         now to the experiments.
	 */
	public final int getConsumedBudget() {
		return this.initialBudget - this.currentBudget;
	}
	
	/**
	 * Returns the budget allocated for a given experiment.
	 * 
	 * @param pExperiment the {@link OcelotExperiment} asking for the budget.
	 * @return an {@code int} greater or equal to zero, the granted budget.
	 */
	public abstract int getExperimentBudget(OcelotExperiment<?> pExperiment);
	
	/**
	 * Returns the budget allocated for a given experiment (for 
	 * cascading test generators).
	 * 
	 * @return an {@code int} greater or equal to zero, the granted budget.
	 */
	public final int getExperimentBudget() {
		return getExperimentBudget(null);
	}
	
	/**
	 * Asks for an extra budget for a given experiment/solution combination. 
	 * If the handler allows it, this method returns the amount of extra
	 * evaluations allowed. Otherwise, returns 0.
	 * 
	 * @param pExperiment the {@link OcelotExperiment} asking for the extra budget.
	 * @param pSolution the {@link Solution} for which the extra budget is necessary.
	 * @return an {@code int} greater or equal to zero, the granted extra budget.
	 */
	public abstract int askForMoreBudget(OcelotExperiment<?> pExperiment, Solution pSolution);
	
	/**
	 * Used to report the amount 
	 * of budget consumed by an experiment
	 * execution.
	 * 
	 * @param pExperiment an {@link OcelotExperiment}.
	 * @param pConsumedBudget an {@code int}, the consumed budget w.r.t. the 
	 *        total budget received by the client.
	 */
	public abstract void reportConsumedBudget(OcelotExperiment<?> pExperiment, int pConsumedBudget);
	
	/**
	 * Used to report the amount 
	 * of budget consumed by an experiment
	 * execution (for cascading test generators).
	 * 
	 * @param pConsumedBudget an {@code int}, the consumed budget w.r.t. the 
	 *        total budget received by the client.
	 */
	public final void reportConsumedBudget(int pConsumedBudget) {
		reportConsumedBudget(null, pConsumedBudget);
	}
	
	/**
	 * Used to report the end of an experiment.
	 * 
	 * @param pExperiment an {@link OcelotExperiment} that ended.
	 */
	public abstract void reportConclusion(OcelotExperiment<?> pExperiment);
	
	/**
	 * Used to report the end of an invocation of a
	 * cascading test generator.
	 */
	public final void reportConclusion() {
		reportConclusion(null);
	}
	
	/**
	 * Sets the total budget. Used by the cascading test
	 * suite generator to distribute budget to the 
	 * generators in cascade. 
	 * 
	 * @param pTotalBudget the new total budget.
	 */
	public abstract void setTotalBudget(int pTotalBudget);
	
	/**
	 * Sets the total number of experiments.
	 * 
	 * @param pNumberOfExperiments the new number of experiments.
	 */
	public abstract void setNumberOfExperiments(int pNumberOfExperiments);
}
