package it.unisa.ocelot.genetic.nodes;

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
 * Problem: Cover a single CFG node.
 */
public final class ProblemTargetSingleNode extends StandardProblem {
	private static final long serialVersionUID = 1930014794768729268L;

	private final CFGNode target;
	private final NodeDistanceListener nodeDistanceListener;
	private final SimulatorListener[] listeners;


	public ProblemTargetSingleNode(boolean pDebug, CFG pCfg, int pArraySize, Range<Double>[] pRanges, CFGNode pTarget) {
		super(ProblemTargetSingleNode.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);
		this.target = pTarget;
		DominanceCalculatorVertices<CFGNode, LabeledEdge> cfgDominanceVertices = new DominanceCalculatorVertices<>(this.cfg, this.cfg.getEntry());
		Set<CFGNode> dominators = cfgDominanceVertices.getStrictDominators(pTarget);
		this.nodeDistanceListener = new NodeDistanceListener(this.cfg, this.target, dominators);
		this.listeners = new SimulatorListener[] { this.nodeDistanceListener };
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		double objective = Utils.safeDouble(this.nodeDistanceListener.getDistance());
		solution.setObjective(0, objective);
	}
	
	@Override
	public double getLastBranchDistance() {
		return this.nodeDistanceListener.getBranchDistance();
	}
}
