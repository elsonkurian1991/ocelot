package it.unisa.ocelot.genetic.settings;

import java.util.List;

import jmetal.core.Algorithm;
import jmetal.core.Problem;
import jmetal.util.JMException;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.algorithms.MOSA;
import it.unisa.ocelot.genetic.algorithms.MOSA_Generic;
import it.unisa.ocelot.genetic.many_objective.MOSAGenericCoverageProblem;
import it.unisa.ocelot.genetic.objectives.GenericObjective;

public class MOSASettingsGeneric extends GASettings {
	private List<GenericObjective> objectives;
	private ConfigManager config;
	
	public MOSASettingsGeneric(Problem pProblem) {
		super(pProblem);
	}
	
	public MOSASettingsGeneric(Problem pProblem, ConfigManager pConfig, List<GenericObjective> objectives) {
		super(pProblem, pConfig);
		
		this.config = pConfig;
		
		this.objectives = objectives;
	}
	
	public Algorithm configure() throws JMException {
		Algorithm algorithm = new MOSA_Generic((MOSAGenericCoverageProblem)problem_, objectives);
		algorithm.setInputParameter("maxCoverage", this.config.getRequiredCoverage());
		return super.configure(algorithm);
    }
}
