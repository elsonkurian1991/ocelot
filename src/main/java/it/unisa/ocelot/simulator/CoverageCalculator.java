package it.unisa.ocelot.simulator;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import it.unisa.ocelot.TestCase;

public abstract class CoverageCalculator<T> {
	public abstract void resetCoverage();

	public final void updateCoverage(Set<TestCase> pTestCases) {
		for (TestCase tc : pTestCases) {
			updateCoverage(tc);
		}
	}
	
	/**
	 * Updates the coverage.
	 * 
	 * @return a {@link TestCase}.
	 */
	public abstract void updateCoverage(TestCase pTestCase);
	
	/**
	 * Gets the uncovered targets.
	 * 
	 * @return a {@link Set}{@code <T>}, the set of uncovered targets.
	 */
	public abstract Set<T> getUncovered();
	
	/**
	 * Gets the covered targets.
	 * 
	 * @return a {@link Set}{@code <T>}, the set of covered targets.
	 */
	public abstract Set<T> getCovered();
	
	/**
	 * Gets the coverage of targets.
	 * 
	 * @return a {@code double}, calculated as the percentage ratio 
	 *         between {@link #getCovered()}{@link Set#size() .size()} and the
	 *         total number of targets in the system to test.
	 */
	public abstract double getCoverage();
	
	public final void dumpCoverageInfo() {
		// Write the uncovered objectives
		try (FileWriter writer = new FileWriter("uncovered.txt")) {
			for (T objective : getUncovered()) {
				writer.append(objective.toString());
			}
		} catch (IOException e) {
			System.out.println("Unable to generate file uncovered.txt");
			e.printStackTrace();
		}

		// Write the covered objectives
		try (FileWriter writer = new FileWriter("covered.txt")) {
			for (T objective : getCovered()) {
				writer.append(objective.toString());
			}
		} catch (IOException e) {
			System.out.println("Unable to generate file covere.txt");
			e.printStackTrace();
		}
	}
}
