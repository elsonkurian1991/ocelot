package it.unisa.ocelot.simulator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.simulator.listeners.CoverageCalculatorListener;
import it.unisa.ocelot.util.Utils;

/**
 * Calculates several coverage metrics.
 */
public abstract class CoverageCalculatorCFG<T> extends CoverageCalculator<T> {
	private final CFG cfg;
	private final CoverageCalculatorListener coverageListener;
	
	private Set<LabeledEdge> edges;
	private Set<LabeledEdge> uncoveredEdges;
	private Set<LabeledEdge> coveredEdges;
	private double edgeCoverage;
	private Set<LabeledEdge> branches;
	private Set<LabeledEdge> uncoveredBranches;
	private Set<LabeledEdge> coveredBranches;
	private double branchCoverage;
	private Set<CFGNode> blocks;
	private Set<CFGNode> uncoveredBlocks;
	private Set<CFGNode> coveredBlocks;
	private double blockCoverage;
	
	protected CoverageCalculatorCFG(CFG pCfg) {
		this.cfg = pCfg;
		this.coverageListener = new CoverageCalculatorListener(this.cfg);
		resetCoverage();
	}
	
	@Override
	public void resetCoverage() {
		for (LabeledEdge edge : this.cfg.edgeSet()) {
			edge.setUncovered();
		}
		this.edges = this.cfg.edgeSet();
		this.uncoveredEdges = new HashSet<>(this.edges);
		this.coveredEdges = new HashSet<>();
		this.branches = this.cfg.edgeSetBranch();
		this.uncoveredBranches = new HashSet<>(this.branches);
		this.coveredBranches = new HashSet<>();
		this.blocks = new HashSet<>(this.cfg.vertexSet());
		this.uncoveredBlocks = new HashSet<>(this.blocks);
		this.coveredBlocks = new HashSet<>();
		updateCoverage();
	}
	
	@Override
	public final void updateCoverage(TestCase testCase) {
		if (testCase.hasCoverageInformationCFG()) {
			//safety update, as possibly this testCase was updated elsewhere
			this.uncoveredEdges.removeAll(testCase.getCoveredEdges());
			this.coveredEdges.addAll(testCase.getCoveredEdges());
			this.uncoveredBranches.removeAll(testCase.getCoveredBranches());
			this.coveredBranches.addAll(testCase.getCoveredBranches());
			this.uncoveredBlocks.removeAll(testCase.getCoveredBlocks());
			this.coveredBlocks.addAll(testCase.getCoveredBlocks());
		} else {
			//TODO Ensure we NEVER RERUN! Inject coverage information in the test at solution evaluation time
			Object[][][] arguments = testCase.getArguments();

			CBridge bridge = new CBridge();
			EventsHandler handler = new EventsHandler();
			try {
				bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0]);
			} catch (IOException e) {
				throw new RuntimeException(e); //TODO improve
			}

			Simulator simulator = new Simulator(this.cfg, handler.getEvents());
			simulator.addListener(this.coverageListener);
			simulator.simulate();

			if (!simulator.isSimulationCorrect()) {
				throw new RuntimeException("Simulation error for arguments " + Utils.printArguments(arguments));
			}
			
			testCase.setCoverageCFG(this.coverageListener.getCoveredPath(), this.cfg);

			this.uncoveredEdges.removeAll(this.coverageListener.getCoveredEdges());
			this.coveredEdges.addAll(this.coverageListener.getCoveredEdges());
			this.uncoveredBranches.removeAll(this.coverageListener.getCoveredBranches());
			this.coveredBranches.addAll(this.coverageListener.getCoveredBranches());
			this.uncoveredBlocks.removeAll(this.coverageListener.getCoveredBlocks());
			this.coveredBlocks.addAll(this.coverageListener.getCoveredBlocks());
		}
	}
	
	private void updateCoverage() {
		this.edgeCoverage = ((double) this.coveredEdges.size()) / this.edges.size();
		this.branchCoverage = ((double) this.coveredBranches.size()) / this.branches.size();
		this.blockCoverage = ((double) this.coveredBlocks.size() + 1) / this.blocks.size(); //TODO for some reason in the old code there was that "+ 1" term
	}
	
	public final Set<LabeledEdge> getUncoveredEdges() {
		return new HashSet<>(this.uncoveredEdges);
	}
	
	public final Set<LabeledEdge> getCoveredEdges() {
		return new HashSet<>(this.coveredEdges);
	}
	
	public final double getEdgeCoverage() {
		return this.edgeCoverage;
	}
	
	public final Set<LabeledEdge> getUncoveredBranches(){
		return new HashSet<>(this.uncoveredBranches);
	}
	
	public final Set<LabeledEdge> getCoveredBranches(){
		return new HashSet<>(this.coveredBranches);
	}
	
	public final double getBranchCoverage() {
		return this.branchCoverage;
	}
	
	public final Set<CFGNode> getUncoveredBlocks() {
		return new HashSet<>(this.uncoveredBlocks);
	}
	
	public final Set<CFGNode> getCoveredBlocks() {
		return new HashSet<>(this.coveredBlocks);
	}
		
	public final double getBlockCoverage() {
		return this.blockCoverage;
	}
}
