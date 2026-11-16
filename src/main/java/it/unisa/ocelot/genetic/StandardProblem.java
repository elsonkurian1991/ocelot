package it.unisa.ocelot.genetic;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.types.CType;
import it.unisa.ocelot.simulator.CBridge;
import it.unisa.ocelot.simulator.EventsHandler;
import it.unisa.ocelot.simulator.SimulationException;
import it.unisa.ocelot.simulator.Simulator;
import it.unisa.ocelot.simulator.SimulatorListener;
import it.unisa.ocelot.util.Utils;

import org.apache.commons.lang3.Range;

import jmetal.core.Problem;
import jmetal.core.Solution;
import jmetal.encodings.solutionType.ArrayParametersSolutionType;
import jmetal.util.JMException;

/**
 * A standard Ocelot problem. It has no constraints.
 * It can be multi-objective.
 */
public abstract class StandardProblem extends Problem {
	private static final long serialVersionUID = -462606769605747252L;
	
	private static final int MAX_TRIES = 10;
	
	protected final boolean debug; //TODO should we really put debug info here???
	protected final CFG cfg;
	protected final CType[] parametersTypes;
	protected final int numberOfParametersTypesPointer;
	protected final int arraySize;
	protected final HashMap<Thread, CBridge> bridges;
	protected final double[][] lowerBounds;
	protected final double[][] upperBounds;

	public StandardProblem(String pProblemName, int pObjectives, boolean pDebug, CFG pCfg, int pArraySize, Range<Double>[] pRanges) {
		//do not change them!
		this.problemName_ = pProblemName;
		int numberOfParametersTypes = pCfg.getParameterTypes().length;
		this.numberOfVariables_ = numberOfParametersTypes + 1; //TODO why +1?
		this.numberOfObjectives_ = pObjectives;
		this.numberOfConstraints_ = 0;		
		this.solutionType_ = new ArrayParametersSolutionType(this);
		
		this.debug = pDebug;
		this.cfg = pCfg;
		this.parametersTypes = pCfg.getParameterTypes();
		this.numberOfParametersTypesPointer = (int) Arrays.stream(this.parametersTypes).filter(CType::isPointer).count();
		this.arraySize = pArraySize;
		
		this.bridges = new HashMap<>(); //lazily filled
		
		// Creates this.lowerLimits and this.upperLimits
		this.lowerBounds = new double[this.numberOfParametersTypesPointer + 2][];
		this.upperBounds = new double[this.numberOfParametersTypesPointer + 2][];
		
		fillBounds(pRanges);
	}
	
	/**
	 * Fills with data this.lowerBounds and this.upperBounds.
	 * 
	 * @param pRanges the {@link Range}{@code <}{@link Double}{@code >[]}
	 *        received as a constructor parameter.
	 */
	private void fillBounds(Range<Double>[] pRanges) {
		// Splits pParametersTypes in two partitions: one for value types 
		// and one for pointer types (keeping the original precedences)
		int numberOfParametersTypes = this.parametersTypes.length;
		int numberOfParametersTypesValue = numberOfParametersTypes - this.numberOfParametersTypesPointer;
		CType[] parametersTypesValue = new CType[numberOfParametersTypesValue];
		CType[] parametersTypesPointer = new CType[this.numberOfParametersTypesPointer];
		
		// These two HashMaps will map the position of a parameter in a split array 
		// to its original position in pParametersTypes
		HashMap<Integer, Integer> posValueParameters = new HashMap<>(); 
		HashMap<Integer, Integer> posPointerParameters = new HashMap<>();
		
		// Fills the four data structures above
		int nextFreeSlotValue = 0;
		int nextFreeSlotPointer = 0;
		for (int i = 0; i < numberOfParametersTypes; i++) {
			if (this.parametersTypes[i].isPointer()) {
				parametersTypesPointer[nextFreeSlotPointer] = this.parametersTypes[i];
				posPointerParameters.put(nextFreeSlotPointer, i);
				nextFreeSlotPointer++;
			} else {
				parametersTypesValue[nextFreeSlotValue] = this.parametersTypes[i];
				posValueParameters.put(nextFreeSlotValue, i);
				nextFreeSlotValue++;
			}
		}
		
		// Fills the limits for the parameters with value type;
		// they all go in the first slot
		this.lowerBounds[0] = new double[numberOfParametersTypesValue];
		this.upperBounds[0] = new double[numberOfParametersTypesValue];
		for (int i = 0; i < numberOfParametersTypesValue; i++) {
			int posOriginal = posValueParameters.get(i);
			if (pRanges != null && posOriginal < pRanges.length && pRanges[posOriginal] != null) {
				// We have a user-specified range for the parameter
				this.lowerBounds[0][i] = pRanges[posOriginal].getMinimum();
				this.upperBounds[0][i] = pRanges[posOriginal].getMaximum();
			} else {
				// We don't have a user-specified range for the parameter: 
				// use the full range for the parameter's type
				this.lowerBounds[0][i] = parametersTypesValue[i].getMinValue();
				this.upperBounds[0][i] = parametersTypesValue[i].getMaxValue();
			}
		}
		
		// Fills the limits for the parameters with pointer type. 
		// This is quite messy: each parameter go in a distinct slot,
		// starting with slot 1, but a slot is filled by an *array* 
		// with length pArraySize containing (all identical) limits.
		//TODO why this structure? Why the arrays must have pArraySize?
		for (int i = 1; i <= this.numberOfParametersTypesPointer; i++) {
			this.lowerBounds[i] = new double[this.arraySize];
			this.upperBounds[i] = new double[this.arraySize];
			int posOriginal = posPointerParameters.get(i);
			if (pRanges != null && posOriginal < pRanges.length && pRanges[posOriginal] != null) {
				// We have a user-specified range for the parameter
				for (int j = 0; j < this.arraySize; j++) {
					this.lowerBounds[i][j] = pRanges[posOriginal].getMinimum();
					this.upperBounds[i][j] = pRanges[posOriginal].getMaximum();
				}
			} else {
				// We don't have a user-specified range for the parameter: 
				// use the full range for the parameter's type
				for (int j = 0; j < this.arraySize; j++) {
					this.lowerBounds[i][j] = parametersTypesPointer[i].getMinValue();
					this.upperBounds[i][j] = parametersTypesPointer[i].getMaxValue();
				}
			}
		}
		
		// Finally, the last slot. Here we put another array of limits, 
		// All these limits define the range 0..numberOfParametersTypesPointer - 1
		//TODO what's the meaning of this last slot?
		int lastSlot = this.numberOfParametersTypesPointer + 1;
		this.lowerBounds[lastSlot] = new double[this.numberOfParametersTypesPointer];
		this.upperBounds[lastSlot] = new double[this.numberOfParametersTypesPointer];
		// Sets up the ranges for the variables references (if any)
		for (int i = 0; i < this.numberOfParametersTypesPointer; i++) {
			this.lowerBounds[lastSlot][i] = 0;
			this.upperBounds[lastSlot][i] = this.numberOfParametersTypesPointer - 1;
		}		
	}

	/**
	 * Checks whether this problem is
	 * serendipitous.
	 * 
	 * @param targetClass the class for the targets
	 *        to cover.
	 * @return {@code true} iff this problem implements
	 *         {@link SerendipitousAlgorithm}{@code <targetClass>}.
	 */
	public final boolean isSerendipitous(Class<?> targetClass) {
		return (this instanceof SerendipitousAlgorithm<?>) &&
		((SerendipitousAlgorithm<?>) this).getTargetClass() == targetClass;	
	}
	
	public final CType[] getParametersTypes() {
		return this.parametersTypes;
	}
	
	public final int getNumberOfArrayVariables() {
		return this.numberOfParametersTypesPointer;
	}

	public final int getArraySize() {
		return this.arraySize;
	}
	
	public final StandardProblem setLimits(int pArray) {
		this.lowerLimit_ = new double[this.lowerBounds[pArray].length];
		this.upperLimit_ = new double[this.lowerBounds[pArray].length];
		for (int i = 0; i < this.lowerLimit_.length; ++i) {
			this.lowerLimit_[i] = this.lowerBounds[pArray][i];
			this.upperLimit_[i] = this.upperBounds[pArray][i];
		}
		return this;
	}
	
	public final double getLowerBound(int pArray, int i) {
		return this.lowerBounds[pArray][i];
	}
	
	public final double getUpperBound(int pArray, int i) {
		return this.upperBounds[pArray][i];
	}
	
	@Override
	public final void evaluate(Solution pSolution) throws JMException {
		JMException e = new JMException("Unable to evaluate the solution " + pSolution.getDecisionVariables().toString());
		
		for (int tries = 0; tries < MAX_TRIES; ++tries) {
			try {
				EventsHandler events = run(pSolution);
				simulate(events);
				completeEvaluation(pSolution);
				if (this.debug) {
					printSolution(pSolution);
				}	
				return; //everything went well, no need to try again
			} catch (SimulationException ex) {
				e.addSuppressed(ex);
				continue; //just to clarify the intent of this code
			}
		}
		
		//tries exhausted: throw JMException e
		e.addSuppressed(new Throwable(Arrays.toString(pSolution.getDecisionVariables())));
		throw e;
	}
	
	//TODO why is it necessary to return the post-evaluation branch distance? Understand the issue, and possibly delete this method.
	public abstract double getLastBranchDistance();
	
	protected abstract SimulatorListener[] getListeners();
	
	protected abstract void completeEvaluation(Solution solution);

	//TODO this is useful only for single-objective problems. Perhaps move it.
	protected final void onError(Solution solution, Throwable e) {
		solution.setObjective(0, Double.POSITIVE_INFINITY); //TODO why?
		System.err.println("An error occurred: " + e.getMessage());
	}
	
	protected final Object[][][] getArguments(Solution pSolution) {
		return Utils.getArguments(pSolution, this.parametersTypes);
	}
	
	/**
	 * Runs a {@link Solution}, i.e., runs the C code with
	 * its arguments as input.
	 * 
	 * @param solution the {@link Solution} to be run.
	 * @return a {@link EventHandler} that gathers all the 
	 *         events happened during the C program execution
	 *         in chronological order.
	 */	
	private EventsHandler run(Solution solution) {
		Object[][][] arguments = getArguments(solution);
		boolean ok = argumentsOk(arguments);
		if (!ok) {
			System.err.println("Warning: invalid pointer for " + Arrays.toString(arguments[2][0]));
			solution.setObjective(0, Double.MAX_VALUE); //TODO why?
			return new EventsHandler(); //empty events
		}

		if (this.debug) {
			System.out.println(Utils.printArguments(arguments));
		}
		
		// Calls the JNI interface, runs the C code
		// and grabs the execution events
		CBridge bridge = getCurrentBridge();
		EventsHandler handler = new EventsHandler();
		try {
			bridge.getEvents(handler, arguments[0][0], arguments[1], arguments[2][0]);
		} catch (IOException e) {
			onError(solution, e);
		} catch (RuntimeException e) {
			onError(solution, e);
		}
		
		return handler;
	}
	
	private boolean argumentsOk(Object[][][] arguments) {
		for (Object obj : arguments[2][0]) {
			Integer pointerRef = (Integer) obj;
			if (pointerRef < 0 || pointerRef >= this.numberOfParametersTypesPointer) {
				return false;
			}
		}
		return true;
	}
		
	//TODO if we go multithreading (or if we define a subclass of StandardProblem that supports multithreading) remember to synchronize this method!
	private CBridge getCurrentBridge() {
		CBridge result = this.bridges.get(Thread.currentThread());
		if (result == null) {
			result = new CBridge(this.bridges.size());
			this.bridges.put(Thread.currentThread(), result); 
		}
		
		return result;
	}
	
	//TODO fix branch tracking and delete the Simulator class
	private void simulate(EventsHandler handler) throws SimulationException {
		// Simulates the events on the CFG if we have some listener
		if (getListeners().length > 0) {
			Simulator simulator = new Simulator(this.cfg, handler.getEvents());
			simulator.addListeners(getListeners());
			simulator.simulate();
		}
	}
	
	private void printSolution(Solution pSolution) {
		if (this.debug) {
			Object[][][] arguments = getArguments(pSolution);
			System.out.println(Utils.printArguments(arguments) + "\nObjective value: " + pSolution.getObjective(0));
		}
	}
}
