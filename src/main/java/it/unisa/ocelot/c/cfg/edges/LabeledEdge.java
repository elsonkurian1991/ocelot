package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.simulator.ExecutionEvent;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * This class represents a (possibly) labeled edge of a CFG. 
 * Any object can be a label. Unlabeled edges are represented
 * by a {@code null} label. It is mutable.
 * 
 * @author simone
 */
public abstract class LabeledEdge extends DefaultWeightedEdge {
	private static final long serialVersionUID = -1013876340016911304L;
	
	public static final int NOT_AN_OBJECTIVE = -1;
	
	/**
	 * A numeric identifier for this edge.
	 * It univocally identifies the corresponding
	 * branch in the C source code <em>relatively
	 * to the branching statement</em> it belongs
	 * (i.e., among all the companion branch edges, 
	 * outgoing from this edge's source vertex).
	 */
	private final int id;
	
	/**
	 * The label of this edge, or {@code null}
	 * if the edge is unlabeled.
	 */
	private final Object label;
	
	//set externally
	/**
	 * When this edge is an objective, 
	 * this fields contains the id of
	 * the associated {@link GenericObjective}.
	 * Otherwise, it has as value NOT_AN_OBJECTIVE.
	 */
	private int objectiveId;
	
	/**
	 * Indicates whether this edge was covered
	 * by an execution.
	 */
	private boolean isCovered;
	
	/**
	 * Constructor (unlabeled).
	 */
	protected LabeledEdge(int pId) {
		this.id = pId;
		this.label = null;
		this.isCovered = false;
	}
	
	/**
	 * Constructor (labeled).
	 * 
	 * @param pLabel the label. It can be 
	 *        any {@link Object} (perhaps better
	 *        if immutable).
	 */
	protected LabeledEdge(int pId, Object pLabel) {
		this.id = pId;
		this.label = pLabel;
		this.isCovered = false;
	}
	
	/**
	 * Returns the id number 
	 * of this edge.
	 *
	 * @return an {@code int}, the id.
	 */
	public int getId() {
		return this.id; 
	}
	
	/**
	 * Returns the label of this edge.
	 * 
	 * @return the {@link Object} used
	 *         to label this edge, or 
	 *         {@code null} if this
	 *         edge is unlabeled.
	 */
	public Object getLabel() {
		return this.label;
	}
	
	/**
	 * Returns the objective id number 
	 * of this edge.
	 * 
	 * @return the progressive {@code int} 
	 *         objective id number
	 *         of this object.
	 */
	public int getObjectiveId() {
		return this.objectiveId;
	}

	/**
	 * Associates this {@link LabeledEdge}
	 * to an objective.
	 * 
	 * @param a {@code int} id number
	 *        identifying the associated 
	 *        objective.
	 */
	public void setObjectiveId(int pObjectiveId) {
		this.objectiveId = pObjectiveId;
	}
	
	/**
	 * Checks whether this edge
	 * has been covered.
	 * 
	 * @return {@code true} iff it was covered.
	 */
	public boolean isCovered() {
		return this.isCovered;
	}

	/**
	 * Sets this edge to covered state.
	 */
	public void setCovered() {
		this.isCovered = true;
	}
	
	/**
	 * Sets this edge to uncovered state.
	 */
	public void setUncovered() {
		this.isCovered = false;
	}
	
	public abstract boolean isBranch();
	
	/**
	 * Returns true if this branch needs an event to be triggered 
	 * in order to execute.
	 * 
	 * @return a {@code boolean}.
	 */
	public abstract boolean needsEvent();
	
	/**
	 * Returns true if an event is the one that 
	 * triggered this branch.
	 * 
	 * @param pEvent an {@link ExecutionEvent}.
	 * @return {@code true} iff {@code pEvent} refers
	 *         to this edge.
	 * @throws RuntimeException if {@link #needsEvent()}{@code == false}.
	 */
	//TODO this method's implementations actually does VERY LENIENT checks.
	public abstract boolean matchesEvent(ExecutionEvent pEvent) throws RuntimeException;
	
	@Override
	public String toString() {
		if (this.label == null) {
			return "";
		}
		return this.label.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		LabeledEdge other = (LabeledEdge) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}
}
