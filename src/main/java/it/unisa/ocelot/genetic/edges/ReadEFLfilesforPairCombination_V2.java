package it.unisa.ocelot.genetic.edges;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.math3.analysis.function.Max;

import io.github.pavelicii.allpairs4j.AllPairs;
import io.github.pavelicii.allpairs4j.AllPairs.AllPairsBuilder;
import io.github.pavelicii.allpairs4j.Case;
import io.github.pavelicii.allpairs4j.Parameter;
import javassist.expr.NewArray;

public class ReadEFLfilesforPairCombination_V2 {
	public static HashSet<EvalFunPathType> listofFunPaths = new HashSet<>();//ok
	public static HashSet<FunctionPair> listofIntRelationKeys = new HashSet<>();//ok
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();//ok
	//public static Map<String, EFLType> pathTCfitness = new HashMap<>();
	//public static Map<String, FitType> files_PC_PairCom_FitnessVals = new HashMap<>();
	public static ArrayList<TestObjStateMachine> files_SM_PC_FitVals= new ArrayList<>();//ok make to list todo
	
	public static void RunEFLfilesforPairCombination() throws IOException{ //RunEFLfilesforPairCombination
		// read the test objectives file
		try (BufferedReader br1 = new BufferedReader(new FileReader("./testObjectives.to"))) { 
			String lineBr1;
			while ((lineBr1 = br1.readLine()) != null) {
				//System.out.println(lineBr1);
				AddFunPath(lineBr1); // add to the listofFunPaths??
				//System.out.println("next");
			}
		} catch (IOException e) {
			System.err.println("Error reading testObjectives.to file: " + e.getMessage());
		}
		
		// read the integration key relationship list from file	
		try (BufferedReader br2 = new BufferedReader(new FileReader("./IntRelKeyList.kl"))) { 
			String lineBr2;
			while ((lineBr2 = br2.readLine()) != null) {
				AddIntegrationKeyList(lineBr2); // add to this listofIntRelationKeys??
			}
		} catch (IOException e) {
			System.err.println("Error reading IntRelKeyList.kl file: " + e.getMessage());
		}

		//System.out.println("listofFunPaths::"+listofFunPaths);

		int i=0;
		int j=0;
		for(EvalFunPathType keySet1: listofFunPaths) {
			for(EvalFunPathType keySet2: listofFunPaths) {
				if(!keySet1.getEvalFunName().equals(keySet2.getEvalFunName())) {
					boolean check=checkForKeySets(keySet1,keySet2);
					boolean isIntRel=checkHaveIntegrationRelation(keySet1,keySet2);
					if(!check&isIntRel) {
						ArrayList<String> keySets=new ArrayList<>();
						keySets.add(keySet1.getEvalFunName());
						keySets.add(keySet2.getEvalFunName());
						listofKeys.put(String.valueOf(j), keySets);
						j++;
						i=generatePairWiseCombinations(keySet1,keySet2,i);
					}

				}
			}
		}
		/*
		//System.out.println(pathTCfitness);
        // Copy the entries of the map into a list
        List<Map.Entry<String, EFLType>> tempListforSort = new ArrayList<>(pathTCfitness.entrySet());

        // Sort the list based on keys
        Collections.sort(tempListforSort, new Comparator<Map.Entry<String, EFLType>>() {
            @Override

            public int compare(Map.Entry<String, EFLType> entry1, Map.Entry<String, EFLType> entry2) {
                // Parse the keys as integers before comparing
                int key1 = Integer.parseInt(entry1.getKey());
                int key2 = Integer.parseInt(entry2.getKey());
                return Integer.compare(key1, key2);
               // return entry1.getKey().compareTo(entry2.getKey());
            }
        });

        // Create a new LinkedHashMap to maintain the insertion order
        Map<String, EFLType> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, EFLType> entry : tempListforSort) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        // Update the original map reference
        pathTCfitness = sortedMap;
     
        System.out.println("pathTCfitness::");
        System.out.println(pathTCfitness);
		//CheckTCisCovered(pathTCfitness);
		
        System.out.println("files_PC_PairCom_FitnessVals::");
		System.out.println(files_PC_PairCom_FitnessVals);
		
		*/
		 //System.out.println("files_SM_PC_FitVals::");
		 //System.out.println(files_SM_PC_FitVals);
	
	}
	
	private static boolean checkHaveIntegrationRelation(EvalFunPathType keySet1, EvalFunPathType keySet2) {
		String fun1=keySet1.getEvalFunName();
		String fun2=keySet2.getEvalFunName();
		//System.out.println("fun1:"+fun1+ " fun2:"+fun2);
		 for(FunctionPair relationkeyLists: listofIntRelationKeys){
			 if((relationkeyLists.getFun1().equals(fun1)) && (relationkeyLists.getFun2().equals(fun2))) {
				 // System.out.println(relationkeyLists.toString());
				 return true;
				 }
			 }
	
		return false;
	}

	private static void AddIntegrationKeyList(String lineBr2) {
		
		lineBr2 = lineBr2.replaceAll("[{};]", "");		
		String values[]=lineBr2.split(",");
		FunctionPair inner= new FunctionPair(values[0], values[1]);
		listofIntRelationKeys.add(inner);	
		//System.out.println(listofIntRelationKeys);
	}

	private static boolean checkForKeySets(EvalFunPathType keySet1, EvalFunPathType keySet2) {
		 if(listofKeys.isEmpty()) {
			 return false;
		 }
		 else {
			 for(Entry <String,ArrayList<String>> keyLists: listofKeys.entrySet()){
				 ArrayList<String> lists = keyLists.getValue();
				 String Chk1=lists.get(0);
				 String Chk2=lists.get(1);
				 if(Chk1.equals(keySet2.toString())&&Chk2.equals(keySet1.toString())) {
					 return true;
				 }
				 /*for(String listVal:keyLists.getValue()) {
					 if(listVal.contains(keySet1)&&listVal.contains(keySet2)) {
						 return true;
					 }
				 }*/
			 }
			
		 }
		 return false;
	}

	public static int generatePairWiseCombinations(EvalFunPathType keySet1,EvalFunPathType keySet2, int i) {

		//HashSet<EvalFunPathType> list= listofFunPaths.;
		ArrayList<String> params1=keySet1.getEvalFunPathList(); //TODO add the getName and add to params
		ArrayList<String> params2= keySet2.getEvalFunPathList();
		//EFLType temp= new EFLType(null, null);
		for(String param1: params1) {
			for(String param2: params2) {
				String testObjOne=param1.toString() ;
				double fitValOne=Double.MAX_VALUE;
				String testObjTwo=param2.toString();
				double fitValTwo=Double.MAX_VALUE;
				TestObjStateMachine testobjSM= new TestObjStateMachine(testObjOne,fitValOne,testObjTwo,fitValTwo);
				//String pairCom=param1.toString()+","+param2.toString();
				files_SM_PC_FitVals.add(testobjSM); 
				//files_SM_PC_FitVals.put(pairCom, testobjSM);
				++i;
			}	
		}
		
		return i; 
	}

	/*public static void CheckTCisCovered(Map<String, EFLType> pathTCfitness2, Object[][][] args2) {
		//System.out.println(tempPathTCfitness);
		//Object[][][] arguments = this.getParameters(solution);
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
		
		System.out.println(argsList);
		for(Entry<String, TestObjStateMachine> entry:files_SM_PC_FitVals.entrySet()) {
			TestObjStateMachine valueList= entry.getValue();
			
			List<FNameFitValType> fnameVals=valueList.getFname_Val(); 
			if((fnameVals.get(0).getFitnessVal()==0)&&(fnameVals.get(1).getFitnessVal()==0)){
				String tempArgs=valueList.getArgumentList();
				if(tempArgs.contentEquals("null")) {
					tempArgs="";
				}
				if(!tempArgs.contains(argsList)) {
					tempArgs=tempArgs+argsList;
				}
				valueList.setTCcovered(true); // check that pairwise combination are covered.
				valueList.setArgumentList(tempArgs);
			
				
			}
		}
			
	}
	*/	
	public static void AddFunPath(String line) {
		// TODO Auto-generated method stub
		//HashSet<EvalFunPathType> evalFunPaths = new HashSet<>();
		String keyVal[]=line.split("=");
		String key=keyVal[0];
		key=key.replaceAll("\\{","").trim(); // line = line.replaceAll("[{};]", "");
		String val=keyVal[1].replaceAll("\\}","").trim();
		val=val.replaceAll(";", "").trim();
		String values[]=val.split(",");
		ArrayList<String> valuesList= new ArrayList<String>();
		for(int i=0;i<values.length;i++) {
			String tempValList=key+"_"+values[i];
			valuesList.add(tempValList);
		}
		EvalFunPathType evalFunPathType = new EvalFunPathType(key, valuesList);
		listofFunPaths.add(evalFunPathType);
	}

	
	
}


