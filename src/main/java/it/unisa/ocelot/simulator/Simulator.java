package it.unisa.ocelot.simulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.util.Utils;

/**
 * Simulates the execution of the program on the CFG in order to calculate the
 * distance metrics and, so, the fitness function.
 * 
 * @author simone
 */
//TODO make it work over *many* CFGs and thus support projects with many C functions/files.
public class Simulator {
	private final CFG cfg;
	private final List<ExecutionEvent> events;
	private final ArrayList<SimulatorListener> listeners;

	// Used during simulation
	private int currentEventIndex;
	
	public Simulator(CFG pCFG, List<ExecutionEvent> pEvents) {
		this.cfg = pCFG;
		this.events = pEvents;
		//It is useless, but it prevents ArrayIndexOutOfBound in "simulate" when there is no event.
		//TODO a bit ugly, can we do better, e.g., by using iterators?
		if (this.events.size() == 0) {
			this.events.add(null);
		}
		this.listeners = new ArrayList<>();
	}
	
	/**
	 * Sets a single listener, removing all the previously
	 * registered ones.
	 * 
	 * @param pListener a {@link SimulatorListener}.
	 */
	public void setListener(SimulatorListener pListener) {
		this.listeners.clear();
		this.listeners.add(pListener);
	}
	
	/**
	 * Adds a listener.
	 * 
	 * @param pListener a {@link SimulatorListener}.
	 */
	public void addListener(SimulatorListener pListener) {
		this.listeners.add(pListener);
	}
	
	/**
	 * Adds many listeners.
	 * 
	 * @param pListeners a varargs of {@link SimulatorListener}s.
	 */
	public void addListeners(SimulatorListener... pListeners) {
		Collections.addAll(this.listeners, pListeners);
	}
	
	/**
	 * Perform the simulation
	 */
	public void simulate() {
		// Resets the event iterator
		reset();
		
		String emergencyLog = ""; //TODO do we really want to log? And where?
		
		// Gets the exit node of the CFG
		// (termination condition)
		CFGNode exitNode = this.cfg.getExit();
		
		// Gets the first event and
		// the entry node of the CFG 
		// and initializes the visit
		CFGNode currentNode = this.cfg.getEntry();
		ExecutionEvent currentEvent = getNextEvent();
		
		// Loop until we reach the exit of the cfg
		while (!currentNode.strongEquals(exitNode)) {
			// Notifies the node visit
			onNodeVisit(currentNode);
			
			// Stores info from the previous iteration
			CFGNode lastNode = currentNode;
			int lastEventIndex = this.currentEventIndex;

			// Takes all the outgoing edges
			// of the current node
			Set<LabeledEdge> currentOutgoingEdges = this.cfg.outgoingEdgesOf(currentNode);
			
			// If on a switch statement, gets all the events for it
			// (one for each edge) and sets currentEvent to the one 
			// for the chosen branch.
			ArrayList<CaseExecutionEvent> caseEvents;
			if (currentNode.isSwitch()) {
				caseEvents = new ArrayList<>();
				
				emergencyLog += "------ Case events -------\n";
				
				// Goes back of one event
				rewind(1);
				
				// Resets the current event (for safety)
				currentEvent = null;
				
				// Gets all the events for the switch statement
				// and sets the current event to the one
				// associated to the edge that was actually 
				// taken during execution
				for (LabeledEdge edge : currentOutgoingEdges) {
					CaseExecutionEvent eventCase = (CaseExecutionEvent) getNextEvent();
					//associates the event with the corresponding edge
					//TODO are we sure this is the right edge?
					eventCase.setEdge(edge); 
					caseEvents.add(eventCase);
					
					// Sets currentEvent
					if (eventCase.isChosen()) {
						currentEvent = eventCase;
					}
				}
			} else {
				//it is not a switch
				caseEvents = null;
			}
			
			// Do we need a new event?
			boolean newEvent = false;
			
			// Scans the current outgoing edges
			for (LabeledEdge edge : currentOutgoingEdges) {
				if (edge.needsEvent()) {
					// Takes the edge matching the execution event
					if (edge.matchesEvent(currentEvent)) {
						currentEvent.setEdge(edge);
						
						// Notifies the visit of the edge 
						// to the listeners.
						if (currentNode.isSwitch()) {
							onEdgeVisit(edge, (CaseExecutionEvent) currentEvent, caseEvents);
						} else {
							onEdgeVisit(edge, (IfExecutionEvent) currentEvent);
						}
						
						//Changes the current node
						currentNode = this.cfg.getEdgeTarget(edge);
						
						//Need a new event!
						newEvent = true;
					} //if matches the execution event
				} else {
					// Ihe edge is a FlowEdge
					onEdgeVisit(edge);
					currentNode = this.cfg.getEdgeTarget(edge);
				} //if needs events
			} //while end not reached
			
			// Checks invariant
			if (currentNode == lastNode && 
				lastEventIndex == this.currentEventIndex) {
				try {
					Utils.writeFile("EMERGENCY_LOG", emergencyLog);
				} catch (IOException e) {
					//nothing that we can do...
				}
				throw new SimulationException("Simulation error");
			}
			
			//If needs a new event, consumes a new event.
			if (newEvent) {
				currentEvent = getNextEvent();
			}
		}
		
		if (!isSimulationCorrect()) {
			throw new SimulationException("Simulation not ended!! " + this.currentEventIndex + " in " + this.events.size());
		}
	}
	
	/**
	 * Checks if the simulation performed is correct.
	 * @return true if the simulation is correct, false otherwise
	 */
	public boolean isSimulationCorrect() {
		if (this.currentEventIndex == 0) {
			//no events
			return (this.events.size() == 1 && this.events.get(0) == null);
		} else {
			return this.currentEventIndex == this.events.size();
		}
	}
	
	/**
	 * Resets the simulation.
	 */
	//TODO just use a (bidirectional) Iterator?
	private void reset() {
		this.currentEventIndex = -1;
	}
	
	/**
	 * Consumes an execution event.
	 * 
	 * @return the next {@link ExecutionEvent}
	 *         or {@code null} if there are
	 *         no other events.
	 */
	//TODO just use a (bidirectional) Iterator?
	private ExecutionEvent getNextEvent() {
		this.currentEventIndex++;
		if (this.currentEventIndex < this.events.size()) {
			return this.events.get(this.currentEventIndex);
		} else {
			return null;
		}
	}
	
	/**
	 * Goes back of {@code number} execution events.
	 * 
	 * @param number number of execution events to go back
	 */
	//TODO just use a (bidirectional) Iterator?
	private void rewind(int number) {
		this.currentEventIndex -= number;
	}
	
	/**
	 * Notifies all listeners of a node visit.
	 * 
	 * @param pNode the visited {@link CFGNode}. 
	 */
	private void onNodeVisit(CFGNode pNode) {
		for (SimulatorListener listener : this.listeners) {
			listener.onNodeVisit(pNode);
		}
	}
	
	/**
	 * Notifies all listeners of an edge visit.
	 * The edge is a flow edge.
	 * 
	 * @param pEdge the visited {@link LabeledEdge}. 
	 */
	private void onEdgeVisit(LabeledEdge pEdge) {
		for (SimulatorListener listener : this.listeners) {
			listener.onEdgeVisit(pEdge);
		}
	}
	
	/**
	 * Notifies all listeners of an edge visit. 
	 * The edge is a branch of a switch statement.
	 * 
	 * @param pEdge the visited {@link LabeledEdge}.
	 * @param pEvent the {@link IfExecutionEvent}
	 *        associated to {@code pEdge}.
	 */
	private void onEdgeVisit(LabeledEdge pEdge, IfExecutionEvent pEvent) {
		for (SimulatorListener listener : this.listeners) {
			listener.onEdgeVisit(pEdge, pEvent);
		}
	}
	
	/**
	 * Notifies all listeners of an edge visit. 
	 * The edge is a branch of a switch statement.
	 * 
	 * @param pEdge the visited {@link LabeledEdge}.
	 * @param pEvent the {@link CaseExecutionEvent}
	 *        associated to {@code pEdge}.
	 * @param pCases a {@link List}{@code <}{@link CaseExecutionEvent}{@code >}
	 *        containing all the events generated by the execution of the 
	 *        switch statement.  
	 */
	private void onEdgeVisit(LabeledEdge pEdge, CaseExecutionEvent pEvent, List<CaseExecutionEvent> pCases) {
		for (SimulatorListener listener : this.listeners) {
			listener.onEdgeVisit(pEdge, pEvent, pCases);
		}
	}
}
