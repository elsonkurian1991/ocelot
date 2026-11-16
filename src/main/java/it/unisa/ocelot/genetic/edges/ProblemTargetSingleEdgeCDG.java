package it.unisa.ocelot.genetic.edges;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CollateralCoverageEvaluator;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.simulator.listeners.CoverageCalculatorListener;
import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;

public final class ProblemTargetSingleEdgeCDG extends StandardProblem {
	private static final long serialVersionUID = 1L;

	private final LabeledEdge target;
	private final List<LabeledEdge> targets;
	private final Set<CFGNode> dominators; //TODO do we need this?
	private final EdgeDistanceListener edgeDistanceListener; //TODO why is called "dominator" listener?
	private final CoverageCalculatorListener coverageCalculatorListener;
	private final SimulatorListener[] listeners;

	public ProblemTargetSingleEdgeCDG(Range<Double>[] pRanges, int pArraySize,
			boolean pDebug, CFG pCfg, LabeledEdge pTarget, List<LabeledEdge> pTargets) {
		super(ProblemTargetSingleEdgeCDG.class.getName(), 1, pDebug, pCfg, pArraySize, pRanges);

		this.target = pTarget;
		this.targets = pTargets;
		DominanceCalculatorVertices<CFGNode, LabeledEdge> cfgDominanceVertices = new DominanceCalculatorVertices<>(this.cfg, this.cfg.getEntry());
		CFGNode targetSource = this.cfg.getEdgeSource(this.target);
		this.dominators = cfgDominanceVertices.getStrictDominators(targetSource);
		this.edgeDistanceListener = new EdgeDistanceListener(this.cfg, this.target, this.dominators, Collections.emptySet());
		this.coverageCalculatorListener = new CoverageCalculatorListener(this.cfg);
		this.listeners = new SimulatorListener[] { this.edgeDistanceListener, this.coverageCalculatorListener }; 
	}
	
	@Override
	protected SimulatorListener[] getListeners() {
		return this.listeners;
	}

	@Override
	protected void completeEvaluation(Solution solution) {
		double objective = Utils.safeDouble(this.edgeDistanceListener.getDistance());
		solution.setObjective(0, objective);
		
		//TODO why are we *also* setting solution.fitness (with a value different from solution.objective(0))? What is this value useful for? Are the calculations ok?
		//TODO perhaps CollateralCoverageEvaluator must be deprecated
		Object[][][] arguments = getArguments(solution);
		double fitnessPC = CalculateFitnessFromEvalPC3.calculateFitness(arguments);
		CollateralCoverageEvaluator collateralCoverageEvaluator = 
				new CollateralCoverageEvaluator(this.cfg, this.target, new HashSet<>(this.targets), this.coverageCalculatorListener.getCoveredBranches());
		double collateralCoverage = collateralCoverageEvaluator.getCollateralCoverage();
		double fitness = Utils.safeDouble(1 - collateralCoverage + fitnessPC);
		solution.setFitness(fitness);
	}

	@Override
	public double getLastBranchDistance() {
		return this.edgeDistanceListener.getBranchDistance();
	}
}
