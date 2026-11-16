package it.unisa.ocelot.c.cfg.dominators;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import it.unisa.ocelot.c.edge_graph.EdgeGraph;
import it.unisa.ocelot.c.edge_graph.Wrapper;

/**
 * Computes edge dominance information for a 
 * {@link DirectedGraph} according to
 * "A Simple, Fast Dominance Algorithm" Cooper et. al.
 * The graph and its entry/exit vertices are passed 
 * through the constructor
 * and cannot be changed: If you want to calculate 
 * domination information for a different set of inputs, 
 * create another instance of this class. <br />
 * 
 * A remark: Note that this class implements 
 * {@link IDominanceCalculator}{@code <E>}, where {@code E}
 * is the type of the <em>edges</em> of the graph
 * for which we want to calculate the dominance
 * information. So expect the related use of 
 * {@code E} as a type in the methods it exports.
 * 
 * Implementation notes: this class builds the generalized De Bruijn 
 * graph (directed line graph) from the graph for which
 * we want to calculate edge dominance information. 
 * Then, it runs the Cooper et al. node dominance algorithm 
 * on the directed line graph, using a default depth-first 
 * pre-order traversal of the graph.<br />
 * 
 * @author giograno
 *
 * @param <V> Type of graph vertices.
 * @param <E> Type of graph edges.
 */
public class DominanceCalculatorEdges<V, E> implements IDominanceCalculator<E> {
	/**
	 * The generalized De Bruijn graph 
	 * (directed line graph) built upon
	 * the graph to analyze.
	 */
	private final EdgeGraph<V, E> edgeGraph;
	
	/**
	 * The algorithm to calculate vertex dominance
	 * on {@code this.edgeGraph}.
	 */
	private final DominanceCalculatorVertices<Wrapper<E>, DefaultEdge> dominators;

	/**
	 * Constructor.
	 * 
	 * @param pGraph a {@link DirectedGraph}{@code <V, E>}.
	 * @param pEntry the entry vertex of {@code pGraph}.
	 *        This vertex will be used as the begin
	 *        point to build the generalized De Bruijn
	 *        graph. It must be an initial vertex 
	 *        (i.e., it must have no incoming edges).
	 * @param pExit the exit vertex of {@code pGraph}.
	 *        This vertex will be used as the end
	 *        point to build the generalized De Bruijn
	 *        graph. It must be a final vertex 
	 *        (i.e., it must have no outgoing edges).
	 */
	public DominanceCalculatorEdges(DirectedGraph<V, E> pGraph, V pEntry, V pExit) {
		if (!pGraph.incomingEdgesOf(pEntry).isEmpty()) {
			throw new RuntimeException("The entry vertex of a graph is not allowed to have incoming edges: failed");
		}
		if (!pGraph.outgoingEdgesOf(pExit).isEmpty()) {
			throw new RuntimeException("The entry vertex of a graph is not allowed to have incoming edges: failed");
		}
		this.edgeGraph = new EdgeGraph<>(pGraph, pEntry, pExit);
		
		//computes all the dominator information eagerly
		this.dominators = new DominanceCalculatorVertices<>(this.edgeGraph, this.edgeGraph.getEntry());
	}
	
	@Override
	public boolean dominates(E dominator, E dominated) {
		return this.dominators.dominates(
				this.edgeGraph.getVertexWrapping(dominator), 
				this.edgeGraph.getVertexWrapping(dominated));
	}
	
	@Override
	public Set<E> getNonDominators() {
		Set<Wrapper<E>> wrappedNonDominators = this.dominators.getNonDominators();
		Set<E> result = new HashSet<>();
		for (Wrapper<E> wrapper : wrappedNonDominators) {
			result.add(wrapper.getWrapped());
		}
		result.remove(null); //TODO why???
		
		return result;
	}
	
	@Override
	public Set<E> getStrictDominators(E edge) {
		Set<Wrapper<E>> strictDominatorsWrappers = this.dominators.getStrictDominators(this.edgeGraph.getVertexWrapping(edge));
		Set<E> strictDominators = new HashSet<>();
		
		for (Wrapper<E> wrapper : strictDominatorsWrappers) {
			strictDominators.add(wrapper.getWrapped());
		}
		
		strictDominators.remove(null); //TODO why???
		
		return strictDominators;
	}
}