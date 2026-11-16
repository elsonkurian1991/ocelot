package it.unisa.ocelot.c.cfg;

import it.unisa.ocelot.c.cfg.edges.FlowEdge;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.types.CType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.graph.ListenableDirectedGraph;

/**
 * A control flow graph for a (exactly one) C function. It is a 
 * graph whose nodes are {@link CFGNode}s and whose edges are 
 * {@link LabeledEdge}s. Labeled edges identify conditional
 * flow of control, and their label is the logical condition 
 * guarding the flow of control through them. Unlabeled edges 
 * represent unconditional flow of control.
 * 
 * @see LabeledEdge
 * @author simone
 */
public class CFG extends ListenableDirectedGraph<CFGNode, LabeledEdge> {
	private static final long serialVersionUID = 6995672951065896769L;
	
	private final List<Double> constantsNumeric;
	private final List<String> constantsString;
	
	private CFGNode entry; //the entry point
	private CFGNode exit;   //the exit point
	private CType[] parameterTypes; //the C types of the function's parameters
	
	/**
	 * Creates an empty CFG
	 */
	public CFG() {
		super(LabeledEdge.class);
		this.constantsNumeric = new ArrayList<>();
		this.constantsString = new ArrayList<>();
	}
	
	/**
	 * Adds the entry point node. The
	 * entry point cannot have incoming
	 * edges.
	 * 
	 * @param pEntry a {@link CFGNode}.
	 */
	public void addVertexEntry(CFGNode pEntry) {
		addVertex(pEntry);
		this.entry = pEntry;
	}
	
	/**
	 * Adds the exit point node. The
	 * exit point cannot have outgoing
	 * edges.
	 * 
	 * @param pExit a {@link CFGNode}.
	 */
	public void addVertexExit(CFGNode pExit) {
		addVertex(pExit);
		this.exit = pExit;
	}
	
	/**
	 * Returns the entry node.
	 * 
	 * @return a {@link CFGNode}.
	 */
	public CFGNode getEntry() {
		return this.entry;
	}
	
	/**
	 * Returns the exit node.
	 * 
	 * @return a {@link CFGNode}.
	 */
	public CFGNode getExit() {
		return this.exit;
	}
	
	/**
	 * Returns the types of the parameters
	 * of the function this CFG was generated for.
	 * 
	 * @return a {@link CType}{@code []}.
	 */
	public CType[] getParameterTypes() {
		return this.parameterTypes;
	}

	/**
	 * Sets the types of the parameters
	 * of the function this CFG was generated for.
	 * 
	 * @param parameterTypes
	 */
	public void setParameterTypes(CType[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * Returns a list of the (literals for) numbers 
	 * found in the C source code.
	 * 
	 * @return a {@link List}{@code <}{@link Double}{@code >}.
	 */
	public List<Double> getConstantsNumeric() {
		return this.constantsNumeric;
	}

	/**
	 * Adds a numeric constant.
	 * 
	 * @param constant a {@link Double}, the 
	 *        constant to add.
	 */
	public void addConstantNumeric(Double constant) {
		this.constantsNumeric.add(constant);
	}

	/**
	 * Returns a list of {@link String} constants found 
	 * in the source code.
	 * 
	 * @return a {@link List}{@code <}{@link String}{@code >}.
	 */
	//TODO not used at the moment.
	public List<String> getConstantsString() {
		return this.constantsString;
	}

	/**
	 * Adds a {@link String} constant.
	 * 
	 * @param constant a {@link String}, the 
	 *        constant to add.
	 */
	public void addConstantString(String constant) {
		this.constantsString.add(constant);
	}
	
	/**
	 * Like {@link #getDecisionEdges()} but without setting
	 * the object id of the edges.
	 * 
	 * @return a {@link Set}{@code <}{@link LabeledEdge}{@code >}.
	 */
	public Set<LabeledEdge> edgeSetBranch() {
		Set<LabeledEdge> result = edgeSet().stream().filter(LabeledEdge::isBranch).collect(Collectors.toSet());
		return result;
	}
	
	/**
	 * Returns all the decision edges in this control flow graph.
	 * A decision edge is any edge in the graph except 
	 * {@link FlowEdge}s. Also, gives to each decision edge
	 * a progressive identification number by setting their
	 * objective id.
	 * 
	 * @return a {@link Set}{@code <}{@link LabeledEdge}{@code >}
	 *         containing all the decision edges of the current
	 *         control flow graph.
	 */
	//TODO shouldn't we pick the ids from GenericObjectives???
	//TODO remove it!!!
	public Set<LabeledEdge> getDecisionEdges() {
		HashSet<LabeledEdge> result = new HashSet<>();
		int nextAvailableObjectiveId = 0;
		for (LabeledEdge edge : edgeSet()) {
			if (!(edge instanceof FlowEdge)) {
				// It is a decision edge: sets its objectiveId 
				// and adds it to the result
				edge.setObjectiveId(nextAvailableObjectiveId);
				result.add(edge);
				nextAvailableObjectiveId++;
			}
		}
		return result;
	}
}
