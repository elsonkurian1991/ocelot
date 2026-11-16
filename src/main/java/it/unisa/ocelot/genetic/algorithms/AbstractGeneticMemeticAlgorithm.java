package it.unisa.ocelot.genetic.algorithms;

import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.genetic.OcelotAlgorithm;
import it.unisa.ocelot.genetic.SerendipitousAlgorithm;
import it.unisa.ocelot.genetic.SerendipitousProblem;
import it.unisa.ocelot.genetic.StandardProblem;

import jmetal.core.Solution;
import jmetal.core.SolutionSet;

public abstract class AbstractGeneticMemeticAlgorithm extends OcelotAlgorithm
implements SerendipitousAlgorithm<LabeledEdge>, SeedableAlgorithm<LabeledEdge> {
	private static final long serialVersionUID = -5075576327846538169L;
	
	protected final SerendipitousProblem<LabeledEdge> serendipitousProblem;
	protected final Set<LabeledEdge> serendipitousPotentials;
	protected final Set<Solution> serendipitousSolutions; //to be filled

	protected SolutionSet startingPopulation;
	protected SolutionSet lastPopulation; //to be set

	@SuppressWarnings("unchecked")
	protected AbstractGeneticMemeticAlgorithm(StandardProblem pStandardProblem) {
		super(pStandardProblem);
		this.serendipitousProblem = this.standardProblem.isSerendipitous(LabeledEdge.class) ? (SerendipitousProblem<LabeledEdge>) this.standardProblem : null;
		this.serendipitousPotentials = new HashSet<>();
		this.serendipitousSolutions = new HashSet<>();
		this.startingPopulation = null;
		this.lastPopulation = null;
	}
	
	protected final void prepareSerendipitous() {
		if (this.standardProblem.isSerendipitous(LabeledEdge.class)) {
			this.serendipitousProblem.setSerendipitousPotentials(this.serendipitousPotentials);
		}
	}

	protected final void checkSerendipitous(Solution pSolution) {
		if (this.standardProblem.isSerendipitous(LabeledEdge.class) && 
			this.serendipitousProblem.getSerendipitousCovered().size() > 0) {
			this.serendipitousPotentials.removeAll(this.serendipitousProblem.getSerendipitousCovered());
			this.serendipitousSolutions.add(pSolution);
		}
	}

	@Override
	public final Class<LabeledEdge> getTargetClass() {
		return LabeledEdge.class;
	}

	@Override
	public final void setSerendipitousPotentials(Set<LabeledEdge> pPotentials) {
		this.serendipitousPotentials.clear();
		this.serendipitousPotentials.addAll(pPotentials);
	}

	@Override
	public final Set<Solution> getSerendipitousSolutions() {
		return this.serendipitousSolutions;
	}

	@Override
	public final void seedStartingPopulation(SolutionSet pSeed, int pKeepNumber) {
		this.startingPopulation = pSeed;

		if (this.startingPopulation != null) {
			for (int i = this.startingPopulation.size() - 1; i >= pKeepNumber; i--) {
				this.startingPopulation.remove(i);
			}
		}
	}

	@Override
	public final SolutionSet getLastPopulation() {
		return this.lastPopulation;
	}
}
