package it.unisa.ocelot.genetic.edges;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

public class CalculateFitnessFromEvalPC3 {
	public static boolean isFirsttime = false;
	public static StringBuilder linesFromFitnessFiles = new StringBuilder();
	public static int fileReadingCount = 0;
	
	public static double calculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		for (Entry<String, FitType> entry : ReadEFLFilesForPairCombination.filesPCPairComFitnessVals.entrySet()) {
			//TODO WHAT??????
			for (FNameFitValType fnameVal : entry.getValue().getFname_Val()) {
				fnameVal.setFitnessVal(Double.MAX_VALUE);
			}
		}
		try (BufferedReader fiValFile = new BufferedReader(new FileReader("./fitnessValues.txt"))) { 
			//TODO this file should be written while parsing
			String lineBr;
			ArrayList<String> keySetsToCheck = new ArrayList<>();
			boolean alreadyFound = false;
			while ((lineBr = fiValFile.readLine()) != null) {
				// TODO read the fitness of each branch
				String listOfItems[] = lineBr.split(";");
				String fName = listOfItems[0];
				String branchName = listOfItems[1];
				String fitnessVal = listOfItems[2];
				fitnessVal = fitnessVal.replaceAll(",", ".");
				double currFitness = Double.parseDouble(fitnessVal);
				String fun_BranchName = fName + "_" + branchName;
				for (Entry<String, FitType> entry : ReadEFLFilesForPairCombination.filesPCPairComFitnessVals.entrySet()) {
					if (entry.getKey().contains(fun_BranchName)) {
						if (!checkIfKeySet(keySetsToCheck, fun_BranchName)) {
							FitType tempFitness = entry.getValue();
							alreadyFound = alreadyFound || tempFitness.isFirst();
							if (currFitness == 0) {
								fillFilesPCPairComFitnessVals(fun_BranchName, currFitness, true, false, !alreadyFound);
							} else {
								fillFilesPCPairComFitnessVals(fun_BranchName, currFitness, false, false, tempFitness.isFirst());
							}
							keySetsToCheck.add(fun_BranchName);
						}
					}
				}
				linesFromFitnessFiles.append(lineBr + "\n");
			}
		} catch (IOException e) {
			System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
		}
		// delete the file every iteration
		//TODO really sure?
		/*Path toDelete = Paths.get("./fitnessValues.txt");
		try {
			Files.delete(toDelete);
		} catch (IOException e) {
			System.err.println("Error deleting file fitnessValues.txt: " + e.getMessage());
		}*/
		
		linesFromFitnessFiles.append("next iteration...\n");
		
		//is it covered?
		boolean isCoveredThisTime = false;
		for (Entry<String, FitType> set : ReadEFLFilesForPairCombination.filesPCPairComFitnessVals.entrySet()) {
			if (!set.getValue().isTestGenerated()) {
				FitType tempVal = set.getValue();
				boolean pairCovered = true;
				for (FNameFitValType fnameVal : tempVal.getFname_Val()) {
					if (Math.abs(fnameVal.getFitnessVal()) > 0) { // to check is >= 0.0
						pairCovered = false;
						fitness = fitness + fnameVal.getFitnessVal();
						//Clamps fitness
						if (fitness == Double.POSITIVE_INFINITY) {
							fitness = Double.MAX_VALUE;
						}
					}
				}
				if (pairCovered) {
					isCoveredThisTime = true;
					tempVal.setTestCovered(pairCovered);
					String tempArgs = tempVal.getArgumentList();
					tempArgs = tempArgs.replaceAll("null", "");
					String args = argsToString(arguments);
					if (!tempArgs.contains(args)) {
						tempArgs = tempArgs + args;
					}
					tempVal.setArgumentList(tempArgs);
				} else {
					tempVal.setTestCovered(pairCovered);
				}
			}
		}
		
		if (isCoveredThisTime) {
			return 0;
		} else {
			return fitness;
		}
	}

	public static String argsToString(Object[][][] args) {
		StringBuilder argsList = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			for (int j = 0; j < args[i].length; j++) {
				for (int k = 0; k < args[i][j].length; k++) {
					argsList.append(args[i][j][k].toString());
					argsList.append(',');
				}
			}
		}
		String result = argsList.substring(0, argsList.length() - 1) + ";";
		return result;
	}

	private static boolean checkIfKeySet(ArrayList<String> keySetsToCheck, String fileNameWOepc) {
		boolean found = false;
		for (String key : keySetsToCheck) {
			if (key.contentEquals(fileNameWOepc)) {
				found = true;
				break;
			}
		}
		return found;
	}

	private static void fillFilesPCPairComFitnessVals(String fileName, double currFitness, boolean testCovered, boolean testGenerated, boolean first) {
		for (Entry<String, FitType> entry : ReadEFLFilesForPairCombination.filesPCPairComFitnessVals.entrySet()) {
			if (entry.getKey().contains(fileName)) {
				// if(!entry.getValue().isTestCovered()) {
				
				//TODO WHAT???????
				for (FNameFitValType fnameVal : entry.getValue().getFname_Val()) {
					if (fnameVal.getfName().toString().contentEquals(fileName)) {
						fnameVal.setFitnessVal(currFitness);
					}
				}
				entry.getValue().setFirst(first);
			}
		}
	}
}
