package it.unisa.ocelot.genetic.many_nodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

import org.apache.commons.lang3.Range;

import jmetal.core.Solution;

/**
 * Problem: Cover many CFG nodes.
 */
public final class ProblemTargetManyNodes extends StandardProblem {
	private static final long serialVersionUID = 1930014794768729268L;

	private final ArrayList<CFGNode> targets;
	private final Map<CFGNode, Set<CFGNode>> dominators;
	private final Map<CFGNode, Integer> fixedApproachLevels;
	private final ManyNodesDistanceListener manyNodesDistanceListener;
	private final SimulatorListener[] listeners;

	public ProblemTargetManyNodes(boolean pDebug, CFG pCfg, int pArraySize, List<CFGNode> pTargets, Range<Double>[] pRanges) {
		super(ProblemTargetManyNodes.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);
		this.targets = new ArrayList<>(pTargets);
		this.dominators = new HashMap<>();
		fillDominators();
		this.fixedApproachLevels = new HashMap<>();
		fillFixedApproachLevels();
		
		// Finally, creates the listeners
		this.manyNodesDistanceListener = 
				new ManyNodesDistanceListener(this.cfg, this.targets, this.fixedApproachLevels, this.dominators);
		this.listeners = new SimulatorListener[] { this.manyNodesDistanceListener };
	}
	
	//This method needs this.targets
	private void fillDominators() {
		//TODO why this particular calculation? Compare with other similar Problem classes (e.g. ManyEdgesCoverageProblem) and possibly put this (or similar) block of code in a reusable method.
		DominanceCalculatorVertices<CFGNode, LabeledEdge> cfgDominanceVertices = new DominanceCalculatorVertices<>(this.cfg, this.cfg.getEntry());
		Set<CFGNode> dominatorsToRemove = new HashSet<>();
		for (int i = 0; i < this.targets.size(); i++) {
			CFGNode target = this.targets.get(i);
			Set<CFGNode> currentDominators = cfgDominanceVertices.getStrictDominators(target);
			currentDominators.removeAll(dominatorsToRemove);
			dominatorsToRemove.addAll(currentDominators);
			this.dominators.put(target, currentDominators);
		}
	}
	
	//This method needs this.targets and this.dominators, so invoke fillDominators() first.
	private void fillFixedApproachLevels() {
		//TODO why this particular calculation? Compare with other similar Problem classes (e.g. ManyEdgesCoverageProblem) and possibly put this (or similar) block of code in a reusable method.
		
		// Calculates single branch distances
		HashMap<CFGNode, Integer> singleBranchDistances = new HashMap<>();
		for (int i = 1; i < this.targets.size(); i++) {
			CFGNode source = this.targets.get(i - 1);
			CFGNode target = this.targets.get(i);
			singleBranchDistances.put(source, this.dominators.get(target).size());
		}
		singleBranchDistances.put(this.targets.get(this.targets.size() - 1), 0);
		
		// Sets up the total distance between each node in the target nodes and the last one
		this.fixedApproachLevels.put(this.targets.get(this.targets.size() - 1), 0);
		for (int i = this.targets.size() - 2; i >= 0; i--) {
			CFGNode node = this.targets.get(i);
			CFGNode nextNode = this.targets.get(i + 1);
			this.fixedApproachLevels.put(node, this.fixedApproachLevels.get(nextNode) + singleBranchDistances.get(node));
		}
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		double objective = Utils.safeDouble(this.manyNodesDistanceListener.getDistance());
		solution.setObjective(0, objective);
	}
	
	@Override
	public double getLastBranchDistance() {
		return this.manyNodesDistanceListener.getBranchDistance();
	}
}
