package it.unisa.ocelot.c.makefile;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Simone Scalabrino.
 */
public class JNIMakefileGeneratorTest {
    @Test
    public void testLinuxGenerator() throws IOException {
        File makefile = new File("jni/makefile");
        if (!makefile.exists())
            assertTrue(makefile.delete());

        JNIMakefileGenerator generator = new LinuxMakefileGenerator();
        generator.generate();

        assertTrue(makefile.exists());
    }

    @Test
    public void testFedoraGenerator() throws IOException {
        File makefile = new File("jni/makefile");
        if (!makefile.exists())
            assertTrue(makefile.delete());

        JNIMakefileGenerator generator = new FedoraMakefileGenerator();
        generator.generate();

        assertTrue(makefile.exists());
    }
}