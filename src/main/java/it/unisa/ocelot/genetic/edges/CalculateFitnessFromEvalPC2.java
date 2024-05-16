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

public class CalculateFitnessFromEvalPC2 {
	
	public static boolean isFirsttime = false;

	public static double CalculateFitness(Object[][][] arguments) {
	//	ReadEFLfilesforPairCombination.RunEFLfilesforPairCombination(); // run this to read the efl file and create pairwise combinations. find a best place to call this
		double fitness = 0.0;
		File directory = new File("./");// ocelot
		List<Parameter> parameterList = new ArrayList<>();
		//copy the list of combination to  filesWithFitnessVals
		boolean isFillPairCom =false;
			
		//AllPairs listPairs=ReadEFLfilesforPairCombination.generateAllPairs(ReadEFLfilesforPairCombination.genParameterListEFL(parameterList));
		//ReadEFLfilesforPairCombination.pathTCfitness.
		//ReadEFLfilesforPairCombination.listofFunPaths.toString();
		//ReadEFLfilesforPairCombination.generateAllPairs(null)
		// Check if the directory exists
		if (directory.exists() && directory.isDirectory()) {
			// Get all files in the directory
			File[] files = directory.listFiles();
			
			// Iterate through the files and select only the .epc files  //ReadEFLfilesforPairCombination.pathTCfitness
			// make it sort by type of files
			if (files != null) {
				for (File file : files) {
					if (file.isFile() && file.getName().endsWith(".epc")) {
						try {
							double currFitness = Double
									.parseDouble(new String(Files.readAllBytes(Paths.get(file.toURI()))));
							String fileNameWOepc=extractFileNameOnly(file);
							//System.out.println(fileNameWOepc);
						//	System.out.println(file + " fitnessEvalPC is :" + currFitness);
							ArrayList<String> keySetsToCheck=new ArrayList<>();
							boolean alreadyFound =false;
					
							for(Entry<String, FitType> entry:ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {
								if(entry.getKey().contains(fileNameWOepc)) {
									//System.out.println(entry);
						            if(!checkIfKeySet(keySetsToCheck,fileNameWOepc)) {
									FitType tempFitness = entry.getValue(); 
								//	if (!tempFitness.isTestGenerated()) {
										alreadyFound= alreadyFound||tempFitness.isFirst();
										
										if(currFitness==0){
											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,!alreadyFound);
										}
										else {

											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,false,false,tempFitness.isFirst());
										}
										/*if (tempFitness.isTestCovered() && currFitness != 0) {
											//FType temp = new FType(Double.MAX_VALUE, true, false,tempFitness.isFirst());
											//filesWithFitnessVals.put(file.toString(), temp);
											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,Double.MAX_VALUE,true,false,tempFitness.isFirst());
										} else if (currFitness == 0 && tempFitness.isTestCovered()) {
											//FType temp = new FType(0, true, false,tempFitness.isFirst());							
											//filesWithFitnessVals.put(file.toString(), temp);
											//FillPairWiseList(fileNameWOepc,currFitness);
											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,tempFitness.isFirst());
										}
										else if(currFitness==0){
											//FType temp = new FType(0, true, false,!alreadyFound);							
											//filesWithFitnessVals.put(file.toString(), temp);
											//FillPairWiseList(fileNameWOepc,currFitness);
											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,!alreadyFound);
										}
										else {
											//FType temp = new FType(currFitness, false, false,tempFitness.isFirst());
											//filesWithFitnessVals.put(file.toString(), temp);
											Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,false,false,tempFitness.isFirst());
										}*/
									//}
									keySetsToCheck.add(fileNameWOepc);
								}
							
							}
								
							}
							/*if (ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.containsKey(fileNameWOepc)) { //fix this no keys 

								FitType tempFitness = ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.get(fileNameWOepc); 
								if (!tempFitness.isTestGenerated()) {

									if (tempFitness.isTestCovered() && currFitness != 0) {
										//FType temp = new FType(Double.MAX_VALUE, true, false,tempFitness.isFirst());
										//filesWithFitnessVals.put(file.toString(), temp);
										Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,Double.MAX_VALUE,true,false,tempFitness.isFirst());
									} else if (currFitness == 0 && tempFitness.isTestCovered()) {
										//FType temp = new FType(0, true, false,tempFitness.isFirst());							
										//filesWithFitnessVals.put(file.toString(), temp);
										//FillPairWiseList(fileNameWOepc,currFitness);
										Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,tempFitness.isFirst());
									}
									else if(currFitness==0){
										//FType temp = new FType(0, true, false,!alreadyFound);							
										//filesWithFitnessVals.put(file.toString(), temp);
										//FillPairWiseList(fileNameWOepc,currFitness);
										Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,!alreadyFound);
									}
									else {
										//FType temp = new FType(currFitness, false, false,tempFitness.isFirst());
										//filesWithFitnessVals.put(file.toString(), temp);
										Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,false,false,tempFitness.isFirst());
									}
								}
							} else {
								if(currFitness==0) {
									//filesWithFitnessVals.put(file.toString(), new FType(0, true, false,true));
									Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,true,false,true);
									//FillPairWiseList(fileNameWOepc,currFitness);
								}
								else {
									
									Fill_Files_PC_PairCom_FitnessVals(fileNameWOepc,currFitness,false,false,false);
									//filesWithFitnessVals.put(file.toString(), new FType(currFitness, false, false,false));
								}
								
							}*/

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
		boolean isCoveredThisTime=false;
		for (Entry<String, FitType> set : ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {			
			
			if (!set.getValue().isTestGenerated()) {
				FitType tempVal= set.getValue();
				boolean testCovered=true;
				for(FNameFitValType fnameVal: tempVal.getFname_Val()){
					if (Math.abs(fnameVal.getFitnessVal()) > 0) {//to check is >= 0.0
					//if(fnameVal.getFitnessVal().equals("0.0")){
						testCovered=false;
						fitness+= fnameVal.getFitnessVal();						
					}					
				}
				if(testCovered) {
					isCoveredThisTime=true;
					tempVal.setTestCovered(testCovered);
					String tempArgs= tempVal.getArgumentList();
					tempArgs=tempArgs.replaceAll("null", "");
					String args=GetArguInString(arguments);
					if(!tempArgs.contains(args)) {
						tempArgs=tempArgs+args;
					}
					tempVal.setArgumentList(tempArgs);
				}
				else {
					tempVal.setTestCovered(testCovered);
				}
				
			}
				/*if(set.getValue().isTestCovered()) {
					System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
					ReadEFLfilesforPairCombination.CheckTCisCovered(ReadEFLfilesforPairCombination.pathTCfitness,arguments);//to update the combinations
					return 0;
				}
				else {
					fitness +=10; //set.getValue().getFitnessValue();//TODO
				}
		*/
			
			

			// }
		}
		//ReadEFLfilesforPairCombination.CheckTCisCovered(ReadEFLfilesforPairCombination.pathTCfitness);//to update the combinations
		
		if(isCoveredThisTime) {
			return 0;
		}
		else {
			//System.out.println("copy fitness." + fitness);
			return fitness;
		}
		
	}



	public static String GetArguInString(Object[][][] args2) {
		String argsList="";
		for (int i = 0; i < args2.length; i++) {
            for (int j = 0; j < args2[i].length; j++) {
                for (int k = 0; k < args2[i][j].length; k++) {
                	//  System.out.println("args2[" + i + "][" + j + "][" + k + "] = " + args2[i][j][k]);
                    argsList=argsList+args2[i][j][k].toString();
                    argsList=argsList+",";
                }
               
            }
            
        }
		argsList=argsList.substring(0, argsList.length()-1);
		argsList=argsList+";";
		
		//System.out.println(argsList);
		return argsList;
	}



	private static boolean checkIfKeySet(ArrayList<String> keySetsToCheck, String fileNameWOepc) {
		// TODO Auto-generated method stub
		boolean isfound=false;
		if(keySetsToCheck.isEmpty()) {
			isfound= false;
		}
		else {
			for (String key : keySetsToCheck) {
				if(key.contentEquals(fileNameWOepc)) {
					isfound= true;
				}
				else {
					isfound= false;
				}
				
			}
		}
		
		return isfound;
	}



	private static void Fill_Files_PC_PairCom_FitnessVals(String fileName, double currFitness, boolean testCovered,
			boolean testGenerated,boolean first) {
		// TODO Auto-generated method stub
		//FitType tempFitType= new FitType(testCovered, testGenerated, first, "", false);
		//List<FNameFitValType> Fname_Vals= new ArrayList<FNameFitValType>();
		for(Entry<String, FitType> entry:ReadEFLfilesforPairCombination.files_PC_PairCom_FitnessVals.entrySet()) {
			if(entry.getKey().contains(fileName)) {
				//if(!entry.getValue().isTestCovered()) {
					for(FNameFitValType fnameVal: entry.getValue().getFname_Val()){
						if(fnameVal.getfName().toString().contentEquals(fileName)) {
							//System.out.println(fnameVal.getfName()+"°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
							fnameVal.setFitnessVal(currFitness);
							//Fname_Vals.add(fnameVal);
						}
					}
					entry.getValue().setFirst(first);
				//}
				/*else {
					if(currFitness>0.0) {
						for(FNameFitValType fnameVal: entry.getValue().getFname_Val()){
							if(fnameVal.getfName().toString().contentEquals(fileName)) {
								System.out.println(fnameVal.getfName()+"°°°°°°°already test convered°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
								fnameVal.setFitnessVal(Double.MAX_VALUE);
								//Fname_Vals.add(fnameVal);
							}
						}
					}
				}*/
			}

		}
		//tempFitType.setFname_Val(Fname_Vals);
		//entry.setValue(tempFitType);
	}




	private static void FillPairWiseList(String fileNameWOepc, double currFitness) {
		//ReadEFLfilesforPairCombination.pathTCfitness
		for(Entry<String, EFLType> entry:ReadEFLfilesforPairCombination.pathTCfitness.entrySet()) {
			if(!entry.getValue().isTCcovered()) { //update the new fitness only is not covered
				for(FNameFitValType fnameVal: entry.getValue().getFname_Val()){
					//System.out.println(fnameVal.getfName()+"-"+fileNameWOepc);
					if(fnameVal.getfName().toString().contentEquals(fileNameWOepc)) {
						//	System.out.println(fnameVal.getfName()+"°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°");
						fnameVal.setFitnessVal(currFitness);
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
