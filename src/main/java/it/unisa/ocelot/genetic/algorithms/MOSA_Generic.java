package it.unisa.ocelot.genetic.algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
import it.unisa.ocelot.util.Front;
import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.MOSARanking_Generic;
import jmetal.util.comparators.CrowdingComparator;
import jmetal.util.comparators.ObjectiveComparator;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Implementation of MOSA (Many-Objective Sorting Algorithm) This algorithm is
 * proposed in the paper:
 * 
 * A. Panichella, F.M. Kifetew, P. Tonella
 * "Reformulating Branch Coverage as a Many-Objective Optimization Problem" ICST
 * 2015
 *
 * @author giograno
 *
 */
public class MOSA_Generic extends OcelotAlgorithm {

	private static final long serialVersionUID = 6934416988941483481L;
	
	// the final Solution Set produced by the algorithm
	private SolutionSet archive;
	// we store here the complete set of target for given problem
	private List<GenericObjective> allTargets;
	@SuppressWarnings("unused")
	private Set<GenericObjective> coveredObjectives;

	private List<Integer> evaluations;

	// MOSA parameters
	/**
	 * The size of the genetic algorithm population
	 * at each iteration.
	 */
	private int populationSize;

	/**
	 * The maximum number of evaluations this algorithm (and 
	 * thus the whole experiment) is allowed to run.
	 */
	private int maxEvaluations;

	/**
	 * The maximum coverage (and thus the whole experiment) is 
	 * allowed to obtain.
	 */
	private double maxCoverage;

	// EvInT additional MOSA parameters
	
	/**
	 * Whether we want to do a random run. A random run replaces
	 * offspring creation by crossover/mutation with randomly
	 * generated "offsprings".
	 */
	private boolean isRandomRun;

	/**
	 * The total time this algorithm (and thus the whole
	 * experiment) is allowed to run, in seconds.
	 */
	private int experimentTime;
	
	//More EvInT calculated data (from parameters)
	/**
	 * Percentage of the population size that is dedicated
	 * to randomly created "offsprings" (they really aren't
	 * offsprings because they are not obtained by crossover
	 * and mutation). Currently set to 10% (set to 100% for
	 * a random run).
	 */
	private double addRandom;

	//EvInT fields
	
	/**
	 * Optional seed population provided by PopulationStore from the previous
	 * run of the MOSA algorithm. If non-null, the first seedPopulation.size() 
	 * solutions of the initial population are taken from here instead of being 
	 * generated randomly. This preserves search progress across iterations.
	 */
	private SolutionSet seedPopulation;
	
	/**
	 * Current working population. Declared as a field (not local variable in
	 * execute()) so getFinalPopulation() can return it after execute() finishes.
	 */
	private SolutionSet population;
	
	/**
	 * Counts the number of completed generations inside the MOSA while loop.
	 * Incremented once per generation (after each offspring creation + archive
	 * update cycle). Used as the x-axis unit in fitness snapshots so the plot
	 * shows convergence per generation rather than per evaluation.
	 */
	private int generationCounter;

	/**
	 * Constructor
	 * 
	 * @param problem Problem to solve
	 * @throws IOException 
	 */
	public MOSA_Generic(MOSAGenericCoverageProblem problem) {
		super(problem);
		//Sanity check: Check that objective IDs match their indices in the problem's target
		List<GenericObjective> targets = problem.getTargetObjectives();
		for (int i = 0; i < targets.size(); i++) {
			if (targets.get(i).getObjectiveID() != i) {
				throw new IllegalStateException("Objective ID/index mismatch at " + i);
			}
		}
		
		archive = null;
		allTargets = new ArrayList<>(targets);
		coveredObjectives = new HashSet<>();
		evaluations = new ArrayList<>();
	}
	
	/**
	 * Sets the seed population to use when initialising the next MOSA run.
	 * Called by GenAndWrite before generateTestSuite() is invoked.
	 * If not called (or called with null), MOSA initialises fully randomly
	 * as before — existing behaviour is preserved.
	 *
	 * @param seedPopulation solutions from the previous iteration's final
	 *                       population, provided by PopulationStore
	 */
	public void setSeedPopulation(SolutionSet seedPopulation) {
		this.seedPopulation = seedPopulation;
	}
	/**
	 * Returns the final population after execute() completes.
	 * Called by GenAndWrite to pass to PopulationStore for the next iteration.
	 *
	 * @return the population SolutionSet at the end of the MOSA run
	 */
	public SolutionSet getFinalPopulation() {
		return population;
	}
	/**
	 * Returns the overall test suite
	 * 
	 * @return a SolutionSet
	 */
	public SolutionSet getArchive() {
		return archive;
	}

	/**
	 * Runs the MOSA algorithm
	 * 
	 * @return a <code>SolutionSet</code> that is a set of non dominated
	 *         solutions as a result of the algorithm execution
	 */
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		int evaluations;

		// Read the parameters
		populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		if (getInputParameter("maxCoverage") != null)
			maxCoverage = ((Double) getInputParameter("maxCoverage")).doubleValue();
		else
			maxCoverage = 1.0d;
		// EvInT additional MOSA parameters
		isRandomRun = ((Boolean) getInputParameter("isRandomRun")).booleanValue();
		experimentTime = ((Integer) getInputParameter("experimentTime")).intValue();
		
		// Additional EvInT calculated field
		addRandom = (isRandomRun ? 1.0 : 0.1);

		//SolutionSet population;
		SolutionSet offspringPopulation;
		SolutionSet union;
		this.archive = new SolutionSet(this.allTargets.size());

		Operator mutationOperator;
		Operator crossoverOperator;
		Operator selectionOperator;

		// Initialize the variables
		population = new SolutionSet(populationSize);
		evaluations = 0;
		generationCounter = 0; // reset at the start of each MOSA run
		
		// Read the operators
		mutationOperator = operators_.get("mutation");
		crossoverOperator = operators_.get("crossover");
		selectionOperator = operators_.get("selection");

		GenericSolution newSolution;

		// Determine how many solutions to seed from the previous iteration.
		// Seeds fill the low part of the population; the rest are random.
		// If no seed is available, all solutions are random.
		int seedCount = (seedPopulation == null) ? 0 : seedPopulation.size();

		// Add seed solutions from the previous iteration's population.
		// These carry over the search progress already made, avoiding
		// re-discovery of the same easy regions from scratch.
		for (int i = 0; i < seedCount && i < populationSize; i++) {
			newSolution = new GenericSolution((GenericSolution) seedPopulation.get(i));
			problem_.evaluate(newSolution);  //TODO is it really necessary to run *again* the test on the seeded solutions?
			evaluations++;
			population.add(newSolution);
		}
		System.err.println("[MOSA] Seeded " + seedCount + " solutions from previous iteration.");

		// Fill the remainder of the population with fresh random solutions.
		// This maintains diversity so MOSA does not get stuck in local optima.
		for (int i = seedCount; i < populationSize; i++) {
			newSolution = new GenericSolution((MOSAGenericCoverageProblem) problem_);
			problem_.evaluate(newSolution);
			evaluations++;
			population.add(newSolution);
		}
		
		// store every T.C. that covers previously uncovered branches in the 
		// archive
		this.updateArchive(population, evaluations);

		long startTime = System.nanoTime();
		while (keepRunning(evaluations, startTime) && calculateCoverage() < maxCoverage) {
			generationCounter++;

			// feedback: print the current global fitness (i.e., the sum of the best fitnesses of
			// the uncovered objectives)
			if (generationCounter % 100 == 0) {
				double globalFitness = 0;
				for (GenericObjective obj : allTargets) {
					if (obj.isCovered()) {
						continue;
					}
					double objFitness = obj.getBestFitness();
					globalFitness += objFitness;
				}
				System.err.println("[MOSA] Iteration: " + generationCounter + ", current global fitness: " + globalFitness);
			}
			
			// Record a fitness snapshot for every objective at the end of each
			// generation so FitnessTracker can plot convergence curves.
			// Recording ALL objectives (not just allTargets subset) lets us see
			// whether non-subset objectives are coincidentally improving too.
			// Snapshot format: [generationCounter, evaluations, bestFitness]
			for (GenericObjective target : allTargets) {
				// For covered objectives record 0.0 — they are done
				double snapshotFitness = target.isCovered() ? 0.0 : target.getBestFitness();
				// Clamp MAX_VALUE to 1.0 for clean CSV output
				if (snapshotFitness == Double.MAX_VALUE) {
					snapshotFitness = 1.0;
				}
				// recordSnapshot stores [generation, evaluations, fitness]
				target.recordSnapshot(generationCounter, evaluations, snapshotFitness);
			}
			
			// creates the offspring population
			offspringPopulation = new SolutionSet(populationSize+(int)(populationSize*addRandom));
			GenericSolution[] parents = new GenericSolution[2];

			// add the random "offsprings"
			for (int i = 0; i < populationSize * addRandom; i++) {
				newSolution = new GenericSolution((MOSAGenericCoverageProblem) problem_);
				problem_.evaluate(newSolution);
				evaluations++;
				offspringPopulation.add(newSolution);
			}

			// add the offsprings (crossover / mutation / selection)
			if (!isRandomRun) {
				for (int i = 0; i < (populationSize / 2); i++) {
					if (evaluations < maxEvaluations) {
						// obtain parents
						parents[0] = (GenericSolution) selectionOperator.execute(population);
						parents[1] = (GenericSolution) selectionOperator.execute(population);
						GenericSolution[] offSpring = (GenericSolution[]) crossoverOperator.execute(parents);
						mutationOperator.execute(offSpring[0]);
						mutationOperator.execute(offSpring[1]);
						problem_.evaluate(offSpring[0]);
						problem_.evaluate(offSpring[1]);
						offspringPopulation.add(offSpring[0]);
						offspringPopulation.add(offSpring[1]);
						evaluations += 2;
					} // if

				} // for
			} // If not random run
			
			// Create the solutionSet union of population and offsprings
			union = ((SolutionSet) population).union(offspringPopulation);
			
			if (!isRandomRun) {

				//modify the objectives before here
				Front fronts = this.preferenceSorting(union);

				int remain = populationSize;
				int frontIndex = 0;
				SolutionSet front = null;
				population.clear(); // population t+1

				// Obtain the next front
				front = fronts.getFront(frontIndex);

				// From NGSA-II select TC that are the best for a specific objectives then TC that are not dominated
				while ((remain > 0) && (remain >= front.size())) {
					this.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());

					// Add the individuals of this front
					for (int i = 0; i < front.size(); i++) {
						population.add(front.get(i));
					}

					// Decrement remain
					remain -= front.size();

					// Obtain next front
					frontIndex++;
					if (remain > 0)
						front = fronts.getFront(frontIndex);
				} // while

				// if remain is less than current front size, insert only the best
				if (remain > 0) {
					// current front contains the individuals to insert
					this.crowdingDistanceAssignment(front, problem_.getNumberOfObjectives());
					front.sort(new CrowdingComparator());

					for (int i = 0; i < remain; i++) {
						population.add(front.get(i));
					}
				}
			} // If not random run


			this.updateArchive(population, evaluations);
			evaluations++;

		}// while
		
		// Feedback: time statistics
		long endTime = System.nanoTime();
		long time = endTime - startTime;
		long durationSeconds = TimeUnit.NANOSECONDS.toSeconds(time);
		long hours = durationSeconds / 3600;
		long minutes = (durationSeconds % 3600) / 60;
		long seconds = durationSeconds % 60;
		System.err.println("Time taken for MOSA_Generic: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");

		this.algorithmStats.setEvaluations(evaluations);

		return this.archive;
	}

	private boolean keepRunning(int evaluations, long startTime) {
		long endTime = System.nanoTime();
		if (experimentTime > 0) {
			if (TimeUnit.NANOSECONDS.toSeconds(endTime - startTime) < experimentTime) {
				return true;
			}
		} else if (maxEvaluations > 0 && evaluations < maxEvaluations) {
			return true;
		}

		System.out.println("This is while loop keepRunning: " + (endTime - startTime) / 1000000000);
		return false;

	}

	/**
	 * Stores every test case that covers previous uncovered branches in the
	 * archive variable as a candidate test case to form the final test suite
	 * 
	 * @param candidates
	 *            a set of candidate test cases
	 */
	private void updateArchive(SolutionSet candidates, int evaluation) {

		// In the first run we use all objectives, then only the active's ones
		// this part is not using in the new version of MOSA, but it is kept for future use
		// GenAndWrite now owns per-iteration activation of the subset before construction.
		/*if (firstRun) {
			for (GenericObjective objective : allTargets) {
				if (config.isDynamicObjectives())
					objective.setActive(false);
			}
			firstRun = false;
		}*/

		for (GenericObjective objective : allTargets) {

			if (objective.isCovered())
				continue;
			Iterator<Solution> iteratorCandidates = candidates.iterator();
			while (iteratorCandidates.hasNext()) {

				GenericSolution currentCandidate = (GenericSolution) iteratorCandidates.next();
				double objectiveScore = currentCandidate.getObjective(objective.getObjectiveID());
				objective.updateBestFitness(objectiveScore);
				if (objectiveScore == 0.0) {
					archive.add(currentCandidate);
					evaluations.add(evaluation);
					break;
				}
			} // while candidates
		} // while targets

	}

	private Front preferenceSorting(SolutionSet candidates) {
		Front front = new Front();
		SolutionSet population = candidates;
		SolutionSet front_0 = new SolutionSet(allTargets.size());

		/*** preference criterion ***/
		double minimum_fitness = Double.MAX_VALUE;
		GenericSolution t_best = null; // best test case
		Set<GenericSolution> solutionsToDelete = new HashSet<>();

		for (GenericObjective target : allTargets) {
			if (target.isCovered())
				continue;
			if (front_0.size() == front_0.getCapacity())
				break; // Exit if front_0 is full
			
			Iterator<Solution> populationIterator = population.iterator();
			while (populationIterator.hasNext()) {

				GenericSolution currentSolution = (GenericSolution) populationIterator.next();
				int idObjective = target.getObjectiveID();
				double currentObjective = currentSolution.getObjective(idObjective);

				if (currentObjective < minimum_fitness) {
					minimum_fitness = currentObjective;
					t_best = currentSolution;
				}// end-if
				
			} // end-while
			t_best.setRank(0); // set rank 0 for preference criterion
			front_0.add(t_best);	// adding to front 0
			solutionsToDelete.add(t_best);

		} // end for

		front.addFront(front_0);

		// Remotion of solution in first front from overall population
		Iterator<Solution> populationIterator = population.iterator();
		while (populationIterator.hasNext()) {
			GenericSolution currentSolution = (GenericSolution) populationIterator.next();
			if (solutionsToDelete.contains(currentSolution))
				populationIterator.remove();
		}

		/*** fast-non dominated-sort ***/
		ArrayList<GenericObjective> uncoveredTargets = new ArrayList<>();
		for (GenericObjective target: allTargets) {
			if (!target.isCovered()) {
				uncoveredTargets.add(target);				
			}
		}
		MOSARanking_Generic ranking = new MOSARanking_Generic(population, uncoveredTargets);

		int remain = populationSize;//populationSize candidates.size()
		remain -= front.getFront(0).size();
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

	public void crowdingDistanceAssignment(SolutionSet solutionSetOrig, int numberOfObjects) {
		int size = solutionSetOrig.size();

		if (size == 0) {
			return;
		}

		//TODO why this code clones all the Solutions in advance instead that doing it on-the-fly as the original Ocelot code? 
		SolutionSet solutionSet = new SolutionSet(solutionSetOrig.size());
		for (int i = 0; i < solutionSetOrig.size(); i++) {
			GenericSolution current = new GenericSolution((GenericSolution) solutionSetOrig.get(i));
			solutionSet.add(current);
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
			if ((allTargets.get(i).isCovered()))
				continue;
			// sort the population by current object
			//try {
			solutionSet.sort(new ObjectiveComparator(i));
			//} catch (IllegalArgumentException e) {
			//	System.out.println("ERROR");
			//}
			minObjective = solutionSet.get(0).getObjective(i);
			maxObjective = solutionSet.get(size - 1).getObjective(i);

			// set infinity crowding distance for first and last element
			solutionSet.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			solutionSet.get(size - 1).setCrowdingDistance(Double.POSITIVE_INFINITY);

			for (int j = 1; j < size - 1; j++) {
				distance = solutionSet.get(j + 1).getObjective(i)
						- solutionSet.get(j - 1).getObjective(i);

				// avoid division by 0 that leads to NaN values
				if (maxObjective - minObjective == 0)
					distance = 0.0;
				else
					distance = distance / (maxObjective - minObjective);

				distance += solutionSet.get(j).getCrowdingDistance();
				solutionSet.get(j).setCrowdingDistance(distance);
			}
		} // for

		for (int i = 0; i < solutionSet.size(); i++) {
			GenericSolution current = (GenericSolution) solutionSet.get(i);
			solutionSetOrig.replace(i, current);
		}
	}

	private double calculateCoverage() {
		double covered = 0;
		int total = 0;
		for (GenericObjective objective : allTargets) {
			if (objective.isCovered()) {
				covered++;
			}
			total++;
		}

		double coverage = covered / total;
		return coverage;
	}

	public List<Integer> getEvaluations() {
		return evaluations;
	}
}
