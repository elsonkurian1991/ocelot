package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.simulator.CaseExecutionEvent;
import it.unisa.ocelot.simulator.ExecutionEvent;

import java.util.HashMap;

/**
 * A {@link LabeledEdge} representing a 'case' branch
 * of a switch C statement. It is labeled.
 * 
 * @author simone
 */
public class CaseEdge extends LabeledEdge {
	private static final long serialVersionUID = 5916153862577936206L;

	//Associates to every edge label the corresponding unique id.
	//TODO HACKISH!!! IMPROVE!!!
	private static final HashMap<String, Integer> INDEX = new HashMap<>();	
	
	/**
	 * Given a textual label, generates
	 * or retrieves an associated id.
	 * 
	 * @param pLabel a {@link String}.
	 * @return an {@code int} id with 
	 *         the property that, for
	 *         every pair of strings
	 *         {@code pLabel1} and
	 *         {@code pLabel2}, 
	 *         {@code pLabel1.} {@link String#equals(Object) equals}{@code (pLabel2)}
	 *         implies that {@code getUniqueId(pLabel1) ==  getUniqueId(pLabel2)}.
	 */
	//TODO HACKISH!!! IMPROVE!!!
	public static int uniqueId(String pLabel) {
		if (INDEX.containsKey(pLabel)) {
			return INDEX.get(pLabel);
		} else {
			int newId = INDEX.size();
			INDEX.put(pLabel, newId);
			return newId;
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param pLabel a {@link String} that uniquely
	 *        identifies the edge, at least between
	 *        all its siblings. (current implementation
	 *        sets it to the case expression 'case <pLabel>:')
	 */
	public CaseEdge(String pLabel) {
		super(uniqueId(pLabel), pLabel);
	}

	public boolean isDefault() {
		return getId() == uniqueId("default");
	}
	
	@Override
	public boolean isBranch() {
		return true;
	}
	
	@Override
	public boolean matchesEvent(ExecutionEvent pEvent) {
		//handles the null parameter
		if (pEvent == null) { 
			return isDefault(); //TODO why???
		}
		
		if (pEvent.getClass() == CaseExecutionEvent.class) {
			return (getId() == pEvent.getId()); //must have the same id
		}
		
		return false;
	}

	@Override
	public boolean needsEvent() {
		return true;
	}
}
