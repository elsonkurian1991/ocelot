package it.unisa.ocelot.suites.benchmarks;

import it.unisa.ocelot.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EvaluationBenchmarkCalculator extends BenchmarkCalculator<Integer> {
	public EvaluationBenchmarkCalculator() {
		super("Number of evaluations benchmark");
	}

	//N.B. pSuite is not used
	@Override
	public void measure(String pLabel, Set<TestCase> pSuite, int pEvaluations) {
		add(pLabel, pEvaluations);
	}

	@Override
	public Map<String, Integer> getResults() {
		HashMap<String, Integer> result = new HashMap<>();

		//values is not cumulative
		for (int i = 0; i < this.values.size(); i++) {
			result.put(this.labels.get(i), this.values.get(i));
		}

		return result;
	}

	@Override
	public Map<String, Integer> getCumulativeResults() {
		HashMap<String, Integer> result = new HashMap<>();

		//values is not cumulative
		int total = 0;
		for (int i = 1; i < this.values.size(); i++) {
			total += this.values.get(i);
			result.put(this.labels.get(i), total);
		}

		return result;
	}
}
