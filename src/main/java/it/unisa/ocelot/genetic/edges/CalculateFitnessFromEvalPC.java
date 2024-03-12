package it.unisa.ocelot.genetic.edges;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.pavelicii.allpairs4j.AllPairs;
import io.github.pavelicii.allpairs4j.Parameter;

public class CalculateFitnessFromEvalPC {
	public static Map<String, FType> filesWithFitnessVals = new HashMap<>();
	public static boolean isFirsttime = false;

	public static double CalculateFitness() {
	//	ReadEFLfilesforPairCombination.RunEFLfilesforPairCombination(); // run this to read the efl file and create pairwise combinations. find a best place to call this
		double fitness = 0.0;
		File directory = new File("./");// ocelot
		List<Parameter> parameterList = new ArrayList<>();
		//AllPairs listPairs=ReadEFLfilesforPairCombination.generateAllPairs(ReadEFLfilesforPairCombination.genParameterListEFL(parameterList));
		//ReadEFLfilesforPairCombination.pathTCfitness.
		//ReadEFLfilesforPairCombination.listofFunPaths.toString();
		//ReadEFLfilesforPairCombination.generateAllPairs(null)
		// Check if the directory exists
		if (directory.exists() && directory.isDirectory()) {
			// Get all files in the directory
			File[] files = directory.listFiles();
			boolean alreadyFound =false;
			for (FType set : filesWithFitnessVals.values()) {
				if(!set.isTestGenerated())
				alreadyFound= alreadyFound||set.isFirst();
			}
			// Iterate through the files and select only the .epc files  //ReadEFLfilesforPairCombination.pathTCfitness
			// make it sort by type of files
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".epc")) {
						try {
							double currFitness = Double
									.parseDouble(new String(Files.readAllBytes(Paths.get(file.toURI()))));
							String fileNameWOepc=extractFileNameOnly(file);
							System.out.println(fileNameWOepc);
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
										FillPairWiseList(fileNameWOepc,currFitness);
									}
									else if(currFitness==0){
										FType temp = new FType(0, true, false,!alreadyFound);							
										filesWithFitnessVals.put(file.toString(), temp);
										FillPairWiseList(fileNameWOepc,currFitness);
									}
									else {
										FType temp = new FType(currFitness, false, false,tempFitness.isFirst());
										filesWithFitnessVals.put(file.toString(), temp);
									}
								}
							} else {
								if(currFitness==0) {
									filesWithFitnessVals.put(file.toString(), new FType(0, true, false,true));
									FillPairWiseList(fileNameWOepc,currFitness);
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
	
		//System.out.println(filesWithFitnessVals);
		for (Entry<String, FType> set : filesWithFitnessVals.entrySet()) {

			//System.out.println(set.getKey() + " = " + set.getValue());
			
			if (!set.getValue().isTestGenerated()) {
				if(set.getValue().isTestCovered()) {
					System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
					ReadEFLfilesforPairCombination.CheckTCisCovered(ReadEFLfilesforPairCombination.pathTCfitness);//to update the combinations
					return 0;
				}
				else {
					fitness += set.getValue().getFitnessValue();
				}
		
			}
			

			// }
		}
		//ReadEFLfilesforPairCombination.CheckTCisCovered(ReadEFLfilesforPairCombination.pathTCfitness);//to update the combinations
		System.out.println("copy fitness." + fitness);
		return fitness;
	}

	private static void FillPairWiseList(String fileNameWOepc, double currFitness) {
		//ReadEFLfilesforPairCombination.pathTCfitness
		for(Entry<String, EFLType> entry:ReadEFLfilesforPairCombination.pathTCfitness.entrySet()) {
			if(!entry.getValue().isTCcovered()) { //update the new fitness only is not covered
				for(Entry<String, Double> fnameVal: entry.getValue().getFname_Val().entrySet()){
					System.out.println(fnameVal.getKey()+"-"+fileNameWOepc);
					if(fnameVal.getKey().toString().contentEquals(fileNameWOepc)) {
						System.out.println(fnameVal.getKey()+"°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
						fnameVal.setValue(currFitness);
					}
				}
			}
		}
	}

	private static String extractFileNameOnly(File file) {
		String fName=file.toString();
		if (fName == null || fName.isEmpty()) {
		    return "";
		  }
		  int indexOfSeparator = Math.max(fName.lastIndexOf('/'), fName.lastIndexOf('\\'));
		  fName= fName.substring(indexOfSeparator + 1);
		  return fName=fName.replaceAll(".epc", "");
	}

}
