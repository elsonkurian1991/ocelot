package it.unisa.ocelot.genetic;

import java.util.Set;

import jmetal.core.Solution;

public interface SerendipitousAlgorithm<E> {
	public Class<E> getTargetClass();
	public void setSerendipitousPotentials(Set<E> pSerendipitousPotentials);
	public Set<Solution> getSerendipitousSolutions();
}
