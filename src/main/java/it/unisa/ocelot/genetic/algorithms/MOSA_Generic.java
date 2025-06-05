package it.unisa.ocelot.genetic.algorithms;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairObjective;
import it.unisa.ocelot.util.Front;
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
 * @author giograno
 *
 */
public class MOSA_Generic extends OcelotAlgorithm {

	// the final Solution Set produced by the algorithm
	private SolutionSet archive;
	// we store here the complete set of target for given problem
	private List<GenericObjective> allTargets;
	@SuppressWarnings("unused")
	private Set<GenericObjective> coveredObjectives;
	//private EdgeGraph<CFGNode, LabeledEdge> edgeGraph;
	private boolean newBatchObjectives;
	// Store tha active range on which you are currently calculating the MOSA
	private boolean activeObjectiveRange;
	
	private CFG cfg;
	private CType[] parametersTypes;
	private ConfigManager config;
	private List<GenericObjective> batchedObjectives;
	
	private boolean firstRun = true;
	
	
	//private Dominators<EdgeWrapper<LabeledEdge>, DefaultEdge> dominators;

	private List<Integer> evaluations;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param problem
	 *            Problem to solve
	 */
	public MOSA_Generic(MOSAGenericCoverageProblem problem, List<GenericObjective> targets, CFG cfg, CType[] parameters, ConfigManager config) {
		super(problem);
		allTargets = new ArrayList<>(targets);
		evaluations = new ArrayList<>();
		this.coveredObjectives = new HashSet<>();
		newBatchObjectives = false;
		
		this.cfg = cfg;
		parametersTypes = parameters;
		this.config = config;
		batchedObjectives = allTargets.subList(4, 104);
		
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
		int populationSize;
		int maxEvaluations;
		int evaluations;
		double maxCoverage;

		// Read the parameters
		populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		if (getInputParameter("maxCoverage") != null)
			maxCoverage = ((Double) getInputParameter("maxCoverage")).doubleValue();
		else
			maxCoverage = 1.0;

		SolutionSet population;
		SolutionSet offspringPopulation;
		SolutionSet union;
		this.archive = new SolutionSet(this.allTargets.size());

		Operator mutationOperator;
		Operator crossoverOperator;
		Operator selectionOperator;

		// Initialize the variables
		population = new SolutionSet(populationSize);
		evaluations = 0;

		// Read the operators
		mutationOperator = operators_.get("mutation");
		crossoverOperator = operators_.get("crossover");
		selectionOperator = operators_.get("selection");

		// Create the initial solutionSet
		Solution newSolution;
		for (int i = 0; i < populationSize; i++) {
			newSolution = new Solution(problem_);
			problem_.evaluate(newSolution);
			evaluations++;
			population.add(newSolution);
		}

		// store every T.C. that covers previously uncovered branches in the archive
		this.updateArchive(population, evaluations);
		
		int newSolutionEval = 5000;

		while (evaluations < maxEvaluations && calculateCoverage() < maxCoverage) {
			
			
			//System.out.println(evaluations + ":--:" + maxEvaluations);
			/*if (evaluations > 100) {
				try {
					MOSAGenericCoverageProblem problem = new MOSAGenericCoverageProblem(this.cfg, this.parametersTypes, config.getTestArraysSize(), 
							config.getTestRanges(), batchedObjectives);
					this.problem_ = problem;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}*/
			
			// Restar the population after a set budget is used
			/*if (evaluations > newSolutionEval) {
				population = new SolutionSet(populationSize);
				System.out.println("---------------------------------------newPopulation");
				for (int i = 0; i < populationSize; i++) {
					newSolution = new Solution(problem_);
					problem_.evaluate(newSolution);
					population.add(newSolution);
				}
				newSolutionEval += 2000;
			}*/
			
			
				
			offspringPopulation = new SolutionSet(populationSize);
			Solution[] parents = new Solution[2];

			for (int i = 0; i < (populationSize / 2); i++) {
				if (evaluations < maxEvaluations) {
					// obtain parents
					parents[0] = (Solution) selectionOperator.execute(population);
					parents[1] = (Solution) selectionOperator.execute(population);
					Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
					mutationOperator.execute(offSpring[0]);
					mutationOperator.execute(offSpring[1]);
					problem_.evaluate(offSpring[0]);
					problem_.evaluate(offSpring[1]);
					offspringPopulation.add(offSpring[0]);
					offspringPopulation.add(offSpring[1]);
					evaluations += 2;
				} // if

			} // for

			// Create the solutionSet union of solutionSet and offSpring
			union = ((SolutionSet) population).union(offspringPopulation);

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

				this.crowdingDistanceAssignmentV2(front, problem_.getNumberOfObjectives());

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
				this.crowdingDistanceAssignmentV2(front, problem_.getNumberOfObjectives());
				front.sort(new CrowdingComparator());

				for (int i = 0; i < remain; i++)
					population.add(front.get(i));
			}

			this.updateArchive(population, evaluations);
			evaluations++;

		}// while
		
		
		
		
		this.algorithmStats.setEvaluations(evaluations);

		return this.archive;
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
		if (firstRun) {
			for (GenericObjective objective : allTargets) {
				objective.setActive(false);
			}
			firstRun = false;
		}
		
		for (GenericObjective objective : allTargets) {

			if (objective.isCovered())
				continue;

			Iterator<Solution> iteratorCandidates = candidates.iterator();
			while (iteratorCandidates.hasNext()) {

				Solution currentCandidate = iteratorCandidates.next();
				double objectiveScore = currentCandidate.getObjective(objective.getObjectiveID());

				if (objectiveScore == 0.0) {
					objective.setCovered(true);
					PC_PairObjective triggeredPair = ((PC_PairObjective) objective).TriggeredPair;
					if (triggeredPair != null && !triggeredPair.isCovered()) {
						//((PC_PairObjective) objective).TriggeredPair.setActive(true);
						//System.out.println("Pair activated");
					}
					int acc = 0;
					for (GenericObjective target : allTargets) {
						if (target.isActive() && !target.isCovered()) {
							acc++;
						}
					}
					System.out.println("Active not covered " + acc);
					archive.add(currentCandidate);
					evaluations.add(evaluation);
					System.out.println("One covered " + archive.size());
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
		Solution t_best = null; // best test case
		Set<Solution> solutionsToDelete = new HashSet<>();

		for (GenericObjective target : allTargets) {
			if (target.isCovered() || !target.isActive())
				continue;
			Iterator<Solution> populationIterator = population.iterator();
			while (populationIterator.hasNext()) {

				Solution currentSolution = populationIterator.next();
				int idObjective = target.getObjectiveID();
				double currentObjective = currentSolution.getObjective(idObjective);

				if (currentObjective < minimum_fitness) {
					minimum_fitness = currentObjective;
					t_best = currentSolution;
				}// end-if

			} // end-while
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

		MOSARanking_Generic ranking = new MOSARanking_Generic(population, allTargets);

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

	public void crowdingDistanceAssignment(SolutionSet solutionSet, int numberOfObjects) {
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
		} // for
	}
	
	public void crowdingDistanceAssignmentV2(SolutionSet solutionSetOrig, int numberOfObjects) {
		SolutionSet solutionSet = new SolutionSet(solutionSetOrig.size());
		for (int i = 0; i < solutionSetOrig.size(); i++) {
			Solution current = new Solution(solutionSetOrig.get(i));
			solutionSet.add(current);
		}
		
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
			Solution current = new Solution(solutionSet.get(i));
			solutionSetOrig.replace(i, current);
		}
	}
	
	 public void fastEpsilonDominanceAssignment(SolutionSet solutionSet, int numberOfObjects) {
	        double value;
			int size = solutionSet.size();
			for (int i = 0; i < size; i++)
				solutionSet.get(i).setCrowdingDistance(0.0);

			for (int i = 0; i < numberOfObjects; i++) {
	            double min = Double.POSITIVE_INFINITY;
	            List<Solution> minSet = new ArrayList<>(solutionSet.size());
	            double max = 0;
	            for (int j = 1; j < size - 1; j++) {
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
		for (GenericObjective objective : allTargets)
			if (!(objective.isCovered()))
				return false;

		return true;
	}
	
	private double calculateCoverage() {
		double covered = 0;
		int total = 0;
		for (GenericObjective objective : allTargets) {
			if (objective.isCovered())
				covered++;
			else {
//				System.out.println("Branch " + edge + " from " + this.controlFlowGraph.getEdgeSource(edge));
			}
			total++;
		}
		
		double coverage = covered / total;
//		System.out.println(coverage);
		return coverage;
	}

	public List<Integer> getEvaluations() {
		return evaluations;
	}
}
