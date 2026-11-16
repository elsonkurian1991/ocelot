package it.unisa.ocelot.simulator;

import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;

/**
 * Calculates CFG coverage metrics, assuming as default 
 * coverage objective metric edge coverage.
 */
public final class CoverageCalculatorEdges extends CoverageCalculatorCFG<LabeledEdge> {
	public CoverageCalculatorEdges(CFG pCfg) {
		super(pCfg);
	}

	@Override
	public Set<LabeledEdge> getUncovered() {
		return getUncoveredEdges();
	}
	
	@Override
	public Set<LabeledEdge> getCovered() {
		return getCoveredEdges();
	}
	
	@Override
	public double getCoverage() {
		return getEdgeCoverage();
	}
}
