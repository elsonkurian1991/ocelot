package it.unisa.ocelot.suites.benchmarks;

import it.unisa.ocelot.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class TimeBenchmarkCalculator extends BenchmarkCalculator<Long> {
	//N.B. times are stored in milliseconds, but are emitted in seconds!
	public TimeBenchmarkCalculator() {
		super("Time benchmark");
	}
	
	//NB: pSuite and pEvaluations are not used
	@Override
	public void measure(String pLabel, Set<TestCase> pSuite, int pEvaluations) {
		add(pLabel, System.currentTimeMillis());
	}
	
	@Override
	public Map<String, Long> getResults() {
		HashMap<String, Long> result = new HashMap<>();
		
		//values is cumulative
		result.put(this.labels.get(0), 0l);
		long previous = this.values.get(0);
		for (int i = 1; i < this.values.size(); i++) {
			long current = this.values.get(i);
			long differenceMilliseconds = current - previous;
			long differenceSeconds = differenceMilliseconds / 1000;
			result.put(this.labels.get(i), differenceSeconds);
			previous = current;
		}
		
		return result;
	}

	@Override
	public Map<String, Long> getCumulativeResults() {
		HashMap<String, Long> result = new HashMap<>();
		
		//values is cumulative
		long first = this.values.get(0);
		for (int i = 0; i < this.values.size(); i++) {
			long differenceMilliseconds = this.values.get(i) - first;
			long differenceSeconds = differenceMilliseconds / 1000;
			result.put(this.labels.get(i), differenceSeconds);
		}
		
		return result;
	}
}
