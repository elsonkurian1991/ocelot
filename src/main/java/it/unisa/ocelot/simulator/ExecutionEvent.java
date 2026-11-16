package it.unisa.ocelot.simulator;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;

/**
 * An event arising during execution
 * of a C program.
 */
public abstract class ExecutionEvent {
	private final int id;

	//pEdge == null signifies that this event
	//is not yet associated to any LabeledEdge.
	private LabeledEdge edge;

	protected ExecutionEvent(int pId) {
		this.id = pId;
		this.edge = null; //no associated edge initially
	}
	
	public final int getId() {
		return this.id;
	}
	
	/**
	 * Gets the {@link LabeledEdge}
	 * associated to this event.
	 * 
	 * @return a {@link LabeledEdge}, 
	 *         or {@code null} if this
	 *         event is not (yet) associated
	 *         to any edge.
	 */
	public final LabeledEdge getEdge() {
		return this.edge;
	}
	
	/**
	 * Associates this event
	 * to a {@link LabeledEdge}.
	 * Can be done only once.
	 * 
	 * @param pEdge a {@link LabeledEdge}.
	 */
	public final void setEdge(LabeledEdge pEdge) {
		if (this.edge == null) {
			this.edge = pEdge;
		} else {
			throw new RuntimeException("Attempted to change the edge associated to an event, but the event is already associated to an edge.");
		}
	}
}
