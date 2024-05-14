package it.unisa.ocelot.runnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import it.unisa.ocelot.genetic.edges.ReadEFLfilesforPairCombination;
import it.unisa.ocelot.runnable.runners.ExecuteExperiment;
import it.unisa.ocelot.runnable.runners.ExecuteWholeCoverage;
import it.unisa.ocelot.runnable.runners.GenAndWrite;
import it.unisa.ocelot.util.Debugger;
import it.unisa.ocelot.util.Utils;

@SuppressWarnings({ "unused", "deprecation", "restriction" })
public class Run {
	public static final String VERSION = "1.0";
	
	public static final String HASH_FILENAME = ".lastbuild.cks";
	private static final String CONFIG_FILENAME = "config.properties";
	
	private static final int RUNNER_ILLEGAL = -1;
	private static final int RUNNER_SIMPLE_EXECUTE = 0;
	private static final int RUNNER_EXPERIMENT = 1;
	private static final int RUNNER_WRITE = 2;
	
	private int runnerType;
	private String[] experimentGenerators;
	private boolean forceBuild;
	private String configFilename;

	private boolean forceNoBuild;
	
	public static void main(String[] args) throws Exception {
		long startTime =System.currentTimeMillis();
		System.out.println("Now deleting old build file.... just for better debuging");
		String filePathToDelete1 = "/home/lta/git/ocelot/.lastbuild.cks";
		deleteFileIfExists(filePathToDelete1);
		String filePathToDelete2 = "/home/lta/git/ocelot/libTest.so";
		deleteFileIfExists(filePathToDelete2);
		deleteOldEvalPCfiles("/home/lta/git/ocelot/");
		ReadEFLfilesforPairCombination.RunEFLfilesforPairCombination(); // run this to read the efl file and create pairwise combinations. find a best place to call this
		Run runner = new Run(args);
		if (runner.mustBuild())
			runner.build();
		runner.saveHash();
		
		runner.run();
		System.out.println(ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals);
		long endTime=System.currentTimeMillis();
		long time=endTime-startTime;
		long hours = time / 3600000;
        long minutes = (time / 60000) % 60;
        long seconds = (time / 1000) % 60;
        System.out.println("Execution time: " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
	}
	private static void deleteOldEvalPCfiles(String directoryPath) {
		File directory = new File(directoryPath);

        // Check if the directory exists
        if (directory.exists() && directory.isDirectory()) {
            // Get all files in the directory
            File[] files = directory.listFiles();

            // Iterate through the files and delete .epc files
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".epc")) {
                        if (file.delete()) {
                            System.out.println("Deleted: " + file.getName());
                        } else {
                            System.out.println("Failed to delete: " + file.getName());
                        }
                    }
                }
            }
        } else {
            System.out.println("Invalid directory path.");
        }
		
	}
	public static void deleteFileIfExists(String filePath) {
        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            try {
                // Use the Files.delete method to delete the file
                Files.delete(path);
                System.out.println("File deleted successfully: " + filePath);
            } catch (IOException e) {
                System.err.println("Error deleting file: " + e.getMessage());
            }
        } else {
            System.out.println("File does not exist: " + filePath+ " --No problem");
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
		System.load("/home/lta/git/ocelot/libTest.so");
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
