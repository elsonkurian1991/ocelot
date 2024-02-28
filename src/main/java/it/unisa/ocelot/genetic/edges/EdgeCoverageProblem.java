package it.unisa.ocelot.genetic.edges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.dominators.Dominators;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.cfg.nodes.CFGNodeNavigator;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.SerendipitousProblem;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.EventsHandler;
import it.unisa.ocelot.simulator.SimulationException;
import it.unisa.ocelot.simulator.Simulator;
import it.unisa.ocelot.util.Utils;

import org.apache.commons.lang3.Range;

import jmetal.core.Solution;
import jmetal.util.JMException;

public class EdgeCoverageProblem extends StandardProblem implements SerendipitousProblem<LabeledEdge> {
	private static final long serialVersionUID = 1930014794768729268L;

	private CFG cfg;
	private LabeledEdge target;
	private Set<CFGNode> dominators;
	private Set<LabeledEdge> serendipitousPotentials;
	private Set<LabeledEdge> serendipitousCovered;

	private boolean debug;

	public EdgeCoverageProblem(CFG pCfg, CType[] pParameters, Range<Double>[] pRanges, int pArraySize)
			throws Exception {
		super(pParameters, pRanges, pArraySize);
		this.cfg = pCfg;
		problemName_ = "EdgeCoverageProblem";
	}
	
	public EdgeCoverageProblem(CFG pCfg, CType[] pParameters, int pArraySize) throws Exception {
		this(pCfg, pParameters, null, pArraySize);
	}

	public CFG getCFG() {
		return cfg;
	}
	
	public void setSerendipitousPotentials(Set<LabeledEdge> serendipitousPotentials) {
		this.serendipitousPotentials = serendipitousPotentials;
		this.serendipitousPotentials.remove(this.target);
	}

	public CFGNodeNavigator navigate() {
		return cfg.getStart().navigate(cfg);
	}

	public void setTarget(LabeledEdge pEdge) {
		this.target = pEdge;
		
		CFGNode parent = this.cfg.getEdgeSource(pEdge);
		
		Dominators<CFGNode, LabeledEdge> dominators = new Dominators<CFGNode, LabeledEdge>(this.cfg, this.cfg.getStart());
		
		this.dominators = dominators.getStrictDominators(parent);
	}

	public double evaluateSolution(Solution solution) throws JMException, SimulationException {
		Object[][][] arguments = this.getParameters(solution);

		CBridge bridge = getCurrentBridge(); // calling the JNI interface to execute the code!!

		EventsHandler handler = new EventsHandler();
		EdgeDistanceListener bdalListener = new EdgeDistanceListener(cfg, target, dominators);
		bdalListener.setSerendipitousPotentials(this.serendipitousPotentials);
		
		try { //grab the  execution events
			bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0]);
		} catch (RuntimeException e) {
			this.onError(solution, e);
			return -1;
		}

		Simulator simulator = new Simulator(cfg, handler.getEvents());

		simulator.addListener(bdalListener);

		simulator.simulate();
		
		this.serendipitousCovered = bdalListener.getSerendipitousCovered();
		this.serendipitousPotentials.removeAll(this.serendipitousCovered);
		//Here we need to read the fitness values to objectives from the files that we wrote before.
		int  i=0;
        double fitnessEvalPC = 0.0;
        System.out.println("calling CalculateFitness in EdgeCov_ before:"+fitnessEvalPC);
		fitnessEvalPC = CalculateFitnessFromEvalPC.CalculateFitness();
		System.out.println("calling CalculateFitness in EdgeCov_ after:"+fitnessEvalPC);
		
		/*double fitnessPC1 = 0.0;
		double fitnessPC2 = 0.0;
		double fitnessPC3 = 0.0;
		try {
			String data = new String(Files.readAllBytes(Paths.get("evalPC1.txt")));
			fitnessPC1 = Double.parseDouble(data);
			data = new String(Files.readAllBytes(Paths.get("evalPC2.txt")));
			fitnessPC2 = Double.parseDouble(data);
			data = new String(Files.readAllBytes(Paths.get("evalPC3.txt")));
			fitnessPC3 = Double.parseDouble(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//double objective =fitnessEvalPC;
		double objective = bdalListener.getNormalizedBranchDistance() + bdalListener.getApproachLevel() + fitnessEvalPC;
		
		solution.setObjective(0, objective);
		
		if (debug)
			System.out.println(Utils.printParameters(arguments) + "\nObjective: " + objective);
		
		return bdalListener.getBranchDistance();
		//return objective;
	}
	
	public Set<LabeledEdge> getSerendipitousCovered() {
		return serendipitousCovered;
	}
}
