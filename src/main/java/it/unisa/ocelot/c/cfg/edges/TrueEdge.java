package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.simulator.ExecutionEvent;
import it.unisa.ocelot.simulator.IfExecutionEvent;

/**
 * A {@link LabeledEdge} representing the 'then' branch
 * of an if C statement. It is labeled.
 * 
 * @author simone
 */
//TODO currently unlabeled, add label (is it necessary?)
public class TrueEdge extends LabeledEdge {
	private static final long serialVersionUID = 5916153862577936206L;
	
	private static final int TRUE_EDGE_ID = 1;
	
	public TrueEdge() {
		//an if C statement has
		//just two companion branches, so we can use
		//the same identifier for all the 'then' 
		//edges
		super(TRUE_EDGE_ID);
	}
	
	@Override
	public boolean isBranch() {
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

	@Override
	public boolean needsEvent() {
		return true;
	}
}
