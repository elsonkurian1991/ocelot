package it.unisa.ocelot.c.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jgrapht.alg.CycleDetector;

import it.unisa.ocelot.c.cfg.edges.EdgeComparator;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Calculate all linearly independent McCabe paths for a given control flow
 * graph.
 * 
 * @author giograno
 *
 */
public final class McCabeCalculator {
	private final CFG cfg;
	private CFGNode nodeStart;
	private final HashSet<CFGNode> nodesVisited;
	private final CycleDetector<CFGNode, LabeledEdge> cycleGraph;
	// paths node by node
	private final List<List<CFGNode>> mcCabePathsByNode;
	// paths edge by edge
	private final List<List<LabeledEdge>> mcCabePaths;
	// paths vector
	private final List<RealVector> mcCabePathsVector;

	/**
	 * Constructor.
	 * 
	 * @param pCfg a {@link CFG}.
	 */
	public McCabeCalculator(CFG pCfg) {
		this.cfg = pCfg;
		this.nodeStart = this.cfg.getEntry();
		this.nodesVisited = new HashSet<>();
		this.cycleGraph = new CycleDetector<>(this.cfg);
		this.mcCabePathsByNode = new ArrayList<>();
		this.mcCabePaths = new ArrayList<>();
		this.mcCabePathsVector = new ArrayList<>();
		calculateMcCabePaths();
	}

	/**
	 * Performs calculation of McCabe linearly independent paths.
	 */
	private void calculateMcCabePaths() {
		//calculates this.mcCabePathsByNode
		findBasis(this.nodeStart, new ArrayList<>());
		
		//calculates this.mcCabePaths
		for (List<CFGNode> nodePath : this.mcCabePathsByNode) {
			ArrayList<LabeledEdge> currentPath = new ArrayList<>();
			for (int i = 0; i < nodePath.size() - 1; i++) {
				CFGNode startNode = nodePath.get(i);
				CFGNode endNode = nodePath.get(i + 1);
				currentPath.add(this.cfg.getEdge(startNode, endNode));
			}
			this.mcCabePaths.add(currentPath);
		}
		List<List<LabeledEdge>> paths = getMcCabePaths();
		List<LabeledEdge> orderedEdges = new ArrayList<>(this.cfg.edgeSet());
		Collections.sort(orderedEdges, new EdgeComparator(this.cfg));

		//calculates this.mcCabePathsVector
		for (List<LabeledEdge> path : paths) {
			double[] vector = new double[orderedEdges.size()];
			for (int i = 0; i < orderedEdges.size(); i++) {
				LabeledEdge edge = orderedEdges.get(i);
				vector[i] = Collections.frequency(path, edge);
			}
			this.mcCabePathsVector.add(new ArrayRealVector(vector));
		}
	}

	/**
	 * Traverse recursively the control flow graph calculating all basis path.
	 * 
	 * @param node a {@link CFGNode}.
	 * @param path an {@link ArrayList}{@code <}{@link CFGNode}{@code >}
	 */
	private void findBasis(CFGNode node, ArrayList<CFGNode> path) {
		// add current node to McCabe path
		path.add(node);

		CFGNode defaultDestination = getDefaultDestination(node);
		HashSet<LabeledEdge> outgoingEdges = new HashSet<>(this.cfg.outgoingEdgesOf(node)); //TODO is this safety copy necessary?

		if (outgoingEdges.isEmpty()) {
			// return path as a solution
			this.mcCabePathsByNode.add(path);
		} else if (this.nodesVisited.contains(node)) {
			findBasis(defaultDestination, new ArrayList<>(path));
		} else { //(node not yet visited)
			this.nodesVisited.add(node);
			findBasis(defaultDestination, new ArrayList<>(path));

			// removing edge from edges to explore
			outgoingEdges.remove(this.cfg.getEdge(node, defaultDestination));

			for (LabeledEdge edge : outgoingEdges) {
				findBasis(this.cfg.getEdgeTarget(edge), new ArrayList<>(path));
			}
		}
	}

	/**
	 * Calculates the vertex targeting by the default edge. Default edge is
	 * any edge that is not a back edge or which later causes a node to have two
	 * incoming edges˙
	 * 
	 * @param node
	 *            node to compute
	 * @return the default edge for given node
	 */
	private CFGNode getDefaultDestination(CFGNode node) {
		Set<LabeledEdge> currentOutgoingEdges = this.cfg.outgoingEdgesOf(node);
		Set<CFGNode> vertexInCycle = this.cycleGraph.findCyclesContainingVertex(node);
		Set<CFGNode> neighborVertex = new HashSet<>();

		for (LabeledEdge labeledEdge : currentOutgoingEdges) {
			CFGNode targetEdge = this.cfg.getEdgeTarget(labeledEdge);
			neighborVertex.add(targetEdge);
		}

		for (CFGNode cfgNode : neighborVertex) {
			if (!vertexInCycle.contains(cfgNode)) {
				return cfgNode;
			}
		}

		ArrayList<CFGNode> neighborList = new ArrayList<>(neighborVertex);
		if (neighborList.isEmpty()) {
			return null;
		}
		return neighborList.get(0);
	}

	/**
	 * Returns all McCabe linearly independent paths (lists of nodes).
	 * 
	 * @return a list of McCabe paths
	 */
	public List<List<CFGNode>> getMcCabePathsByNodes() {
		return this.mcCabePathsByNode;
	}

	/**
	 * Returns all McCabe linearly independent paths (lists of edges).
	 * 
	 * @return a list of McCabe paths
	 */
	public List<List<LabeledEdge>> getMcCabePaths() {
		return this.mcCabePaths;
	}


	public List<RealVector> getMcCabeVectorPaths() {
		return this.mcCabePathsVector;
	}
}
