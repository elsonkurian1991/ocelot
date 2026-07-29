package it.unisa.ocelot.genetic.many_objective;

import java.util.List;
import org.apache.commons.lang3.Range;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.VariableTranslator;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.genetic.objectives.chains.BranchChainManager;
import it.unisa.ocelot.genetic.solutions.CacheAccessor;
import it.unisa.ocelot.genetic.solutions.GenericSolution;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.EventsHandler;
import it.unisa.ocelot.simulator.SimulationException;
import it.unisa.ocelot.simulator.Simulator;
import jmetal.core.Solution;
import jmetal.util.JMException;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Class representing a many-objective optimization branch coverage problem
 * 
 * @author giograno
 *
 */
public class MOSAGenericCoverageProblem extends StandardProblem {

	private final CFG cfg;
	
	private final List<GenericObjective> objectives;

	private static final long serialVersionUID = 1L;

	public MOSAGenericCoverageProblem(CFG cfg, CType[] parameters, int pArraySize,
			Range<Double>[] ranges, List<GenericObjective> objectives) throws Exception {
		super(parameters, ranges, pArraySize);
		numberOfObjectives_ = objectives.size();
		this.cfg = cfg;
		this.objectives = objectives;
	}

	/**
	 * Evaluate a solution
	 * 
	 * @param solution
	 *            The solution to evaluate
	 */
	@Override
	public double evaluateSolution(Solution solution) throws JMException, SimulationException {
		VariableTranslator translator = new VariableTranslator(solution);
		Object[][][] arguments = translator.translateArray(this.parameters);
		
		CBridge bridge = getCurrentBridge();
		EventsHandler handler = new EventsHandler();
		try {
			bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0]);
		} catch (RuntimeException e) {
			this.onError(solution, e);
			return -1;
		}

		Simulator simulator = new Simulator(cfg, handler.getEvents());
		
		simulator.simulate();
		
		if (solution instanceof GenericSolution) {
			BranchChainManager.cacheFitnessValues((GenericSolution) solution);
		}
		
		//Not important, MOSA uses his own algorithm for given objectives
		return 0;
	}

	/**
	 * Returns the set of objectives representing the target of our coverage
	 * problem
	 * 
	 * @return a List of Labeled objectives
	 */
	public List<GenericObjective> getTargetObjectives() {
		return this.objectives;
	}

	/**
	 * Returns the control flow graph of the function under test
	 * 
	 * @return a CFG
	 */
	public CFG getControlFlowGraph() {
		return this.cfg;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
