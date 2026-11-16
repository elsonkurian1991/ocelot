package it.unisa.ocelot.suites.benchmarks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;

public final class TestSuiteSizeBenchmarkCalculator extends BenchmarkCalculator<Integer> {
	public TestSuiteSizeBenchmarkCalculator(CFG pCFG) {
		super("Test suite size benchmark");
	}

	//NB: pEvaluations is not used
	@Override
	public void measure(String pLabel, Set<TestCase> pSuite, int pEvaluations) {
		add(pLabel, pSuite.size());
	}

	@Override
	public Map<String, Integer> getResults() {
		HashMap<String, Integer> result = new HashMap<>();

		//values is cumulative
		result.put(this.labels.get(0), 0);
		int previous = this.values.get(0);
		for (int i = 1; i < this.values.size(); i++) {
			int current = this.values.get(i);
			int difference = current - previous;
			result.put(this.labels.get(i), difference);
			previous = current;
		}

		return result;
	}

	@Override
	public Map<String, Integer> getCumulativeResults() {
		HashMap<String, Integer> result = new HashMap<>();

		//values is cumulative
		int first = this.values.get(0);
		for (int i = 0; i < this.values.size(); i++) {
			int difference = this.values.get(i) - first;
			result.put(this.labels.get(i), difference);
		}

		return result;
	}
}
