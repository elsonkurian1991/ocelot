package it.unisa.ocelot.runnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;

import it.unisa.ocelot.c.Builder;
import it.unisa.ocelot.c.BuildingException;
import it.unisa.ocelot.c.StandardBuilder;
import it.unisa.ocelot.c.makefile.DynamicMakefileGenerator;
import it.unisa.ocelot.c.makefile.JNIMakefileGenerator;
import it.unisa.ocelot.c.makefile.LinuxMakefileGenerator;
import it.unisa.ocelot.c.makefile.MacOSXMakefileGenerator;
import it.unisa.ocelot.c.makefile.WindowsMakefileGenerator;
import it.unisa.ocelot.conf.ConfigManager;
import it.unisa.ocelot.genetic.objectives.GenericObjective;
import it.unisa.ocelot.runnable.runners.ExecuteExperiment;
import it.unisa.ocelot.runnable.runners.ExecuteWholeCoverage;
import it.unisa.ocelot.runnable.runners.GenAndWrite;
import it.unisa.ocelot.util.Debugger;
import it.unisa.ocelot.util.Utils;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
@SuppressWarnings({ "unused", "deprecation" })
public class Run {
	public static final String VERSION = "1.0";

	public static final String HASH_FILENAME = ".lastbuild.cks";
	/*
	 * config_lmc_cdg
	 * config_emdm_cdg
	 * config_toyseven_cdg
	 * config_toysix_cdg
	 * config_toyeight_cdg
	 * config_toynine_cdg
	 * config_toyten_cdg
	 * config_toyeleven_cdg
	 * config_toy_a.properties
	 * config_toyninesmall
	 * config_toyninesmalltwo
	 * config_twelve.properties
	 */
	private static final String CONFIG_FILENAME = "config_twelve.properties";
	public static final String LOCALUSER_DIR=System.getProperty("user.dir"); 

	private static final int RUNNER_ILLEGAL = -1;
	private static final int RUNNER_SIMPLE_EXECUTE = 0;
	private static final int RUNNER_EXPERIMENT = 1;
	private static final int RUNNER_WRITE = 2;

	private int runnerType;
	private String[] experimentGenerators;
	private boolean forceBuild;
	private String configFilename;
	private boolean forceNoBuild;
	public static StringBuilder logWriter = new StringBuilder();

	public static void main(String[] args) throws Exception {
		// Visual Header Banner
		System.out.println("==================================================");
		System.out.println("                WELCOME TO EvInT                  ");
		System.out.println("==================================================");
		TimeUnit.SECONDS.sleep(1);
		// Friendly Configuration Reminder
		System.out.println("\n[!] REMINDER: Please ensure your config file is updated.");
		System.out.println("[✓] Assuming valid configuration... Launching EvInT!\n");
		TimeUnit.SECONDS.sleep(1);
		System.out.println("--------------------------------------------------");

		System.out.println("[INFO] Cleaning target workspace...");
		System.out.println("[>] Deleting old build files...");
		// deleting the old build files.
		String filePathToDelete1 = LOCALUSER_DIR+"/.lastbuild.cks";
		deleteFileIfExists(filePathToDelete1);
		String filePathToDelete2 = LOCALUSER_DIR+"/libTest.so";
		deleteFileIfExists(filePathToDelete2);
		String filePathToDelete3 = LOCALUSER_DIR+"/fitnessValues.txt"; //do 
		deleteFileIfExists(filePathToDelete3);
		String filePathToDelete4 = LOCALUSER_DIR+"/testObjectives.to"; //do 
		deleteFileIfExists(filePathToDelete4);
		String filePathToDelete5 = LOCALUSER_DIR+"/fitnessValues.bin"; //do 
		deleteFileIfExists(filePathToDelete5);
		String filePathToDelete6 = LOCALUSER_DIR+"/cdg_output.txt"; //do 
		deleteFileIfExists(filePathToDelete6);

		System.out.println("[✓] Old build files successfully removed.");
		System.out.println("--------------------------------------------------\n");

		long startTime =System.currentTimeMillis();

		//Main execution part start
		Run runner = new Run(args);
		if (runner.mustBuild())
			runner.build();
		runner.saveHash();
		runner.run();
		//Main execution part end
		
		long endTime = System.currentTimeMillis();
		long time = endTime - startTime;
		long hours = TimeUnit.MILLISECONDS.toHours(time);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(time) % 60;
		long seconds = TimeUnit.MILLISECONDS.toSeconds(time) % 60;

		System.out.println("Execution time: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
		logWriter.append("Execution time: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
		logWriter.append("\n");

		createLogFile(logWriter);

		deleteTargetSourceFilesFromJni();
	}
	private static void deleteTargetSourceFilesFromJni() throws IOException {
		ConfigManager getConfInfo=ConfigManager.getInstance();		
		String[] listFiles= getConfInfo.getTestIncludePaths();
		String tragetSourceFolder=getConfInfo.getTestBasedir();
		File outputJniFolder = new File(tragetSourceFolder, "jni");
		// Create the 'jni' subfolder if it doesn't exist
		if (!outputJniFolder.exists()) {
			boolean created = outputJniFolder.mkdirs();
			if (!created) {
				System.out.println("Failed to create output JNI folder: " + outputJniFolder.getAbsolutePath());
				return;
			}
		}
		for(int i=0;i<listFiles.length;i++) {
			//System.out.println(supportFiles[i]);
			int lastIndex=listFiles[i].lastIndexOf('/');
			File sourceFile = new File("jni/", listFiles[i].substring(lastIndex+1));
			File destinationFile = new File(outputJniFolder, listFiles[i].substring(lastIndex+1));

			if (sourceFile.exists()) {
				try {
					Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					System.out.println("Failed to move: " + sourceFile.getAbsolutePath());
					e.printStackTrace();
				}
			} else {
				System.out.println("File not found: " + sourceFile.getAbsolutePath());
			}
			//delete the unwanted header files
			String headerFileName=listFiles[i].substring(lastIndex+1);
			headerFileName=headerFileName.replace(".c", ".h");
			File headerFile = new File("jni/",headerFileName);
			if(headerFile.exists()) {
				headerFile.delete();
			}

		}
		//spcl case
		File kcg_imported_functions = new File("jni/","kcg_imported_functions.h");
		if(kcg_imported_functions.exists()) {
			kcg_imported_functions.delete();
		}
		System.out.println("Files moved to "+outputJniFolder+"/ for backup.");

	}
	public static void createLogFile(StringBuilder logWriter) throws IOException {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
		String formatedDateTime = now.format(formatter);
		String fileName= "Log_"+formatedDateTime+".txt";
		String dir_FileName =LOCALUSER_DIR+"/ocelot_logs/"+fileName;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(dir_FileName))){
			writer.write(logWriter.toString());
		}

	}
	
	public static void deleteFileIfExists(String filePath) {
		Path path = Paths.get(filePath);

		try {
			// Atomic check-and-delete provided by NIO2
			boolean deleted = Files.deleteIfExists(path);

			if (deleted) {
				System.out.println("[✓] Deleted old build file: " + filePath);
			} else {
				System.out.println("[i] File not found (skipping): " + filePath);
			}
		} catch (IOException e) {
			System.err.println("[X] Error deleting file (" + filePath + "): " + e.getMessage());
		}
	}


	public Run(String[] args) throws IOException {
		this.runnerType = RUNNER_WRITE;
		this.forceBuild = false;
		this.forceNoBuild = false;
		this.configFilename = CONFIG_FILENAME;

		ConfigManager.setFilename(CONFIG_FILENAME);

		this.experimentGenerators = ConfigManager.getInstance().getExperimentGenerators();
		for (String arg : args) {
			interpret(arg);
		}

		if (this.runnerType == RUNNER_ILLEGAL) {
			throw new IllegalArgumentException("Please, specify the type of runner (simple, experiment or write)");
		}

	}

	public boolean mustBuild() {
		if (this.forceBuild)
			return true;

		if (this.forceNoBuild) {
			System.err.println("WARNING: Forcing the system not to build. This could lead to errors.");
			return false;
		}

		try {
			String hash = makeHash();

			String previousHash = Utils.readFile(HASH_FILENAME);
			return (!previousHash.equals(hash));
		} catch (IOException e) {
			System.err.println("No previous build.");
			return true;
		}
	}

	public void saveHash() {
		String hash;
		try {
			hash = makeHash();
		} catch (IOException e) {
			System.err.println("Unable to create an hashfile. Configuration file unreadable");
			return;
		}

		try {
			Utils.writeFile(HASH_FILENAME, hash);
		} catch (IOException e) {
			System.err.println("Unable to write an hashfile. Permission denied.");
		}
	}

	private String makeHash() throws IOException {
		FileInputStream streamConfig = new FileInputStream(new File(CONFIG_FILENAME));
		FileInputStream streamTranslationUnit = new FileInputStream(ConfigManager.getInstance().getTestFilename());
		File libFile;
		libFile = new File("libTest.so");
		if (!libFile.exists())
			libFile = new File("Test.dll");
		if (!libFile.exists())
			libFile = new File("libTest.jnilib");
		FileInputStream streamLib = new FileInputStream(libFile);

		String md5version = DigestUtils.md5Hex("OCELOT" + VERSION);
		String md5config = DigestUtils.md5Hex(streamConfig);
		String md5file = DigestUtils.md5Hex(streamTranslationUnit);
		String md5lib = DigestUtils.md5Hex(streamLib);
		String md5final = DigestUtils.md5Hex(md5version + md5config + md5file + md5lib);

		streamConfig.close();
		streamTranslationUnit.close();
		streamLib.close();

		return md5version + md5config + md5file + md5lib + md5final;
	}

	public void build() throws Exception {
		ConfigManager config = ConfigManager.getInstance();
		Builder builder = new StandardBuilder( 
				config.getTestFilename(), 
				config.getTestFunction(), 
				config.getTestIncludePaths());

		JNIMakefileGenerator generator = new DynamicMakefileGenerator(config);
		//		String os = System.getProperty("os.name");
		//		if (os.contains("Win"))
		//			generator = new WindowsMakefileGenerator();
		//		else if (os.contains("Mac"))
		//			generator = new MacOSXMakefileGenerator();
		//		else if (os.contains("nix") || os.contains("nux") || os.contains("aix"))
		//			generator = new LinuxMakefileGenerator();
		//		//else if (os.contains("sunos"))
		//		else {
		//			throw new BuildingException("Your operative system \"" + os + "\" is not supported");
		//		}

		for (String linkLibrary : config.getTestLink())
			generator.addLinkLibrary(linkLibrary);

		builder.setMakefileGenerator(generator);
		builder.setOutput(System.out);

		builder.build();
	}

	public void run() throws Exception {
		System.load(LOCALUSER_DIR+"/libTest.so");
		switch (this.runnerType) {
		case RUNNER_SIMPLE_EXECUTE:
			System.out.println("Running simple coverage test");
			new ExecuteWholeCoverage().run();
			break;
		case RUNNER_EXPERIMENT:
			System.out.println("Running experiment");
			if (this.experimentGenerators == null)
				new ExecuteExperiment().run();
			else
				new ExecuteExperiment(this.experimentGenerators).run();
			break;
		case RUNNER_WRITE:
			System.out.println("Running coverage and writing");
			new GenAndWrite().run();
			break;
		}

		//		if (ConfigManager.getInstance().getDebug()) {
		Debugger.printAll();
		//		}
	}

	public void interpret(String arg) {
		String[] parts = arg.split("\\=");

		if (arg.equals("-b") || arg.equals("--build")) {
			this.forceBuild = true;
			return;
		}

		if (arg.equals("-B") || arg.equals("--no-build")) {
			this.forceNoBuild = true;
			return;
		}
		if (arg.equals("--profile")) {
			try {
				System.out.println("Profiling countdown:");
				for (int i = 10; i >= 1; i--) { 
					System.out.println(i);
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
			}

			return;
		}

		if (arg.equals("-v") || arg.equals("--version")) {
			System.out.println("Ocelot version " + VERSION);
			System.exit(0);
			return;
		}

		if (parts.length != 2)
			throw new IllegalArgumentException("The passed parameter is not valid: " + arg);

		String property = parts[0];
		String value = parts[1];

		boolean changedProperty = false;

		if (property.equalsIgnoreCase("type")) {
			if (value.equalsIgnoreCase("simple")) {
				this.runnerType = RUNNER_SIMPLE_EXECUTE;
			} else if (value.equalsIgnoreCase("experiment")) {
				this.runnerType = RUNNER_EXPERIMENT;
			} else if (value.equalsIgnoreCase("write")) {
				this.runnerType = RUNNER_WRITE;
			} else
				throw new IllegalArgumentException("Illegal run type '" + value + "'. Use 'simple', 'experiment' or 'write'.");
		} else if (property.equalsIgnoreCase("config")) {
			if (changedProperty)
				throw new IllegalArgumentException("Illegal config position: set the configuration file before editing specific properties.");

			this.configFilename = value;
			ConfigManager.setFilename(value);
		} else if (property.equalsIgnoreCase("expgen")) {
			String[] generators = value.split("\\,");
			this.experimentGenerators = generators;
		} else {
			try {
				ConfigManager.getInstance().setProperty(property, value);
				changedProperty = true;
			} catch (IOException e) {
				throw new RuntimeException("Error: unable to open configuration file. " + e.getMessage());
			}
		}
	}
}
