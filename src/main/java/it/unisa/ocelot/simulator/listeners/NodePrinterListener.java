package it.unisa.ocelot.simulator.listeners;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;
import it.unisa.ocelot.simulator.SimulatorListener;

import java.util.List;

public final class NodePrinterListener implements SimulatorListener {

	@Override
	public void onEdgeVisit(LabeledEdge pEdge) {
		System.out.println("--> Flow visiting " + pEdge.toString());

	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		System.out.println("--> Condition visiting " + pEdge.toString() + 
				"\n\tDistanceT:" + pEvent.getDistanceThen() +
				"\n\tDistanceF:" + pEvent.getDistanceElse());
	}

	@Override
	public void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		System.out.println("--> Case choice " + pEdge.toString());
		for (CaseExecutionEvent pCase : pCases) {
			System.out.println("\t" + pCase.getId() + ", distance:" + pCase.getDistance());
		}
	}

	@Override
	public void onNodeVisit(CFGNode pNode) {
		System.out.println("* In node " + pNode.toString());
	}

}
