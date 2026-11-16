package it.unisa.ocelot.c.makefile;

import java.io.IOException;

import it.unisa.ocelot.c.BuildingException;
import it.unisa.ocelot.conf.ConfigManager;

public class DynamicMakefileGenerator extends JNIMakefileGenerator {
	private final ConfigManager config;
	
	/**
	 * Which name do we want to give to the
	 * platform library that will be the output
	 * of compilation?
	 */
	private String libName;
	
	public DynamicMakefileGenerator(ConfigManager config) {
		super("jni/Makefile"); //TODO fix path
		this.config = config;

		try {
			this.libName = getLibraryNameFromOS();
		} catch (BuildingException e) {
			//TODO rethrow?
			e.printStackTrace();
		}
	}
	
	private String getLibraryNameFromOS() throws BuildingException {
		String os = System.getProperty("os.name");
		if (os.contains("Win")) {
			return "Test.dll";
		} else if (os.contains("Mac")) {
			return "libTest.jnilib";
		} else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			return "libTest.so";
		} else {
			throw new BuildingException("Your operating system \"" + os + "\" is not supported");
		}
	}

	@Override
	public String getCCompiler() {
		return this.config.getCompilerCommand();
	}

	@Override
	public String getJavaHome() {
		return this.config.getJavaHome();
	}

	@Override
	public String getSystemInclude() {
		return this.config.getSystemInclude();
	}

	@Override
	public String[] getGlib2Paths() {
		return this.config.getGlib2Paths();
	}

	@Override
	public String[] getJavaPaths() {
		return this.config.getJavaPaths();
	}

	@Override
	public String getMoreOptions() {
		return this.config.getMoreOptions();
	}

	@Override
	public String getCFlags() {
		return this.config.getCFlags();
	}

	@Override
	public String getLibName() {
		return this.libName;
	}

	@Override
	public Process make() throws IOException {
		return Runtime.getRuntime().exec(new String[] {this.config.getMakeCommand(), "--directory=jni"});
	}
}
