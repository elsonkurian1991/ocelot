package it.unisa.ocelot.c.cfg.nodes;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.CaseEdge;
import it.unisa.ocelot.c.cfg.edges.FalseEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.edges.TrueEdge;

import java.util.ArrayList;
import java.util.List;

//TODO how is this class supposed to work????
@Deprecated
public final class CFGNodeNavigator {
	private CFG cfg;
	private CFGNode currentNode;
	private List<LabeledEdge> edges;
	
	@Deprecated
	public CFGNodeNavigator(CFG pCFG, CFGNode pNode) {
		this.cfg = pCFG;
		this.currentNode = pNode;
		this.edges = new ArrayList<>();
	}
	
	@Deprecated
	public CFGNodeNavigator goFlow() {
		LabeledEdge flowEdge = null;
		for (LabeledEdge edge : this.cfg.outgoingEdgesOf(this.currentNode)) {
			flowEdge = edge;
		}
		
		if (flowEdge != null) {
			this.currentNode = this.cfg.getEdgeTarget(flowEdge);
			this.edges.add(flowEdge);
		}
		
		return this;
	}
	
	@Deprecated
	public CFGNodeNavigator goTrue() {
		for (LabeledEdge edge : this.cfg.outgoingEdgesOf(this.currentNode)) {
			if (edge instanceof TrueEdge) {
				this.currentNode = this.cfg.getEdgeTarget(edge);
				this.edges.add(edge);
			}
		}
		
		return this;
	}
	
	@Deprecated
	public CFGNodeNavigator goFalse() {
		for (LabeledEdge edge : this.cfg.outgoingEdgesOf(this.currentNode)) {
			if (edge instanceof FalseEdge) {
				this.currentNode = this.cfg.getEdgeTarget(edge);
				this.edges.add(edge);
			}
		}
		
		return this;
	}
	
	@Deprecated
	public CFGNodeNavigator goCase(String pLabel) {
		for (LabeledEdge edge : this.cfg.outgoingEdgesOf(this.currentNode)) {
			if (edge instanceof CaseEdge) {
				CaseEdge caseEdge = (CaseEdge) edge;
				if (caseEdge.getLabel().toString().equals(pLabel)) {
					this.currentNode = this.cfg.getEdgeTarget(edge);
					this.edges.add(caseEdge);
				}
			}
		}
		
		return this;
	}
	
	/**
	 * Returns the node navigated
	 * @return
	 */
	@Deprecated
	public CFGNode node() {
		return this.currentNode;
	}
	
	@Deprecated
	public LabeledEdge edge() {
		return this.edges.get(this.edges.size() - 1);
	}
	
	/**
	 * Returns the path navigated
	 * @return
	 */
	@Deprecated
	public List<LabeledEdge> path() {
		return this.edges;
	}

	@Deprecated
	@Override
	public String toString() {
		return "CFGNodeNavigator [currentNode=" + currentNode + ", edges="
				+ edges + "]";
	}
}
