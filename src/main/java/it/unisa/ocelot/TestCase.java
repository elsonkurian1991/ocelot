package it.unisa.ocelot;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.genetic.solutions.CacheAccessor;
import jmetal.core.Solution;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCase implements CacheAccessor {
	private int id;
	private Solution solution; 
	private Object[][][] parameters;
	private Object oracle;
	private List<LabeledEdge> coveredPath;
	private Set<LabeledEdge> coveredEdges;
	private Object cacheObject;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Solution getSolution() {
		return solution;
	}
	public void setSolution(Solution solution) {
		this.solution = solution;
	}
	public Set<LabeledEdge> getCoveredEdges() {
		return coveredEdges;
	}
	public List<LabeledEdge> getCoveredPath() {
		return coveredPath;
	}
	public void setCoveredPath(List<LabeledEdge> coveredEdges) {
		this.coveredPath = coveredEdges;
		this.coveredEdges = new HashSet<>(coveredEdges);
	}
	public Object[][][] getParameters() {
		return parameters;
	}
	public void setParameters(Object[][][] parameters) {
		this.parameters = parameters;
	}
	public Object getOracle() {
		return oracle;
	}
	public void setOracle(Object oracle) {
		this.oracle = oracle;
	}
	@Override
	public Object getCacheObject() {
		return this.cacheObject;
	}
	@Override
	public void setCacheObject(Object cacheObject) {
		this.cacheObject = cacheObject;
	}
}
