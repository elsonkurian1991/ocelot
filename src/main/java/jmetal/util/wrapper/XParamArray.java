package jmetal.util.wrapper;

import jmetal.core.Solution;
import jmetal.core.SolutionType;
import jmetal.encodings.solutionType.ArrayParametersSolutionType;
import jmetal.encodings.variable.ArrayParameters;
import jmetal.util.Configuration;
import jmetal.util.JMException;

/**
 * Wrapper for accessing real-coded solutions.
 */
public class XParamArray {
	private final Solution solution_; //a Solution
	private final SolutionType type_; //this.solution_'s SolutionType.

	/**
	 * Constructor.
	 * 
	 * @param solution a {@link Solution}.
	 */
	public XParamArray(Solution solution) {
		this.solution_ = solution;
		this.type_ = solution.getType();
	}
	
	//TODO the next method rely on reflection. Is it necessary? Can we do better? 

	/**
	 * Gets the value of a encodings.variable.
	 * 
	 * @param kind index of the encodings.variable
	 * @param index index of the encodings.variable
	 * @return the value of the encodings.variable
	 * @throws JMException
	 */
	public double getValue(int kind, int index) throws JMException {
		if (this.solution_.getType().getClass() == ArrayParametersSolutionType.class) {
			return ((ArrayParameters) (this.solution_.getDecisionVariables()[kind])).array_[index];
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.getValue, solution type " +
					this.type_ + " invalid") ;		
			return 0.0 ;
		}
	}

	/**
	 * Sets the value of a encodings.variable
	 * 
	 * @param index Index of the encodings.variable
	 * @param value Value to be assigned
	 * @throws JMException
	 */
	public void setValue(int kind, int index, double value) throws JMException {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			((ArrayParameters)(this.solution_.getDecisionVariables()[kind])).array_[index]=value;
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.setValue, solution type " +
					this.type_ + " invalid");
		}
	}	

	/**
	 * Gets the lower bound of a encodings.variable
	 * 
	 * @param index Index of the encodings.variable
	 * @return The lower bound of the encodings.variable
	 * @throws JMException
	 */
	public double getLowerBound(int kind, int index) throws JMException {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			return ((ArrayParameters)(this.solution_.getDecisionVariables()[kind])).getLowerBound(index);
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.getLowerBound, solution type " +
					this.type_ + " invalid") ;		
			return 0.0;
		}
	}

	/**
	 * Gets the upper bound of a encodings.variable
	 * 
	 * @param index Index of the encodings.variable
	 * @return The upper bound of the encodings.variable
	 * @throws JMException
	 */
	public double getUpperBound(int kind, int index) throws JMException {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			return ((ArrayParameters)(this.solution_.getDecisionVariables()[kind])).getUpperBound(index);
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.getUpperBound, solution type " +
					this.type_ + " invalid") ;		
			return 0.0 ;
		}
	}

	/**
	 * Returns the number of variables of the solution
	 * @return
	 */
	public int getNumberOfDecisionVariables(int kind) {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			return ((ArrayParameters)(this.solution_.getDecisionVariables()[kind])).getLength() ;
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.size, solution type " +
					this.type_ + " invalid") ;		
			return 0;
		}
	}
	
	/**
	 * Returns the number of variables of the solution
	 * @return
	 */
	public int size(int kind) {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			return ((ArrayParameters)(this.solution_.getDecisionVariables()[kind])).getLength() ;
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.size, solution type " +
					this.type_ + " invalid") ;		
			return 0;
		}
	}
	
	public int kinds() {
		if (this.type_.getClass() == ArrayParametersSolutionType.class) {
			return this.solution_.getDecisionVariables().length;
		} else {
			Configuration.logger_.severe("jmetal.util.wrapper.XParam.kinds, solution type " +
					this.type_ + " invalid") ;		
			return 0;
		}
	}
}