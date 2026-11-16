package it.unisa.ocelot.genetic.algorithms;

import java.util.Comparator;
import java.util.Iterator;

import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.comparators.FitnessComparator;
import jmetal.util.comparators.ObjectiveComparator;
import jmetal.util.parallel.IParallelEvaluator;

public class CDG extends OcelotAlgorithm {
	private static final long serialVersionUID = 7006139362097837208L;

	IParallelEvaluator parallelEvaluator; //currently unused

	public CDG(StandardProblem pProblem, IParallelEvaluator pParallelEvaluator) {
		super(pProblem);
		this.parallelEvaluator = pParallelEvaluator;
	}

	@SuppressWarnings({ "unused", "rawtypes" })
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// Reads the parameters
		int populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		int maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();

		// Makes the comparators
		Comparator objectiveComparator = new ObjectiveComparator(0); // Single objective comparator
		Comparator fitnessComparator = new FitnessComparator();

		// Makes SolutionSets
		SolutionSet population = new SolutionSet(populationSize);
		SolutionSet offspringPopulation = new SolutionSet(populationSize);
		SolutionSet achievedPopulation = new SolutionSet(populationSize); //solutions that reach target

		// Reads the operators
		Operator mutationOperator = this.operators_.get("mutation");
		Operator crossoverOperator = this.operators_.get("crossover");
		Operator selectionOperator = this.operators_.get("selection");

		// Starts counting the number of evaluations
		int evaluations = 0;

		// Creates the initial population
		for (int i = 0; i < populationSize; i++) {
			Solution newSolution = new Solution(this.problem_);
			// parallelEvaluator_.addSolutionForEvaluation(newSolution);
			this.problem_.evaluate(newSolution);
			evaluations++;
			population.add(newSolution);
		}
		population.sort(objectiveComparator);
		
		// These variables store the best results
		double bestFitness = Double.MAX_VALUE;
		Solution bestFitnessSolution = null;
		boolean zeroFitnessAchieved = false;
		
		// Runs the GA
		while (evaluations < maxEvaluations && !zeroFitnessAchieved) {
			// Copies the best pair of individuals in the population
			// to the offspring population
			offspringPopulation.add(new Solution(population.get(0)));
			offspringPopulation.add(new Solution(population.get(1)));

			for (int i = 0; i < (populationSize / 2) - 1; i++) {
				// stochastic universal sampling
				population.sort(objectiveComparator);

				// selection
				Solution[] parents = new Solution[2];
				parents[0] = (Solution) selectionOperator.execute(population);
				parents[1] = (Solution) selectionOperator.execute(population);

				// crossover
				Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);

				// mutation
				mutationOperator.execute(offSpring[0]);
				mutationOperator.execute(offSpring[1]);

				// Evaluation
				this.problem_.evaluate(offSpring[0]);
				this.problem_.evaluate(offSpring[1]);
				
				// Adds to offsprings
				offspringPopulation.add(offSpring[0]);
				offspringPopulation.add(offSpring[1]);
				
				// updates number of evaluations
				evaluations += 2;
			}

			// The offspring population becomes the new current population
			population.clear();
			for (int i = 0; i < populationSize; i++) {
				population.add(offspringPopulation.get(i));
			}
			offspringPopulation.clear();

			// Updates the best results
			Iterator<Solution> it = population.iterator();
			while (it.hasNext()) {
				Solution current = it.next();
				double currentFitness = current.getFitness();

				if (current.getObjective(0) == 0 && currentFitness < bestFitness) {
					bestFitness = currentFitness;
					bestFitnessSolution = new Solution(current);
					if (currentFitness == 0) {
						zeroFitnessAchieved = true;
					}
					
					// Updates the stats
					this.algorithmStats.setStat("UpdateFitness", currentFitness);
				}
			}
		}
		
		// Updates the stats
		this.algorithmStats.addEvaluations(evaluations);

		// Builds the result SolutionSet (just one
		// individual, the one with best fitness)
		SolutionSet result = new SolutionSet(1);
		if (bestFitnessSolution == null) {
			// No best solution: picks the best one from
			// the population
			this.algorithmStats.setStat("NullSolution", true);
			population.sort(objectiveComparator);
			result.add(population.get(0));
		} else {
			result.add(bestFitnessSolution);
		}
		
		return result;
	}
}
