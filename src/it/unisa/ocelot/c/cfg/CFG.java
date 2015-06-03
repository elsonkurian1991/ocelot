package it.unisa.ocelot.c.cfg;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.graph.ListenableDirectedGraph;

/**
 * Control Flow Graph, a graph which nodes are CFGNode and which edges are LabeledEdge, so edges
 * that has an object as a label to identify the condition in which the the control flows from a
 * vertex to another one. For instance, a condition will have two edges: an edge labeled true and
 * an edge labeled false.
 * @see LabeledEdge
 * @author simone
 *
 */
public class CFG extends ListenableDirectedGraph<CFGNode, LabeledEdge> {
	private static final long serialVersionUID = 6995672951065896769L;
	
	private CFGNode start;
	private CFGNode end;
	private CFGNode target;
	
	private List<Double> constantNumbers;
	private List<String> constantStrings;
	
	@SuppressWarnings("rawtypes")
	private Class[] parameterTypes;
	
	/**
	 * Creates an empty CFG
	 */
	public CFG() {
		super(LabeledEdge.class);
		
		this.constantNumbers = new ArrayList<Double>();
		this.constantStrings = new ArrayList<String>();
	}
	
	/**
	 * Sets the starting node
	 * @param pStartNode
	 */
	public void setStart(CFGNode pStartNode) {
		this.start = pStartNode;
	}
	
	/**
	 * Sets the ending node
	 * @param pEndNode
	 */
	public void setEnd(CFGNode pEndNode) {
		this.end = pEndNode;
	}
	
	/**
	 * Returns the starting node
	 * @return
	 */
	public CFGNode getStart() {
		return start;
	}
	
	/**
	 * Returns the ending node
	 * @return
	 */
	public CFGNode getEnd() {
		return end;
	}
	
	@Deprecated
	public void setTarget(CFGNode target) {
		this.target = target;
	}
	
	@Deprecated
	public CFGNode getTarget() {
		return target;
	}

	/**
	 * Returns the types of the parameters
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Class[] getParameterTypes() {
		return parameterTypes;
	}

	/**
	 * Sets the types of the parameters
	 * @param parameterTypes
	 */
	@SuppressWarnings("rawtypes")
	public void setParameterTypes(Class[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * Returns a list of constant numbers (literals) found in the source code
	 * @return
	 */
	public List<Double> getConstantNumbers() {
		return constantNumbers;
	}

	/**
	 * Sets the list of constant numbers
	 * @param constantNumbers
	 */
	public void setConstantNumbers(List<Double> constantNumbers) {
		this.constantNumbers = constantNumbers;
	}

	/**
	 * Returns a list of constant strings found in the source code
	 * @return
	 */
	public List<String> getConstantStrings() {
		return constantStrings;
	}

	/**
	 * Sets the list of constant strings
	 * @param constantStrings
	 */
	public void setConstantStrings(List<String> constantStrings) {
		this.constantStrings = constantStrings;
	}
}