package it.unisa.ocelot.c.cfg;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

import org.jgrapht.graph.ListenableDirectedWeightedGraph;

public final class WeightedCFG extends ListenableDirectedWeightedGraph<CFGNode, LabeledEdge> {
	private static final long serialVersionUID = -6271487203259467525L;
	
	private final CFG cfg;
	
	public WeightedCFG(CFG pCfg) {
		super(LabeledEdge.class);
		
		for (CFGNode node : pCfg.vertexSet()) {
			addVertex(node);
		}
		
		for (LabeledEdge edge : pCfg.edgeSet()) {
			addEdge(pCfg.getEdgeSource(edge), pCfg.getEdgeTarget(edge), edge);
			setEdgeWeight(edge, 0);
		}
		
		this.cfg = pCfg;
	}
	
	public CFGNode getEntry() {
		return this.cfg.getEntry();
	}
	
	public CFGNode getExit() {
		return this.cfg.getExit();
	}
}
