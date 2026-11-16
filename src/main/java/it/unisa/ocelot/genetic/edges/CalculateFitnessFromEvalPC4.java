package it.unisa.ocelot.genetic.edges;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class CalculateFitnessFromEvalPC4 {
	public static double calculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		
		// Reset the objects in ReadEFLFilesForPairCombinationV2.listOfSMs
		// assigning them maximum branch distance
		for (List<TestObjStateMachine> smList : ReadEFLFilesForPairCombinationV2.listOfSMs) {
			for (TestObjStateMachine sm : smList) {
				sm.setBdOne(1.0);
				sm.setBdTwo(1.0);
			}
		}
		
		// Reads the branch distances from the file
		try (BufferedReader fileReader = new BufferedReader(new FileReader("./fitnessValues.txt"))) {
			// Reads the whole file into branchesToDistances
			String line  = fileReader.readLine();
			HashMap<String, Double> branchesToDistances = new HashMap<>();
			while (line  != null) {
				FunBranchNameAndDistance infoFromLine =  readInfoFromLine(line);
				branchesToDistances.put(infoFromLine.getFunBranchName(), infoFromLine.getBranchDistance());
				line = fileReader.readLine();
			}
			
			// Updates all the objects in ReadEFLFilesForPairCombinationV2.listOfSMs
			for (List<TestObjStateMachine> smList : ReadEFLFilesForPairCombinationV2.listOfSMs) {
				for (TestObjStateMachine sm : smList) {
					if (branchesToDistances.get(sm.getTestObjOne()) != null) {
						FunBranchNameAndDistance infoFromLine = new FunBranchNameAndDistance(sm.getTestObjOne(), branchesToDistances.get(sm.getTestObjOne()));
						sm.transition(infoFromLine);
					}
					if (branchesToDistances.get(sm.getTestObjTwo()) != null) {
						FunBranchNameAndDistance infoFromLine= new FunBranchNameAndDistance(sm.getTestObjTwo(), branchesToDistances.get(sm.getTestObjTwo()));
						sm.transition(infoFromLine);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error while reading fitnessValues.txt file: " + e.getMessage());
		}
		
		//checks coverage
		boolean isCoveredThisTime = false;
		for (List<TestObjStateMachine> smList : ReadEFLFilesForPairCombinationV2.listOfSMs) {
			for (TestObjStateMachine sm : smList) {
				if (!sm.isGenerated()) {
					if (sm.bdOne > 1 || sm.bdTwo > 1) {
						System.err.println("Wrong branch distance value! bdOne=" + sm.bdOne + ", bdTwo=" + sm.bdTwo);
					}
					if (sm.isCovered()) {
						isCoveredThisTime = true;
						
						//adds arguments to sm's argument list
						String argumentsSMString = sm.getArgumentList();
						argumentsSMString = argumentsSMString.replaceAll("null", "");
						String argumentsString = argumentsToString(arguments);
						if (!argumentsSMString.contains(argumentsString)) {
							argumentsSMString = argumentsSMString + argumentsString;
						}
						sm.setArgumentList(argumentsSMString);
					} else {
						//updates branchDistance
						fitness += (sm.bdOne + sm.bdTwo);
						
						//clamps
						if (fitness == Double.POSITIVE_INFINITY) {
							fitness= Double.MAX_VALUE;
						}
					}
				}
			}
		}
		if (isCoveredThisTime) {
			return 0.0;
		} else {
			return fitness;
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
		FunBranchNameAndDistance infoFromLine = new FunBranchNameAndDistance(functionAndBranchName, branchDistance);
		return infoFromLine;
	}

	public static String argumentsToString(Object[][][] arguments) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < arguments.length; i++) {
			for (int j = 0; j < arguments[i].length; j++) {
				for (int k = 0; k < arguments[i][j].length; k++) {
					result.append(arguments[i][j][k]);
					result.append(",");
				}
			}
		}
		result.deleteCharAt(result.length() - 1);
		result.append(";");
		return result.toString();
	}

	/*private static boolean checkIfKeySet(ArrayList<String> keySetsToCheck, String fileNameWOepc) {
		// TODO Auto-generated method stub
		boolean isfound;
		if (keySetsToCheck.isEmpty()) {
			isfound = false;
		} else {
			for (String key : keySetsToCheck) {
				//TODO this if statement is incorrect!!!!!
				if (key.contentEquals(fileNameWOepc)) {
					isfound = true;
				}
			}
		}

		return isfound;
	}*/

	/*private static void Fill_Files_PC_PairCom_FitnessVals(String fileName, double currFitness, boolean testCovered,
			boolean testGenerated, boolean first) {
		// TODO Auto-generated method stub
		// FitType tempFitType= new FitType(testCovered, testGenerated, first, "",
		// false);
		// List<FNameFitValType> Fname_Vals= new ArrayList<FNameFitValType>();
		for (Entry<String, FitType> entry : ReadEFLfilesforPairCombination_V2.files_PC_PairCom_FitnessVals.entrySet()) {
			if (entry.getKey().contains(fileName)) {
				// if(!entry.getValue().isTestCovered()) {
				for (FNameFitValType fnameVal : entry.getValue().getFname_Val()) {
					if (fnameVal.getfName().toString().contentEquals(fileName)) {
						// System.out.println(fnameVal.getfName()+"°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
						fnameVal.setFitnessVal(currFitness);
						// Fname_Vals.add(fnameVal);
					}
				}
				entry.getValue().setFirst(first);
				// }
				
			}

		}
		// tempFitType.setFname_Val(Fname_Vals);
		// entry.setValue(tempFitType);
	}
    */
	/*private static void FillPairWiseList(String fileNameWOepc, double currFitness) {
		// ReadEFLfilesforPairCombination.pathTCfitness
		for (Entry<String, EFLType> entry : ReadEFLfilesforPairCombination_V2.pathTCfitness.entrySet()) {
			if (!entry.getValue().isTCcovered()) { // update the new fitness only is not covered
				for (FNameFitValType fnameVal : entry.getValue().getFname_Val()) {
					// System.out.println(fnameVal.getfName()+"-"+fileNameWOepc);
					if (fnameVal.getfName().toString().contentEquals(fileNameWOepc)) {
						// System.out.println(fnameVal.getfName()+"°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
						fnameVal.setFitnessVal(currFitness);
					}
				}
			}
		}
	}
    */
	/*private static String extractFileNameOnly(File file) {
		String fName = file.toString();
		if (fName == null || fName.isEmpty()) {
			return "";
		}
		int indexOfSeparator = Math.max(fName.lastIndexOf('/'), fName.lastIndexOf('\\'));
		fName = fName.substring(indexOfSeparator + 1);
		return fName = fName.replaceAll(".epc", "");
	}
	*/

}
