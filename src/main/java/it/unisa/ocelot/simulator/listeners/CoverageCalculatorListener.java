package it.unisa.ocelot.simulator.listeners;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CoverageCalculatorListener implements SimulatorListener {
	private final CFG cfg;
	private final HashSet<LabeledEdge> coveredBranches;
	private final HashSet<CFGNode> coveredNodes;
	private final ArrayList<LabeledEdge> coveredPath;

	public CoverageCalculatorListener(CFG pCFG) {
		this.cfg = pCFG;
		this.coveredBranches = new HashSet<>();
		this.coveredNodes = new HashSet<>();
		this.coveredPath = new ArrayList<>();
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		this.coveredPath.add(pEdge);
		if (pEdge.isBranch()) {
			this.coveredBranches.add(pEdge);
		}
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		onEdgeVisit(pEdge);
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		onEdgeVisit(pEdge);
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		this.coveredNodes.add(pNode);
	}
	
	public Set<LabeledEdge> getCoveredBranches() {
		return new HashSet<>(this.coveredBranches);
	}

	public Set<CFGNode> getCoveredBlocks() {
		return new HashSet<>(this.coveredNodes);
	}
	
	public Set<LabeledEdge> getCoveredEdges() {
		return new HashSet<>(this.coveredPath);
	}
	
	public List<LabeledEdge> getCoveredPath() {
		return new ArrayList<>(this.coveredPath);
	}

	public double getBranchCoverage() {
		Set<LabeledEdge> branches = new HashSet<>();
		for (LabeledEdge edge : this.cfg.edgeSet()) {
			if (!(edge instanceof FlowEdge)) {
				branches.add(edge);
			}
		}
		
		return getCoveredBranches().size() / (double) branches.size();
	}

	public double getBlockCoverage() {
		Set<CFGNode> nodes = this.cfg.vertexSet();
		return (this.coveredNodes.size() + 1) / (double) nodes.size();
	}
}