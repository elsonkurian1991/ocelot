package it.unisa.ocelot.genetic.settings;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.util.JMException;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
public class MOSASettingsGeneric extends GenericGASettings {
	//MOSA parameters
	private double maxCoverage = 1.0d;
	// EvInT additional MOSA parameters
	private boolean isRandomRun = false;
	private int experimentTime = 0;
	
	public MOSASettingsGeneric(Problem pProblem) {
		super(pProblem);
	}
	
	public MOSASettingsGeneric(Problem pProblem, ConfigManager pConfig) {
		super(pProblem, pConfig);
		
		//gets the settings
		try {
			this.maxCoverage = pConfig.getRequiredCoverage();
		} catch (NumberFormatException e) {}
		
		try {
			this.isRandomRun = pConfig.isRandomRun();
		} catch (NumberFormatException e) {}
		
		try {
			this.experimentTime = pConfig.getExperimentTime();
		} catch (NumberFormatException e) {}
	}
	
	@Override
	public Algorithm configure(Algorithm algorithm) throws JMException {
		super.configure(algorithm);
		algorithm.setInputParameter("maxCoverage", this.maxCoverage);
		algorithm.setInputParameter("isRandomRun", this.isRandomRun);
		algorithm.setInputParameter("experimentTime", this.experimentTime);
		return algorithm;
	}
	
	@Override
	public Algorithm configure() throws JMException {
		Algorithm algorithm = new MOSA_Generic((MOSAGenericCoverageProblem) problem_);
		return configure(algorithm);
    }
}
