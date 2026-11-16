package it.unisa.ocelot.c.makefile;

import java.io.IOException;

@Deprecated
public class FedoraMakefileGenerator extends JNIMakefileGenerator {
	@Deprecated
	public FedoraMakefileGenerator() {
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
		return "/usr/lib/jvm/java/";
	}

	@Deprecated
	@Override
	public String getSystemInclude() {
		return "/usr/include";
	}

	@Deprecated
	@Override
	public String[] getGlib2Paths() {
		return new String[] {"/usr/lib/x86_64-linux-gnu/glib-2.0/include", "$(SYSTEM_INCLUDE)/glib-2.0", "/usr/lib64/glib-2.0/include/"};
	}

	@Deprecated
	@Override
	public String[] getJavaPaths() {
		return new String[] {"$(JAVA_HOME)/include", "$(JAVA_HOME)/include/linux"};
	}

	@Deprecated
	@Override
	public String getCFlags() {
		return "-shared -fpic";
	}

	@Deprecated
	@Override
	public String getLibName() {
		return "libTest.so";
	}
	
	@Deprecated
	@Override
	public Process make() throws IOException {
		return Runtime.getRuntime().exec(new String[] {"make", "--directory=jni"});
	}

	@Deprecated
	@Override
	public String getMoreOptions() {
		return "-lrt";
	}
}
