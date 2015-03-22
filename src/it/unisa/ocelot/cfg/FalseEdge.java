package it.unisa.ocelot.cfg;

import it.unisa.ocelot.simulator.ExecutionEvent;


/**
 * This class represents an edge with a label. The label could an object of any kind.
 * @author simone
 *
 */
public class FalseEdge extends LabeledEdge {
	private static final long serialVersionUID = -6097816767281519267L;
	
	public FalseEdge() {
		super(false);
	}

	@Override
	public boolean matchesExecution(ExecutionEvent pEvent) {
		return pEvent.choise == 0;
	}

	@Override
	public boolean needsEvent() {
		return true;
	}
}