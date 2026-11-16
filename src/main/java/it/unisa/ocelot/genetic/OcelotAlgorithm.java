package it.unisa.ocelot.genetic;

import it.unisa.ocelot.genetic.algorithms.AlgorithmStats;
import it.unisa.ocelot.genetic.algorithms.SeedableAlgorithm;

import jmetal.core.Algorithm;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;

public abstract class OcelotAlgorithm extends Algorithm {
	private static final long serialVersionUID = -7835611261947665056L;
	
	protected final StandardProblem standardProblem;
	protected final AlgorithmStats algorithmStats;

	public OcelotAlgorithm(StandardProblem pStandardProblem) {
		super(pStandardProblem);
		this.standardProblem = pStandardProblem;
		this.algorithmStats = new AlgorithmStats();
	}
	
	/**
	 * Checks whether this algorithm is
	 * serendipitous.
	 * 
	 * @param targetClass the class for the targets
	 *        to cover.
	 * @return {@code true} iff this algorithm implements
	 *         {@link SerendipitousAlgorithm}{@code <targetClass>}.
	 */
	public final boolean isSerendipitous(Class<?> targetClass) {
		return (this instanceof SerendipitousAlgorithm<?>) &&
		((SerendipitousAlgorithm<?>) this).getTargetClass() == targetClass;	
	}
	
	/**
	 * Checks whether this algorithm is
	 * seedable.
	 * 
	 * @param targetClass the class for the targets
	 *        to cover.
	 * @return {@code true} iff this algorithm implements
	 *         {@link SeedableAlgorithm}{@code <targetClass>}.
	 */
	public final boolean isSeedable(Class<?> targetClass) {
		return (this instanceof SeedableAlgorithm<?>) &&
		((SeedableAlgorithm<?>) this).getTargetClass() == targetClass;	
	}
	
	/**
	 * Executes the algorithm with a given budget.
	 * 
	 * @param pBudget an {@code int} >= 0, the budget.
	 *        Currently the budget is the value of 
	 *        the maxEvaluations input parameter of 
	 *        the algorithm.
	 * @return the final {@link SolutionSet}.
	 * @throws ClassNotFoundException
	 * @throws JMException
	 */
	//TODO change from max evaluations to max generations!
	public final SolutionSet executeBounded(int pBudget) throws ClassNotFoundException, JMException {
		setInputParameter("maxEvaluations", pBudget);
		return execute();
	}
	
	public final StandardProblem getStandardProblem() {
		return this.standardProblem;
	}
	
	public final AlgorithmStats getStats() {
		return this.algorithmStats;
	}
}
