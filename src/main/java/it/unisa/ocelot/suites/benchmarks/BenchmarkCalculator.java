package it.unisa.ocelot.suites.benchmarks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unisa.ocelot.TestCase;

/**
 * An abstract calculator for benchmarks.
 * 
 * @param <T> the type of the values returned by the benchmark.
 */
public abstract class BenchmarkCalculator<T> {
	private final String name;
	private final HashMap<String, Integer> labelCategories;
	protected final ArrayList<String> labels;
	protected final ArrayList<T> values;

	protected BenchmarkCalculator(String pName) {
		this.name = pName;
		this.labelCategories = new HashMap<>();
		this.labels = new ArrayList<>();
		this.values = new ArrayList<>();
	}
	
	public String getName() {
		return this.name;
	}

	public final List<String> getLabels() {
		return this.labels;
	}
	
	public final void start(Set<TestCase> pSuite) {
		measure("Start", pSuite, 0); 
	}
	
	public final void end(Set<TestCase> pSuite, int pEvaluations) {
		measure("End", pSuite, pEvaluations);
	}
	
	public abstract void measure(String pLabel, Set<TestCase> pSuite, int pEvaluations);
	public abstract Map<String, T> getResults();
	public abstract Map<String, T> getCumulativeResults();

	public final void removeLast() {
		this.labels.remove(this.labels.size() - 1);
		this.values.remove(this.values.size() - 1);
	}

	protected final void add(String pLabel, T value) {
		this.labels.add(getRealLabel(pLabel));
		this.values.add(value);
	}


	private String getRealLabel(String pLabel) {
		if (this.labelCategories.containsKey(pLabel)) {
			this.labelCategories.put(pLabel, this.labelCategories.get(pLabel) + 1);
		} else {
			this.labelCategories.put(pLabel, 1);
		}

		return pLabel + " " + this.labelCategories.get(pLabel);
	}
	
	public final String getPrintableResults() {
		return getPrintableResultsBoth(getResults());
	}

	public final String getPrintableCumulativeResults() {
		return getPrintableResultsBoth(getCumulativeResults());
	}
	
	private String getPrintableResultsBoth(Map<String, T> benchmarkResults) {
		StringBuilder result = new StringBuilder();
		for (String label : this.labels) {
			result.append(label);
			result.append(": ");
			result.append(benchmarkResults.get(label).toString());
			result.append('\n');
		}
		return result.toString();
	}
	
	@Override
	public final String toString() {
		return getName() + "\n" + getPrintableResults();
	}
}
