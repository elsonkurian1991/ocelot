package it.unisa.ocelot.c.cdg;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.simulator.ExecutionEvent;

public class MyLabeledEdge extends LabeledEdge {

	public MyLabeledEdge() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean matchesExecution(ExecutionEvent pEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean needsEvent() {
		// TODO Auto-generated method stub
		return false;
	}

}
