package it.unisa.ocelot;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TestCase {
	private final int id;
	private final Solution solution;
	private final Object[][][] arguments;
	private Set<? extends GenericObjective> coveredObjectives;
	private List<LabeledEdge> coveredPath;
	private Set<LabeledEdge> coveredEdges;
	private Set<LabeledEdge> coveredBranches;
	private Set<CFGNode> coveredBlocks;
	
	public TestCase(int pId, Solution pSolution, CType[] pParametersTypes) {
		this.id = pId;
		this.solution = pSolution;
		this.arguments = Utils.getArguments(this.solution, pParametersTypes);
		this.coveredObjectives = null;
		this.coveredEdges = null;
		this.coveredBranches = null;
		this.coveredBlocks = null;
	}
	
	public TestCase(int pId, Object[][][] pArguments) {
		this.id = pId;
		this.solution = null;
		this.arguments = pArguments;
		this.coveredObjectives = null;
		this.coveredEdges = null;
		this.coveredBranches = null;
		this.coveredBlocks = null;
	}
	
	public int getId() {
		return this.id;
	}
	
	public Solution getSolution() {
		return this.solution;
	}
	
	public Object[][][] getArguments() {
		return this.arguments;
	}
	
	public boolean hasCoverageInformationObjectives() {
		return this.coveredObjectives != null;
	}
	
	public Set<? extends GenericObjective> getCoveredObjectives() {
		return Collections.unmodifiableSet(this.coveredObjectives);
	}
	
	public boolean hasCoverageInformationCFG() {
		return this.coveredPath != null;
	}
	
	public List<LabeledEdge> getCoveredPath() {
		return Collections.unmodifiableList(this.coveredPath);
	}
	
	public Set<LabeledEdge> getCoveredEdges() {
		return Collections.unmodifiableSet(this.coveredEdges);
	}
	
	public Set<LabeledEdge> getCoveredBranches() {
		return Collections.unmodifiableSet(this.coveredBranches);
	}
	
	public Set<CFGNode> getCoveredBlocks() {
		return Collections.unmodifiableSet(this.coveredBlocks);
	}
	
	/**
	 * Sets the coverage of the test (GenericObjective-based).
	 * 
	 * @param pCoveredObjectives the {@link GenericObjective}s covered by the test execution.
	 */
	public void setCoverageObjectives(Set<? extends GenericObjective> pCoveredObjectives) {
		if (hasCoverageInformationObjectives()) {
			throw new RuntimeException("Attempted to set coverage information for a test case twice");
		}
		this.coveredObjectives = Collections.unmodifiableSet(new HashSet<>(pCoveredObjectives));
	}
	
	/**
	 * Sets the coverage of the test (CFG-based).
	 * 
	 * @param pCoveredPath the path covered by the test execution.
	 * @param pCfg the {@link CFG} of the executed function.
	 */
	public void setCoverageCFG(List<LabeledEdge> pCoveredPath, CFG pCfg) {
		if (hasCoverageInformationCFG()) {
			throw new RuntimeException("Attempted to set coverage information for a test case twice");
		}
		this.coveredPath = pCoveredPath;
		this.coveredEdges = Collections.unmodifiableSet(new HashSet<>(pCoveredPath));
		this.coveredBranches = pCoveredPath.stream().filter(LabeledEdge::isBranch).collect(Collectors.toUnmodifiableSet());
		Set<CFGNode> coveredBlocks = pCoveredPath.stream().map(pCfg::getEdgeTarget).collect(Collectors.toSet());
		if (pCoveredPath.size() > 0) {
			coveredBlocks.add(pCfg.getEdgeSource(pCoveredPath.get(0)));
		}
		this.coveredBlocks = Collections.unmodifiableSet(coveredBlocks);
	}
}
