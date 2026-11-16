package it.unisa.ocelot.c;

import it.unisa.ocelot.c.makefile.JNIMakefileGenerator;

import java.io.IOException;
import java.io.PrintStream;


public abstract class Builder {
	protected final PrintStream stream;
	protected final JNIMakefileGenerator makefileGenerator;
	
	protected Builder(PrintStream pStream, JNIMakefileGenerator pMakefileGenerator) {
		this.stream = pStream;
		this.makefileGenerator = pMakefileGenerator;
	}

	public abstract void build() throws IOException, BuildingException;
}
