package it.unisa.ocelot.genetic.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Operator;
import jmetal.core.Solution;
import jmetal.core.SolutionSet;
import jmetal.util.JMException;
import jmetal.util.comparators.ObjectiveComparator;

/**
 * A general genetic algorithm.
 * 
 * @author giograno
 */
public final class GeneticAlgorithm extends AbstractGeneticMemeticAlgorithm {
	private static final long serialVersionUID = -2679014653669190929L;

	/**
	 * Constructor.
	 * 
	 * @param pStandardProblem the {@link StandardProblem} to solve.
	 */
	public GeneticAlgorithm(StandardProblem pStandardProblem) {
		super(pStandardProblem);
	}

	/**
	 * Runs the algorithm.
	 * 
	 * @return a {@link SolutionSet} of non dominated solutions
	 *         as result of the algorithm execution.
	 * @throws JMException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("rawtypes")
	public SolutionSet execute() throws JMException, ClassNotFoundException {
		// Read the operators
		Operator mutationOperator = this.operators_.get("mutation");
		Operator crossoverOperator = this.operators_.get("crossover");
		Operator selectionOperator = this.operators_.get("selection");

		// Read the parameters
		int populationSize = ((Integer) getInputParameter("populationSize")).intValue();
		int maxEvaluations = ((Integer) getInputParameter("maxEvaluations")).intValue();
		//TODO int numberOfThreads = ((Integer) getInputParameter("threads")).intValue();

		Comparator comparator = new ObjectiveComparator(0); // Single objective comparator

		//TODO parallelEvaluator_.startEvaluator(problem_); ???

		// Create the initial population.
		SolutionSet population;
		if (this.startingPopulation == null && this.lastPopulation == null) {
			// Random population.
			population = new SolutionSet(populationSize);
			for (int i = 0; i < population.size(); i++) {
				Solution solution = new Solution(this.problem_);
				population.add(solution);
			}
		} else if (this.lastPopulation != null) { //this.lastPopulation has priority over this.startingPopulation to allow restarts
			population = this.lastPopulation; //TODO ensure it has populationSize?
		} else { //this.startingPopulation != null
			population = this.startingPopulation; //TODO cut it to populationSize?
		}

		boolean targetCovered = false;
		int evaluations = 0;

		// Evaluate solutions in initial population and check serendipity.
		for (int i = 0; i < population.size(); i++) {
			Solution solution = population.get(i);
			prepareSerendipitous();
			this.problem_.evaluate(solution);
			evaluations++;
			checkSerendipitous(solution);
			if (solution.getObjective(0) == 0.0) {
				targetCovered = true;
				break;
			}
		}

		// Exit upon coverage.
		if (targetCovered) {
			// Returns.
			population.sort(comparator);

			// Return a population with the best individual
			SolutionSet resultPopulation = new SolutionSet(1);
			resultPopulation.add(population.get(0));
			this.lastPopulation = population;

			// Set stats
			this.algorithmStats.addEvaluations(evaluations);

			return resultPopulation;
		}

		// Make more (random) solutions to be merged to the population
		// up to its capacity.
		List<Solution> solutionList = new ArrayList<>();
		for (int i = 0; i < population.getCapacity() - population.size(); i++) {
			Solution solution = new Solution(this.problem_);
			solutionList.add(solution);
		}

		// Evaluate solutions in new population and check serendipity.
		for (Solution solution : solutionList) {
			prepareSerendipitous();
			this.problem_.evaluate(solution);
			evaluations++;
			checkSerendipitous(solution);
			population.add(solution);
			if (solution.getObjective(0) == 0.0) {
				targetCovered = true; //we cover once we find the objective 0.
				break;
			}
		}

		// Exit upon coverage (again).
		if (targetCovered) {
			// Returns.
			population.sort(comparator);

			// Return a population with the best individual
			SolutionSet resultPopulation = new SolutionSet(1);
			resultPopulation.add(population.get(0));
			this.lastPopulation = population;

			// Set stats
			this.algorithmStats.addEvaluations(evaluations);

			return resultPopulation;
		}

		// Generations.
		population.sort(comparator);
		SolutionSet offspringPopulation = new SolutionSet(populationSize);
		while (evaluations < maxEvaluations && !targetCovered) {
			// Copy the best two individuals to the offspring population.
			offspringPopulation.add(new Solution(population.get(0)));
			offspringPopulation.add(new Solution(population.get(1)));

			// Make more offsprings.
			Solution[] parents = new Solution[2];
			ArrayList<Solution> solutions = new ArrayList<>();
			for (int i = 0; i < (populationSize / 2) - 1; i++) {
				// obtain parents
				parents[0] = (Solution) selectionOperator.execute(population);
				parents[1] = (Solution) selectionOperator.execute(population);
				Solution[] offSpring = (Solution[]) crossoverOperator.execute(parents);
				mutationOperator.execute(offSpring[0]);
				mutationOperator.execute(offSpring[1]);
				solutions.add(offSpring[0]);
				solutions.add(offSpring[1]);
			}

			// Evaluate offsprings.
			for (Solution solution : solutions) {
				prepareSerendipitous();
				this.problem_.evaluate(solution);
				evaluations++;
				checkSerendipitous(solution);
				offspringPopulation.add(solution);
				if (solution.getObjective(0) == 0.0) {
					targetCovered = true;
					break;
				}
			}
			
			// Exit upon coverage (again).
			//TODO perhaps the population here is broken (wrong size): this impacts only this.lastPopulation
			if (targetCovered) {
				// Add offsprings (the best individual is there).
				population = population.union(offspringPopulation);
				
				// Sort.
				population.sort(comparator);
				this.lastPopulation = population;
				
				// Set stats.
				this.algorithmStats.addEvaluations(evaluations);
				
				// Return a population with the best individual.
				SolutionSet result = new SolutionSet(1);
				result.add(population.get(0));
				return result;
			}

			// Add offsprings to population.
			int populationCapacity = population.getCapacity();
			population.setCapacity(population.size() + offspringPopulation.size()); //temporarily increased to host offsprings
			for (int i = 0; i < offspringPopulation.size(); i++) {
				population.add(offspringPopulation.get(i));
			}

			// Keep the best individuals up to population capacity.
			population.sort(comparator);
			for (int i = population.getCapacity() - 1; i >= populationCapacity; i--) {
				population.remove(i);
			}
			population.setCapacity(populationCapacity);
			
			// Reset offsprings.
			offspringPopulation.clear();
		}

		// Sorts and stores the last population.
		population.sort(comparator);
		this.lastPopulation = population;

		// Set stats.
		this.algorithmStats.addEvaluations(evaluations);

		// Return a population with the best individual.
		SolutionSet result = new SolutionSet(1);
		result.add(population.get(0));
		return result;
	}
}
