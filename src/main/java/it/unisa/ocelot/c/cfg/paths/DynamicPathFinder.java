package it.unisa.ocelot.c.cfg.paths;

import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.KShortestPaths;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.TooManyFailedPathsException;
import it.unisa.ocelot.c.cfg.WeightedCFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;

public final class DynamicPathFinder {
	private static final int WEIGHT_COVERED = 1;
	private static final int MAX_FAILED_PATHS_INITIAL = 5;
	private static final int MAX_FAILED_PATHS = 20;
	
	private final WeightedCFG cfgWeighted;
	private int k;
	private int maxK;
	private KShortestPaths<CFGNode, LabeledEdge> shortestPaths;

	public DynamicPathFinder(CFG pCfg) {
		//TODO why a weighted CFG, when the only weight given is 1, to mean "covered"? LabeledEdges already have a bit for remembering whether a edge is covered or not: Why not using that?
		this.cfgWeighted = new WeightedCFG(pCfg);
		this.k = 0;
		this.maxK = MAX_FAILED_PATHS_INITIAL;
		this.shortestPaths = new KShortestPaths<>(this.cfgWeighted, this.cfgWeighted.getEntry(), this.maxK);
	}
	
	public List<LabeledEdge> getMaxCoveragePath() {
		List<GraphPath<CFGNode, LabeledEdge>> paths = this.shortestPaths.getPaths(this.cfgWeighted.getExit());
		if (this.k < paths.size()) {
			return paths.get(this.k).getEdgeList();
		} else {
			return null;
		}
	}

	public void notifyFailedPath() throws TooManyFailedPathsException {
		this.k++;
		
		if (this.k >= this.maxK) {
			this.maxK *= 2;
			this.shortestPaths = new KShortestPaths<>(this.cfgWeighted, this.cfgWeighted.getEntry(), this.maxK);
		}
		
		if (this.k > MAX_FAILED_PATHS) {
			throw new TooManyFailedPathsException("" + MAX_FAILED_PATHS);
		}
	}
	
	public void markPathAsCovered(List<LabeledEdge> path) {
		for (LabeledEdge edge : path) {
			this.cfgWeighted.setEdgeWeight(edge, WEIGHT_COVERED);
		}
		reset();
	}
	
	private void reset() {
		this.k = 0;
		//TODO not resetting maxK?
		this.shortestPaths = new KShortestPaths<>(this.cfgWeighted, this.cfgWeighted.getEntry(), MAX_FAILED_PATHS_INITIAL);
	}
}
