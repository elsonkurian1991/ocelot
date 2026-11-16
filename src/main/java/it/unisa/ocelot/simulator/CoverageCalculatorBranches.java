package it.unisa.ocelot.simulator;

import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;

/**
 * Calculates CFG coverage metrics, assuming as default 
 * coverage objective metric branch coverage.
 */
public final class CoverageCalculatorBranches extends CoverageCalculatorCFG<LabeledEdge> {
	public CoverageCalculatorBranches(CFG pCfg) {
		super(pCfg);
	}

	@Override
	public Set<LabeledEdge> getUncovered() {
		return getUncoveredBranches();
	}
	
	@Override
	public Set<LabeledEdge> getCovered() {
		return getCoveredBranches();
	}
	
	@Override
	public double getCoverage() {
		return getBranchCoverage();
	}
}
