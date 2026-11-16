package it.unisa.ocelot.simulator;

import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

/**
 * Calculates CFG coverage metrics, assuming as default 
 * coverage objective metric branch coverage.
 */
public final class CoverageCalculatorBlocks extends CoverageCalculatorCFG<CFGNode> {
	public CoverageCalculatorBlocks(CFG pCfg) {
		super(pCfg);
	}

	@Override
	public Set<CFGNode> getUncovered() {
		return getUncoveredBlocks();
	}
	
	@Override
	public Set<CFGNode> getCovered() {
		return getCoveredBlocks();
	}
	
	@Override
	public double getCoverage() {
		return getBlockCoverage();
	}
}
