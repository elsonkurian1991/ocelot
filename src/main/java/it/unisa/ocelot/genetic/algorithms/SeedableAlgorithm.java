package it.unisa.ocelot.genetic.algorithms;

import jmetal.core.SolutionSet;

public interface SeedableAlgorithm<E> {
	public Class<E> getTargetClass();
	public void seedStartingPopulation(SolutionSet pSeed, int pKeepNumber); //pSeed can be null: in such case, it is ignored, as if it were nonnull and empty
	public SolutionSet getLastPopulation();
}
