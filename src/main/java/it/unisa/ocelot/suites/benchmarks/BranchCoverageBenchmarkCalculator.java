package it.unisa.ocelot.suites.benchmarks;

import it.unisa.ocelot.TestCase;
import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.simulator.CoverageCalculatorEdges;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BranchCoverageBenchmarkCalculator extends BenchmarkCalculator<Double> {
	private final CoverageCalculatorEdges calculator;
	
	public BranchCoverageBenchmarkCalculator(CFG pCfg) {
		super("Branch coverage benchmark");
		
		this.calculator = new CoverageCalculatorEdges(pCfg);
	}
	
	//NB: pEvaluations is not used
	@Override
	public void measure(String pLabel, Set<TestCase> pSuite, int pEvaluations) {
		this.calculator.updateCoverage(pSuite);
		add(pLabel, this.calculator.getBranchCoverage());
	}
	
	@Override
	public Map<String, Double> getResults() {
		HashMap<String, Double> result = new HashMap<>();
		
		//values is cumulative
		result.put(this.labels.get(0), 0d);
		double previous = this.values.get(0);
		for (int i = 1; i < this.values.size(); i++) {
			double current = this.values.get(i);
			double difference = current - previous;
			result.put(this.labels.get(i), difference);
			previous = current;
		}
		
		return result;
	}

	@Override
	public Map<String, Double> getCumulativeResults() {
		HashMap<String, Double> result = new HashMap<>();
		
		//values is cumulative
		double first = this.values.get(0);
		for (int i = 0; i < this.values.size(); i++) {
			double difference = this.values.get(i) - first;
			result.put(this.labels.get(i), difference);
		}
		
		return result;
	}
}
