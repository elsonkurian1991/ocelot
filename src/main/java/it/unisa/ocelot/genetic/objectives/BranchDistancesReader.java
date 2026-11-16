package it.unisa.ocelot.genetic.objectives;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndDistance;

/**
 * A cache for branch distances.
 * Only for the last run test (based on 
 * reading the current fitnessValues.txt file).
 */
public final class BranchDistancesReader {
	/**
	 * A cache for branch distances.
	 * Maps {@link String}s identifying
	 * function + branch combinations
	 * to the corresponding distances.
	 */
	private static HashMap<String, Double> branchDistances = new HashMap<>();

	/**
	 * Reads from fitnessValues.txt the 
	 * branch distances from a C test run.
	 */
	public static void read() {
		branchDistances.clear();
		try (BufferedReader fitnessValuesFile = new BufferedReader(new FileReader("./fitnessValues.txt"))) {
			String line = fitnessValuesFile.readLine();
			while (line != null) {
				FunBranchNameAndDistance infoFromLine = readInfoFromLine(line);
				branchDistances.put(infoFromLine.getFunBranchName(), infoFromLine.getBranchDistance());
				line = fitnessValuesFile.readLine();
			}
		} catch (IOException e) {
			System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
		}
	}

	private static FunBranchNameAndDistance readInfoFromLine(String line) {
		String listOfItems[] = line.split(";");
		String functionName = listOfItems[0];
		String branchName = listOfItems[1];
		String branchDistanceString = listOfItems[2];
		String functionAndBranchName = functionName + ":" + branchName;
		branchDistanceString = branchDistanceString.replace(",", ".");
		double branchDistance = Double.parseDouble(branchDistanceString);
		if (branchDistance > 1) {
			System.err.println("Wrong branch distance value from fitnessValues.txt, function+branch=" + functionAndBranchName + ", branchDistance=" + branchDistance);
			return null;
		}
		FunBranchNameAndDistance infoFromLine = new FunBranchNameAndDistance(functionAndBranchName, branchDistance);
		return infoFromLine;
	}

	/**
	 * Returns the last scan of fitnessValues.txt 
	 * as a map from {@link String}s identifying
	 * function + branch combinations
	 * to the corresponding distances.
	 */
	public static HashMap<String, Double> get() {
		return branchDistances;
	}
}
