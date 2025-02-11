package it.unisa.ocelot.genetic.edges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
import it.unisa.ocelot.runnable.Run;
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

		Dominators<CFGNode, LabeledEdge> dominators = new Dominators<CFGNode, LabeledEdge>(this.cfg,
				this.cfg.getStart());

		this.dominators = dominators.getStrictDominators(parent);
	}

	public double evaluateSolution(Solution solution) throws JMException, SimulationException {
		// Remove previously generated fitnessValues file
		File file = new File("fitnessValues.txt");
		try {
			Files.deleteIfExists(file.toPath());
			//System.out.println("fitnessValues-----deleted");	
		} catch (IOException e) {
			System.err.println("Error deleting file fitnessValues.txt: from::>evaluateSolution () " + e.getMessage());
		}
		Object[][][] arguments = this.getParameters(solution);

		CBridge bridge = getCurrentBridge(); // calling the JNI interface to execute the code!!

		EventsHandler handler = new EventsHandler();
		EdgeDistanceListener bdalListener = new EdgeDistanceListener(cfg, target, dominators);
		bdalListener.setSerendipitousPotentials(this.serendipitousPotentials);

		try { // grab the execution events
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
		double objective;
		if (Run.isExpWithEvalFun) {
			// Here we need to read the fitness values to objectives from the files that we
			// wrote before.
			int i = 0;
			double fitnessEvalPC = 0.0;
			// System.out.println("calling CalculateFitness in EdgeCov_
			// before:"+fitnessEvalPC);
			fitnessEvalPC = CalculateFitnessFromEvalPC3.CalculateFitness(arguments);

			objective = bdalListener.getNormalizedBranchDistance() + bdalListener.getApproachLevel() + fitnessEvalPC;

		} else {
			objective = bdalListener.getNormalizedBranchDistance() + bdalListener.getApproachLevel();
		}

		solution.setObjective(0, objective);

		if (debug)
			System.out.println(Utils.printParameters(arguments) + "\nObjective: " + objective);

		return bdalListener.getBranchDistance();
	}

	public Set<LabeledEdge> getSerendipitousCovered() {
		return serendipitousCovered;
	}
}
