package it.unisa.ocelot.c.cfg.nodes;

import it.unisa.ocelot.c.cfg.CFG;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * This class represents a node of the Control Flow Graph. It references the
 * corresponding syntax tree node.
 * 
 * @author simone
 *
 */
public class CFGNode implements Comparable<CFGNode> {
	private static int lastId = -1; //TODO should it be atomic?

	private static int getID() {
		lastId++;
		return lastId;
	}

	/**
	 * Resets the incremental id counter.
	 */
	//TODO why do we want to reset the counter???
	public static void reset() {
		lastId = -1; // This is not an error! It starts from -1, it is incremented the first time, so it will be 0.
	}
	
	private final int id;
	private final List<IASTNode> nodes;
	private final String name;
	
	private boolean isSwitch;

	/**
	 * Constructor (node associated to an AST node). 
	 * 
	 * @param pNode the {@link IASTNode} of the syntax tree from which this 
	 *        CFG node is generated.
	 */
	public CFGNode(IASTNode pNode) {
		this.id = getID();
		this.nodes = new ArrayList<>();
		this.nodes.add(pNode);
		this.name = toString();
		this.isSwitch = false;
	}

	/**
	 * Constructor (node not associated to any AST node).
	 */
	public CFGNode(String pName) {
		this.id = getID();
		this.nodes = new ArrayList<>();
		this.name = pName;
		this.isSwitch = false;
	}

	/**
	 * Returns the ID of the node
	 * 
	 * @return
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * Joins another CFGNode to this one.
	 * 
	 * @param pNode CFGNode to be joined
	 */
	public void join(CFGNode pNode) {
		this.nodes.addAll(pNode.nodes);
	}

	/**
	 * Returns the AST-node of the list
	 * 
	 * @return
	 */
	public IASTNode getLeadingNode() {
		return this.nodes.get(0);
	}

	/**
	 * Returns all the AST-nodes of the list
	 * 
	 * @return
	 */
	public List<IASTNode> getNodes() {
		return this.nodes;
	}

	//TODO make a factory method for all these cases!!!
	
	public boolean isContinue() {
		return (this.nodes.size() == 1 && getLeadingNode() instanceof IASTContinueStatement);
	}

	public boolean isBreak() {
		return (this.nodes.size() == 1 && getLeadingNode() instanceof IASTBreakStatement);
	}

	public boolean isGoto() {
		return (this.nodes.size() == 1 && getLeadingNode() instanceof IASTGotoStatement);
	}

	public boolean isCase() {
		return (this.nodes.size() > 0 && getLeadingNode() instanceof IASTCaseStatement);
	}

	public boolean isSwitch() {
		return this.isSwitch;
	}

	public void setSwitch() {
		this.isSwitch = true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		CFGNode node = (CFGNode) obj;
		if (this.nodes.size() > 0) {
			return node.nodes.equals(this.nodes);
		} else {
			return node.name.equals(this.name);
		}
	}

	public boolean strongEquals(CFGNode pNode) {
		return this.id == pNode.id;
	}

	@Deprecated
	public CFGNodeNavigator navigate(CFG cfg) {
		CFGNodeNavigator navigator = new CFGNodeNavigator(cfg, this);
		return navigator;
	}

	@Override
	public String toString() {
		String result = "" + this.id + ": ";
		if (this.nodes.size() > 0) {
			for (IASTNode node : this.nodes) {
				if (node != null) { //TODO does node == null really happens???
					result += node.getRawSignature() + "\n";
				}
			}
		} else {
			result += this.name;
		}
					
		return result;
	}

	@Override
	public int compareTo(CFGNode o) {
		return Integer.valueOf(this.id).compareTo(o.id);
	}
}
