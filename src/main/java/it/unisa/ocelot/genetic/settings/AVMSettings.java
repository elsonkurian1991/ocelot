package it.unisa.ocelot.genetic.settings;

import java.util.List;

import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.StandardProblem;
import it.unisa.ocelot.genetic.StandardSettings;
import it.unisa.ocelot.genetic.algorithms.AVM;
import jmetal.util.JMException;

/**
 * Configures the {@link AVM} algorithm.
 */
public final class AVMSettings extends StandardSettings {
	private static final double EPSILON = 0.001;
	private static final double DELTA = 1;
	
	private final StandardProblem standardProblem;
	
	public AVMSettings(StandardProblem pProblem, List<Double> pConstantsNumeric, ConfigManager pConfigManager) {
		super(pProblem, pConstantsNumeric, pConfigManager);
		this.standardProblem = (StandardProblem) this.problem_;
	}
	
	@Override
	protected AVM doConfigure() throws JMException {
		double epsilon;
		try {
			epsilon = this.configManager.getAvmEpsilon();
		} catch (NumberFormatException e) {
			//fall back on defaults
			epsilon = EPSILON;
		}
		
		double delta;
		try {
			delta = this.configManager.getAvmDelta();
		} catch (NumberFormatException e) {
			//fall back on defaults
			delta = DELTA;
		}
		
		AVM algorithm = new AVM(this.standardProblem);
        algorithm.setInputParameter("epsilon", epsilon);
        algorithm.setInputParameter("delta", delta);
        return algorithm;
    }
}
