package it.unisa.ocelot.genetic.objectives;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import it.unisa.ocelot.genetic.edges.FunBranchNameAndFitness;

public class BranchDistanceCache {

	private static HashMap<String, Double> fitnessHashMap = new HashMap<String, Double>();

	public static void cacheFitnessValues() {
		fitnessHashMap.clear();
		try (BufferedReader f_Val_File = new BufferedReader(new FileReader("./fitnessValues.txt"))) {
			String lineBr = f_Val_File.readLine();
			while (lineBr != null) {
				FunBranchNameAndFitness infoFromLinebr = readInfoFromLine(lineBr);
				fitnessHashMap.put(infoFromLinebr.getFunBranchName(), infoFromLinebr.getCurrFitnessVal());
				lineBr = f_Val_File.readLine();
			}
		} catch (IOException e) {
			System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
		}
	}

	private static FunBranchNameAndFitness readInfoFromLine(String lineBr) {
		FunBranchNameAndFitness infoFromLinebr = new FunBranchNameAndFitness();
		String listOfItems[] = lineBr.split(";");
		String fName = listOfItems[0];
		String branchName = listOfItems[1];
		String fitnessVal = listOfItems[2];
		String fun_BranchName = fName + "_" + branchName;
		fitnessVal = fitnessVal.replace(",", ".");
		double currFitness = Double.parseDouble(fitnessVal);
		if (currFitness > 1)
			System.err.println("Wrong fitness value! Branch:" + fun_BranchName + " Fitness:" + currFitness);
		infoFromLinebr.setFunBranchName(fun_BranchName);
		infoFromLinebr.setCurrFitnessVal(currFitness);
		return infoFromLinebr;
	}

	public static HashMap<String, Double> getBranchDistances() {
		return fitnessHashMap;
	}

}
