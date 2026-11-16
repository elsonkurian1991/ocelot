package it.unisa.ocelot.suites.generators.many_objective;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.DominanceCalculatorVertices;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.edge_graph.EdgeGraph;
import it.unisa.ocelot.c.edge_graph.Wrapper;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.suites.generators.CascadeableTestSuiteGenerator;

/**
 * Many-Objective test suite generator. Approach inspired by Panichella et al. (Reformulating Branch Coverage as a 
 * Many-Objective Optimization Problem). The set of branches that have to be covered is reduced: only non-dominant
 * branches are selected as targets.
 * @author simone
 *
 */
//TODO sooner or later this class will not be a subclass of MOSATestSuiteGenerator anymore
public final class ReducedMOSATestSuiteGenerator extends MOSATestSuiteGenerator implements CascadeableTestSuiteGenerator {
	public ReducedMOSATestSuiteGenerator(ConfigManager config, CFG cfg) {
		super(config, cfg, getRealTargets(cfg));
	}
	
	private static Set<LabeledEdge> getRealTargets(CFG pCfg) {
		//Creates an EdgeGraph for the CFG, gets all the 
		//non-dominator nodes and retrieves the original 
		//edges from the wrappers in a list that will be used
		EdgeGraph<CFGNode, LabeledEdge> graph = new EdgeGraph<>(pCfg, pCfg.getEntry(), pCfg.getExit());
		
		DominanceCalculatorVertices<Wrapper<LabeledEdge>, DefaultEdge> dominators = 
				new DominanceCalculatorVertices<>(graph, graph.getEntry());
		
		Set<Wrapper<LabeledEdge>> nonDominators = dominators.getNonDominators();
		nonDominators.remove(graph.getExit());
		HashSet<LabeledEdge> realTargets = new HashSet<>();
		int nextAvailableObjectiveId = 0;
		for (Wrapper<LabeledEdge> wrapper : nonDominators) {
			LabeledEdge target = wrapper.getWrapped();
			target.setObjectiveId(nextAvailableObjectiveId);
			realTargets.add(target);
			nextAvailableObjectiveId++;
		}
		
		return realTargets;
	}
}
