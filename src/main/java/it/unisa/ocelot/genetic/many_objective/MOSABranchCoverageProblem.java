package it.unisa.ocelot.genetic.many_objective;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.edges.EdgeDistanceListener;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;

/**
 * Problem: Cover all branches in a program, as a multi-objective problem.
 */
public final class MOSABranchCoverageProblem extends StandardProblem {
	private static final long serialVersionUID = 1L;
	
	/**
	 * The targets, a set of CFG edges. They
	 * must be a subset of the branches of
	 * the CFG.
	 */
	private final HashSet<LabeledEdge> targets;
	
	/**
	 * Maps each target to the dominators
	 * of the start node of the target.
	 */
	private final HashMap<LabeledEdge, Set<CFGNode>> dominators;
	
	/**
	 * Maps each target to the corresponding
	 * listener.
	 */
	private final HashMap<LabeledEdge, EdgeDistanceListener> targetsToListeners;

	/**
	 * All the listeners. It contains exactly
	 * the listeners in targetsToListeners.values().
	 */
	private final SimulatorListener[] listeners;

	public MOSABranchCoverageProblem(boolean pDebug, CFG pCfg, int pArraySize, Range<Double>[] pRanges, Set<LabeledEdge> pTargets) {
		super(MOSABranchCoverageProblem.class.getName(), pTargets.size(), pDebug, pCfg, pArraySize, pRanges); //note that there is exactly one objective for each target
		if (!pCfg.edgeSetBranch().containsAll(pTargets)) {
			throw new RuntimeException("MOSABranchCoverageProblem.constructor: This is a branch coverage problem, but the specified targets are not (all) branches: check the pTargets parameter.");
		}
		this.targets = new HashSet<>(pTargets); //safety copy of pTargets
		this.dominators = new HashMap<>();
		
		fillDominators();
		
		// Creates the listeners, one for each uncovered target,
		// and puts them in this.targetsToListeners and in 
		// this.listeners
		this.targetsToListeners = new HashMap<>();
		ArrayList<SimulatorListener> listeners = new ArrayList<>();
		for (LabeledEdge labeledEdge : this.targets) {
			if (labeledEdge.isCovered()) {
				continue;
			}
			EdgeDistanceListener edgeDistanceListener = new EdgeDistanceListener(this.cfg, labeledEdge, this.dominators.get(labeledEdge), Collections.emptySet());
			this.targetsToListeners.put(labeledEdge, edgeDistanceListener);
			listeners.add(edgeDistanceListener);
		}
		this.listeners = listeners.toArray(new SimulatorListener[0]);
	}
	
	private void fillDominators() {
		DominanceCalculatorVertices<CFGNode, LabeledEdge> dominators = new DominanceCalculatorVertices<>(this.cfg, this.cfg.getEntry());
		for (LabeledEdge edge : this.targets) {	
			this.dominators.put(edge, dominators.getStrictDominators(this.cfg.getEdgeSource(edge)));
		}
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		for (LabeledEdge labeledEdge : this.targets) {
			EdgeDistanceListener edgeDistanceListener = this.targetsToListeners.get(labeledEdge);
			double objective = Utils.safeDouble(edgeDistanceListener.getDistance());
			solution.setObjective(labeledEdge.getObjectiveId(), objective);
		}		
	}
	
	@Override
	public double getLastBranchDistance() {
		return 0; // this method is not used by MOSA
	}
	
	//methods that can possibly be deleted (see also MOSA_Generic)

	//TODO do we really need to get the CFG from HERE???????
	/**
	 * Returns the control flow graph of the 
	 * function under test.
	 * 
	 * @return a {@link CFG}.
	 */
	public CFG getCFG() {
		return this.cfg; //TODO make a safety copy, or wrap it in a read-only view
	}

	//TODO do we really need to get the targets from HERE???????
	/**
	 * Returns the set of branches representing the target of our coverage
	 * problem
	 * 
	 * @return a {@link Set}{@code <}{@link LabeledEdge}{@code >}.
	 *         They are all branches of the CFG this problem
	 *         stores.
	 */
	public Set<LabeledEdge> getTargets() {
		return this.targets;
	}
}
