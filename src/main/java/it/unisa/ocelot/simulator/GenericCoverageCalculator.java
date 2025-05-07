package it.unisa.ocelot.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.genetic.objectives.BranchDistanceCache;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.util.Utils;

public class GenericCoverageCalculator {
	private CFG cfg;
	private List<GenericObjective> objectives;
	private List<GenericObjective> coveredObjectives = new ArrayList<GenericObjective>();
	private double objectiveCoverage = 0;

	public GenericCoverageCalculator(CFG cfg, List<GenericObjective> objectives) {
		this.cfg = cfg;
		this.objectives = objectives;
	}

	public void calculateCoverage(List<Object[][][]> pParametersList) {
		coveredObjectives.clear(); 
		for (Object[][][] params : pParametersList) {
			CBridge bridge = new CBridge();
			EventsHandler h = new EventsHandler();

			bridge.getEvents(h, params[0][0], params[1], params[2][0]);

			Simulator simulator = new Simulator(cfg, h.getEvents());

			simulator.simulate();

			// LUCA: read fitnessValues.txt (branch fitnesses) file and store it. More
			// efficient than reading it for every objective.
			BranchDistanceCache.cacheFitnessValues();
			for (GenericObjective objective : objectives) {
				double fitness = objective.getFitness(params);
				if (fitness == 0)
					coveredObjectives.add(objective);
			}

			if (!simulator.isSimulationCorrect())
				throw new RuntimeException("Simulation error for parameters " + Utils.printParameters(params));
		}
	}

	public void calculateCoverage(Object[][][] pParameters) {
		List<Object[][][]> parametersList = new ArrayList<Object[][][]>();
		parametersList.add(pParameters);
		this.calculateCoverage(parametersList);
	}

	public void calculateCoverage(Set<TestCase> pTestCases) {
		for(TestCase tc : pTestCases) {
			calculateCoverage(tc.getParameters());
		}
		this.objectiveCoverage = ((double) this.coveredObjectives.size()) / this.objectives.size();
	}

	public double getObjectiveCoverage() {
		return this.objectiveCoverage;
	}

	public List<GenericObjective> getCoveredObjectives() {
		return this.coveredObjectives;
	}
	
	public List<GenericObjective> getUncoveredObjectives() {
		List<GenericObjective> uncovered = new ArrayList<GenericObjective>(this.objectives);
		uncovered.removeAll(this.coveredObjectives);
		return uncovered;
	}

}
