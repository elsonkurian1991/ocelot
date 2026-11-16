package jmetal.operators.localSearch;

import java.util.HashMap;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.util.JMException;
import jmetal.util.wrapper.XParamArray;

/**
 * Operator for the Alternating Variable Method local search heuristic
 * N.b: a memetic algorithm does not provide a restart to avoid local optima
 * 
 * @author giograno
 *
 */
public class AlternativeVariableMethodSearch extends Operator {
	private static final int backwardMove = -1;
	private static final int forwardMove = 1;
	private static final int localOptima = 0;

	// AVM parameters
	private static final double initialMove = 0.1;
	private static final double repeatBase = 2;
	private static final int precision = 1;

	private static final long serialVersionUID = 1L;

	private StandardProblem problem;
	private int objectiveId; // the ID for the object in the fitness vector
	private int maxEvaluations; 
	private int evaluations; // evaluations actually consumed
	private int moveNoX;  // the number of moves toward a direction

	private BestSolution bestSolution; // best solution by the algorithm

	public static class AVMBuilder {
		// Required parameters
		private StandardProblem problem;
		private int maxEvaluations;
		// Optional parameters
		private HashMap<String, Object> parameters = null;
		private int objectiveId = 0;
		private int currentEvaluations = 0;  // zero by default

		/**
		 * Constructor of AVM Builder
		 * 
		 * @param pProblem the {@link StandardProblem} to solve.
		 * @param pMaxEvaluations maximum evaluations.
		 */
		public AVMBuilder(StandardProblem pProblem, int pMaxEvaluations) { 
			this.problem = pProblem;
			this.maxEvaluations = pMaxEvaluations;	 
		}

		public AVMBuilder consumedEvaluations(int evaluations) 
		{ this.currentEvaluations = evaluations; return this; }

		public AVMBuilder parameters(HashMap<String, Object> parameters) 
		{ this.parameters = parameters; return this; }

		public AVMBuilder objectiveId(LabeledEdge target) 
		{ this.objectiveId = target.getObjectiveId(); return this; }

		public AlternativeVariableMethodSearch build() {
			if (this.problem == null)  {
				throw new IllegalStateException("You need a problem to solve");
			} 

			if (this.maxEvaluations == 0) {
				throw new IllegalStateException("You need a maximum iteration number");
			}

			return new AlternativeVariableMethodSearch(this);
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param builder an {@link AVMBuilder}
	 */
	private AlternativeVariableMethodSearch(AVMBuilder builder) {
		super(builder.parameters);
		this.problem = builder.problem;
		this.objectiveId = builder.objectiveId;
		this.maxEvaluations = builder.maxEvaluations;
		this.evaluations = builder.currentEvaluations;
		this.moveNoX = 0; 
	}

	@Override
	public Object execute(Object solution) throws JMException {
		Solution currentSolution = new Solution((Solution)solution);

		// Set initial solution as the currently best
		this.bestSolution = new BestSolution(currentSolution, currentSolution.getObjective(this.objectiveId));

		boolean isTargetCovered = false;

		while (this.evaluations < this.maxEvaluations && !isTargetCovered) {

			XParamArray bestGAModifier = new XParamArray(this.bestSolution.solution);

			// Store the last updated solution element
			int lastUpdatedArray = bestGAModifier.kinds() - 1;
			int lastUpdatedVariable = bestGAModifier.size(lastUpdatedArray) - 1;

			// Remember the last updated variable 
			while (lastUpdatedVariable == -1) {
				lastUpdatedArray--;
				lastUpdatedVariable = bestGAModifier.size(lastUpdatedArray) - 1;
			}

			boolean isLocalOptimum = false;
			int currentArray = 0;
			int currentVariable = -1;
			int direction = localOptima;

			while (!isLocalOptimum && this.evaluations < this.maxEvaluations && !isTargetCovered) {

				if (direction == localOptima) {

					if (currentVariable + 1 == bestGAModifier.size(currentArray)) {
						currentVariable = 0; // Restart from the first variable

						if (currentArray + 1 == bestGAModifier.kinds()) {
							currentArray = 0; // Restart from the first array
						} else { 
							currentArray++; // Look for the next array
						}

						// Go to the next valid array (at least one variable)
						while (bestGAModifier.size(currentArray) == 0) {
							currentArray++;
							if (currentArray == bestGAModifier.kinds()) {
								currentArray = 0;
							}
						}

					} else { // the array is not finished yet 
						currentVariable++; // becomes 0 at first iteration
					}

					// Gets the direction (and updates if a good direction is found)
					direction = this.getDirection(this.bestSolution, currentArray, currentVariable);

					if (direction == localOptima) {
						if (this.bestSolution.solution.getObjective(this.objectiveId) == 0.0) {
							isTargetCovered = true;
						}

						if (lastUpdatedArray == currentArray && lastUpdatedVariable == currentVariable) {
							isLocalOptimum = true;
						}

						continue;
					} // end looking for local optimum or for a solution
				}

				// Iterations toward a direction
				lastUpdatedArray = currentArray;
				lastUpdatedVariable = currentVariable;

				// Create a new solution 
				BestSolution improvedSolution = this.improveSolutionTowardADirection(bestSolution, 
						currentArray, currentVariable, direction);

				if (improvedSolution.branchDistance < this.bestSolution.branchDistance) {
					this.bestSolution.solution = improvedSolution.solution;
					this.bestSolution.branchDistance = improvedSolution.branchDistance;
					this.moveNoX++;
				} else {
					direction = localOptima;
					moveNoX = 0;
				}

				// Check for solution covered
				if (bestSolution.solution.getObjective(this.objectiveId) == 0.0) {
					isTargetCovered = true;
				}

			} // end improvement for a variable
		} // main while

		return this.bestSolution.solution;
	}

	private int getDirection(BestSolution bestSolution, int array, int element) 
			throws JMException {
		Solution solutionUp = new Solution(bestSolution.solution);
		Solution solutionDown = new Solution(bestSolution.solution);

		XParamArray modifierUp = new XParamArray(solutionUp);
		XParamArray modifierDown = new XParamArray(solutionDown);

		double currentValue = modifierDown.getValue(array, element);

		// Check for bound overflow
		if (currentValue + initialMove > modifierUp.getUpperBound(array, element)) {
			modifierUp.setValue(array, element, currentValue);
		} else {
			modifierUp.setValue(array, element, currentValue + initialMove);
		}

		// Check for bound underflow
		if (currentValue - initialMove < modifierDown.getUpperBound(array, element)) {
			modifierDown.setValue(array, element, currentValue);
		} else { 
			modifierDown.setValue(array, element, currentValue - initialMove);
		}

		this.problem.evaluate(solutionUp);
		double branchDistanceUp = this.problem.getLastBranchDistance();
		this.problem.evaluate(solutionDown);
		double branchDistanceDown = this.problem.getLastBranchDistance();
		this.evaluations += 2;

		if (branchDistanceUp < branchDistanceDown) {
			// Compare with the current best solution
			if (branchDistanceUp < this.bestSolution.branchDistance) {
				// The solution up improves the best solution

				this.bestSolution.solution = solutionUp;
				this.bestSolution.branchDistance = branchDistanceUp;

				if (this.bestSolution.solution.getObjective(this.objectiveId) == 0.0) {
					return localOptima; // Objective covered
				}

				return backwardMove;
			} else {
				return localOptima;
			}
		} else if (branchDistanceDown < branchDistanceUp) {
			if (branchDistanceDown < this.bestSolution.branchDistance) {

				this.bestSolution.solution = solutionDown;
				this.bestSolution.branchDistance = branchDistanceDown;

				if (this.bestSolution.solution.getObjective(this.objectiveId) == 0.0) {
					return localOptima; // Objective covered
				}

				return forwardMove;
			} else {
				return localOptima;
			}
		} else {
			return localOptima;
		}
	}

	private BestSolution improveSolutionTowardADirection (BestSolution bestSolution, 
			int array, int variable, int direction) throws JMException {

		Solution solution = new Solution(bestSolution.solution);
		XParamArray modifier = new XParamArray(solution);

		double currentValue = modifier.getValue(array, variable);
		double move = Math.pow(repeatBase, moveNoX) * Math.pow(10, precision * -1) * direction;

		currentValue += move;

		// Clamps currentValue
		if (currentValue > modifier.getUpperBound(array, variable)) {
			currentValue = modifier.getUpperBound(array, variable);
		} else if (currentValue < modifier.getLowerBound(array, variable)) {
			currentValue = modifier.getLowerBound(array, variable);
		}

		modifier.setValue(array, variable, currentValue);

		this.problem.evaluate(solution);
		double branchDistance = this.problem.getLastBranchDistance();
		this.evaluations++;

		return new BestSolution(solution, branchDistance);
	}

	public int getEvaluations() {
		return this.evaluations;
	}

	/**
	 * Auxiliary class which store the best solution reached by the AVM
	 * algorithm
	 */
	class BestSolution {
		public Solution solution;
		public double branchDistance;

		public BestSolution(Solution solution, double branchDistance) {
			this.solution = solution;
			this.branchDistance = branchDistance;
		}
	}
}