package it.unisa.ocelot.c.cfg.dominators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * Computes vertex dominance information of a 
 * {@link DirectedGraph} according to
 * "A Simple, Fast Dominance Algorithm" Cooper et. al.
 * The graph, its entry vertex, and possibly the order
 * of nodes visit are passed through the constructor
 * and cannot be changed: If you want to calculate 
 * domination information for another different graph, 
 * create another instance of this class. <br />
 * 
 * A remark: This class implements 
 * {@link IDominanceCalculator}{@code <V>}, where {@code V}
 * is the type of the <em>vertices</em> of the graph
 * for which we want to calculate the dominance
 * information. So expect the related use of 
 * {@code V} as a type in the methods it exports.
 *  
 * @author giograno
 *
 * @param <V> Type of graph vertices.
 * @param <E> Type of graph edges.
 */
public class DominanceCalculatorVertices<V, E> implements IDominanceCalculator<V> {
	/**
	 * The graph to analyze.
	 */
	private final DirectedGraph<V, E> graph;
	
	/**
	 * The result of a depth-first, preorder 
	 * visit of {@link #graph}'s vertices.
	 */
	private final List<V> vertexPreOrderVisit;
	
	/**
	 * Inverts {@link #vertexPreOrderVisit}, considered as a mapping 
	 * with type {@link Integer} -> {@code V}, yielding a corresponding mapping 
	 * with type {@code V} -> {@link Integer}. It is just for convenience.
	 */
	private final HashMap<V, Integer> vertexPreOrderVisitInverted;
	
	/**
	 * Maps a vertex of {@link #graph} to its immediate dominator.
	 * It needs not to be total, in which case, if {@code v} is a
	 * vertex of {@link #graph} without an immediate dominator,
	 * {@link #immediateDominatorOf immediateDominatorOf.}{@link HashMap#get(Object) get(v)}
	 * will return {@code null}.
	 */
	private final HashMap<V, V> immediateDominatorOf;
	
	/**
	 * The dominator tree of {@link #graph}.
	 */
	private final SimpleDirectedGraph<V, DefaultEdge> dominatorTree;
	
	/**
	 * A {@link HashSet} containing all (and only) the
	 * vertices of {@link #graph} that are not dominators
	 * of another vertex.
	 */
	private final HashSet<V> nonDominators;

	/**
	 * Constructor (default visit order).
	 * 
	 * @param pGraph a {@link DirectedGraph}{@code <V, E>}.
	 * @param pEntry the entry vertex of {@code pGraph}. The
	 *        dominators of {@code pGraph} will be calculated
	 *        along a default depth-first, preorder visit of 
	 *        the vertices of {@code pGraph} starting from 
	 *        {@code pEntry}.
	 */
	public DominanceCalculatorVertices(DirectedGraph<V, E> pGraph, V pEntry) {
		this(pGraph, dfsPreOrder(pGraph, pEntry));
	}

	/**
	 * Constructor (user-specified visit order).
	 * 
	 * @param pGraph a {@link DirectedGraph}{@code <V, E>}.
	 * @param pVisit the result of a pre-order visit of  
	 *        {@code pGraph} vertices, passed as a 
	 *        {@link List}{@code <V>} whose first element 
	 *        is the assumed entry vertex of {@code pGraph}, 
	 *        and then all the other vertices in {@code pGraph} 
	 *        follow in the total order produced by the visit.
	 */
	public DominanceCalculatorVertices(DirectedGraph<V, E> pGraph, List<V> pVisit) {
		if (!pGraph.incomingEdgesOf(pVisit.get(0)).isEmpty()) {
			throw new RuntimeException("The entry vertex of a graph is not allowed to have incoming edges: failed");
		}
		this.graph = pGraph;
		this.vertexPreOrderVisit = pVisit; //no safety copy: we just read pVisit
		this.vertexPreOrderVisitInverted = new HashMap<>();
		for (int i = 0; i < this.vertexPreOrderVisit.size(); i++) {
			this.vertexPreOrderVisitInverted.put(this.vertexPreOrderVisit.get(i), i);
		}
		
		//computes all dominator info eagerly
		this.immediateDominatorOf = computeImmediateDominators();
		this.dominatorTree = computeDominatorTree();
		this.nonDominators = computeNonDominators();
	}

	@Override
	public boolean dominates(V dominator, V dominated) {
		if (dominator.equals(dominated)) {
			return true; //(non-strict) domination is reflexive
		}
		//scans through the immediate dominators chain, starting 
		//from dominated, as long as the visited 
		//vertex is >= dominator
		V v = this.immediateDominatorOf.get(dominated);
		while (v != null && getOrder(v) >= getOrder(dominator) && !v.equals(dominator)) {
			v = this.immediateDominatorOf.get(v);
		}
		return dominator.equals(v); //did the above search end in dominator?
	}

	@Override
	public Set<V> getStrictDominators(V vertex) {
		Set<V> result = new HashSet<>();
		
		result.add(vertex); //TODO who added this statement? This way we return the *nonstrict* dominators! Clarify.
		
		//scans
		V dominated = vertex;
		V immediateDominator = this.immediateDominatorOf.get(vertex);
		while (immediateDominator != dominated) {
			result.add(immediateDominator);
			dominated = immediateDominator;
			immediateDominator = this.immediateDominatorOf.get(dominated);
		} 
		
		return result;
	}

	@Override
	public Set<V> getNonDominators() {
		return new HashSet<>(this.nonDominators); //safety (shallow) copy
	}

	/**
	 * Gets the complete dominator tree of the
	 * graph.
	 * 
	 * @return the dominator tree of the graph as a 
	 *         {@link DirectedGraph}{@code <V, ? extends }{@link DefaultEdge}{@code >}.
	 */
	@SuppressWarnings("unchecked")
	public DirectedGraph<V, DefaultEdge> getDominatorTree() {
		return (DirectedGraph<V, DefaultEdge>) this.dominatorTree.clone(); //safety (shallow) copy
		//sorry, DirectedGraph does not have a copy constructor:
		//we must use clone (ugly but it works).
	}
	
	/**
	 * A default, depth-first, preorder visit of a
	 * graph, used when no custom visit is specified
	 * through the constructor. Basically, it uses
	 * the standard {@link DepthFirstIterator} provided
	 * by jgrapht to scan the graph.
	 * 
	 * @param <V> Vertices of the graph.
	 * @param <E> Edges of the graph.
	 * @param pGraph a {@link DirectedGraph}{@code <V, E>}.
	 *        This is the graph that will be visited.
	 * @param pEntry the entry vertex of {@code pGraph}. 
	 *        The visit will start from it.
	 * @return a {@link List}{@code <V>} whose first element
	 *         is {@code pEntry}, and then all the other 
	 *         vertices of {@code pGraph} follow in the
	 *         total order produced by this visit algorithm.
	 */
	private static <V, E> List<V> dfsPreOrder(DirectedGraph<V, E> pGraph, V pEntry) {
		DepthFirstIterator<V, E> dfsIterator = new DepthFirstIterator<>(pGraph, pEntry);
		dfsIterator.setCrossComponentTraversal(false); //do not traverse pGraph across connected components
		ArrayList<V> result = new ArrayList<>();
		dfsIterator.forEachRemaining(result::add); //traverses pGraph and accumulates in result
		return result;
	}

	/**
	 * Computes the immediate dominators map of 
	 * {@link #graph this.graph}.
	 * 
	 * @return a {@link HashMap}{@code <V, V>}
	 *         mapping vertices of {@link #graph this.graph} 
	 *         to their respective immediate dominators.
	 *         If a vertex {@code v} has no immediate dominators,
	 *         {@code v} will be mapped to {@code null}.
	 */
	private HashMap<V, V> computeImmediateDominators() {
		HashMap<V, V> result = new HashMap<>();
		V entryVertex = this.vertexPreOrderVisit.getFirst();
		result.put(entryVertex, entryVertex);

		boolean changed;
		do {
			changed = false;
			for (V v : this.vertexPreOrderVisit) {
				if (v.equals(entryVertex)) {
					continue; //already done
				}

				V oldIdom = result.get(v);
				V newIdom = null;
				for (E edge : this.graph.incomingEdgesOf(v)) {
					V pre = this.graph.getEdgeSource(edge);
					if (result.get(pre) == null) { //not yet analyzed
						continue;
					}
					if (newIdom == null) {
						//If we only have one (defined) predecessor pre,
						//then pre is dominator of v 
						newIdom = pre;
					} else {
						//Compute the intersection of all defined predecessors
						//of v
						newIdom = intersectIDoms(result, pre, newIdom);
					}
				}
				if (newIdom == null) {
					throw new RuntimeException("Dominator calculation did not find a dominator for (non-entry) vertex " + v +". Are you sure your graph is connected?");
				}
				if (!newIdom.equals(oldIdom)) {
					changed = true;
					result.put(v, newIdom);
				}
			}
		} while (changed);

		return result;
	}
	
	private SimpleDirectedGraph<V, DefaultEdge> computeDominatorTree() {
		SimpleDirectedGraph<V, DefaultEdge> retVal = new SimpleDirectedGraph<>(DefaultEdge.class);
		for (V v : this.graph.vertexSet()) {
			retVal.addVertex(v);
			V immediateDominator = this.immediateDominatorOf.get(v);
			if (immediateDominator != null && !v.equals(immediateDominator)) {
				retVal.addVertex(immediateDominator);
				retVal.addEdge(immediateDominator, v);
			}
		}
		return retVal;
	}
	
	private HashSet<V> computeNonDominators() {
		HashSet<V> retVal = new HashSet<>();
		for (V v : this.dominatorTree.vertexSet()) {
			if (this.dominatorTree.outgoingEdgesOf(v).size() == 0) {
				retVal.add(v);
			}
		}
		return retVal;
	}

	private V intersectIDoms(HashMap<V, V> iDom, V v1, V v2) {
		while (v1 != v2) {
			if (getOrder(v1) < getOrder(v2)) {
				v2 = iDom.get(v2);
			} else {
				v1 = iDom.get(v1);
			}
		}
		return v1;
	}

	private int getOrder(V vertex) {
		return this.vertexPreOrderVisitInverted.get(vertex);
	}
	
	//stuff we do not use
	
	/**
	 * Gets the table of immediate dominators. Note that by convention, the
	 * graph's entry is dominated by itself (so <code>IDOM(v)</code> is a total
	 * function).</br> Note that the set <code>DOM(v)</code> of the dominators of
	 * vertex v is given by
	 * <ul>
	 * <li/><code>DOM(Entry) = Entry</code>
	 * <li/><code>DOM(v) = n \cup DOM(IDOM(v))</code>
	 * </ul>
	 * 
	 * @return the immediate dominators table.
	 */
	/*public HashMap<V, V> getIDoms() {
		return this.idom;
	}*/
	
	/**
	 * Returns the set of vertices that are dominated by an edge.
	 * 
	 * @param edge the edge for which we want to find the dominated nodes.
	 * @return the {@link Set}{@code V} set of nodes dominated by {@code edge}
	 */
	/*public Set<V> getDominatedNodes(E edge) {
		Set<V> dominatedNodes = new HashSet<>();
		V dominator = this.graph.getEdgeTarget(edge);

		DepthFirstIterator<V, DefaultEdge> depthFirstIterator = new DepthFirstIterator<>(
				this.dominatorTree, dominator);

		while (depthFirstIterator.hasNext()) {
			V v = depthFirstIterator.next();			
			if (v != dominator) { //TODO not equals?
				dominatedNodes.add(v);
			}
		}

		return dominatedNodes;
	}*/
}