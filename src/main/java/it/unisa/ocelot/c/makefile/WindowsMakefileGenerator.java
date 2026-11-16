package it.unisa.ocelot.c.makefile;

import java.io.IOException;

@Deprecated
public class WindowsMakefileGenerator extends JNIMakefileGenerator {

	private static String GTK_HOME = "C:/gtk/";

	@Deprecated
	public WindowsMakefileGenerator() {
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
		return "C:/'Program Files'/Java/jdk1.8.0_45";
	}

	@Deprecated
	@Override
	public String getSystemInclude() {
		return "";
	}

	@Deprecated
	@Override
	public String[] getGlib2Paths() {
		return new String[] { 
				GTK_HOME + "include/gtk-3.0", 
				GTK_HOME + "include/cairo",
				GTK_HOME + "include/pango-1.0", 
				GTK_HOME + "include/atk-1.0",
				GTK_HOME + "include/pixman-1",
				GTK_HOME + "include", 
				GTK_HOME + "include/freetype2",
				GTK_HOME + "include/gdk-pixbuf-2.0",
				GTK_HOME + "include/libpng15", 
				GTK_HOME + "include/glib-2.0",
				GTK_HOME + "lib/glib-2.0/include"};
	}

	@Deprecated
	@Override
	public String[] getJavaPaths() {
		return new String[] { "$(JAVA_HOME)/include",
				"$(JAVA_HOME)/include/win32" };
	}

	@Deprecated
	@Override
	public String getCFlags() {
		return "-shared -m64 -LC:/gtk/lib";
	}

	@Deprecated
	@Override
	public String getLibName() {
		return "Test.dll";
	}

	@Deprecated
	@Override
	public Process make() throws IOException {
		return Runtime.getRuntime().exec(new String[] {"mingw32-make", "--directory=jni"});
	}
	
	@Deprecated
	@Override
	public String getMoreOptions() {
		return "";
	}
}
