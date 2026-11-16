package it.unisa.ocelot.c.cfg;

import it.unisa.ocelot.c.cfg.nodes.CFGNode;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides information about a sub-graph. More precisely, it indicates which are the input nodes, the
 * output nodes, the nodes representing case statements, the "break"s and the "continue"s of a sub-graph.
 * @author simone
 *
 */
public final class SubGraph {
	private List<CFGNode> inputs;
	private List<CFGNode> outputs;
	private List<CFGNode> cases;
	private List<CFGNode> breaks;
	private List<CFGNode> continues;
	
	/**
	 * Creates an empty sub-graph
	 */
	public SubGraph() {
		this.inputs = new ArrayList<>();
		this.outputs = new ArrayList<>();
		this.cases = new ArrayList<>();
		this.breaks = new ArrayList<>();
		this.continues = new ArrayList<>();
	}
	
	/**
	 * Adds a CFGNode to the list of input nodes of the sub-graph.
	 * @param pNode Node to be added
	 */
	public void addInput(CFGNode pNode) {
		this.inputs.add(pNode);
	}
	
	/**
	 * Adds a list of CFGNode to the list of input nodes of the sub-graph.
	 * @param pNodes Nodes to be added
	 */
	public void addInput(List<CFGNode> pNodes) {
		this.inputs.addAll(pNodes);
	}
	
	/**
	 * Inherits all the input nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritInput(SubGraph pGraph) {
		this.inputs.addAll(pGraph.getInputs());
	}
	
	/**
	 * Adds a CFGNode to the list of output nodes of the sub-graph.
	 * @param pNode Node to be added
	 */
	public void addOutput(CFGNode pNode) {
		this.outputs.add(pNode);
	}
	
	/**
	 * Adds a list of CFGNode to the list of output nodes of the sub-graph.
	 * @param pNodes Nodes to be added
	 */
	public void addOutput(List<CFGNode> pNodes) {
		this.outputs.addAll(pNodes);
	}
	
	/**
	 * Inherits all the output nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritOutput(SubGraph pGraph) {
		this.outputs.addAll(pGraph.getOutputs());
	}
	
	/**
	 * Removes all output nodes
	 * @param pGraph
	 */
	public void resetOutput() {
		this.outputs.clear();
	}
	
	/**
	 * Adds a CFGNode to the list of cases nodes of the sub-graph.
	 * @param pNode Node to be added
	 */
	public void addCase(CFGNode pNode) {
		this.cases.add(pNode);
	}
	
	/**
	 * Adds a list of CFGNode to the list of case nodes of the sub-graph.
	 * @param pNodes Nodes to be added
	 */
	public void addCase(List<CFGNode> pNodes) {
		this.cases.addAll(pNodes);
	}
	
	/**
	 * Inherits all the case nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritCase(SubGraph pGraph) {
		this.cases.addAll(pGraph.getCases());
	}
	
	/**
	 * Adds a CFGNode to the list of break nodes of the sub-graph.
	 * @param pNode Node to be added
	 */
	public void addBreak(CFGNode pNode) {
		this.breaks.add(pNode);
	}
	
	/**
	 * Adds a list of CFGNode to the list of break nodes of the sub-graph.
	 * @param pNodes Nodes to be added
	 */
	public void addBreak(List<CFGNode> pNodes) {
		this.breaks.addAll(pNodes);
	}
	
	/**
	 * Inherits all the break nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritBreak(SubGraph pGraph) {
		this.breaks.addAll(pGraph.getBreaks());
	}
	
	/**
	 * Adds a CFGNode to the list of continue nodes of the sub-graph.
	 * @param pNode Node to be added
	 */
	public void addContinue(CFGNode pNode) {
		this.continues.add(pNode);
	}
	
	/**
	 * Adds a list of CFGNode to the list of continue nodes of the sub-graph.
	 * @param pNodes Nodes to be added
	 */
	public void addContinue(List<CFGNode> pNodes) {
		this.continues.addAll(pNodes);
	}
	
	/**
	 * Inherits all the continue nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritContinue(SubGraph pGraph) {
		this.continues.addAll(pGraph.getContinues());
	}
	
	/**
	 * Inherits all the break, continue and case nodes from another sub-graph
	 * @param pNode Sub-graph from which inherit the nodes.
	 */
	public void inheritOthers(SubGraph pGraph) {
		this.inheritContinue(pGraph);
		this.inheritBreak(pGraph);
		this.inheritCase(pGraph);
	}
	
	public void joinVertices(CFGNode pToRemove, CFGNode pNewVertex) {
		if (this.breaks.contains(pToRemove)) {
			this.breaks.remove(pToRemove);
			this.breaks.add(pNewVertex);
		}
		
		if (this.continues.contains(pToRemove)) {
			this.continues.remove(pToRemove);
			this.continues.add(pNewVertex);
		}
	}
	
	/**
	 * Returns the list of input nodes of the sub-graph
	 * @return
	 */
	public List<CFGNode> getInputs() {
		return inputs;
	}
	
	/**
	 * Returns the list of output nodes of the sub-graph
	 * @return
	 */
	public List<CFGNode> getOutputs() {
		return outputs;
	}
	
	/**
	 * Returns the list of case nodes of the sub-graph
	 * @return
	 */
	public List<CFGNode> getCases() {
		return cases;
	}
	
	/**
	 * Returns the list of break nodes of the sub-graph
	 * @return
	 */
	public List<CFGNode> getBreaks() {
		return breaks;
	}
	
	/**
	 * Returns the list of continue nodes of the sub-graph
	 * @return
	 */
	public List<CFGNode> getContinues() {
		return continues;
	}
}
