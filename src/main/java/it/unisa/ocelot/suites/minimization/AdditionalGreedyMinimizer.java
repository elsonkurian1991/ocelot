package it.unisa.ocelot.suites.minimization;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.simulator.CoverageCalculator;

import java.util.HashSet;
import java.util.Set;

public final class AdditionalGreedyMinimizer extends TestSuiteMinimizer<LabeledEdge> {
	public AdditionalGreedyMinimizer(ConfigManager pConfigManager, CoverageCalculator<LabeledEdge> pCoverageCalculator) {
		super(pConfigManager, pCoverageCalculator);
	}

	@Override
	public Set<TestCase> minimize(Set<TestCase> pTestCases) {
		Set<TestCase> selected = new HashSet<>();
		Set<LabeledEdge> covered = new HashSet<>();
		Set<LabeledEdge> totalCovered = new HashSet<>();
		
		for (TestCase tc : pTestCases) {
			totalCovered.addAll(tc.getCoveredEdges());
		}
		
		do {
			TestCase bestTestCase = null;
			HashSet<LabeledEdge> bestCoveredEdges = new HashSet<>();
			
			//Search the test case with max additional coverage
			for (TestCase tc : pTestCases) {
				HashSet<LabeledEdge> tcCoveredEdges = new HashSet<>(tc.getCoveredEdges());
				tcCoveredEdges.removeAll(covered);
				if (tcCoveredEdges.size() > bestCoveredEdges.size()) {
					bestTestCase = tc;
					bestCoveredEdges = tcCoveredEdges;
				}
			}
			
			if (bestTestCase != null) {
				selected.add(bestTestCase);
				covered.addAll(bestCoveredEdges);
			}
		} while (!covered.equals(totalCovered));
		
		return selected;
	}
}
