package it.unisa.ocelot.genetic.algorithms;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.BranchObjective;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PCPairObjective;
import it.unisa.ocelot.util.Fronts;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.MOSARanking_Generic;
import jmetal.util.comparators.CrowdingComparator;
import jmetal.util.comparators.ObjectiveComparator;

/**
 * Implementation of MOSA (Many-Objective Sorting Algorithm) This algorithm is
 * proposed in the paper:
 * 
 * A. Panichella, F.M. Kifetew, P. Tonella
 * "Reformulating Branch Coverage as a Many-Objective Optimization Problem" ICST
 * 2015
 *
 * This algorithm only solves {@link MOSAGenericCoverageProblem}s. 
 *
 * @author giograno
 */
public final class MOSA_Generic extends OcelotAlgorithm {
	private static final long serialVersionUID = 3571512689366142192L;

	// we store here the complete set of target for given problem
	private List<? extends GenericObjective> objectives;

	private boolean firstRun = true;

	/**
	 * Maps each generation number to the total number 
	 * of evaluations that have been performed until
	 * that point.
	 */
	private final ArrayList<Integer> evaluations;

	// Used during algorithm execution

	/**
	 * The archive, i.e., the SolutionSet containing
	 * the Solutions that at each iteration cover
	 * more targets. This will be returned as the
	 * final output of the algorithm.
	 */
	private SolutionSet archive;

	/**
	 * Constructor.
	 * 
	 * @param pProblem a {@link MOSAGenericCoverageProblem} to solve.
	 */
	public MOSA_Generic(MOSAGenericCoverageProblem pProblem) {
		super(pProblem);
		//TODO are these targets? Are these objectives? Are we confusing the two? What's their general relation?
		this.objectives = new ArrayList<>(pProblem.getTargets());		
		this.evaluations = new ArrayList<>();
	}

	/**
	 * Returns the archive. The archive contains,
	 * at the end of the execution of this algorithm, 
	 * the generated test suite.
	 * 
	 * @return a SolutionSet
	 */
	public SolutionSet getArchive() {
		return this.archive;
	}

	/**
	 * Runs the MOSA algorithm.
	 * 
	 * @return a {@link SolutionSet} that is a set of non dominated
	 *         solutions as a result of the algorithm execution.
	 */
	//TODO ensure that the algorithm works even if some member in this.objectives is *already covered*!
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// Read the operators
		Operator selectionOperator = this.operators_.get("selection");
		Operator crossoverOperator = this.operators_.get("crossover");
		Operator mutationOperator = this.operators_.get("mutation");

		// Read the other parameters
		final int populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		final int maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		final double maxCoverage = ((Double) getInputParameter("maxCoverage")).doubleValue();
		final int maxGenerations = ((Integer) getInputParameter("maxGenerations")).intValue();
		final int maxTime = ((Integer) getInputParameter("maxTime")).intValue();
		final boolean isRandomRun = ((Boolean) getInputParameter("isRandomRun")).booleanValue();
		final double addRandom = ((Double) getInputParameter("addRandom")).doubleValue();
		final boolean isDynamicObjectives = ((Boolean) getInputParameter("isDynamicObjectives")).booleanValue();
		final String optimizeFor = (String) getInputParameter("optimizeFor");

		// Start counting the number of evaluations
		int evaluations = 0;

		// Define the SolutionSet for the population
		// that will be calculated at each generation
		SolutionSet population = new SolutionSet(populationSize);

		// Puts randomly generated solutions in the
		// (initial) population
		for (int i = 0; i < populationSize; i++) {
			Solution solution = new Solution(this.problem_);
			this.problem_.evaluate(solution);
			evaluations++;
			population.add(solution);
		}

		// Deactivate all the objectives
		// if in "dynamic objectives" mode.
		//TODO why???
		if (this.firstRun && isDynamicObjectives) {
			for (GenericObjective objective : this.objectives) {
				objective.setInactive();
			}
			this.firstRun = false;
		}

		// Creates and initializes the archive
		this.archive = new SolutionSet(this.objectives.size());
		updateArchive(population);

		// Start counting the number of generations
		int generations = 0; 

		// Starts timing to determine if we go beyond
		// the allotted time
		long startTime = System.nanoTime();

		// Main loop (each iteration of this loop calculates a new generation)
		while (keepRunning(evaluations, maxEvaluations, generations, maxGenerations, maxCoverage, startTime, maxTime)) {
			generations++;

			// Makes the offspring population
			SolutionSet offspringPopulation = new SolutionSet(populationSize + (int)(populationSize * addRandom));
			Solution[] parents = new Solution[2];

			// Adds some random solutions to the offspring population
			for (int i = 0; i < populationSize * 0.1; i++) {
				Solution solution = new Solution(this.problem_);
				this.problem_.evaluate(solution);
				evaluations++;
				offspringPopulation.add(solution);
			}

			if (isRandomRun) {
				//TODO what is offspringPopulation in this case?
			} else {
				for (int i = 0; i < (populationSize / 2); i++) {
					// Select a pair of parents
					parents[0] = (Solution) selectionOperator.execute(population);
					parents[1] = (Solution) selectionOperator.execute(population);

					// Make offsprings by crossover of parents
					Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);

					// Mutate offsprings
					mutationOperator.execute(offSpring[0]);
					mutationOperator.execute(offSpring[1]);

					// Evaluate the offsprings and
					// update the total number of evaluations
					this.problem_.evaluate(offSpring[0]);
					evaluations++;
					this.problem_.evaluate(offSpring[1]);
					evaluations++;

					// Add the offspring to the offspring population
					offspringPopulation.add(offSpring[0]);
					offspringPopulation.add(offSpring[1]);
				}
			}

			// Merges the population with its offsprings,
			// puts the result in union
			SolutionSet union = population.union(offspringPopulation);

			// Updates the objectives
			//TODO ??? No it doesn't, apparently it does nothing!!! Perhaps delete???
			/*
			for (GenericObjective objective : this.objectives) {
				double objectiveValueBest = Double.MAX_VALUE;
				if (!objective.isActive()) { //inactive or covered
					continue;
				}
				for (int i = 0; i < union.size(); i++) {
					if (union.get(i).getObjective(objective.getId()) <  objectiveValueBest) {
						objectiveValueBest = union.get(i).getObjective(objective.getId());
					}
				}

				//TODO apparently now we have determined the best value for objective's value. What should we do with it???
			}*/

			if (isRandomRun) {
				//TODO how do we update the population by means of union???
			} else {
				// Fill population with the best
				// populationSize solutions in union,
				// according to the Pareto fronts 
				// and the crowding distance ranking:

				// 1- Delete the population that was calculated
				// at the previous iteration
				population.clear();

				// 2- Calculate the Pareto subfronts of union
				Fronts fronts = preferenceSorting(union);

				// 3- Gets the first subfront
				SolutionSet subfront = fronts.getFront(0);

				// 4- Scans the subfronts in their ranking
				// order and adds all the solutions in
				// them to the population, until the considered 
				// subfront is too big to be completely added 
				// to the population 
				int frontIndex = 0;
				int remain = populationSize;
				while (remain > 0 && remain >= subfront.size()) {
					// Set the crowding distance
					// to the solutions in the front
					//TODO since we add the whole front, is this calculation really necessary? I don't think so...
					//assignCrowdingDistance(subfront, this.problem_.getNumberOfObjectives());

					// Add the individuals of this front to the population
					for (int i = 0; i < subfront.size(); i++) {
						population.add(subfront.get(i));
					}
					//TODO can't we replace the above loop with population = population.union(front)?

					// Decrement remain
					remain -= subfront.size();

					// Obtain the next subfront (if there is still
					// some remaining individuals to put into the
					// population)
					frontIndex++;
					if (remain > 0) { //TODO is this guard to the next statement useless? Maybe it is just not to overflow frontIndex. Check again.
						subfront = fronts.getFront(frontIndex);
					}
				}

				// 5- If remain is less than current front size, 
				// insert in it only the best remain solutions 
				// of the current front, based on the ranking 
				// given by crowding distance
				if (remain > 0) {
					// current front contains the individuals to insert
					assignCrowdingDistance(subfront, this.problem_.getNumberOfObjectives());
					subfront.sort(new CrowdingComparator());

					// Put in the population the best 
					// remain solutions in the current front
					for (int i = 0; i < remain; i++) {
						population.add(subfront.get(i));
					}
				}
			}

			// Update the archive with the solutions
			// in the new population that cover some
			// objective
			updateArchive(population);

			// Store the number of evaluations 
			// done until the end of this 
			// generation
			this.evaluations.add(evaluations);
		}

		// Set the stats with this.evaluations
		this.algorithmStats.addEvaluations(evaluations);


		// Calculate the list of covered and uncovered branches 
		HashSet<String> coveredBranches = new HashSet<String>();
		HashSet<String> maybeUncoveredBranches = new HashSet<String>();
		if ("Pairs".equals(optimizeFor)) {
			// Objectives are branch chain pairs
			for (GenericObjective objective : this.objectives) {
				if (objective.isCovered()) {
					coveredBranches.add(((PCPairObjective) objective).sm.getTestObjOne());
					coveredBranches.add(((PCPairObjective) objective).sm.getTestObjTwo());
				} else if (objective.isActive()){
					if (((PCPairObjective) objective).sm.getBdOne() == 0.0) {
						coveredBranches.add(((PCPairObjective) objective).sm.getTestObjOne());
					}
					maybeUncoveredBranches.add(((PCPairObjective) objective).sm.getTestObjOne());
					maybeUncoveredBranches.add(((PCPairObjective) objective).sm.getTestObjTwo());
				}
			}
		} else if ("Branches".equals(optimizeFor)) {
			// Objectives are branches
			for (GenericObjective objective : this.objectives) {
				if (objective.isCovered()) {
					coveredBranches.add(((BranchObjective) objective).getBranchId());
				} else if (objective.isActive()) {
					maybeUncoveredBranches.add(((BranchObjective) objective).getBranchId());
				} else {
					System.err.println("Error: unclear state of an objective: id=" + objective.getId());
				}
			}
		}
		maybeUncoveredBranches.removeAll(coveredBranches);

		// Print to file the lists of covered and uncovered branches 
		try (FileWriter uncoveredWriter = new FileWriter("uncoveredBranches.txt")) {
			for (String branch : maybeUncoveredBranches) {
				uncoveredWriter.write(branch);
				uncoveredWriter.write('\n');
			}
		} catch (IOException e) {
			System.out.println("Unable to generate file uncoveredBranches.txt");
			e.printStackTrace();
		}

		try (FileWriter coveredWriter = new FileWriter("coveredBranches.txt")) {
			for (String branch : coveredBranches) {
				coveredWriter.write(branch);
				coveredWriter.write('\n');
			}
		} catch (IOException e) {
			System.out.println("Unable to generate file coveredBranches.txt");
			e.printStackTrace();
		}

		// Returns the archive
		return this.archive;
	}

	private boolean keepRunning(int evaluations, int maxEvaluations, int generations, int maxGenerations, double  maxCoverage, long startTime, long maxTime) {
		if (maxTime != 0) {
			long endTime = System.nanoTime();
			if ( (endTime - startTime) / 1000000000 >= maxTime) {
				return false;
			}
		}
		if (maxEvaluations != 0) {
			if (evaluations >= maxEvaluations) { 
				return false;
			}
		} 
		if (maxGenerations != 0) {
			if (generations >= maxGenerations) { 
				return false;
			}
		}
		//TODO if (maxCoverage != 0)???
		if (calculateCoverage() >= maxCoverage) {
			return false;
		}
		return true;
	}

	/**
	 * Adds to the archive a number of {@link Solution}s 
	 * that cover some previously uncovered target. Also, 
	 * marks all the covered targets.
	 * 
	 * @param candidates a {@link SolutionSet}. This method will add
	 *        to the archive all the {@link Solution}s in it that
	 *        cover some previously uncovered target.
	 */
	private void updateArchive(SolutionSet candidates) {		
		for (GenericObjective objective : this.objectives) {
			if (objective.isActive()) {
				// Scans candidates in search for a
				// candidate Solution that covers target
				Iterator<Solution> iteratorCandidates = candidates.iterator();
				while (iteratorCandidates.hasNext()) {
					Solution candidate = iteratorCandidates.next();

					// Determines whether candidate 
					// covers the objective 
					objective.setCovered(candidate);
					if (objective.isCovered()) {
						// Logs
						this.algorithmStats.log("Covered objective " + objective.toString());

						// Adds the candidate to the archive
						this.archive.add(candidate);

						// If the objective has an uncovered companion,
						// it's the right time to ensure it is active
						//TODO is it right???
						if (objective.hasCompanion()) {
							GenericObjective companion = objective.getCompanion();
							if (!companion.isCovered()) {
								companion.setActive();
							}
						}

						// Looks for all objectives (even if inactive) 
						// collaterally covered by candidate
						// and marks them as covered
						for (GenericObjective collateralObjective : this.objectives) {
							if (collateralObjective.isActive()) {
								// Determines whether candidate 
								// covers the collateral objective 
								collateralObjective.setCovered(candidate);
								if (collateralObjective.isCovered()) {
									// Logs
									this.algorithmStats.log("Collaterally covered objective " + collateralObjective.toString());
								}
							}
						}

						// Stops scanning candidates and 
						// moves to the next objective
						break;
					}
				}
			}
		}		
	}

	private Fronts preferenceSorting(SolutionSet candidates) {
		Fronts front = new Fronts();
		SolutionSet population = candidates;
		SolutionSet front_0 = new SolutionSet(objectives.size());

		/*** preference criterion ***/

		Set<Solution> solutionsToDelete = new HashSet<>();

		for (GenericObjective target : objectives) {
			if (target.isCovered() || !target.isActive()) {
				continue;
			}
			Iterator<Solution> populationIterator = population.iterator();
			double minimum_fitness = Double.MAX_VALUE;
			// best test case
			Solution t_best = null;
			while (populationIterator.hasNext()) {
				int idObjective = target.getId();
				Solution currentSolution = populationIterator.next();
				double currentObjective = currentSolution.getObjective(idObjective);

				if (currentObjective < minimum_fitness) {
					minimum_fitness = currentObjective;
					t_best = currentSolution;
				}
			}
			//System.out.println(target.toString() + minimum_fitness);
			t_best.setRank(0); // set rank 0 for preference criterion
			front_0.add(t_best); // adding to front 0
			solutionsToDelete.add(t_best);

		} // end for

		front.addFront(front_0);

		// Remotion of solution in first front from overall population
		Iterator<Solution> populationIterator = population.iterator();
		while (populationIterator.hasNext()) {
			Solution currentSolution = populationIterator.next();
			if (solutionsToDelete.contains(currentSolution))
				populationIterator.remove();
		}

		/*** fast-non dominated-sort ***/

		MOSARanking_Generic ranking = new MOSARanking_Generic(population, this.objectives);

		int remain = population.size();
		int front_number = 0;
		SolutionSet currentFront = new SolutionSet(candidates.size());

		while (remain > 0) {
			currentFront = ranking.getSubfront(front_number);
			remain -= currentFront.size();
			front.addFront(currentFront);
			front_number++;
		}

		return front;
	}

	//public List<SolutionSet> rankDominatedAndNonDominated(population) {}

	/* This is not used.
	private void crowdingDistanceAssignment(SolutionSet solutionSet, int numberOfObjects) {
		int size = solutionSet.size();

		if (size == 0) {
			return;
		}

		if (size == 1) {
			solutionSet.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			return;
		}

		// initialize to 0.0 all crowding distances
		for (int i = 0; i < size; i++)
			solutionSet.get(i).setCrowdingDistance(0.0);

		double minObjective = 0.0;
		double maxObjective = 0.0;
		double distance = 0.0;

		for (int i = 0; i < numberOfObjects; i++) {
			// sort the population by current object
			//try {
			solutionSet.sort(new ObjectiveComparator(i));
			//} catch (IllegalArgumentException e) {
			//	System.out.println("ERROR");
			//}
			minObjective = solutionSet.get(0).getObjective(i);
			maxObjective = solutionSet.get(size - 1).getObjective(i);

			// set infinity crowding distance for first and last element
			Solution current0 = new Solution(solutionSet.get(0));
			current0.setCrowdingDistance(Double.POSITIVE_INFINITY);
			solutionSet.replace(0, current0); // replace needed to avoid
												// override
			Solution currentLast = new Solution(solutionSet.get(size - 1));
			currentLast.setCrowdingDistance(Double.POSITIVE_INFINITY);
			solutionSet.replace(size - 1, currentLast);

			for (int j = 1; j < size - 1; j++) {
				distance = solutionSet.get(j + 1).getObjective(i)
						- solutionSet.get(j - 1).getObjective(i);

				// avoid division by 0 that leads to NaN values
				if (maxObjective - minObjective == 0)
					distance = 0.0;
				else
					distance = distance / (maxObjective - minObjective);

				distance += solutionSet.get(j).getCrowdingDistance();
				Solution current = new Solution(solutionSet.get(j));
				current.setCrowdingDistance(distance);
				solutionSet.replace(j, current);
			}
		}
	}*/

	/**
	 * Crowding distance calculation.
	 * 
	 * @param solutionSet the {@link SolutionSet} containing the {@link Solution}s
	 *        for which we want to calculate the crowding distance. Typically, 
	 *        a Pareto front.
	 * @param numberOfObjectives how many objective functions to be optimized?
	 */
	public void assignCrowdingDistance(SolutionSet solutionSet, int numberOfObjectives) {
		int populationSize = solutionSet.size();

		// Handles the trivial situations
		if (populationSize == 0) {
			return;
		} else if (populationSize == 1) {
			solutionSet.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			return;
		}

		// Makes a deep copy of the input solution set.
		// All the calculation will be made on this copy,
		// rather than on the input solutionSet.
		SolutionSet solutionSetTmp = new SolutionSet(populationSize);
		for (int i = 0; i < populationSize; i++) {
			Solution s = new Solution(solutionSet.get(i)); //clones
			solutionSetTmp.add(s);
		}

		// Initialize all the crowding distances
		// to 0.0
		for (int i = 0; i < populationSize; i++) {
			solutionSetTmp.get(i).setCrowdingDistance(0.0);
		}

		// i scans all the objective functions
		for (int i = 0; i < numberOfObjectives; i++) {
			// Skips inert objectives
			if ((this.objectives.get(i).isCovered()) || !(this.objectives.get(i).isActive())) {
				continue;
			}

			// Sorts the population based on  
			// the value of i-th objective 
			solutionSetTmp.sort(new ObjectiveComparator(i));

			// Sets infinity crowding distance for the
			// two individuals having minimum and maximum
			// value of the i-th objective  
			Solution minSolution = solutionSetTmp.get(0);
			Solution maxSolution = solutionSetTmp.get(populationSize - 1);
			minSolution.setCrowdingDistance(Double.POSITIVE_INFINITY);
			maxSolution.setCrowdingDistance(Double.POSITIVE_INFINITY);

			// Determines the range of variation for the i-th
			// objective across the whole population
			double minObjective = minSolution.getObjective(i);
			double maxObjective = maxSolution.getObjective(i);
			double range = maxObjective - minObjective;

			if (range == 0) {
				// Nothing to do: Zero range means all the
				// individuals have same i-th objective.
				// Crowding distance does not change.
				continue; //not really necessary
			} else {
				// Scans the population (minSolution and maxSolution
				// excluded) and increments the crowding distance
				// of all the individuals
				for (int k = 1; k < populationSize - 1; k++) {
					// Calculates the increment to the crowding
					// distance for the k-th individual. This 
					// depends on the i-th objective values of
					// its immediate neighbours.
					Solution prevNeighbor = solutionSetTmp.get(k - 1);
					Solution nextNeighbor = solutionSetTmp.get(k + 1);
					double prevObjective = prevNeighbor.getObjective(i);
					double nextObjective = nextNeighbor.getObjective(i);
					double rangeNeighbors = nextObjective - prevObjective;
					double distanceIncrement = rangeNeighbors / range;

					// Increments the crowding distance of the k-th
					// individual
					Solution kth = solutionSetTmp.get(k);
					double distanceNew = kth.getCrowdingDistance() + distanceIncrement;
					kth.setCrowdingDistance(distanceNew);
				}
			}
		}

		// Finally, writes the result from the temporary 
		// back in the input solution set (deep copy again)
		for (int i = 0; i < populationSize; i++) {
			Solution s = new Solution(solutionSetTmp.get(i));
			solutionSet.replace(i, s);
		}
	}

	public void fastEpsilonDominanceAssignment(SolutionSet solutionSet, int numberOfObjects) {
		double value;
		int populationSize = solutionSet.size();

		// Resets the population's crowding distance
		for (int i = 0; i < populationSize; i++) {
			solutionSet.get(i).setCrowdingDistance(0.0);
		}

		for (int i = 0; i < numberOfObjects; i++) {
			double min = Double.POSITIVE_INFINITY;
			List<Solution> minSet = new ArrayList<>(solutionSet.size());
			double max = 0;
			for (int j = 1; j < populationSize - 1; j++) {
				Solution test = solutionSet.get(j);
				value = test.getObjective(i);
				if (value < min) {
					min = value;
					minSet.clear();
					minSet.add(test);
				} else if (value == min)
					minSet.add(test);

				if (value > max) {
					max = value;
				}
			}

			if (max == min)
				continue;

			for (Solution test : minSet) {
				double numer = (solutionSet.size() - minSet.size());
				double demon = solutionSet.size();
				test.setCrowdingDistance(Math.max(test.getCrowdingDistance(), numer / demon));
			}
		}
	}

	@SuppressWarnings("unused")
	private boolean allTargetsCovered() {
		for (GenericObjective objective : this.objectives) {
			if (objective.isCovered()) {
				continue;
			} else {
				return false;
			}
		}
		return true;
	}

	//TODO delete and use a CoverageCalculator
	private double calculateCoverage() {
		double covered = 0;
		for (GenericObjective objective : this.objectives) {
			if (objective.isCovered()) {
				covered++;
			}
		}

		double coverage = covered / this.objectives.size();
		return coverage;
	}

	public List<Integer> getEvaluations() {
		return this.evaluations;
	}
}
