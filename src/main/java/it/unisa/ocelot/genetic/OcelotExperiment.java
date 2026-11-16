package it.unisa.ocelot.genetic;

import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.algorithms.AlgorithmStats;
import it.unisa.ocelot.genetic.algorithms.SeedableAlgorithm;
import it.unisa.ocelot.suites.budget.BudgetManager;

import jmetal.core.Algorithm;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.experiments.Experiment;
import jmetal.experiments.Settings;
import jmetal.util.JMException;

/**
 * Experiment with a single algorithm, a single problem, 
 * a single objective, a single run, and a single solution 
 * (plus possibly a number of serendipitous solutions).
 * 
 * @author simone
 */
public abstract class OcelotExperiment<T> extends Experiment {	
	/**
	 * Peruse it in subclasses to get 
	 * configuration stuff.
	 */
	protected final ConfigManager configManager;

	/**
	 * Peruse it in subclasses if you want to
	 * adjust the budget for some reason.
	 */
	protected final BudgetManager budgetManager;

	/**
	 * An alias to algorithms[0], cast to 
	 * OcelotAlgorithm. Peruse it in subclasses,
	 * but only after invocation of 
	 * {@link #algorithmSettings(String, int, Algorithm[])}
	 * that will initialize it, and do not change its
	 * value to keep coherence with {@code this.algorithm}
	 * (or baaaaad things may happen).
	 */
	protected OcelotAlgorithm algorithm;

	/**
	 * Another alias to algorithms[0], cast to 
	 * SerendipitousAlgorithm<T>, if the
	 * algorithm is serendipitous, otherwise null. 
	 * Peruse it in subclasses,
	 * but only after invocation of 
	 * {@link #algorithmSettings(String, int, Algorithm[])}
	 * that will initialize it, and do not change its
	 * value to keep coherence with {@code this.algorithm}
	 * (or baaaaad things may happen).
	 */
	protected SerendipitousAlgorithm<T> serendipitousAlgorithm;
	
	/**
	 * The serendipitous potentials. Empty by default,
	 * to fill if the algorithm is serendipitous.
	 */
	private final Set<T> serendipitousPotentials;
	
	/**
	 * The serendipitous solutions, if there are any.
	 */
	private final Set<Solution> serendipitousSolutions;
		
	/**
	 * The population used as initial seed.
	 */
	private final SolutionSet seed;
	
	/**
	 * How many members of the initial seed
	 * population must be actually
	 * used to seed the algorithm.
	 */
	private final int keepOfSeed;
	
	/**
	 * Constructor (with seeding).
	 * 
	 * @param pConfigManager a {@link ConfigManager}.
	 * @param pBudgetManager a {@link BudgetManager}.
	 * @param pSeed a {@link SolutionSet}, the initial seed population.
	 * @param pKeepOfSeed an {@code int}, the number of
	 *        the solutions in the initial population that must be
	 *        actually used to seed the algorithm.
	 */
	protected OcelotExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager, SolutionSet pSeed, int pKeepOfSeed) {
		//TODO Use these parameters instead of refusing everything and putting dummy values in them.
		this.experimentBaseDirectory_ = pConfigManager.getResultsFolder();
		this.independentRuns_ = pConfigManager.getExperimentRuns(); //TODO is this parameter ever used?
		this.algorithmNameList_ = new String[] { pConfigManager.getAlgorithm() };
		this.algorithmSettings_ = new Settings[1];
		this.paretoFrontFile_ = new String[2];
		this.indicatorList_ = new String[] { "HV", "SPREAD", "EPSILON" };
		
		this.configManager = pConfigManager;
		this.budgetManager = pBudgetManager;
		this.serendipitousPotentials = new HashSet<>();
		this.serendipitousSolutions = new HashSet<>();
		this.seed = pSeed;
		this.keepOfSeed = pKeepOfSeed;
	}
	
	/**
	 * Constructor (without seeding).
	 * 
	 * @param pConfigManager a {@link ConfigManager}.
	 * @param pBudgetManager a {@link BudgetManager}.
	 */
	protected OcelotExperiment(ConfigManager pConfigManager, BudgetManager pBudgetManager) {
		this(pConfigManager, pBudgetManager, null, 0);
	}
	
	public abstract Class<T> getTargetClass();
	
	/**
	 * Sets the serendipitous potentials for this algorithm,
	 * in the case it is serendipitous.
	 * 
	 * @param pPotentials
	 */
	//TODO delete this method and put it into the algorithm's settings, or in another place
	public final void setSerendipitousPotentials(Set<T> pPotentials) {
		this.serendipitousPotentials.clear();
		this.serendipitousPotentials.addAll(pPotentials);
	}

	/**
	 * You don't need to explicitly invoke this method
	 * anymore, since it will be invoked by {@link #basicRun()}
	 * as a first thing, to ensure that your algorithm
	 * will be correctly configured before running.
	 * To define the algorithm settings
	 * you must override {@link #algorithmSettingsOcelot(Algorithm[])}
	 * in a subclass. This method will invoke {@link #algorithmSettingsOcelot(Algorithm[])}
	 * doing all the correctness checks before and after
	 * invocation.<br />
	 * 
	 * If for any reason you need to invoke this method by 
	 * yourself (which is always preferrable to invoking
	 * {@link #algorithmSettingsOcelot(Algorithm[])})
	 * please ensure its preconditions
	 * (i.e. it must be {@code this.algorithm.length == 1}, and
	 * {@code (this.algorithm[0] == null || this.algorithm[0] instanceof OcelotAlgorithm})
	 * and its postconditions (i.e., it must be {@code this.algorithm[0] instanceof OcelotAlgorithm}.
	 * 
	 * @param problemName will be ignored (no problem customization).
	 * @param problemId will be ignored (no problem customization).
	 * @param algorithms it always must be equal to {@code this.algorithm}
	 *        (what else would you want to set???).
	 * @throws RuntimeException if any invocation preconditions or postconditions
	 *         are violated, or if {@link #algorithmSettingsOcelot(Algorithm[])}
	 *         throws a {@link JMException} to signal failure while
	 *         creating the algorithm.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void algorithmSettings(String problemName, int problemId, Algorithm[] algorithms) {
		//note that algorithmSettingsOcelot may change the 
		//algorithm array and replace algorithm[0] with 
		//another implementation: That's why we get it 
		//(and recheck it) *after* its invocation.
		if (algorithms.length == 1) {
			if (algorithms[0] == null || algorithms[0] instanceof OcelotAlgorithm) {
				//input array ok: call algorithmSettingsOcelot
				try {
					algorithmSettingsOcelot(algorithms);
				} catch (JMException e) {
					//something failed
					RuntimeException ex = new RuntimeException("OcelotExperiment error: the algorithmSettingsOcelot method raised a JMException.");
					ex.addSuppressed(e);
					throw ex;
				}
				
				//checks the output
				this.algorithm = null;
				this.serendipitousAlgorithm = null;
				if (algorithms[0] == null) {
					throw new RuntimeException("OcelotExperiment error: failure while trying to build a suitable Algorithm.");
				} else {
					//the next cast will not fail
					this.algorithm = (OcelotAlgorithm) algorithms[0];
					
					//more settings
					if (this.algorithm.isSeedable(getTargetClass())) {
						((SeedableAlgorithm<T>) this.algorithm).seedStartingPopulation(this.seed, this.keepOfSeed);
					}
					
					if (this.configManager.isSerendipitous() && this.algorithm.isSerendipitous(getTargetClass())) {
						this.serendipitousAlgorithm = (SerendipitousAlgorithm<T>) this.algorithm;
						this.serendipitousAlgorithm.setSerendipitousPotentials(this.serendipitousPotentials);			
						StandardProblem standardProblem = this.algorithm.getStandardProblem();
						if (standardProblem.isSerendipitous(getTargetClass())) {
							SerendipitousProblem<T> serendipitousProblem = (SerendipitousProblem<T>) standardProblem;
							serendipitousProblem.setSerendipitousPotentials(this.serendipitousPotentials);
						} else {
							throw new RuntimeException("OcelotExperiment error: you provided a serendipitous algorithms[0] contains an Algorithm that is not an OcelotAlgorithm.");
						}
					}
				}
			} else {
				throw new RuntimeException("OcelotExperiment error: you broke an invariant on the algorithms array: algorithms[0] contains an Algorithm that is not an OcelotAlgorithm.");
			}
		} else {
			throw new RuntimeException("OcelotExperiment error: you broke an invariant on the algorithms array: algorithms has not length one.");
		}
	}
	
	/**
	 * Settings for the only algorithm (always at algorithms[0], if present)
	 * and the only problem this experiment is made of.
	 * 
	 * @param algorithms an array of {@link Algorithm}s.
	 *        It will have exactly length 1 and its only
	 *        member will be an {@link OcelotAlgorithm}. 
	 * @throws JMException 
	 */
	protected abstract void algorithmSettingsOcelot(Algorithm[] algorithms) throws JMException;

	public final Solution basicRun() throws ClassNotFoundException, JMException {
		// Creates the algorithm
		//TODO we pass an empty algorithms array: the responsibility of creating an instance of it is left to subclasses. This is not ok: use the same mechanism used by default by jmetal. 
		algorithmSettings(this.problemList_[0], 0, new Algorithm[1]);

		// Gets the budget
		int budget = this.budgetManager.getExperimentBudget(this);
		
		// Runs the algorithm (just once) with budget
		SolutionSet solutionSet = this.algorithm.executeBounded(budget);
				
		// Stores the serendipitous solutions, if they exist
		if (this.serendipitousAlgorithm != null) {
			this.serendipitousSolutions.addAll(this.serendipitousAlgorithm.getSerendipitousSolutions());
		}
		
		// Updates the budget manager, so it can decide whether
		// to grant extra budget or not
		this.budgetManager.reportConsumedBudget(this, this.algorithm.getStats().getEvaluations());
		
		// Gets the (only) solution
		//TODO what if solutionSet is bigger than one? Is it ok to pick just the first one?
		Solution solution = solutionSet.get(0);
		
		// If the solution is not optimal, tries to run again for a while
		if (solution.getObjective(0) != 0.0) {
			int extraBudget = this.budgetManager.askForMoreBudget(this, solution);
			if (extraBudget > 0) {
				//runs the algorithm (for the second time) with extra budget
				this.algorithm.executeBounded(extraBudget);
				
				// Stores the serendipitous solutions, if they exist
				if (this.serendipitousAlgorithm != null) {
					this.serendipitousSolutions.addAll(this.serendipitousAlgorithm.getSerendipitousSolutions());
				}
				
				// Updates the budget manager, so it is ready for the 
				// next experiment
				this.budgetManager.reportConsumedBudget(this, this.algorithm.getStats().getEvaluations());
			}
		}
		
		// End of experiment
		this.budgetManager.reportConclusion(this);
		
		return solution;
	}

	public final SolutionSet multiObjectiveRun() throws ClassNotFoundException, jmetal.util.JMException {
		// Creates the algorithm
		//TODO we pass an empty algorithms array: the responsibility of creating an instance of it is left to subclasses. This is not ok: use the same mechanism used by default by jmetal. 
		algorithmSettings(this.problemList_[0], 0, new Algorithm[1]);
		
		// Runs the algorithm (just once) without budget
		//TODO use budget manager? delete budget manager?
		SolutionSet solutions = this.algorithm.execute();

		// Stores the serendipitous solutions, if they exist
		if (this.serendipitousAlgorithm != null) {
			this.serendipitousSolutions.addAll(this.serendipitousAlgorithm.getSerendipitousSolutions());
		}
		
		// Updates the budget manager
		this.budgetManager.reportConsumedBudget(this, this.algorithm.getStats().getEvaluations());
		
		// End of experiment
		this.budgetManager.reportConclusion(this);
		
		// Returns the result
		return solutions;
	}

	public Set<Solution> getSerendipitousSolutions() {
		return new HashSet<>(this.serendipitousSolutions);
	}

	public AlgorithmStats getStats() {
		return this.algorithm.getStats();
	}
	
	@SuppressWarnings("unchecked")
	public SolutionSet getLastPopulation() {
		if (this.algorithm.isSeedable(getTargetClass())) {
			return ((SeedableAlgorithm<T>) this.algorithm).getLastPopulation();
		} else {
			return null;
		}
	}
}
