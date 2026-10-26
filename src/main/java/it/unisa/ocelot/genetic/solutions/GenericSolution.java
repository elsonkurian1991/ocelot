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
	// Execution data (branch-distance map) of the last evaluation. Unlike cacheObject,
	// which update() clears, this is kept so objectives outside the problem's target
	// list can be evaluated later. Never mutated after creation, so copies share it.
	private Object executionData;
	
	/** Discards execution data so a failed evaluation cannot leave a parent's data behind. */
	public void clearExecutionData() {
	    this.executionData = null;
	}
	public GenericSolution(MOSAGenericCoverageProblem problem) throws ClassNotFoundException {
		super(problem);
		cacheObject = null;
	}

	public GenericSolution(MOSAGenericCoverageProblem problem, Variable [] variables){
		super(problem, variables);
		cacheObject = null;
	}

	public GenericSolution(GenericSolution solution) {
		super(solution);
		cacheObject = solution.cacheObject;
		executionData = solution.executionData;
	}

	public Object getCacheObject() {
		return cacheObject;
	}
	public boolean hasExecutionData() {
	    return executionData != null;
	}
	public void update(Object cacheObject) {
		this.cacheObject = cacheObject;
		this.executionData = cacheObject;
		setAllFitnessValuesFromCache();
		this.cacheObject = null;
	}

	private void setAllFitnessValuesFromCache() {
		for (GenericObjective objective : objectives()) {
			super.setObjective(objective.getObjectiveID(), objective.getFitness(this));
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

	private List<GenericObjective> objectives() {
		Problem problem = this.getProblem();
		if (problem instanceof MOSAGenericCoverageProblem) {
			return ((MOSAGenericCoverageProblem) problem).getTargetObjectives();
		} else {
			throw new IllegalStateException("GenericSolution is not associated with a MOSAGenericCoverageProblem");
		}
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


	/**
	 * Evaluates any objective (even one not in this solution's problem) against the
	 * execution data of the last evaluation. Does not touch the objective vector.
	 */
	public double evaluateObjective(GenericObjective objective) {
	    if (executionData == null) {
	        throw new IllegalStateException("Solution has not been evaluated yet");
	    }
	    Object saved = this.cacheObject;
	    this.cacheObject = executionData;
	    try {
	        return objective.getFitness(this);
	    } finally {
	        this.cacheObject = saved;
	    }
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
