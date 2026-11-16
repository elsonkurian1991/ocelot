package it.unisa.ocelot.simulator;

/**
 * An {@link ExecutionEvent} triggered by
 * the traversal of one of two branches 
 * of an 'if' statement.
 */
public class IfExecutionEvent extends ExecutionEvent {
	private final double distanceThen;
	private final double distanceElse;
		
	/**
	 * Constructor.
	 * 
	 * @param pId an {@code int} identifying uniquely  
	 *        this event.
	 * @param pDistanceThen the branch distance from
	 *        the 'then' branch of the if statement.        
	 * @param pDistanceElse the branch distance from
	 *        the 'else' branch of the if statement.
	 */
	public IfExecutionEvent(int pId, double pDistanceThen, double pDistanceElse) {
		super(pId);
		this.distanceThen = pDistanceThen;
		this.distanceElse = pDistanceElse;
	}
	
	public double getDistanceThen() {
		return this.distanceThen;
	}
	
	public double getDistanceElse() {
		return this.distanceElse;
	}
	
	@Override
	public String toString() {
		String res = "IfExecutionEvent [id=" + getId() + 
				", distanceThen=" + this.distanceThen +
				", distanceElse=" + this.distanceElse +
				"]";
		return res;
	}
}
