package it.unisa.ocelot.genetic.edges;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.analysis.function.Max;

import io.github.pavelicii.allpairs4j.AllPairs;
import io.github.pavelicii.allpairs4j.Parameter;

public class CalculateFitnessFromEvalPC3 {

	public static boolean isFirsttime = false;
	public static StringBuilder linesFromFitnessFiles = new StringBuilder();

	public static double CalculateFitness(Object[][][] arguments) {
		//System.out.println(ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals);
		double fitness = 0.0;
		//  to rest the fitness value for each iterations.
		for (Entry<String, FitType> entry : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {
			for (FNameFitValType fnameVal : entry.getValue().getFname_Val()) {
				fnameVal.setFitnessVal(Double.MAX_VALUE);
			}
		}
		//System.out.println(ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals);
		try (BufferedReader fiValFile = new BufferedReader(new FileReader("./fitnessValues.txt"))) { // TODO this file
																										// should
																										// written while
																										// parseing
																										// KQuery files.
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
				for (Entry<String, FitType> entry : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals
						.entrySet()) {
					// System.out.println(entry.getKey());
					// System.out.println(fun_BranchName);
					if (entry.getKey().contains(fun_BranchName)) {
						// System.out.println(entry.getKey());
						if (!checkIfKeySet(keySetsToCheck, fun_BranchName)) {
							FitType tempFitness = entry.getValue();
							alreadyFound = alreadyFound || tempFitness.isFirst();

							if (currFitness == 0) {
								Fill_Files_PC_PairCom_FitnessVals(fun_BranchName, currFitness, true, false,
										!alreadyFound);
							} else {

								Fill_Files_PC_PairCom_FitnessVals(fun_BranchName, currFitness, false, false,
										tempFitness.isFirst());
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
		/*Path pathTodel = Paths.get("./fitnessValues.txt");// delete the file every iteration
		try {
			Files.delete(pathTodel);
			// System.out.println("File fitnessValues.txt deleted successfully: ");
		} catch (IOException e) {
			System.err.println("Error deleting file fitnessValues.txt: " + e.getMessage());
		}*/
		linesFromFitnessFiles.append("next iteration...\n");
		// System.out.println(ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals);

		
		// till here.....................................
		// System.out.println(filesWithFitnessVals);
		boolean isCoveredThisTime = false;
		for (Entry<String, FitType> set : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {

			if (!set.getValue().isTestGenerated()) {
				FitType tempVal = set.getValue();
				boolean pairCovered = true;
				for (FNameFitValType fnameVal : tempVal.getFname_Val()) {
					if (Math.abs(fnameVal.getFitnessVal()) > 0) {// to check is >= 0.0
						// if(fnameVal.getFitnessVal().equals("0.0")){
						pairCovered = false;
						fitness = fitness + fnameVal.getFitnessVal();
						if (fitness == Double.POSITIVE_INFINITY)
							fitness = Double.MAX_VALUE;
					}
				}
				if (pairCovered) {
					isCoveredThisTime = true;
					tempVal.setTestCovered(pairCovered);
					String tempArgs = tempVal.getArgumentList();
					tempArgs = tempArgs.replaceAll("null", "");
					String args = GetArguInString(arguments);
					if (!tempArgs.contains(args)) {
						tempArgs = tempArgs + args;
					}
					tempVal.setArgumentList(tempArgs);
				} else {
					tempVal.setTestCovered(pairCovered);
				}

			}
			/*
			 * if(set.getValue().isTestCovered()) {
			 * System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$"
			 * ); ReadEFLfilesforPairCombination.CheckTCisCovered(
			 * ReadEFLfilesforPairCombination.pathTCfitness,arguments);//to update the
			 * combinations return 0; } else { fitness +=10;
			 * //set.getValue().getFitnessValue();//TODO }
			 */

			// }
		}
		// ReadEFLfilesforPairCombination.CheckTCisCovered(ReadEFLfilesforPairCombination.pathTCfitness);//to
		// update the combinations
		//System.out.println(ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals);
		if (isCoveredThisTime) {
			return 0;
		} else {
			// System.out.println("copy fitness." + fitness);
			return fitness;
		}

	}

	public static String GetArguInString(Object[][][] args2) {
		String argsList = "";
		for (int i = 0; i < args2.length; i++) {
			for (int j = 0; j < args2[i].length; j++) {
				for (int k = 0; k < args2[i][j].length; k++) {
					// System.out.println("args2[" + i + "][" + j + "][" + k + "] = " +
					// args2[i][j][k]);
					argsList = argsList + args2[i][j][k].toString();
					argsList = argsList + ",";
				}

			}

		}
		argsList = argsList.substring(0, argsList.length() - 1);
		argsList = argsList + ";";

		// System.out.println(argsList);
		return argsList;
	}

	private static boolean checkIfKeySet(ArrayList<String> keySetsToCheck, String fileNameWOepc) {
		// TODO Auto-generated method stub
		boolean isfound = false;
		if (keySetsToCheck.isEmpty()) {
			isfound = false;
		} else {
			for (String key : keySetsToCheck) {
				if (key.contentEquals(fileNameWOepc)) {
					isfound = true;
				} else {
					isfound = false;
				}

			}
		}

		return isfound;
	}

	private static void Fill_Files_PC_PairCom_FitnessVals(String fileName, double currFitness, boolean testCovered,
			boolean testGenerated, boolean first) {
		// TODO Auto-generated method stub
		// FitType tempFitType= new FitType(testCovered, testGenerated, first, "",
		// false);
		// List<FNameFitValType> Fname_Vals= new ArrayList<FNameFitValType>();
		for (Entry<String, FitType> entry : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {
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
				/*
				 * else { if(currFitness>0.0) { for(FNameFitValType fnameVal:
				 * entry.getValue().getFname_Val()){
				 * if(fnameVal.getfName().toString().contentEquals(fileName)) {
				 * System.out.println(fnameVal.getfName()
				 * +"°°°°°°°already test convered°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
				 * fnameVal.setFitnessVal(Double.MAX_VALUE); //Fname_Vals.add(fnameVal); } } } }
				 */
			}

		}
		// tempFitType.setFname_Val(Fname_Vals);
		// entry.setValue(tempFitType);
	}

	private static void FillPairWiseList(String fileNameWOepc, double currFitness) {
		// ReadEFLfilesforPairCombination.pathTCfitness
		for (Entry<String, EFLType> entry : ReadEFLfilesforPairCombination.pathTCfitness.entrySet()) {
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

	private static String extractFileNameOnly(File file) {
		String fName = file.toString();
		if (fName == null || fName.isEmpty()) {
			return "";
		}
		int indexOfSeparator = Math.max(fName.lastIndexOf('/'), fName.lastIndexOf('\\'));
		fName = fName.substring(indexOfSeparator + 1);
		return fName = fName.replaceAll(".epc", "");
	}

}
