package it.unisa.ocelot.genetic.edges;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.CollateralCoverageEvaluator;
import it.unisa.ocelot.c.cfg.dominators.Dominators;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.VariableTranslator;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.EventsHandler;
import it.unisa.ocelot.simulator.SimulationException;
import it.unisa.ocelot.simulator.Simulator;
import it.unisa.ocelot.simulator.listeners.CoverageCalculatorListener;
import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;
import jmetal.util.JMException;

public class CDG_BasedProblem extends StandardProblem {

	private CFG controlFlowGraph;
	private List<LabeledEdge> branches;

	private LabeledEdge target;
	
	private Set<CFGNode> dominators;

	private boolean debug;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CDG_BasedProblem(CFG controlFlowGraph, CType[] parameters, int pArraySize,
			Range<Double>[] ranges, List<LabeledEdge> branches, LabeledEdge target) throws Exception {
		super(parameters, ranges, pArraySize);
		this.controlFlowGraph = controlFlowGraph;
		this.branches = branches;
		this.target = target;
		
		CFGNode parent = this.controlFlowGraph.getEdgeSource(this.target);
		
		Dominators<CFGNode, LabeledEdge> dominators = new Dominators<CFGNode, LabeledEdge>(this.controlFlowGraph, this.controlFlowGraph.getStart());
		
		this.dominators = dominators.getStrictDominators(parent);
	}

	@Override
	public double evaluateSolution(Solution solution) throws JMException, SimulationException {
		VariableTranslator translator = new VariableTranslator(solution);
		Object[][][] arguments = translator.translateArray(this.parameters);

		if (debug)
			System.out.println(Utils.printParameters(arguments));

		CBridge bridge = getCurrentBridge();
		EventsHandler handler = new EventsHandler();
		bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0]);

		// listener
		EdgeDistanceListener dominatorListener = new EdgeDistanceListener(this.controlFlowGraph, target, this.dominators);
		//DominatorListener dominatorListener = new DominatorListener(controlFlowGraph, target);
		CoverageCalculatorListener coverageCalculatorListener = new CoverageCalculatorListener(
				controlFlowGraph);

		// simulation
		Simulator simulator = new Simulator(controlFlowGraph, handler.getEvents());
		simulator.addListener(dominatorListener);
		simulator.addListener(coverageCalculatorListener);
		simulator.simulate();

		CollateralCoverageEvaluator collateralCoverageEvaluator = new CollateralCoverageEvaluator(
				controlFlowGraph, target, branches, coverageCalculatorListener.getCoveredBranches());

		solution.setObjective(0, dominatorListener.getFitness());
//		double l_e = collateralCoverageEvaluator.calculateCollateralCoverage();
//		System.out.println(l_e);
//		solution.setFitness(1-l_e);
		//Here we need to read the fitness values to detFitness from the files that we wrote before.
		double fitnessEvalPC = 0.0;
		System.out.println("calling CalculateFitness in CDG_ before:"+fitnessEvalPC);
		fitnessEvalPC = CalculateFitnessFromEvalPC2.CalculateFitness(arguments);
		System.out.println("calling CalculateFitness in CDG_ after:"+fitnessEvalPC);
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
		}
		*/
		System.out.println("FinalEvalPC is :"+fitnessEvalPC);
		solution.setFitness(1 - collateralCoverageEvaluator.calculateCollateralCoverage()
				+ fitnessEvalPC);
		
		return dominatorListener.getBranchDistance();
	}

	


	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
