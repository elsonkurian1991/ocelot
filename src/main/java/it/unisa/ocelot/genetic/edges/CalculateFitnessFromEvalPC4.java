package it.unisa.ocelot.genetic.edges;

import static org.mockito.Mockito.RETURNS_SMART_NULLS;

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

import javax.swing.text.StyleContext.SmallAttributeSet;

import org.apache.commons.math3.analysis.function.Max;

import io.github.pavelicii.allpairs4j.AllPairs;
import io.github.pavelicii.allpairs4j.Parameter;

public class CalculateFitnessFromEvalPC4 {

	
	public static double CalculateFitness(Object[][][] arguments) {
		double fitness = 0.0;
		//  to reset the fitness value for each iterations.
		for (TestObjStateMachine sm : ReadEFLfilesforPairCombination_V2.files_SM_PC_FitVals) {
			sm.setFitValOne(1);
			sm.setFitValTwo(1);
		}
		// read the fitness values from the file.
		try (BufferedReader f_Val_File = new BufferedReader(new FileReader("./fitnessValues.txt"))) { 
			String lineBr  = f_Val_File.readLine();
			HashMap<String, Double> fitnessHashMap = new HashMap<String, Double>();
			while (lineBr  != null) {
				FunBranchNameAndFitness infoFromLinebr =  readInfoFromLine(lineBr);
				fitnessHashMap.put(infoFromLinebr.getFunBranchName(), infoFromLinebr.getCurrFitnessVal());
				lineBr  = f_Val_File.readLine();
			}
			for (TestObjStateMachine sm : ReadEFLfilesforPairCombination_V2.files_SM_PC_FitVals) {
				//update the fitness values for each state machine (considering each pair as a SM).
				if (fitnessHashMap.get(sm.getTestObjOne()) != null) {
					FunBranchNameAndFitness infoFromLinebr= new FunBranchNameAndFitness(sm.getTestObjOne(), fitnessHashMap.get(sm.getTestObjOne()));
					sm.transition(infoFromLinebr);}
				if (fitnessHashMap.get(sm.getTestObjTwo()) != null) {
					FunBranchNameAndFitness infoFromLinebr= new FunBranchNameAndFitness(sm.getTestObjTwo(), fitnessHashMap.get(sm.getTestObjTwo()));
					sm.transition(infoFromLinebr);}
			}
		}
		catch (IOException e) {
			System.err.println("Error reading fitnessValues.txt file: " + e.getMessage());
		}
		//check the fitness and state is covered or not? of each SM
		boolean isCoveredThisTime = false;
		for (TestObjStateMachine sm : ReadEFLfilesforPairCombination_V2.files_SM_PC_FitVals) {
			if (!sm.isGenerated()) {
				//FitType tempVal = set.getValue();
				boolean pairCovered = true;
				if(!sm.isCovered()) {//is SM is covered?
					pairCovered = false;
					if(sm.fitValOne>1 || sm.fitValTwo>1)
						System.err.println("Wrong fitness value! Val1:"+sm.fitValOne+" Val2:"+sm.fitValTwo);
					fitness += sm.fitValOne+sm.fitValTwo; //add the fitness values
					if(fitness == Double.POSITIVE_INFINITY) {
						fitness= Double.MAX_VALUE;
					}
				}				
				if (pairCovered) {
					isCoveredThisTime = true;
					String tempArgs = sm.getArgumentList();
					tempArgs = tempArgs.replaceAll("null", "");
					String args = GetArguInString(arguments);
					if (!tempArgs.contains(args)) {
						tempArgs = tempArgs + args;
					}
					sm.setArgumentList(tempArgs);
				} 
			}
			
		}
		if (isCoveredThisTime) {
			return 0; //return 0 if the SM is covered in this time else sum of fitness values
		} else {
			return fitness;
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
		infoFromLinebr.setFunBranchName(fun_BranchName);
		infoFromLinebr.setCurrFitnessVal(currFitness);
		return infoFromLinebr;
	}

	public static String GetArguInString(Object[][][] args2) {
		StringBuilder argsList = new StringBuilder();
		for (int i = 0; i < args2.length; i++) {
			for (int j = 0; j < args2[i].length; j++) {
				for (int k = 0; k < args2[i][j].length; k++) {
					argsList.append(args2[i][j][k]);
					argsList.append(",");
				}

			}

		}
		argsList.deleteCharAt(argsList.length() - 1);
		argsList.append(";");
		return argsList.toString();
	}

	/*private static boolean checkIfKeySet(ArrayList<String> keySetsToCheck, String fileNameWOepc) {
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
