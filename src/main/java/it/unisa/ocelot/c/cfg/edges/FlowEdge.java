package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.simulator.ExecutionEvent;


/**
 * A {@link LabeledEdge} representing an unconditional
 * flow between two C statements. It is unlabeled.
 * 
 * @author simone
 */
public class FlowEdge extends LabeledEdge {
	private static final long serialVersionUID = -6097816767281519267L;
	
	private static final int FLOW_EDGE_ID = 2;
	
	public FlowEdge() {
		super(FLOW_EDGE_ID);
	}

	@Override
	public boolean isBranch() {
		return false;
	}
	
	@Override
	public boolean needsEvent() {
		return false;
	}

	@Override
	public boolean matchesEvent(ExecutionEvent pEvent) {
		throw new RuntimeException("Should never ask whether a flow edge matches an event");
	}
	
	@Override
	public String toString() {
		return "FlowEdge";
	}
}
