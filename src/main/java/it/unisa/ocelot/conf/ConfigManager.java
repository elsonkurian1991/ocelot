package it.unisa.ocelot.conf;

import it.unisa.ocelot.c.cfg.CFG;
import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.c.cfg.nodes.CFGNode;
import it.unisa.ocelot.c.cfg.nodes.CFGNodeNavigator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("deprecation")
public final class ConfigManager {
	private static final String STATIC_CONTENT = "---------STATIC+CONTENT";

	private static String filename;
	private static String content;
	private static ConfigManager instance;
	
	private String myFilename;
	private Properties properties;
	
	static {
		setFilename("config.properties"); //default
	}
	
	/**
	 * Sets the name of the configuration file. 
	 * To be called before {@link #getInstance}.
	 * 
	 * @param pFilename a {@link String}.
	 */
	public static void setFilename(String pFilename) {
		filename = pFilename;
		content = null;
	}
	
	/**
	 * Sets the content of the configuration file. 
	 * To be called before {@link #getInstance}.
	 * 
	 * @param pContent a {@link String}.
	 */
	public static void setDirectContent(String pContent) {
		filename = STATIC_CONTENT + UUID.nameUUIDFromBytes(pContent.getBytes());
		content = pContent;
	}
	
	/**
	 * Returns an instance of the configuration manager. 
	 * Call {@link #setFilename} or {@link #setDirectContent} 
	 * before this method. This method strives to be efficient
	 * by returning the same instance of {@link ConfigManager}
	 * as long as the configuration file name or the 
	 * configuration content are reset by invoking again 
	 * {@link #setFilename} or {@link #setDirectContent}.
	 * When this happens, this methods rebuilds a new 
	 * configuration manager, that will be returned by the
	 * subsequent calls of this method until the next reset.
	 * 
	 * @return a {@link ConfigManager}.
	 * @throws IOException if there is an error opening the file.
	 */
	public static ConfigManager getInstance() throws IOException {
		if (instance == null || !instance.myFilename.equals(filename)) {
			if (filename.startsWith(STATIC_CONTENT)) {
				instance = new ConfigManager(filename, content);
			} else {
				instance = new ConfigManager(filename);
			}
		}
		return instance;
	}
	
	private ConfigManager(String pFilename) throws IOException {
		this.myFilename = pFilename;
		this.properties = new Properties();
		this.properties.load(new FileInputStream(pFilename));
	}
	
	private ConfigManager(String pFilename, String pContent) throws IOException {
		this.myFilename = pFilename;
		this.properties = new Properties();
		this.properties.load(new StringReader(pContent));
	}
	
	//TODO remove
	public void setProperty(String key, String value) {
		this.properties.setProperty(key, value);
	}
	
	//From here all the defined properties follow 
	
	//config
	
	public String getJavaHome() {
		return this.properties.getProperty("config.javaHome", "/usr/lib/jvm/java-8-openjdk");
	}
	
	public String getSystemInclude() {
		return this.properties.getProperty("config.systemInclude", "/usr/include");
	}
	
	public String[] getGlib2Paths() {
		String paths = this.properties.getProperty("config.glib2Paths", "");
		if (paths.isEmpty()) {
			return new String[0];
		} else {
			return paths.split("\\,");
		}
	}
	
	public String[] getJavaPaths() {
		String paths = this.properties.getProperty("config.javaPaths", "");
		if (paths.isEmpty()) {
			return new String[0];
		} else {
			return paths.split("\\,");
		}
	}
	
	public String getCFlags() {
		return this.properties.getProperty("config.cflags", "-shared -fpic");
	}
	
	public String getCompilerCommand() {
		return this.properties.getProperty("config.cc", "gcc");
	}
	
	public String getMakeCommand() {
		return this.properties.getProperty("config.make", "make");
	}
	
	public String getMoreOptions() {
		return this.properties.getProperty("config.options", "");
	}
	
	//experiment

	/**
	 * Returns the amount of time in seconds the genetic algorithm will run (uses evaluations.max if its value is zero)
	 * 
	 * @return
	 */
	public int getExperimentTime() {
		return Integer.parseInt(this.properties.getProperty("experiment.time", "0"));
	}

	/**
	 * Returns the output folder.
	 * 
	 * @return a {@link String}.
	 */
	public String getOutputFolder() {
		return this.properties.getProperty("experiment.output.folder");
	}

	/**
	 * Returns if configuration file allows printing the solution on the standard output.
	 * 
	 * @return a {@code boolean}.
	 */
	public boolean getPrintResults() {
		return Boolean.parseBoolean(this.properties.getProperty("experiment.results.print", "false"));
	}

	/**
	 * Returns the folder that will contain the results of the genetic algorithm calculations.
	 * 
	 * @return
	 */
	public String getResultsFolder() {
		return this.properties.getProperty("experiment.results.folder");
	}
	
	public String[] getExperimentGenerators() {
		String generators = this.properties.getProperty("experiment.generators", "");
		String[] array = generators.split("\\,");
		return array;
	}

	public boolean isExperimentMinimization() {
		return Boolean.parseBoolean(this.properties.getProperty("experiment.minimization", "false"));
	}

	/**
	 * Returns the number of independent runs of the experiment (genetic algorithm) (1 if not specified).
	 * 
	 * @return
	 */
	public int getExperimentRuns() {
		return Integer.parseInt(this.properties.getProperty("experiment.runs", "1"));
	}

	public String getOptimizeFor() {
		return this.properties.getProperty("experiment.optimize", "Pairs");
	}

	public String getEvaluateOn() {
		return this.properties.getProperty("experiment.evaluate", "Pairs");
	}

	public boolean isRandomRun() {
		return Boolean.parseBoolean(this.properties.getProperty("experiment.random", "false"));
	}

	public boolean isDynamicObjectives() {
		return Boolean.parseBoolean(this.properties.getProperty("experiment.dynamic", "false"));
	}

	public boolean isSplitBooleans() {
		return Boolean.parseBoolean(this.properties.getProperty("experiment.splitbooleans", "false"));
	}
	
	//population
	
	/**
	 * Returns the population size for the genetic algorithm (100 if not specified).
	 * 
	 * @return
	 */
	public int getPopulationSize() {
		return Integer.parseInt(this.properties.getProperty("population.size", "100"));
	}
	
	//evaluations
	
	/**
	 * Returns the maximum number of evaluations for the genetic algorithm,
	 * where 0 means no limit (0 if not specified). A solution evaluation
	 * calculates the values of the solution's objectives by running 
	 * the C program. So this value signifies the maximum
	 * number of times an algorithm can run the C program.
	 * 
	 * @return an {@code int}, the maximum number of evaluations.
	 */
	public int getEvaluationsMax() {
		return Integer.parseInt(this.properties.getProperty("evaluations.max", "0"));
	}
	
	//algorithm
	
	/**
	 * Returns the maximum amount of time in seconds the genetic algorithm 
	 * is allowed to run, where 0 means no limit (no limit if not specified).
	 * 
	 * @return an {@code int}, the maximum time in seconds.
	 */
	public int getAlgorithmTime() {
		return Integer.parseInt(this.properties.getProperty("algorithm.time", "0"));
	}
	
	/**
	 * Returns the maximum number of generations (meaning, the number of 
	 * times the population generates a new population) the genetic 
	 * algorithm is allowed to perform.
	 * 
	 * @return an {@code int}, the maximum number of generations. 
	 */
	public int getAlgorithmGenerations() {
		return Integer.parseInt(this.properties.getProperty("algorithm.generations", "0"));
	}
	
	//crossover
	
	/**
	 * Returns the probability of crossover for the genetic algorithm (0.9 if not specified)
	 * 
	 * @return a {@code double}.
	 * @throws NumberFormatException if the property is not set to a number.
	 */
	public double getCrossoverProbability() {
		return Double.parseDouble(this.properties.getProperty("crossover.probability", "0.9"));
	}
	
	//mutation
	
	/**
	 * Returns the probability of mutation for the genetic algorithm, where -1
	 * means the inverse of the total number of variables of the problem 
	 * (-1 if not specified).
	 * 
	 * @return a {@code double}.
	 * @throws NumberFormatException if the property is not set to a number.
	 */
	public double getMutationProbability() {
		return Double.parseDouble(this.properties.getProperty("mutation.probability", "-1"));
	}
	
	/**
	 * Returns {@code true} if the mutation probability has to be dynamic; 
	 * {@code false} by default.
	 * 
	 * @return a {@code boolean}.
	 */
	public boolean isDynamicMutation() {
		return Boolean.parseBoolean(this.properties.getProperty("mutation.dynamic", "false"));
	}
	
	//TODO remove
	public void setDynamicMutation(boolean value) {
		this.properties.setProperty("mutation.dynamic", (value ? "true" : "false"));
	}
	                                                                            
	//operators
	
	public boolean isMetaMutatorEnabled() {
		return Boolean.parseBoolean(this.properties.getProperty("operators.mutator.metamutator", "false"));
	}

	//threads
	
	/**
	 * Returns the number of threads for the genetic algorithm 
	 * (1 if not specified).
	 * 
	 * @return an {@code int}.
	 */
	public int getThreads() {
		return Integer.parseInt(this.properties.getProperty("threads", "1"));
	}
	
	//test
	
	/**
	 * Returns the base directory from which all the filenames in the config file will start.
	 * 
	 * @return
	 */
	public String getTestBasedir() {
		String basedir = this.properties.getProperty("test.basedir", "./");
		if (!basedir.endsWith("/")) {
			basedir += "/";
		}
		return basedir;
	}

	/**
	 * Returns the name of the name of the file which contains the function to be tested (required). 
	 * It uses property "test.basedir" as base directory if it is not {@code null}.
	 * 
	 * @return
	 */
	public String getTestFilename() {
		String basedir = getTestBasedir();
		if (basedir == null) {
			return this.properties.getProperty("test.filename");
		} else {
			return basedir + this.properties.getProperty("test.filename");
		}
	}
	
	/**
	 * Returns the name of the function to be tested (required).
	 * 
	 * @return a {@link String}.
	 */
	public String getTestFunction() {
		return this.properties.getProperty("test.function");
	}
	
	/**
	 * Returns the ranges for the parameters of the function to be tested. Each range should be in the form "N:M" and
	 * the ranges should be separated by a space.
	 * 
	 * @return
	 */
	public Range<Double>[] getTestRanges() {
		String rangesString = this.properties.getProperty("test.parameters.ranges", "");
		if (rangesString.isEmpty()) {
			return null;
		}
		
		String[] ranges = StringUtils.split(rangesString, ' ');
		@SuppressWarnings("unchecked")
		Range<Double>[] result = new Range[ranges.length];
		
		for (int i = 0; i < ranges.length; i++) {
			String rangeString = ranges[i];
			String[] rangeParts = StringUtils.split(rangeString, ':');
			double from = Double.parseDouble(rangeParts[0]);
			double to = Double.parseDouble(rangeParts[1]);
			result[i] = Range.between(from, to);
		}
		
        return result;
	}

	/**
	 * Returns the array of paths in which the source files will be searched.
	 * Currently it is the same as {@link #getTestIncludePaths()}, but in 
	 * future we will define its own property.
	 * 
	 * @return
	 */
	public String[] getTestSourcePaths() {
		return getTestIncludePaths();
	}

	/**
	 * Returns the array of paths in which the include headers will be searched.
	 * 
	 * @return
	 */
	public String[] getTestIncludePaths() {
		String includeStrings = this.properties.getProperty("test.includes", "");
		String[] includes = StringUtils.split(includeStrings, ',');
		String basedir = this.getTestBasedir();
		for (int i = 0; i < includes.length; i++) {
			if (includes[i].startsWith("/") || basedir == null) {
				//it is an absolute path, or no basedir specified: 
				//leave the path as is 
			} else {
				//it is a relative path and basedire specified: 
				//prepend basedir to the path 
				includes[i] = basedir + includes[i];
			}
		}
		return includes;
	}
	
	/**
	 * Returns the list of test arguments for a simple test.
	 * Requires to specify the list of parameters types.
	 * 
	 * @return
	 */
	public Object[] getTestArguments() {
		String property = this.properties.getProperty("test.parameters.types", "");
		String[] types = StringUtils.split(property, ',');
		String argsString = this.properties.getProperty("test.simple", "");
		String[] args = StringUtils.split(argsString, ' ');
		if (types.length != args.length) {
    		throw new RuntimeException("Lists of parameter types and arguments do not have same length");
		}
		
		Object[] arguments = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i].startsWith("int")) {
				arguments[i] = Integer.parseInt(args[i]);
			} else if (types[i].startsWith("double")) {
				arguments[i] = Double.parseDouble(args[i]);
			} else {
        		throw new RuntimeException("Unexpected parameter type " + types[i]);
			}
		}
		
		return arguments;
	}
	
	/**
	 * Returns {@code true} if debug is active, {@code false} otherwise.
	 * 
	 * @return a {@code boolean}.
	 */
	public boolean getDebug() {
		return this.properties.getProperty("test.debug", "false").equalsIgnoreCase("true");
	}
	
	/**
	 * Returns {@code true} if UI is active, {@code false} otherwise.
	 * 
	 * @return
	 */
	public boolean getUI() {
		return this.properties.getProperty("test.ui", "false").equalsIgnoreCase("true");
	}
	
	public int getTestArraysSize() {
		return Integer.parseInt(this.properties.getProperty("test.arrays.size", "100"));
	}
	
	public String[] getUnitComponents() {
		return this.properties.getProperty("test.components", "").replaceAll("\\s+","").split(",");
	}
	
	public String[] getTestLink() {
		String link = this.properties.getProperty("test.link", "");
		if (link.equals("")) {
			return new String[0];
		} else {
			return link.split("\\,");
		}
	}

	//suite
	
	public String getTestSuiteGenerator() {
		return this.properties.getProperty("suite.generator");
	}
	
	public String getTestSuiteMinimizer() {
		return this.properties.getProperty("suite.minimizer");
	}
	
	public int getReducedMcCabeCoverageTimes() {
		return Integer.parseInt(this.properties.getProperty("suite.generator.rmc.times", "1"));
	}
	
	public int getRandomSizeLimit() {
		return Integer.parseInt(this.properties.getProperty("suite.generator.random.limit.tc", "-1"));
	}
	
	public int getRandomTimeLimit() {
		return Integer.parseInt(this.properties.getProperty("suite.generator.random.limit.time", "-1"));
	}
	
	public int getRandomGranularity() {
		return Integer.parseInt(this.properties.getProperty("suite.generator.random.granularity", "100"));
	}
	
	public double getRequiredCoverage() {
		return Double.parseDouble(this.properties.getProperty("suite.coverage", "1.0"));
	}

	//TODO delete
	public void setAlgorithm(String pAlgorithm) {
		this.properties.setProperty("suite.generator.algorithm", pAlgorithm);
	}
	
	public String getAlgorithm() {
		return this.properties.getProperty("suite.generator.algorithm", "GeneticAlgorithm");
	}
	
	public boolean isDMCSeed() {
		return Boolean.parseBoolean(this.properties.getProperty("suite.generator.dmc.seed", "false"));
	}

	public int getDMCSeedSize() {
		return Integer.parseInt(this.properties.getProperty("suite.generator.dmc.seed.size", "50"));
	}
	
	public boolean isSerendipitous() {
		return Boolean.parseBoolean(this.properties.getProperty("suite.generator.serendipitous", "false"));
	}
	
	public String getBudgetManager() {
		return this.properties.getProperty("suite.generator.budgetmanager", "Basic");
	}
	
	//TODO delete
	public void setBudgetManager(String pBudgetManager) {
		this.properties.setProperty("suite.generator.budgetmanager", pBudgetManager);
	}
	
	public List<String> getCascadeGenerators() {
		String cascade = this.properties.getProperty("suite.generator.cascade", "");
		String[] array = cascade.split("\\,");
		List<String> list = new ArrayList<>();
		for (String generator : array) {
			list.add(generator);
		}
		return list;
	}
	
	//avm
	
	public double getAvmEpsilon() {
		return Double.parseDouble(this.properties.getProperty("avm.epsilon", "0.001"));
	}
	
	public double getAvmDelta() {
		return Double.parseDouble(this.properties.getProperty("avm.delta", "1"));
	}
	
	//deprecated

	@Deprecated
	private CFGNodeNavigator getTestTargetNavigator(CFG pCfg) {
		String targetString = this.properties.getProperty("test.target", "");
		
		CFGNodeNavigator navigator = pCfg.getEntry().navigate(pCfg);
		
		String[] targets = StringUtils.split(targetString, ",");
		for (String target : targets) {
			if (target.equalsIgnoreCase("flow")) {
				navigator = navigator.goFlow();
			} else if (target.equalsIgnoreCase("true")) {
				navigator = navigator.goTrue();
			} else if (target.equalsIgnoreCase("false")) {
				navigator = navigator.goFalse();
			} else {
				navigator = navigator.goCase(target);
			}
		}
		
		return navigator;
	}
	
	/**
	 * Returns the target node specified in the configuration file.  
	 * Will not be used in future.
	 * 
	 * @param pCfg Control Flow Graph
	 * @return Target node to be reached by the genetic algorithm
	 */
	@Deprecated
	public CFGNode getTestTarget(CFG pCfg) {
		return getTestTargetNavigator(pCfg).node();
	}
	
	/**
	 * Returns the target path specified in the configuration file.  
	 * Will not be used in future.
	 * 
	 * @param pCfg Control Flow Graph
	 * @return Target path to be reached by the genetic algorithm
	 */
	@Deprecated
	public List<LabeledEdge> getTestTargetPath(CFG pCfg) {
		return getTestTargetNavigator(pCfg).path();
	}
	

}