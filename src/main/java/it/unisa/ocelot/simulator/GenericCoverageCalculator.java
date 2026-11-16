package it.unisa.ocelot.simulator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.genetic.objectives.GenericObjective;

import jmetal.core.Solution;

//This class is progressive: its constructor filters out the already covered objectives.
public final class GenericCoverageCalculator extends CoverageCalculator<GenericObjective> {
	private final Set<? extends GenericObjective> objectives;
	private final Set<GenericObjective> coveredObjectives;
	private final Set<? extends GenericObjective> uncoveredObjectives;
	private double coverage;

	/**
	 * Constructor.
	 * 
	 * @param pObjectives all the {@link GenericObjective}s.
	 */
	public GenericCoverageCalculator(Set<? extends GenericObjective> pObjectives) {
		this.objectives = new HashSet<>(pObjectives);
		this.coveredObjectives = pObjectives.stream().filter(GenericObjective::isCovered).collect(Collectors.toSet());
		this.uncoveredObjectives = pObjectives.stream().filter(o -> !o.isCovered()).collect(Collectors.toSet());
		updateCoverage();
	}
	
	@Override
	public void resetCoverage() {
		//this should never be invoked: this class is progressive
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void updateCoverage(TestCase pTestCase) {
		if (pTestCase.hasCoverageInformationObjectives()) {
			//safety update, as possibly this testCase was updated elsewhere
			this.coveredObjectives.addAll(pTestCase.getCoveredObjectives());
			this.uncoveredObjectives.removeAll(pTestCase.getCoveredObjectives());
		} else {
			Solution solution = pTestCase.getSolution();
			HashSet<GenericObjective> coveredObjectivesByTest = new HashSet<>();
			for (Iterator<? extends GenericObjective> itUncovered = this.uncoveredObjectives.iterator();
					itUncovered.hasNext(); ) {
				GenericObjective objective = itUncovered.next();
				if (objective.isCovered(solution)) {
					objective.setCovered();
					//note that the objective might already have been
					//set covered earlier, but this code works anyways
					coveredObjectivesByTest.add(objective);
					this.coveredObjectives.add(objective);
					itUncovered.remove();
				}
			}
			pTestCase.setCoverageObjectives(coveredObjectivesByTest);
		}
		updateCoverage();
	}
	
	private void updateCoverage() {
		this.coverage = ((double) this.coveredObjectives.size()) / this.objectives.size();
	}

	@Override
	public Set<GenericObjective> getUncovered() {
		return new HashSet<>(this.uncoveredObjectives);
	}

	@Override
	public Set<GenericObjective> getCovered() {
		return new HashSet<>(this.coveredObjectives);
	}

	@Override
	public double getCoverage() {
		return this.coverage;
	}
}
