package it.unisa.ocelot.c.edge_graph;

import java.util.HashMap;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

/* TODO DominatorsEdgeGraph and this class 
 * have confusing interfaces.
 * Rework the interfaces so that EdgeGraph is a
 * ListenableDirectedGraph<E, DefaultEdge> whenever
 * the underlying graph is a DirectedGraph<V, E>.
 * Possibly also reconsider the interface of
 * DominatorsEdgeGraph.
 */
/**
 * Generalized De Bruijn graph (directed line graph).
 * This graph is  "built upon" another directed graph,
 * that by convenience we will call the <em>underlying</em> 
 * graph. The vertices of this graph are the edges 
 * of the underlying graph, plus two vertices that
 * do not correspond to any edge of the underlying
 * graph and that act as entry and exit points. 
 * The edges of this graph are in one-to-one 
 * correspondence with all the pairs of consecutive 
 * edges in the underlying graph. This graph is 
 * listenable.<br />
 * 
 * Note: an {@code EdgeGraph<V, E>} is <em>not</em>
 * a graph with {@code V} as vertices and {@code E}
 * as edges! Actually, {@code V} and {@code E} are
 * respectively the vertices and edges of the 
 * <em>underlying</em> graph.
 * If you check this class declaration you will 
 * notice that an {@code EdgeGraph} is a
 * {@link ListenableDirectedGraph}{@code <}{@link Wrapper}{@code <E>, }{@link DefaultEdge}{@code >}.
 * The important point to notice is that the vertices 
 * of this graph will have type {@link Wrapper}{@code <E>},
 * i.e., they <em>wrap</em> edges (with type {@code E}) 
 * of the underlying graph. This reflects an implementation 
 * need: we must wrap the edges of the underlying graph 
 * for them to be correctly used in 
 * this graph. Consider also that this graph has two 
 * additional vertices (entry and exit) that are not 
 * associated to any edge of the underlying graph. 
 * They will be represented by suitable {@link Wrapper}s 
 * that do not wrap anything.<br />
 * 
 * If you want to get the underlying graph's edge 
 * corresponding to a vertex of this graph, just unwrap
 * it (unwrapping the entry and exit vertices will
 * return {@code null}). On converse, if you have 
 * an edge of the underlying graph and you need the 
 * corresponding vertex in this graph, use the 
 * {@link #getVertexWrapping(Object) getVertexWrapping}
 * method.
 * 
 * @param <V> The type of the vertices of the 
 *            <em>underlying</em> graph (<code>not</code>
 *            of this graph).
 * @param <E> The type of the edges of the 
 *            <em>underlying</em> graph (<code>not</code>
 *            of this graph).
 */
public class EdgeGraph<V, E> extends ListenableDirectedGraph<Wrapper<E>, DefaultEdge> {
	private static final long serialVersionUID = 1483545535478173500L;
	
	private final Wrapper<E> entry;
	private final Wrapper<E> exit;
	private final HashMap<E, Wrapper<E>> vertexOf;

	/**
	 * Constructor.
	 * 
	 * @param pGraph a {@link DirectedGraph}{@code <V, E>}. 
	 *        This is the underlying graph from which this 
	 *        graph will be derived. After construction
	 *        the modifications to {@code pGraph} will
	 *        <em>not</em> be reflected into this graph,
	 *        that will be completely independent on
	 *        {@code pGraph}, except the fact that unwrapping
	 *        a vertex of this graph returns an edge of the
	 *        {@code pGraph}, so if such edges are mutable
	 *        the effect of mutation will be visible after
	 *        unwrapping.
	 * @param pBegin the vertex of {@code pGraph} 
	 *        from which the construction of this edge 
	 *        graph will begin. It must be an initial 
	 *        vertex (i.e., no incoming edges).
	 * @param pEnd the vertex of {@code pGraph} where 
	 *        the construction of this edge graph will 
	 *        end. It must be a final vertex (i.e., 
	 *        no outgoing edges).
	 */
	public EdgeGraph(DirectedGraph<V, E> pGraph, V pBegin, V pEnd) {
		super(DefaultEdge.class);
		if (!pGraph.containsVertex(pBegin) || !pGraph.containsVertex(pEnd)) {
			throw new RuntimeException("pBegin (or pEnd) is not a vertex of pGraph");
		}
		if (!pGraph.incomingEdgesOf(pBegin).isEmpty()) {
			throw new RuntimeException("pBegin has incoming edges in pGraph");
		}
		if (!pGraph.outgoingEdgesOf(pEnd).isEmpty()) {
			throw new RuntimeException("pEnd has outgoing edges in pGraph");
		}
		
		this.entry = new Wrapper<>();
		addVertex(this.entry);
		this.exit = new Wrapper<>();
		addVertex(this.exit);
		this.vertexOf = generateVerticesIntermediate(pGraph);
		generateEdges(pGraph, pBegin, pEnd);
		//TODO is there a way to freeze this graph, at this point, simpler than overriding all its mutators?
	}
	
	//construction of this graph
	
	/**
	 * Generates all the vertices of 
	 * this graph other than this.entry 
	 * and this.exit.
	 * 
	 * @param pGraph the underlying, directed graph.
	 * @return a convenience {@link HashMap}{@code <E, }{@link Wrapper}{@code <E>>}
	 *         that maps any edge in {@code pGraph}
	 *         to the node in this graph that wraps 
	 *         the edge.
	 */
	private HashMap<E, Wrapper<E>> generateVerticesIntermediate(DirectedGraph<V, E> pGraph) {
		//Creates all the other vertices. A vertex wraps 
		//one edge in pGraph. Vertices in this graph are
		//in exact one-to-one association with the edges 
		//in pGraph. The hash map vertexOf associates
		//an edge e of pGraph with the vertex vertexOf(e) 
		//that wraps it. vertexOf is returned to be 
		//cached.
		HashMap<E, Wrapper<E>> vertexOf = new HashMap<>();
		for (E edge : pGraph.edgeSet()) {
			Wrapper<E> vertex = new Wrapper<E>(edge);
			addVertex(vertex);
			vertexOf.put(edge, vertex);
		}
		return vertexOf;
	}

	/**
	 * Generates all the edges of this graph.
	 * 
	 * @param pGraph the underlying, directed graph.
	 * @param pBegin the vertex of {@code pGraph} 
	 *        from which this construction will begin.
	 * @param pEnd the vertex of {@code pGraph} 
	 *        where this construction will end.
	 */
	private void generateEdges(DirectedGraph<V, E> pGraph, V pBegin, V pEnd) {
		//Creates the outgoing edges of this.entry
		for (E edge : pGraph.outgoingEdgesOf(pBegin)) {
			addEdge(this.entry, this.vertexOf.get(edge));
		}
		
		//Creates all the intermediate edges;
		//For every pair of consecutive pGraph 
		//edges e1 : A -> B, e2 : B -> C, 
		//creates an edge w : vertex(e1) -> vertex(e2).
		for (E edge : pGraph.edgeSet()) {
			V targetNode = pGraph.getEdgeTarget(edge);
			for (E edgeNext : pGraph.outgoingEdgesOf(targetNode)) {
				addEdge(this.vertexOf.get(edge), this.vertexOf.get(edgeNext));
			}
		}
		
		//Creates the incoming edges of this.end
		for (E edge : pGraph.incomingEdgesOf(pEnd)) {
			addEdge(vertexOf.get(edge), this.exit);
		}
	}
	
	//public getters

	/**
	 * Gets the entry vertex of this
	 * graph.
	 * 
	 * @return a {@link Wrapper}{@code <E>}
	 *         that does not wrap any edge
	 *         of the underlying graph.
	 */
	public Wrapper<E> getEntry() {
		return this.entry;
	}
	
	/**
	 * Gets the exit vertex of this
	 * graph.
	 * 
	 * @return a {@link Wrapper}{@code <E>}
	 *         that does not wrap any edge
	 *         of the underlying graph.
	 */
	public Wrapper<E> getExit() {
		return this.exit;
	}
	
	/**
	 * Given an edge of the underlying graph,
	 * returns the corresponding vertex in this
	 * graph.
	 * 
	 * @param pEdge an edge of the underlying graph.
	 *        It must not be {@code null} (if you want
	 *        the entry and exit vertices, use
	 *        the {@link #getEntry()} and {@link #getExit()}
	 *        methods).
	 * @return a {@link Wrapper}{@code <E>}, the 
	 *         vertex in this graph that corresponds
	 *         to {@code pEdge}, and wraps it.
	 * @throws RuntimeException if {@code pEdge} is
	 *         {@code null} or is not an edge of the
	 *         underlying graph.
	 */
	public Wrapper<E> getVertexWrapping(E pEdge) {
		if (pEdge == null) {
			throw new NullPointerException("Attempted to invoke EdgeGraph.getVertexWrapping with null parameter");
		}
		if (this.vertexOf.containsKey(pEdge)) {
			//found
			return this.vertexOf.get(pEdge);
		} else {
			//there is no vertex in this EdgeGraph
			//that wraps pEdge! Wrong input?
			throw new RuntimeException("No vertex of this graph wraps pEdge. Possibly pEdge is not an edge in the graph underlying this EdgeGraph");
		}
	}
}
