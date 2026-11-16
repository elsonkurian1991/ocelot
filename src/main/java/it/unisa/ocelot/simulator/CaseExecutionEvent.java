package it.unisa.ocelot.simulator;

/**
 * An {@link ExecutionEvent} triggered by
 * the traversal of a branch of a 'switch' 
 * statement.
 */
public class CaseExecutionEvent extends ExecutionEvent {
	private final double distance;
	private final boolean chosen;
	
	/**
	 * Constructor.
	 * 
	 * @param pId an {@code int} identifying uniquely  
	 *        this event.
	 * @param pDistance the branch distance from
	 *        the branch associated to this event.        
	 * @param pChosen {@code true} iff the execution
	 *        passed through the branch associated to
	 *        this event. In such case it must be
	 *        {@code pDistance == 0}.
	 */
	public CaseExecutionEvent(int pId, double pDistance, boolean pChosen) {
		super(pId);
		this.distance = pDistance;
		this.chosen = pChosen;
	}
		
	public double getDistance() {
		return this.distance;
	}
	
	public boolean isChosen() {
		return this.chosen;
	}
	
	@Override
	public String toString() {
		String res = "CaseExecutionEvent [id=" + getId() + 
				", distance=" + this.distance + 
				", chosen=" + this.chosen + 
				"]";
		return res;
	}
}
