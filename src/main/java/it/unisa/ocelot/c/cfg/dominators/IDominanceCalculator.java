package it.unisa.ocelot.c.cfg.dominators;

import java.util.Set;

/**
 * Interface for a generic calculator of graph domination
 * information (vertex or edge), for a given directed graph. 
 * 
 * @param <X> Type of graph vertices (for vertex domination)
 *            or edges (for edge domination).
 */
public interface IDominanceCalculator<X> {
	/**
	 * Check whether a graph element dominates another element
	 * of the same kind.
	 * 
	 * @param dominator a vertex or an edge of the graph.
	 * @param dominated a vertex or an edge of the graph.
	 * @return {@code true} iff {@code dominator} dominates {@code dominated}.
	 */
	public boolean dominates(X dominator, X dominated);
	
	/**
	 * Returns the elements in the graph that strictly 
	 * dominate another element of the same kind.
	 * 
	 * @param element a vertex or an edge of the graph.
	 * @return a {@link Set}{@code <X>} containing all the 
	 *         strict dominators of {@code element}.
	 */
	public Set<X> getStrictDominators(X element);
		
	/**
	 * Returns the elements of the graph that do not 
	 * dominate another element of the same kind.
	 * 
	 * @return a {@link Set}{@code <X>} containing all 
	 *         the graph vertices or edges that do not 
	 *         dominate any other vertex or edge of 
	 *         the graph.
	 */
	public Set<X> getNonDominators();
}
