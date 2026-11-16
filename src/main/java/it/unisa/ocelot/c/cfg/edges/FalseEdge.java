package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;


/**
 * A {@link LabeledEdge} representing the 'else' branch
 * of an 'if' C statement. It is labeled.
 * 
 * @author simone
 */
//TODO currently unlabeled, add label (is it necessary?)
public class FalseEdge extends LabeledEdge {
	private static final long serialVersionUID = -6097816767281519267L;
	
	private static final int FALSE_EDGE_ID = 0;
	
	public FalseEdge() {
		//an if C statement has
		//just two companion branches, so we can use
		//the same identifier for all the 'else' 
		//edges
		super(FALSE_EDGE_ID);
	}

	@Override
	public boolean isBranch() {
		return true;
	}
	
	@Override
	public boolean needsEvent() {
		return true;
	}

	@Override
	public boolean matchesEvent(ExecutionEvent pEvent) {
		if (pEvent == null) {
			return false;
		}
		
		if (pEvent.getClass() == IfExecutionEvent.class) {
			return (getId() == pEvent.getId()); //must have same id
		}
		
		return false;
	}
}
