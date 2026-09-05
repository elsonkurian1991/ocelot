package it.unisa.ocelot.genetic.objectives.branch_chains;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTNode;

import it.unisa.ocelot.c.cdg.CDGNode;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Represents a single branch-chain: a path from entry to a specific leaf node.
 * Contains both the path structure and the target leaf.
 * 
 * This is the AST-based representation suitable for fitness calculation.
 */
public class BranchChain {

	private CDGNode leafNode;
	private List<PathStep> path;
	private String label;  // Format: "unitComponentName:branchChain1"
	private int chainNumber;
	
	public BranchChain(CDGNode leaf, List<PathStep> path) {
		this.leafNode = leaf;
		this.path = path;
		this.label = "";
		this.chainNumber = -1;
	}
	
	public BranchChain(CDGNode leaf, List<PathStep> path, String unitComponentName, int chainNumber) {
		this.leafNode = leaf;
		this.path = path;
		this.chainNumber = chainNumber;
		this.label = unitComponentName + ":branchChain" + chainNumber;
	}
	public CDGNode getLeafNode() {
		return leafNode;
	}

	public List<PathStep> getPath() {
		return path;
	}
	/**
	 * Returns the unique label for this chain.
	 * Format: "functionName:branch1"
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * Sets the label for this chain.
	 * @param unitComponentName The function/unit component name
	 * @param chainNumber The chain number (1-indexed)
	 */
	public void setLabel(String unitComponentName, int chainNumber) {
		this.chainNumber = chainNumber;
		this.label = unitComponentName + ":branchChain" + chainNumber;
	}

	/**
	 * Calculates and return the fitness value as sum of all condition fitVals.
	 */
	public double calculateBranchChainFitness(HashMap<String, Object> fitnessMap) {
		double branchDistanceSum = 0.0d;
		int numBranchConditions = 0;
		for (PathStep step : path) {
			if (step.hasBranchCondition()) {
				//this is branch with conditions
				numBranchConditions++;
				Object branchDistance = step.getBranchDistance(fitnessMap);
				if (branchDistance == null) {
					branchDistanceSum += 1.0;
				} else {
					branchDistanceSum += ((Double) branchDistance).doubleValue();
				}

			}
		}
		if (numBranchConditions > 0) {
			return branchDistanceSum / (double) numBranchConditions;
		} else {
			return 0.0d;
		}
	}

	/**
	 * Returns the chain number within its unit component.
	 */
	public int getChainNumber() {
		return chainNumber;
	}

	/**
	 * Returns all AST nodes along this execution path.
	 * Useful for code coverage and instrumentation.
	 */
	public List<IASTNode> getExecutionNodes() {
		List<IASTNode> nodes = new ArrayList<>();

		for (PathStep step : path) {
			nodes.addAll(step.getFrom().getASTNodes());
		}
		nodes.addAll(leafNode.getASTNodes());

		return nodes;
	}

	/**
	 * Converts this branch-chain to human-readable text.
	 */
	public String toTextRepresentation() {
		StringBuilder sb = new StringBuilder();

		// Show path with branch decisions
		sb.append("  Path: ");
		for (int i = 0; i < path.size(); i++) {
			PathStep step = path.get(i);
			sb.append("Node[").append(step.getFrom().getId()).append("]");
			if (!step.getBranchLabel().equals("FLOW")) {
				sb.append(" --[").append(step.getBranchLabel()).append("]--> ");
			} else {
				sb.append(" --> ");
			}
		}
		sb.append("Node[").append(leafNode.getId()).append("] (LEAF)\n");
		sb.append("  Leaf content: ").append(leafNode.getLabel()).append("\n");

		return sb.toString();
	}

	@Override
	public String toString() {
		return "BranchChain[leaf=" + leafNode.getId() + ", length=" + path.size() + "]";
	}
	public void setLabel(String newLabel) {
		this.label = newLabel;
		// Optionally, you could also parse the newLabel to update unitComponentName and chainNumber if needed

	}


}
