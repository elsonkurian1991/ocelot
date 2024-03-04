package it.unisa.ocelot.genetic.edges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CalculateFitnessFromEvalPC {
	public static Map<String, FType> filesWithFitnessVals = new HashMap<>();
	public static boolean isFirsttime = false;

	public static double CalculateFitness() {

		double fitness = 0.0;
		File directory = new File("./");// ocelot

		// Check if the directory exists
		if (directory.exists() && directory.isDirectory()) {
			// Get all files in the directory
			File[] files = directory.listFiles();
			boolean alreadyFound =false;
			for (FType set : filesWithFitnessVals.values()) {
				if(!set.isTestGenerated())
				alreadyFound= alreadyFound||set.isFirst();
			}
			// Iterate through the files and select only the .epc files
			// make it sort by type of files
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".epc")) {
						try {
							double currFitness = Double
									.parseDouble(new String(Files.readAllBytes(Paths.get(file.toURI()))));

							System.out.println(file + " fitnessEvalPC is :" + currFitness);
							if (filesWithFitnessVals.containsKey(file.toString())) {

								FType tempFitness = filesWithFitnessVals.get(file.toString());
								if (!tempFitness.isTestGenerated()) {

									if (tempFitness.isTestCovered() && currFitness != 0) {
										FType temp = new FType(Double.MAX_VALUE, true, false,tempFitness.isFirst());
										filesWithFitnessVals.put(file.toString(), temp);
									} else if (currFitness == 0 && tempFitness.isTestCovered()) {
										FType temp = new FType(0, true, false,tempFitness.isFirst());							
										filesWithFitnessVals.put(file.toString(), temp);
									}
									else if(currFitness==0){
										FType temp = new FType(0, true, false,!alreadyFound);							
										filesWithFitnessVals.put(file.toString(), temp);
									}
									else {
										FType temp = new FType(currFitness, false, false,tempFitness.isFirst());
										filesWithFitnessVals.put(file.toString(), temp);
									}
								}
							} else {
								if(currFitness==0) {
									filesWithFitnessVals.put(file.toString(), new FType(0, true, false,true));
								}
								else {
									filesWithFitnessVals.put(file.toString(), new FType(currFitness, false, false,false));
								}
								
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		} else {
			System.out.println("Invalid directory path.");
		}
	
		System.out.println(filesWithFitnessVals);
		for (Entry<String, FType> set : filesWithFitnessVals.entrySet()) {

			System.out.println(set.getKey() + " = " + set.getValue());
			
			if (!set.getValue().isTestGenerated()) {
				if(set.getValue().isTestCovered()) {
					return 0;
				}
				else {
					fitness += set.getValue().getFitnessValue();
				}
		
			}
			

			// }
		}
		System.out.println("copy fitness." + fitness);
		return fitness;
	}

}
