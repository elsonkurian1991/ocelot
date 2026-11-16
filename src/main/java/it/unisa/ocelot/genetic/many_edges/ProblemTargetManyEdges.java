package it.unisa.ocelot.genetic.many_edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorEdges;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

import jmetal.core.Solution;

/**
 * Problem: Cover many CFG edges.
 */
public final class ProblemTargetManyEdges extends StandardProblem {
	private static final long serialVersionUID = 1930014794768729268L;

	private final ArrayList<LabeledEdge> targets;
	private final Map<LabeledEdge, Set<LabeledEdge>> dominators;
	private final Map<LabeledEdge, Integer> fixedApproachLevels;
	private final ManyEdgesDistanceListener manyEdgesDistanceListener;
	private final SimulatorListener[] listeners;

	public ProblemTargetManyEdges(boolean pDebug, CFG pCfg, int pArraySize, List<LabeledEdge> pTargets, Range<Double>[] pRanges) {
		super(ProblemTargetManyEdges.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);
		this.targets = new ArrayList<>(pTargets);
		this.dominators = new HashMap<>();
		this.fixedApproachLevels = new HashMap<>();
		
		//TODO fix the code from here!!!! 
		//TODO (again, do not delete it... fix it!!!!)

		// Completes this.dominators and this.fixedApproachLevels
		fillDominators();
		fillFixedApproachLevels();
		
		// Finally, creates the listeners
		this.manyEdgesDistanceListener = 
				new ManyEdgesDistanceListener(this.cfg, this.targets, this.fixedApproachLevels, this.dominators);
		this.listeners = new SimulatorListener[] { this.manyEdgesDistanceListener };
	}
	
	//This method needs this.targets
	private void fillDominators() {
		// Deletes all the dominators from the targets
		//TODO why we delete all the targets and put as targets only non-dominators??? Why are we doing it here??? If this.targets must be preserved, do a copy!!!!!
		DominanceCalculatorEdges<CFGNode, LabeledEdge> cfgDominanceEdges = new DominanceCalculatorEdges<>(this.cfg, this.cfg.getEntry(), this.cfg.getExit());
		this.targets.retainAll(cfgDominanceEdges.getNonDominators());
		
		// Fills this.dominators
		HashSet<LabeledEdge> alreadySeenDominators = new HashSet<>();
		for (int i = 0; i < this.targets.size(); i++) {
			LabeledEdge target = this.targets.get(i);			
			Set<LabeledEdge> currentDominators = cfgDominanceEdges.getStrictDominators(target);
			currentDominators.removeAll(alreadySeenDominators);
			alreadySeenDominators.addAll(currentDominators);
			this.dominators.put(target, currentDominators);
		}
	}
	
	//This method needs this.targets and this.dominators, so invoke fillDominators() first.
	private void fillFixedApproachLevels() {
		// Calculates this.fixedApproachLevels
		//TODO why this calculation? Compare with other similar Problem classes (e.g. ManyNodesCoverageProblem) and possibly put this (or similar) block of code in a reusable method.
		HashMap<LabeledEdge, Integer> singleBranchDistances = new HashMap<>();
		for (int i = 1; i < this.targets.size(); i++) {
			LabeledEdge source = this.targets.get(i - 1);
			LabeledEdge target = this.targets.get(i);
			singleBranchDistances.put(source, this.dominators.get(target).size());
		}
		singleBranchDistances.put(this.targets.get(this.targets.size() - 1), 0);
		
		//Sets up the total distance between each node in the target nodes and the last one
		this.fixedApproachLevels.put(this.targets.get(this.targets.size() - 1), 0);
		for (int i = this.targets.size() - 2; i >= 0; i--) {
			LabeledEdge edge = this.targets.get(i);
			LabeledEdge nextEdge = this.targets.get(i+1);
			this.fixedApproachLevels.put(edge, this.fixedApproachLevels.get(nextEdge) + singleBranchDistances.get(edge));
		}
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		double objective = Utils.safeDouble(this.manyEdgesDistanceListener.getDistance());
		solution.setObjective(0, objective);
	}
	
	@Override
	public double getLastBranchDistance() {
		return this.manyEdgesDistanceListener.getBranchDistance();
	}
}
