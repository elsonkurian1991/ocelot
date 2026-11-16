package it.unisa.ocelot.genetic;

import java.util.Set;

import jmetal.core.Solution;

public interface SerendipitousProblem<E> {
	public Class<E> getTargetClass();
	public void setSerendipitousPotentials(Set<E> pSerendipitousPotentials);
	public Set<E> getSerendipitousCovered();
}
