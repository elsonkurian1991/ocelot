package it.unisa.ocelot.c.makefile;

import java.io.IOException;

@Deprecated
public class MacOSXMakefileGenerator extends JNIMakefileGenerator {
	@Deprecated
	public MacOSXMakefileGenerator() {
		super("jni/makefile");
	}

	@Deprecated
	@Override
	public String getCCompiler() {
		return "gcc";
	}

	@Deprecated
	@Override
	public String getJavaHome() {
		return "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/include/";
	}

	@Deprecated
	@Override
	public String getSystemInclude() {
		return "/usr/include";
	}

	@Deprecated
	@Override
	public String[] getGlib2Paths() {
		return new String[] {"/usr/local/Cellar/glib/2.42.1/include/glib-2.0/", "/usr/local/Cellar/glib/2.42.1/lib/glib-2.0/include/"};
	}

	@Deprecated
	@Override
	public String[] getJavaPaths() {
		return new String[] {"$(JAVA_HOME)", "$(JAVA_HOME)darwin"};
	}

	@Deprecated
	@Override
	public String getCFlags() {
		return "-dynamiclib -framework JavaVM";
	}

	@Deprecated
	@Override
	public String getLibName() {
		return "libTest.jnilib";
	}
	
	@Deprecated
	@Override
	public Process make() throws IOException {
		return Runtime.getRuntime().exec(new String[] {"make", "--directory=jni"});
	}
	
	@Deprecated
	@Override
	public String getMoreOptions() {
		return "";
	}
}
