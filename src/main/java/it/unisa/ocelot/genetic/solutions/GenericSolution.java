package it.unisa.ocelot.genetic.solutions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.core.Variable;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class GenericSolution extends Solution implements Serializable {

	private static final long serialVersionUID = 1456425403777198793L;

	private Object cacheObject;

	public GenericSolution(MOSAGenericCoverageProblem problem) throws ClassNotFoundException {
		super(problem);
		cacheObject = null;
	}

	public GenericSolution(MOSAGenericCoverageProblem problem, Variable [] variables) {
		super(problem, variables);
		this.cacheObject = null;
	}

	public GenericSolution(GenericSolution solution) {
		super(solution);
		cacheObject = solution.cacheObject;
	}

	public Object getCacheObject() {
		return cacheObject;
	}

	public void update(Object cacheObject) {
		this.cacheObject = cacheObject;
		setAllFitnessValuesFromCache();
	}

	private void setAllFitnessValuesFromCache() {
		for (GenericObjective objective : objectives()) {
			setObjectiveFitness(objective);
		}
	}

	private List<GenericObjective> objectives() {
		Problem problem = getProblem();
		if (problem instanceof MOSAGenericCoverageProblem) {
			return ((MOSAGenericCoverageProblem) problem).getTargetObjectives();
		} else {
			throw new IllegalStateException("GenericSolution is not associated with a MOSAGenericCoverageProblem");
		}
	}

	public List<GenericObjective> getCoveredObjectives() {
		List<GenericObjective> objectives = objectives();
		List<GenericObjective> coveredObjectives = new ArrayList<>();
		for (GenericObjective objective : objectives) {
			double objectiveFitness = getObjective(objective.getObjectiveID());
			if (objectiveFitness == 0.0d) {
				coveredObjectives.add(objective);
			}
		}
		return coveredObjectives;
	}

	public Collection<? extends GenericObjective> getCoveredObjectives(List<GenericObjective> objectives) {
		List<GenericObjective> coveredObjectives = new ArrayList<>();
		for (GenericObjective objective : objectives) {
			double objectiveFitness = Double.MAX_VALUE;
			if(objective.getObjectiveID() >= getNumberOfObjectives()) {
				continue;
			}
			objectiveFitness = getObjective(objective.getObjectiveID());
			if (objectiveFitness == 0.0d) {
				coveredObjectives.add(objective);
			}
		}
		return coveredObjectives;
	}

	public void setObjectiveFitness(GenericObjective objective) {
		setObjective(objective.getObjectiveID(), objective.getFitness(this));
	}

	public double getObjectiveFitness(GenericObjective objective) {
		return getObjective(objective.getObjectiveID());
	}
	
	/**
	 * Evaluates an objective against the given execution data, without changing
	 * this solution. Used to verify coverage on freshly re-executed data.
	 * For verfication purposes, the objective does not need to be in this solution's problem.
	 */
	public double evaluateObjectiveOn(Object data, GenericObjective objective) {
	    Object saved = this.cacheObject;
	    this.cacheObject = data;
	    try {
	        return objective.getFitness(this);
	    } finally {
	        this.cacheObject = saved;
	    }
	}
}
