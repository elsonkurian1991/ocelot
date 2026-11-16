package it.unisa.ocelot.c.cfg.edges;

import it.unisa.ocelot.c.cfg.CFG;

import java.util.Comparator;

/**
 * This Comparator for {@link LabeledEdge}s
 * compares against the ids of the edges
 * source nodes.
 */
public class EdgeComparator implements Comparator<LabeledEdge> {
	private final CFG cfg;
	
	public EdgeComparator(CFG pCfg) {
		this.cfg = pCfg;
	}

	@Override
	public int compare(LabeledEdge o1, LabeledEdge o2) {
		int idSourceNodeEdge1 = this.cfg.getEdgeSource(o1).getId();
		int idSourceNodeEdge2 = this.cfg.getEdgeSource(o2).getId();
		if (idSourceNodeEdge1 > idSourceNodeEdge2) {
			return 1;
		} else if (idSourceNodeEdge1 < idSourceNodeEdge2) {
			return -1;
		} else {
			return 0;
		}
	}
}
