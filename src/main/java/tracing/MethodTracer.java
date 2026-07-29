/*package tracing;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Aspect
public class MethodTracer {
	private static final boolean ENABLED = false;
    // scope this to just the packages you're debugging — tracing the WHOLE
    // project is extremely noisy and slows the run down a lot
    @Pointcut("execution(* it.unisa.ocelot..*(..))")
    /*
     * @Pointcut("execution(* it.unisa.ocelot.genetic..*(..)) || "
            + "execution(* it.unisa.ocelot.c.cdg..*(..))")
     */
   /* public void traced() {}
  
    private static final String LOG_FILE = "methodTracking.txt";
    private static PrintWriter writer;

    // tracks which fully-qualified class names have already been logged,
    // so each class is written only once no matter how many times its
    // methods are entered. LinkedHashSet preserves first-seen order.
    private static final Set<String> seenClasses =
        Collections.synchronizedSet(new LinkedHashSet<>());

    static {
    	if (ENABLED) {
    		try {
                // append = true so multiple runs don't overwrite previous traces;
                // change to 'false' if you want a fresh file each run
                writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // make sure the file is flushed/closed when the JVM exits
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }));
    	}
        
    }

    @Before("traced()")
    public void logEntry(JoinPoint jp) {
        if (!ENABLED || writer == null) {
            return;
        }

        String declaringType = jp.getSignature().getDeclaringTypeName();

        // add() returns false if the class was already present, so this
        // block only runs the first time we see a given class
        if (seenClasses.add(declaringType)) {
            try {
                int lastDot = declaringType.lastIndexOf('.');
                String packageName = (lastDot >= 0)
                    ? declaringType.substring(0, lastDot)
                    : "(default package)";
                String className = (lastDot >= 0)
                    ? declaringType.substring(lastDot + 1)
                    : declaringType;

                writer.println(packageName
                    + "." + className);
            } catch (Throwable t) {
                // avoid letting tracing itself crash the traced program
            }
        }
    }
}*/