package it.unisa.ocelot.simulator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CBridge {
	private static boolean initialized;

	private int coreId;
	
	/**
	 * Initializes the C native part. To be called before everything else.
	 */
	public synchronized static void initialize(int values, int arrays, int pointers) {
		if (initialized) {
			System.err.println("Attempted double initialization of CBridge class");
		} else {
			privInit(values, arrays, pointers);
			initialized = true;
		}
	}
	
	private synchronized native static void privInit(int values, int arrays, int pointers);
	
	/**
	 * Constructor. Creates a new {@link CBridge} 
	 * on core process id 0.
	 */
	public CBridge() {
		this.coreId = 0;
	}
	
	/**
	 * Constructor. Creates a new {@link CBridge} 
	 * on a caller-specified core process id.
	 * 
	 * @param pCoreId an {@code int}, the core process id.
	 */
	public CBridge(int pCoreId) {
		this.coreId = pCoreId;
	}
		
	/**
	 * Executes the test function in order to populate the given the event handler
	 * @param pHandler Event handler, it will contain the result of the test (each non-trivial choice)
	 * @param pValues Parameters of the function
	 */
	public void getEvents(EventsHandler pHandler, Object[] pValues, Object[][] pArrays, Object[] pPointers) throws IOException {
		if (!initialized) {
			throw new RuntimeException("CBridge.getEvents: ERROR: You must invoke CBridge.initialize before starting to evaluate");
		}
		
		File file = new File("fitnessValues.txt");
		try {
			Files.deleteIfExists(file.toPath());
		} catch (IOException e) {
			System.err.println("CBridge.getEvents: ERROR: Unable to delete the file fitnessValues.txt: " + e.getMessage());
			throw e;
		}
		
		double[] values = new double[pValues.length];
		for (int i = 0; i < pValues.length; i++) {
			values[i] = ((Number) pValues[i]).doubleValue();
		}
		
		double[][] arrays = new double[pArrays.length][];
		for (int i = 0; i < pArrays.length; i++) {
			arrays[i] = new double[pArrays[i].length];
			for (int j = 0; j < pArrays[i].length; j++) {
				arrays[i][j] = ((Number) pArrays[i][j]).doubleValue();
			}
		}
		
		double[] pointers = new double[pPointers.length];
		for (int i = 0; i < pPointers.length; i++) {
			pointers[i] = ((Number) pPointers[i]).doubleValue();
		}
		
		// Actually runs the C program and gets all the execution events
		getEvents(pHandler, values, arrays, pointers);
	}
	
	public native void getEvents(EventsHandler pHandler, double[] values, double[][] arrays, double[] pointers);
	
	public native void memoryDump();
	
	public static native void shutdown();
	
	public void onCrash() {
		System.err.println("A crash occurred...");
	}
}
