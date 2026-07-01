package it.unisa.ocelot.genetic.algorithms;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cdg.BranchChainManager;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.BranchManager;
import it.unisa.ocelot.genetic.objectives.BranchObjective;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairObjective;
import it.unisa.ocelot.genetic.objectives.PC_PairsManager;
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
	private List<GenericObjective> archiveTargets;
	@SuppressWarnings("unused")
	private Set<GenericObjective> coveredObjectives;
	
	private CFG cfg;
	private CType[] parametersTypes;
	private ConfigManager config;
	
	private boolean firstRun = true;
	
	private double addRandom;
	
	int allottedTime;
	
	private int itCounter;
	
	private int populationSize;
	
	
	//private Dominators<EdgeWrapper<LabeledEdge>, DefaultEdge> dominators;

	private List<Integer> evaluations;
	  /**
     * Optional seed population provided by PopulationStore from the previous
     * iteration. If non-null, the first seedPopulation.size() solutions of the
     * initial population are taken from here instead of being generated randomly.
     * This preserves search progress across iterations.
     */
    private SolutionSet seedPopulation;
    /**
     * Current working population. Declared as a field (not local variable in
     * execute()) so getFinalPopulation() can return it after execute() finishes.
     */
    private SolutionSet population;
    /**
     * Interval (in evaluations) at which MOSA records a fitness snapshot for
     * each objective. Used by MABController to compute velocity.
     * Snapshot every 100 evaluations by default — tune here if needed.
     */
    private static final int SNAPSHOT_INTERVAL = 100;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor
	 * 
	 * @param problem
	 *            Problem to solve
	 * @throws IOException 
	 */
	public MOSA_Generic(MOSAGenericCoverageProblem problem, List<GenericObjective> targets, CFG cfg, CType[] parameters, ConfigManager config) {
		super(problem);
		allTargets = new ArrayList<>(targets);
		archiveTargets=BranchChainManager.loadObjectives();
		evaluations = new ArrayList<>();
		this.coveredObjectives = new HashSet<>();
		
		this.cfg = cfg;
		parametersTypes = parameters;
		this.config = config;
		allottedTime = config.getExecutionTime();
		
		if (config.isRandomRun())
			addRandom = 1.0;
		else
			addRandom = 0.1;
		
		itCounter = 0;
		
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
		
		int maxEvaluations;
		int evaluations;
		double maxCoverage;

		// Read the parameters
		
		maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		if (getInputParameter("maxCoverage") != null)
			maxCoverage = ((Double) getInputParameter("maxCoverage")).doubleValue();
		else
			maxCoverage = 1.0;
		populationSize = ((Integer) getInputParameter("populationSize")).intValue();

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

		// Read the operators
		mutationOperator = operators_.get("mutation");
		crossoverOperator = operators_.get("crossover");
		selectionOperator = operators_.get("selection");

		// Create the initial solutionSet THIS IS OLD VERSION
		/*Solution newSolution;
		for (int i = 0; i < populationSize; i++) {
			//System.err.println("Population count: " + i);
			newSolution = new Solution(problem_);
			problem_.evaluate(newSolution);
			evaluations++;
			population.add(newSolution);
		}*/
		   Solution newSolution;
		   
	        // Determine how many solutions to seed from the previous iteration.
	        // Seeds fill the first half of the population; the rest are random.
	        // If no seed is available (first iteration), all solutions are random.
	        int seedCount = (seedPopulation != null) ? seedPopulation.size() : 0;
	 
	        // Add seed solutions from the previous iteration's population.
	        // These carry over the search progress already made, avoiding
	        // re-discovery of the same easy regions from scratch.
	        for (int i = 0; i < seedCount && i < populationSize; i++) {
	            newSolution = new Solution(seedPopulation.get(i));
	            problem_.evaluate(newSolution);
	            evaluations++;
	            population.add(newSolution);
	        }
	 
	        // Fill the remainder of the population with fresh random solutions.
	        // This maintains diversity so MOSA does not get stuck in local optima.
	        for (int i = seedCount; i < populationSize; i++) {
	            newSolution = new Solution(problem_);
	            problem_.evaluate(newSolution);
	            evaluations++;
	            population.add(newSolution);
	        }
		// store every T.C. that covers previously uncovered branches in the archive
		this.updateArchive(population, evaluations);

		long startTime = System.nanoTime();
		while ( keepRunning(evaluations, maxEvaluations, startTime) && calculateCoverage() < maxCoverage) {
			itCounter++;
				
			offspringPopulation = new SolutionSet(populationSize+(int)(populationSize*addRandom));
			Solution[] parents = new Solution[2];
			
			for (int i = 0; i < populationSize * 0.1; i++) {
				//System.err.println("Population count: " + i);
				newSolution = new Solution(problem_);
				problem_.evaluate(newSolution);
				evaluations++;
				offspringPopulation.add(newSolution);
			}

			if (!config.isRandomRun()) {
			for (int i = 0; i < (populationSize / 2); i++) {
				//System.err.println("Population count: " + i);
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
			} // If not random run
			// Create the solutionSet union of solutionSet and offSpring
			union = ((SolutionSet) population).union(offspringPopulation);
			
			double globalFitness = 0;
			
			for (GenericObjective obj:allTargets) {
				double objFitness = Double.MAX_VALUE;
				if (obj.isCovered() || !obj.isActive())
					continue;
				for(int i = 0; i < union.size(); i++) {
					if (union.get(i).getObjective(obj.getObjectiveID()) <  objFitness)
						objFitness = union.get(i).getObjective(obj.getObjectiveID());
				}
				globalFitness += objFitness;
			}
			//System.out.println("current global fitness: "+globalFitness);
			
			
			this.updateArchive(union, evaluations);
			// Record a fitness snapshot every SNAPSHOT_INTERVAL evaluations.
	        // MABController reads these after the run to compute per-objective
	        // velocity (rate of fitness improvement).
	        if (evaluations % SNAPSHOT_INTERVAL == 0) {
	            for (GenericObjective target : allTargets) {
	                if (!target.isCovered()) {
	                    // bestFitness is updated by preferenceSorting — record it now
	                    target.recordSnapshot(evaluations, target.bestFitness);
	                }
	            }
	        }
			if(!config.isRandomRun()) {

			//modify the objectives before here
			Front fronts = this.preferenceSorting(union);

			int remain = populationSize;
			int frontIndex = 0;
			SolutionSet front = null;
			population.clear(); // population t+1

			// Obtain the next front
			front = fronts.getFront(frontIndex);
			
			int c = 0;
			
			// From NGSA-II select TC that are the best for a specific objectives then TC that are not dominated
			while ((remain > 0) && (remain >= front.size())) {
				if (c > 0)
					//System.out.println(c);
				c++;
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
			//System.out.println("Remain: " + remain + " Front: " + front.size());

			// if remain is less than current front size, insert only the best
			if (remain > 0) {
				if (c > 0)
					//System.out.println(c);
				// current front contains the individuals to insert
				this.crowdingDistanceAssignmentV2(front, problem_.getNumberOfObjectives());
				front.sort(new CrowdingComparator());

				for (int i = 0; i < remain; i++) {
					population.add(front.get(i));
				}
			}
			} // If not random run
			
			/*for (GenericObjective obj:allTargets) {
				double objFitness = Double.MAX_VALUE;
				if (obj.isCovered() || !obj.isActive())
					continue;
				for(int i = 0; i < union.size(); i++) {
					if (union.get(i).getObjective(obj.getObjectiveID()) <  objFitness)
						objFitness = union.get(i).getObjective(obj.getObjectiveID());
				}
				globalFitness += objFitness;
			}
			System.out.println(globalFitness);*/
			
			for (GenericObjective target : allTargets) {
				if(target.counter > config.maxIterForObjective())
					target.setActive(false);
			}

			
			evaluations++;

		}// while
		long endTime = System.nanoTime();
		long time=endTime-startTime;

		long durationSeconds = time / 1_000_000_000;

		long hours = durationSeconds / 3600;
		long minutes = (durationSeconds % 3600) / 60;
		long seconds = durationSeconds % 60;

		System.err.println("Time taken for MOSA_Generic: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
		// Print to file covered and uncovered branches 
		// different print whether we are in pair optimize or branch optimize
		
		Set<String> coveredBranches = new HashSet<String>();
		Set<String> maybeUncoveredBranches = new HashSet<String>();
		if (config.getOptimizeFor().equals("Pairs")) {
			for (GenericObjective obj:allTargets) {
				if (obj instanceof PC_PairObjective) {
				if (obj.isCovered()) {
						coveredBranches.add(((PC_PairObjective) obj).sm.getTestObjOne());
						coveredBranches.add(((PC_PairObjective) obj).sm.getTestObjTwo());
					}
				else if (obj.isActive()){
					
						if(((PC_PairObjective) obj).sm.getFitValOne() == 0.0) {
							coveredBranches.add(((PC_PairObjective) obj).sm.getTestObjOne());
						}
						maybeUncoveredBranches.add(((PC_PairObjective) obj).sm.getTestObjOne());
						maybeUncoveredBranches.add(((PC_PairObjective) obj).sm.getTestObjTwo());
					}
				}
			}
			maybeUncoveredBranches.removeAll(coveredBranches);
		}
		else if (config.getOptimizeFor().equals("Branches")) {
			for (GenericObjective obj:allTargets) {
				if (!obj.isCovered() & obj.isActive()) {
					maybeUncoveredBranches.add(((BranchObjective) obj).testObj);
				}
				else if(obj.isCovered()) {
					coveredBranches.add(((BranchObjective) obj).testObj);
				}
				else
					System.err.println("[Error] Line 311 Mosa_Generic");
			}
		}
		try {
		      FileWriter uncoveredWriter = new FileWriter("uncoveredBranches.txt");
		      FileWriter coveredWriter = new FileWriter("coveredBranches.txt");
		      
		      for (String branch : maybeUncoveredBranches)
		    	  uncoveredWriter.write(branch + "\n");
		      uncoveredWriter.close();
		      
		      for (String branch : coveredBranches)
		    	  coveredWriter.write(branch + "\n");
		      coveredWriter.close();
		      
		    } catch (IOException e) {
		      System.out.println("Unable to generate file uncoveredBranches.txt");
		      e.printStackTrace();
		    }
		
		
		this.algorithmStats.setEvaluations(evaluations);

		return this.archive;
	}

	private boolean keepRunning(int evaluations, int maxEvaluations, long startTime) {
		long endTime = System.nanoTime();
		if (allottedTime != 0) {
			
			if ( (endTime - startTime) / 1000000000 < allottedTime)
				return true;
		}
		else {
			if (evaluations < maxEvaluations) 
				return true;
		}

		System.out.println("This is while loop keepRunning: "+(endTime - startTime) / 1000000000);
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
		if (firstRun) {
			for (GenericObjective objective : allTargets) {
				if (config.isDynamicObjectives())
					objective.setActive(false);
			}
			firstRun = false;
		}
		
		for (GenericObjective objective : allTargets) {

			if (objective.isCovered())
				continue;
			//System.out.println("Checking objective " + objective.getObjectiveID() + " isActive=" + objective.isActive());
			Iterator<Solution> iteratorCandidates = candidates.iterator();
			while (iteratorCandidates.hasNext()) {

				Solution currentCandidate = iteratorCandidates.next();
				double objectiveScore = currentCandidate.getObjective(objective.getObjectiveID());
				//System.out.println("  -> score=" + objectiveScore);
				if (objectiveScore == 0.0) {
					objective.setCovered(true);
					GenericObjective triggeredPair = objective.TriggeredPair;
					
					// Add the solution that covered the objective to the objective object
					//((PC_PairObjective) objective).DiscovererTestCase = currentCandidate;
					if (triggeredPair != null && !triggeredPair.isCovered()) {
						objective.TriggeredPair.setActive(true);
						//System.out.println("Pair activated");
					}
					
					
					archive.add(currentCandidate);
					evaluations.add(evaluation);
					//System.out.println("One more covered, Total covered until now: " + archive.size()+" out of "+allTargets.size());
					break;
				}
			} // while candidates
		} // while targets
		
		int acc = 0;
		for (GenericObjective target : allTargets) {
			if (target.isActive() && !target.isCovered()) {
				acc++;
			}
		}
		//System.out.println("Active not covered " + acc);
	}

	private Front preferenceSorting(SolutionSet candidates) {

		Front front = new Front();
		SolutionSet population = candidates;
		SolutionSet front_0 = null;
		if (populationSize < allTargets.size())
			front_0 = new SolutionSet(populationSize);
		else
			front_0 = new SolutionSet(allTargets.size());

		/*** preference criterion ***/
		
		 
		Set<Solution> solutionsToDelete = new HashSet<>();

		for (GenericObjective target : allTargets) {
			if (target.isCovered() || !target.isActive())
				continue;
			if (front_0.size() == front_0.getCapacity())
				break; // Exit if front_0 is full
			Iterator<Solution> populationIterator = population.iterator();
			double minimum_fitness = Double.MAX_VALUE;
			// best test case
			Solution t_best = null;
			while (populationIterator.hasNext()) {
				
				int idObjective = target.getObjectiveID();
				Solution currentSolution = populationIterator.next();
				double currentObjective = currentSolution.getObjective(idObjective);

				if (currentObjective < minimum_fitness) {
					minimum_fitness = currentObjective;
					t_best = currentSolution;
				}
				
				if (currentObjective < target.bestFitness) {
					target.counter = 0;
					target.bestFitness = currentObjective;
				}

			} // end-while
			target.counter++;
			//System.out.println(target.toString() + minimum_fitness);
			t_best.setRank(0); // set rank 0 for preference criterion
			front_0.add(t_best);	// adding to front 0
			
			
			solutionsToDelete.add(t_best);

		} // end for

		front.addFront(front_0);
		/*if(front_0.size()>=populationSize) {
			return front;
		}*/
		// Remotion of solution in first front from overall population
		Iterator<Solution> populationIterator = population.iterator();
		while (populationIterator.hasNext()) {
			Solution currentSolution = populationIterator.next();
			if (solutionsToDelete.contains(currentSolution))
				populationIterator.remove();
		}

		/*** fast-non dominated-sort ***/
		ArrayList<GenericObjective> notCovTargets = new ArrayList<>();
		for( GenericObjective tar: allTargets) {
			if(!tar.isCovered() && tar.isActive()){
				notCovTargets.add(tar);				
			}
		}
		
		MOSARanking_Generic ranking = new MOSARanking_Generic(population, notCovTargets, populationSize);//allTargets notCovTargets
		


		int remain = populationSize;//populationSize candidates.size()
		remain -=front.getFront(0).size();
		int front_number = 0;
		SolutionSet currentFront = new SolutionSet(candidates.size());
		
			while (remain > 0) {
				try {
				currentFront = ranking.getSubfront(front_number);
				}catch (Exception e) {
		            e.printStackTrace();
		        }
				
				//System.out.println("remain1: " + remain);
				//System.out.println("currentFront.size(): " + currentFront.size());
				remain -= currentFront.size();
				//System.out.println("remain2: " + remain);
				front.addFront(currentFront);
				front_number++;
			}
		
		

		return front;
	}
	
	//public List<SolutionSet> rankDominatedAndNonDominated(population) {}

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
			if ((allTargets.get(i).isCovered())||  !(allTargets.get(i).isActive()))
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
