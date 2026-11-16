package it.unisa.ocelot.genetic.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AlgorithmStats {
	private Map<String, Object> stats;
	private String log;
	private ArrayList<Integer> evaluationsHistory;
	
	public AlgorithmStats() {
		this.stats = new HashMap<>();
		this.log = "";
		this.evaluationsHistory = new ArrayList<>();
	}
	
	public void setStat(String pKey, Object pValue) {
		this.stats.put(pKey, pValue);
	}
	
	public Object getStat(String pKey) {
		return this.stats.get(pKey);
	}
	
	public void addEvaluations(int pEvaluations) {
		this.evaluationsHistory.add(pEvaluations);
	}
	
	public int getEvaluations() {
		return this.evaluationsHistory.stream().reduce(0, Integer::sum);
	}
	
	public List<Integer> getEvaluationsHistory() {
		return Collections.unmodifiableList(this.evaluationsHistory);
	}
	
	public void log(String pString) {
		this.log += "---------------------------------------------------\n";
		this.log += "Log entry " + new Date().toString() + "\n";
		this.log += pString + "\n";
	}
	
	public String getLog() {
		return this.log;
	}
}
