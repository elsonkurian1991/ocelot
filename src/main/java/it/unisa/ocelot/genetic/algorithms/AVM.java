package it.unisa.ocelot.genetic.algorithms;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.wrapper.XParamArray;

/**
 * Alternating Variable Method.
 */
public final class AVM extends AbstractGeneticMemeticAlgorithm {
	private static final long serialVersionUID = 720067051970162535L;

	private static class SolutionBundle {
		public Solution solution;
		public double branchDistance;
		
		public SolutionBundle() {
			this(null, 0);
		}

		public SolutionBundle(Solution pSolution, double pBranchDistance) {
			this.solution = pSolution;
			this.branchDistance = pBranchDistance;
		}
	}

	private final Map<Integer, Map<Integer, Boolean>> discreteValues;
	private final Comparator<Solution> comparator;
	private final SolutionBundle solutionBundle;
	
	private double delta;
	private double epsilon;
	private double derivate;
	private Solution seededSolution;
	private int evaluations;
	
	/**
	 * Constructor.
	 * 
	 * @param pStandardProblem {@link StandardProblem} to solve.
	 * @throws JMException
	 */
	@SuppressWarnings("unchecked")
	public AVM(StandardProblem pStandardProblem) throws JMException {
		super(pStandardProblem);

		this.discreteValues = new HashMap<>();
		initializeDiscreteness(pStandardProblem.getParametersTypes());
		this.comparator = new ObjectiveComparator(0);
		this.solutionBundle = new SolutionBundle();
	}

	private void initializeDiscreteness(CType[] types) throws JMException {
		Solution test;
		try {
			test = new Solution(this.problem_);
		} catch (ClassNotFoundException e) {
			final JMException exc = new JMException("Unexpected ClassNotFoundException while creating random Solution: " + e.getMessage());
			exc.addSuppressed(e);
			throw exc;
		}
		XParamArray modifier = new XParamArray(test);

		int typeIndex = 0;
		for (int i = 0; i < modifier.kinds(); i++) {
			HashMap<Integer, Boolean> kindMap = new HashMap<>();

			for (int j = 0; j < modifier.size(i); j++) {
				if (i == modifier.kinds() - 1 && i > 0) {
					// Pointers are always discrete
					kindMap.put(j, true);
				} else {
					kindMap.put(j, types[typeIndex].isDiscrete());					
					if (i == 0) {
						typeIndex++;
					}
				}
			}

			if (i != 0) {
				typeIndex++;
			}

			this.discreteValues.put(i, kindMap);
		}
	}

	/**
	 * Runs the AVM algorithm.
	 * 
	 * @return a {@link SolutionSet} that is a set of non dominated
	 *         solutions as a result of the algorithm execution
	 * @throws JMException
	 * @throws ClassNotFoundException 
	 */
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		this.delta = ((Double) getInputParameter("delta")).doubleValue();
		this.epsilon = ((Double) getInputParameter("epsilon")).doubleValue();
		if (this.seededSolution == null && this.startingPopulation != null && this.startingPopulation.size() > 0) {
			this.seededSolution = this.startingPopulation.get(0);
		} else {
			this.seededSolution = null;
		}

		int strength = 0;

		if (this.seededSolution != null) {
			this.solutionBundle.solution = new Solution(this.seededSolution);
			prepareSerendipitous();
			this.standardProblem.evaluate(this.solutionBundle.solution);
			this.solutionBundle.branchDistance = this.standardProblem.getLastBranchDistance();
			checkSerendipitous(this.solutionBundle.solution);
			this.evaluations++;
		}

		boolean targetCovered = false;

		// Generations
		boolean dontRestart = (this.solutionBundle.solution != null);
		while (this.evaluations < maxEvaluations && !targetCovered) {
			//Creates a random solution
			if (!dontRestart) {
				this.solutionBundle.solution = new Solution(this.standardProblem);
				prepareSerendipitous();
				this.standardProblem.evaluate(this.solutionBundle.solution);
				this.solutionBundle.branchDistance = this.standardProblem.getLastBranchDistance();
				this.evaluations++;
				checkSerendipitous(this.solutionBundle.solution);
			}
			dontRestart = false;

			XParamArray solutionModifier = new XParamArray(this.solutionBundle.solution);

			int lastUpdatedKind = solutionModifier.kinds() - 1;
			int lastUpdatedVariable = solutionModifier.size(lastUpdatedKind) - 1;
			//In order to find local optima, it stores the last updated kind/variable
			while (lastUpdatedVariable == -1) {
				lastUpdatedKind--;
				lastUpdatedVariable = solutionModifier.size(lastUpdatedKind) - 1;
			}

			boolean localOptimum = false;
			int currentKind = 0;
			int currentVariable = -1; //It will become 0 in the first iteration
			double direction = 0;
			boolean firstSelectionChange = false;

			//TODO sure we want to put the this.evaluations < maxEvaluations check here???
			while (!localOptimum && this.evaluations < maxEvaluations && !targetCovered) {

				//If we are in a optimum local to the specific kind/variable...
				if (direction == 0) {
					strength = 0;

					//If update overflows the number of variables of this kind
					if (currentVariable + 1 == solutionModifier.size(currentKind)) {
						currentVariable = 0; //Reset variable

						//If update overflows the number of kinds
						if (currentKind + 1 == solutionModifier.kinds()) {
							currentKind = 0; //Reset kind
						} else { //If the update doesn't overflow the number of kinds
							currentKind++; //Normally increments the kind
						}

						//Searches for the kind with at least a variable
						while (solutionModifier.size(currentKind) == 0) {
							currentKind++;
							if (currentKind == solutionModifier.kinds()) {
								currentKind = 0;
							}
						}
					} else { //If the update doesn't overflow the number of variables
						currentVariable++; //Normally increments the variable
					}

					//Gets the direction of update (minus or plus). Besides, it updates the solution.
					direction = getDirection(this.solutionBundle, currentKind, currentVariable);

					if (direction == 0.0) {
						if (this.solutionBundle.solution.getObjective(0) == 0.0) {
							targetCovered = true;
						}

						if (lastUpdatedKind == currentKind && lastUpdatedVariable == currentVariable) {
							localOptimum = true;
						}

						continue;
					} else {
						firstSelectionChange = true;
					}
				}

				//Updates the last updated kind/variable to the current ones
				lastUpdatedKind = currentKind;
				lastUpdatedVariable = currentVariable;

				// Create a new solution, updated towards the specified direction.
				SolutionBundle newSolutionBundle = newSolutionBundle(
						this.solutionBundle, 
						currentKind, currentVariable, 
						direction, strength);

				int comparison = this.comparator.compare(newSolutionBundle.solution, this.solutionBundle.solution);
				if (comparison == -1 || (comparison == 0 && newSolutionBundle.branchDistance < this.solutionBundle.branchDistance)) {
					this.solutionBundle.solution = newSolutionBundle.solution;
					this.solutionBundle.branchDistance = newSolutionBundle.branchDistance;
					strength++;
					firstSelectionChange = false;
				} else if (firstSelectionChange) {
					if (strength >= 0) {
						strength -= 1;
					} else {
						strength *= 2;
					}
				} else {
					direction = 0;
				}

				if (this.solutionBundle.solution.getObjective(0) == 0.0) {
					targetCovered = true;
				}
			}
		}

		// Return a population with the best individual
		SolutionSet resultPopulation = new SolutionSet(1);
		resultPopulation.add(this.solutionBundle.solution);
		this.lastPopulation = new SolutionSet(1);
		this.lastPopulation.add(this.solutionBundle.solution);

		// Set stats
		this.algorithmStats.addEvaluations(this.evaluations);
		
		return resultPopulation;
	}


	private SolutionBundle newSolutionBundle(SolutionBundle pSolutionBundle, int pCurrentKind, int pCurrentVariable, double pDirection, int pStrength)
	throws JMException {
		Solution solution = new Solution(pSolutionBundle.solution);
		XParamArray solutionModifier = new XParamArray(solution);

		// Starts calculating updateValue
		double updateValue = solutionModifier.getValue(pCurrentKind, pCurrentVariable);
		double difference = this.derivate * this.delta * pDirection * Math.pow(1.3, pStrength);
		if (Math.abs(difference) < this.epsilon) {
			difference = this.epsilon * pDirection;
		}
		updateValue = updateValue + difference;

		// Clamps updateValue to the allowed range
		if (updateValue > solutionModifier.getUpperBound(pCurrentKind, pCurrentVariable)) {
			updateValue = solutionModifier.getUpperBound(pCurrentKind, pCurrentVariable);
		} else if (updateValue < solutionModifier.getLowerBound(pCurrentKind, pCurrentVariable)) {
			updateValue = solutionModifier.getLowerBound(pCurrentKind, pCurrentVariable);
		}

		// Rounds updateValue to the epsilonth decimal
		double roundEpsilon = Math.pow(10, Math.floor(Math.log10(this.epsilon)));
		updateValue = Math.floor(updateValue / roundEpsilon) * roundEpsilon; 

		solutionModifier.setValue(pCurrentKind, pCurrentVariable, updateValue);

		prepareSerendipitous();
		this.standardProblem.evaluate(solution);
		double branchDistance = this.standardProblem.getLastBranchDistance();
		this.evaluations++;
		checkSerendipitous(solution);

		this.derivate = 
				derivate(solution.getObjective(0), branchDistance, 
						pSolutionBundle.solution.getObjective(0), 
						pSolutionBundle.branchDistance, 
						Math.abs(difference));

		return new SolutionBundle(solution, branchDistance);
	}

	private double getDirection(SolutionBundle pSolutionBundle, int pCurrentKind, int pCurrentVariable) throws JMException {
		double epsilon;
		if (isDiscrete(pCurrentKind, pCurrentVariable)) {
			epsilon = 1;
		} else {
			epsilon = this.epsilon;
		}

		Solution solutionPlus = new Solution(pSolutionBundle.solution);
		Solution solutionMinus = new Solution(pSolutionBundle.solution);
		XParamArray modifierPlus = new XParamArray(solutionPlus);
		XParamArray modifierMinus = new XParamArray(solutionMinus);

		double currentValue = modifierPlus.getValue(pCurrentKind, pCurrentVariable);
		if (currentValue + epsilon > modifierPlus.getUpperBound(pCurrentKind, pCurrentVariable)) {
			modifierPlus.setValue(pCurrentKind, pCurrentVariable, currentValue);
		} else {
			modifierPlus.setValue(pCurrentKind, pCurrentVariable, currentValue + epsilon);
		}
		if (currentValue - epsilon < modifierMinus.getLowerBound(pCurrentKind, pCurrentVariable)) {
			modifierMinus.setValue(pCurrentKind, pCurrentVariable, currentValue);
		} else {
			modifierMinus.setValue(pCurrentKind, pCurrentVariable, currentValue - epsilon);
		}

		prepareSerendipitous();
		this.standardProblem.evaluate(solutionPlus);
		double branchDistancePlus = this.standardProblem.getLastBranchDistance();
		this.evaluations++;
		checkSerendipitous(solutionPlus);

		prepareSerendipitous();
		this.standardProblem.evaluate(solutionMinus);
		double branchDistanceMinus = this.standardProblem.getLastBranchDistance();
		this.evaluations++;
		checkSerendipitous(solutionMinus);

		int comparation = this.comparator.compare(solutionPlus, solutionMinus);

		if (comparation < 0 || (comparation == 0 && branchDistancePlus < branchDistanceMinus)) { //if solPlus < solMinus
			int comparation2 = this.comparator.compare(solutionPlus, pSolutionBundle.solution);
			if (comparation2 < 0 || (comparation2 == 0 && branchDistancePlus < pSolutionBundle.branchDistance)) { //if solPlus < solution
				this.derivate = derivate(
						solutionPlus.getObjective(0), branchDistancePlus, 
						pSolutionBundle.solution.getObjective(0), pSolutionBundle.branchDistance,
						epsilon);

				this.solutionBundle.solution = solutionPlus;
				this.solutionBundle.branchDistance = branchDistancePlus;

				if (this.solutionBundle.solution.getObjective(0) == 0.0) {
					return 0.0;
				}

				return 1;
			} else {
				return 0;
			}
		} else if (comparation > 0 || (comparation == 0 && branchDistancePlus > branchDistanceMinus)) {
			int comparation2 = this.comparator.compare(solutionMinus, pSolutionBundle.solution);
			if (comparation2 < 0 || (comparation2 == 0 && branchDistanceMinus < pSolutionBundle.branchDistance)) {
				this.derivate = this.derivate(
						solutionMinus.getObjective(0), branchDistanceMinus,
						pSolutionBundle.solution.getObjective(0), pSolutionBundle.branchDistance,
						epsilon);

				this.solutionBundle.solution = solutionMinus;
				this.solutionBundle.branchDistance = branchDistanceMinus;

				if (this.solutionBundle.solution.getObjective(0) == 0.0) {
					return 0.0;
				}

				return -1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}

	private boolean isDiscrete(int pKind, int pVariable) {
		return this.discreteValues.get(pKind).get(pVariable);
	}

	private double derivate(double a, double bdA, double b, double bdB, double dx) {
		double alA = Math.floor(a);
		double alB = Math.floor(b);

		if (alA == alB) {
			return Math.abs(bdA - bdB)/dx;
		} else {
			return (alA - alB)/dx;
		}
	}
}
