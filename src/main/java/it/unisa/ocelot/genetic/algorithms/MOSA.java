package it.unisa.ocelot.genetic.algorithms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorEdges;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.many_objective.MOSABranchCoverageProblem;
import it.unisa.ocelot.util.Fronts;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.MOSARanking;
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
 * This algorithm only solves {@link MOSABranchCoverageProblem}s.
 *
 * @author giograno
 */
public class MOSA extends OcelotAlgorithm {
	private static final long serialVersionUID = 1L;

	/**
	 * The CFG of the C function for which we
	 * are generating tests.
	 */
	private final CFG cfg;
	
	/**
	 * The targets, i.e., a subset of the CFG edges
	 * associated to the branches of the C function
	 * we want to cover. It must be contained in
	 * this.branches.
	 */
	private final HashSet<LabeledEdge> targets; //the targets; must be a subset of this.branches

	/**
	 * The branches, i.e., all the CFG edges
	 * associated to some branch of the C function. 
	 */
	//TODO do we need it?
	private final HashSet<LabeledEdge> branches; //all the branches
	
	/**
	 * Edge dominance information for the CFG.
	 * Necessary to calculate branch coverage
	 * for the branches that are not targets.
	 */
	//TODO do we need it?
	private final DominanceCalculatorEdges<CFGNode, LabeledEdge> cfgDominanceEdges;
	
	//TODO do we need it?
	@SuppressWarnings("unused")
	private final Set<LabeledEdge> coveredBranches; //the covered branches (not just the targets)

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
	 * @param pProblem the {@link MOSABranchCoverageProblem} to solve.
	 */
	public MOSA(MOSABranchCoverageProblem pProblem) {
		super(pProblem);
		this.cfg = pProblem.getCFG();
		this.branches = new HashSet<>(this.cfg.edgeSetBranch());
		this.targets = new HashSet<>(pProblem.getTargets());
		this.cfgDominanceEdges = new DominanceCalculatorEdges<>(this.cfg, this.cfg.getEntry(), this.cfg.getExit());
		
		this.coveredBranches = new HashSet<>();
	}

	/**
	 * Runs the MOSA algorithm.
	 * 
	 * @return a {@link SolutionSet}, that is a set of non dominated
	 *         solutions as result of the algorithm execution.
	 */
	@Override
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// Read the operators
		Operator selectionOperator = this.operators_.get("selection");
		Operator crossoverOperator = this.operators_.get("crossover");
		Operator mutationOperator = this.operators_.get("mutation");

		// Read the other parameters
		final int populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		final int maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		final int maxGenerations = ((Integer) getInputParameter("maxGenerations")).intValue();
		final double maxCoverage = ((Double) getInputParameter("maxCoverage")).doubleValue();
		final int maxTime = ((Integer) getInputParameter("maxTime")).intValue();

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
		
		// Creates and initializes the archive
		this.archive = new SolutionSet(this.targets.size());
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
			SolutionSet offspringPopulation = new SolutionSet(populationSize);
			for (int i = 0; i < (populationSize / 2); i++) {
				
				// Select a pair of parents
				Solution[] parents = new Solution[2];
				parents[0] = (Solution) selectionOperator.execute(population);
				parents[1] = (Solution) selectionOperator.execute(population);

				// Make offsprings by crossover of parents
				Solution[] offspring = (Solution[]) crossoverOperator.execute(parents);

				// Mutate offsprings
				mutationOperator.execute(offspring[0]);
				mutationOperator.execute(offspring[1]);

				// Evaluate the offsprings and
				// update the total number of evaluations
				this.problem_.evaluate(offspring[0]);
				evaluations++;
				this.problem_.evaluate(offspring[1]);
				evaluations++;

				// Add the offspring to the offspring population
				offspringPopulation.add(offspring[0]);
				offspringPopulation.add(offspring[1]);
			}

			// Merge the population with its offsprings,
			// puts the result in union
			SolutionSet union = population.union(offspringPopulation);

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

				// Add all the individuals of this front to the population
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
				// Sort the current front by crowding distance
				assignCrowdingDistance(subfront, this.problem_.getNumberOfObjectives());
				subfront.sort(new CrowdingComparator());

				// Put in the population the best 
				// remain solutions in the current front
				for (int i = 0; i < remain; i++) {
					population.add(subfront.get(i));
				}
			}

			// Update the archive with the solutions
			// in the new population that cover some
			// objective
			updateArchive(population);
			
			// Store the number of evaluations 
			// done until the end of this 
			// generation
			this.algorithmStats.addEvaluations(evaluations);
		}

		// Returns the archive
		return this.archive;
	}
	
	private boolean keepRunning(int evaluations, int maxEvaluations, int generations, int maxGenerations, double maxCoverage, long startTime, long maxTime) {
		if (maxTime != 0) {
			long endTime = System.nanoTime();
			if ( (endTime - startTime) / 1000000000 >= maxTime) {
				return false;
			}
		}
		if (maxEvaluations != 0){
			if (evaluations >= maxEvaluations) { 
				return false;
			}
		} 
		if (maxGenerations != 0){
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
		//TODO this method is really inefficient. Perhaps it should be improved to cope with the fact that we may have lots of targets.
		//TODO maybe we should first iterate candidates, then targets: This way the second target scanning loop for collateral targets can be deleted, and we can just detect collateral targets by going on the already started loop.
		for (LabeledEdge target : this.targets) {
			// Skips already covered targets
			if (target.isCovered()) {
				continue;
			}

			// Scans candidates in search for a
			// candidate Solution that covers target
			Iterator<Solution> iteratorCandidates = candidates.iterator();
			while (iteratorCandidates.hasNext()) {
				Solution candidate = iteratorCandidates.next();
				
				// Gets from candidate the value of 
				// the objective associated to target
				double objectiveValue = candidate.getObjective(target.getObjectiveId());

				if (objectiveValue == 0.0) {
					// Target covered!
					setCovered(target);

					// Adds the candidate to the archive
					this.archive.add(candidate);
					
					// Logs the covered target
					this.algorithmStats.log("Covered branch " + target.toString() + " from " +
							this.cfg.getEdgeSource(target).toString() + " to " + 
							this.cfg.getEdgeTarget(target).toString());

					// Looks for all targets 
					// collaterally covered by candidate
					// and marks them as covered
					for (LabeledEdge collateralTarget : this.targets) {
						// Skips already covered targets
						if (collateralTarget.isCovered()) {
							continue;
						}
						
						// Gets from candidate the value of 
						// the objective associated to collateralTarget
						double collateralObjective = candidate.getObjective(collateralTarget.getObjectiveId());
						if (collateralObjective == 0.0) {
							setCovered(collateralTarget);
							
							// Logs the covered target
							this.algorithmStats.log("Collaterally covered branch " + collateralTarget.toString() + " from " + 
									this.cfg.getEdgeSource(collateralTarget).toString() + " to " + 
									this.cfg.getEdgeTarget(collateralTarget).toString());
						}
					}
					
					// Stops scanning candidates and 
					// moves to the next target
					break;
				}
			}
		}
	}
	
	/**
	 * Set a target, and all the branches that
	 * dominate it, and that are not targets 
	 * themselves, as covered.
	 * 
	 * @param pTarget the edge to be set as covered.
	 */
	private void setCovered(LabeledEdge pTarget) {
		pTarget.setCovered();

		// Sets as covered all the branches
		// that dominate pTarget, and that are
		// not targets.
		//TODO is it really necessary?
		Set<LabeledEdge> edgeDominators = null; //lazily initialized
		for (LabeledEdge branch : this.branches) {
			if (!this.targets.contains(branch)) {
				// Lazy initialization of edgeDominators
				// (now we need it)
				if (edgeDominators == null) {
					edgeDominators = this.cfgDominanceEdges.getStrictDominators(pTarget);
				}
				
				if (edgeDominators.contains(branch)) {
					branch.setCovered();
				}
			}
		}
	}

	private Fronts preferenceSorting(SolutionSet candidates) {
		// Make a local copy of candidates named population
		SolutionSet population = new SolutionSet(candidates.getCapacity());
		Iterator<Solution> candidatesIterator = candidates.iterator();
		while (candidatesIterator.hasNext()) {
			population.add(candidatesIterator.next());
		}

		//TODO is this the right point to put them?
		double minimumFitness = Double.MAX_VALUE;
		Solution bestSolution = null;
		
		// Calculate subfront 0
		SolutionSet subfront_0 = new SolutionSet(this.targets.size());
		HashSet<Solution> subfront_0_hashset = new HashSet<>(); //identical to front_0, but a HashSet!
		for (LabeledEdge target : this.targets) {
			//TODO shouldn't we declare minimumFitness and bestSolution HERE??? So they are reset after each target, before bestSolution calculation.
			Iterator<Solution> populationIterator = population.iterator();
			while (populationIterator.hasNext()) {
				Solution currentSolution = populationIterator.next();
				int idObjective = target.getObjectiveId();
				double currentObjective = currentSolution.getObjective(idObjective);
				if (currentObjective < minimumFitness) {
					minimumFitness = currentObjective;
					bestSolution = currentSolution;
				}
			}
			bestSolution.setRank(0);
			subfront_0.add(bestSolution);
			subfront_0_hashset.add(bestSolution);
		}
		
		// Create the list of subfronts
		// and adds subfront_0 to it
		Fronts fronts = new Fronts();
		fronts.addFront(subfront_0);

		// Remove subfront 0 from population
		Iterator<Solution> populationIterator = population.iterator();
		while (populationIterator.hasNext()) {
			Solution currentSolution = populationIterator.next();
			if (subfront_0_hashset.contains(currentSolution)) {
				populationIterator.remove();
			}
		}

		// Fast-non dominated-sort
		MOSARanking ranking = new MOSARanking(population, this.targets);

		// Build all the remaining subfronts
		int remain = population.size();
		int subfrontNumber = 0;
		while (remain > 0) {
			SolutionSet currentFront = ranking.getSubfront(subfrontNumber);
			remain -= currentFront.size();
			fronts.addFront(currentFront);
			subfrontNumber++;
		}

		return fronts;
	}

	/**
	 * Calculates and sets the crowding
	 * distance to all the solutions
	 * in a solution set.
	 * 
	 * @param solutionSet the input {@link SolutionSet}.
	 *        This method will calculate and set the 
	 *        crowding distances of all the {@link Solution}s
	 *        in it.
	 * @param numberOfObjectives the number of objectives.
	 */
	private void assignCrowdingDistance(SolutionSet solutionSet, int numberOfObjectives) {
		int size = solutionSet.size();

		// manage the trivial cases
		if (size == 0) {
			return;
		} else if (size == 1) {
			solutionSet.get(0).setCrowdingDistance(Double.POSITIVE_INFINITY);
			return;
		}

		// initialize to 0.0 all crowding distances
		for (int i = 0; i < size; i++) {
			solutionSet.get(i).setCrowdingDistance(0.0);
		}

		double distance = 0.0;

		for (int i = 0; i < numberOfObjectives; i++) {
			// sort the population by current objective
			solutionSet.sort(new ObjectiveComparator(i));
			
			// get the minimum and maximum value of the
			// objective in the population
			double minObjective = solutionSet.get(0).getObjective(i);
			double maxObjective = solutionSet.get(size - 1).getObjective(i);

			// set infinity crowding distance for first and last 
			// solution in the population (i.e., the ones having
			// minimum and maximum value of the objective)

			//TODO unclear why it needs to make a clone and replace
			Solution current0 = new Solution(solutionSet.get(0));
			current0.setCrowdingDistance(Double.POSITIVE_INFINITY);
			solutionSet.replace(0, current0); // replace needed to avoid override

			//TODO unclear why it needs to make a clone and replace
			Solution currentLast = new Solution(solutionSet.get(size - 1));
			currentLast.setCrowdingDistance(Double.POSITIVE_INFINITY);
			solutionSet.replace(size - 1, currentLast); // replace needed to avoid override

			for (int j = 1; j < size - 1; j++) {
				distance = solutionSet.get(j + 1).getObjective(i)
						- solutionSet.get(j - 1).getObjective(i);

				// avoid division by 0 that leads to NaN values
				if (maxObjective - minObjective == 0) {
					distance = 0.0;
				} else {
					distance = distance / (maxObjective - minObjective);
				}
				distance += solutionSet.get(j).getCrowdingDistance();
				
				// Sets the calculated crowding distance 
				//TODO unclear why it needs to make a clone and replace
				Solution current = new Solution(solutionSet.get(j));
				current.setCrowdingDistance(distance);
				solutionSet.replace(j, current); // replace needed to avoid override
			}
		}
	}

	@SuppressWarnings("unused")
	private boolean allTargetsCovered() {
		for (LabeledEdge edge : this.targets) {
			if (!(edge.isCovered())) {
				return false;
			}
		}

		return true;
	}
	
	private double calculateCoverage() {
		double covered = 0;
		int total = 0;
		for (LabeledEdge edge : this.branches) {
			if (edge.isCovered()) {
				covered++;
			}
			total++;
		}
		
		double coverage = covered / total;
		return coverage;
	}
}
