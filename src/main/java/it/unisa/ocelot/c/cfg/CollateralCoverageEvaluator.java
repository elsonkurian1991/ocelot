package it.unisa.ocelot.c.cfg;

import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

public class CollateralCoverageEvaluator {
	private final CFG cfg;
	private final DominanceCalculatorVertices<CFGNode, LabeledEdge> dominance;
	private final DirectedGraph<CFGNode, DefaultEdge> cdg;
	private final Set<LabeledEdge> covered; // M
	private final Set<LabeledEdge> coveredByCurrentSolution; // P(x)
	private final int potentiallyCoverable; // L_e(n)
	private final double collateralCoverage;


	public CollateralCoverageEvaluator(CFG pCfg, LabeledEdge pTarget,
			Set<LabeledEdge> pAll, Set<LabeledEdge> pCoveredByCurrentSolution) {
		this.cfg = pCfg;
		this.dominance = new DominanceCalculatorVertices<>(pCfg, pCfg.getEntry());
		this.cdg = this.dominance.getDominatorTree();
		this.covered = pAll.stream().filter(LabeledEdge::isCovered).collect(Collectors.toSet());
		this.coveredByCurrentSolution = pCoveredByCurrentSolution;
		this.potentiallyCoverable = calculatePotentiallyCoverable(pTarget, 0);
		this.collateralCoverage = calculateCollateralCoverage();
	}

	private int calculatePotentiallyCoverable(LabeledEdge edge, int potentiallyCoverable) {
		CFGNode childNode = this.cdg.getEdgeTarget(edge);

		if (this.cdg.outDegreeOf(childNode) <= 1) { //TODO why not of cfg?
			return 0; // not a branching node
		}

		Set<DefaultEdge> outgoingFromCDG = this.cdg.outgoingEdgesOf(childNode);
		Set<LabeledEdge> outgoingFromCFG = this.cfg.outgoingEdgesOf(childNode);

		Set<LabeledEdge> commonOutgoings = new HashSet<>();
		for (LabeledEdge cfgEdge : outgoingFromCFG) {
			CFGNode cfgSouce = this.cfg.getEdgeSource(cfgEdge);
			CFGNode cfgTarget = this.cfg.getEdgeTarget(cfgEdge);
			for (DefaultEdge cdgEdge : outgoingFromCDG) {
				CFGNode cdgSource = this.cdg.getEdgeSource(cdgEdge);
				CFGNode cdgTarget = this.cdg.getEdgeTarget(cdgEdge);
				if (cfgSouce == cdgSource && cfgTarget == cdgTarget) {
					commonOutgoings.add(cfgEdge);
				}
			}
		}

		int max = Integer.MIN_VALUE;
		for (LabeledEdge outEdge : commonOutgoings) {
			int currentCollateral = calculatePotentiallyCoverable(outEdge, potentiallyCoverable);
			if (currentCollateral > max) {
				max = currentCollateral;
			}
		}
		
		potentiallyCoverable += max;

		if (edge.isCovered()) {
			return potentiallyCoverable;
		} else {
			return (1 + potentiallyCoverable);
		}
	}

	private double calculateCollateralCoverage() {
		int notCoveredByCurrentSolution = Math.abs(this.covered.size() - this.coveredByCurrentSolution.size()); //TODO why abs? Shouldn't be always positive?
		if (notCoveredByCurrentSolution > this.potentiallyCoverable) {
			return 1.0;
		} else if (this.potentiallyCoverable == 0) {
			return 0;
		} else {
			return (notCoveredByCurrentSolution / this.potentiallyCoverable);
		}
	}

	public double getCollateralCoverage() {
		return this.collateralCoverage;
	}
}
