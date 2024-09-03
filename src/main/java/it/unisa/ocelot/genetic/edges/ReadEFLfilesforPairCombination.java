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

public class ReadEFLfilesforPairCombination {
	public static HashSet<EvalFunPathType> listofFunPaths = new HashSet<>();
	public static HashSet<FunctionPair> listofIntRelationKeys = new HashSet<>();
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();
	public static Map<String, EFLType> pathTCfitness = new HashMap<>();
	public static Map<String, FitType> files_PC_PairCom_FitnessVals = new HashMap<>();
	/*public static void main(String[] args) throws IOException {
		RunEFLfilesforPairCombination();
	}*/
	public static void RunEFLfilesforPairCombination() throws IOException{ //RunEFLfilesforPairCombination
		// TODO Auto-generated method stub

		try (BufferedReader br1 = new BufferedReader(new FileReader("./testObjectives.to"))) { // TODO this file should written while parseing KQuery files.
			String lineBr1;
			while ((lineBr1 = br1.readLine()) != null) {
				System.out.println(lineBr1);
				AddFunPath(lineBr1);
				System.out.println("next");
				// You can process the line here as needed
			}
		} catch (IOException e) {
			System.err.println("Error reading evalFunList.efl file: " + e.getMessage());
		}
		try (BufferedReader br2 = new BufferedReader(new FileReader("./IntRelKeyList.kl"))) { // TODO somehow .
			String lineBr2;
			while ((lineBr2 = br2.readLine()) != null) {
				//System.out.println(lineBr2);
				//AddFunPath(listofFunPaths,lineBr2);
				AddIntegrationKeyList(lineBr2);
				// You can process the line here as needed
			}
		} catch (IOException e) {
			System.err.println("Error reading the file: " + e.getMessage());
		}
		//System.out.println(listofIntRelationKeys.toString());
		//System.out.println(listofFunPaths);
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
						//System.out.println(pathTCfitness);
					}

				}
			}
		}
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

		
		
		
		/*List<Parameter> parameterList = new ArrayList<>();
		parameterList=genParameterListEFL(parameterList);
		
		AllPairs allPairs = generateAllPairs(parameterList);
		// or use Iterator

		System.out.println(allPairs);
		int size =allPairs.getTestCombinationSize();
		System.out.println(size);
		System.out.println(allPairs.getGeneratedCases());
		
		//int i=0;
		for(Case genList :allPairs.getGeneratedCases()) {
			System.out.println(genList);
			System.out.println(genList.entrySet());
			Map<String,Double> temp_Fname_Val= new HashMap<>();
			for(Entry<String, Object> entry:genList.entrySet()) {
				//System.out.println(entry.getKey());
				//System.out.println(entry.getValue());
				temp_Fname_Val.put(entry.getValue().toString(),Double.MAX_VALUE);
			}
			EFLType temp= new EFLType(false, temp_Fname_Val);
			pathTCfitness.put(String.valueOf(i), temp);
			++i;
		}
*/
        //System.out.println(pathTCfitness);
		//CheckTCisCovered(pathTCfitness);
		
		System.out.println(files_PC_PairCom_FitnessVals);
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
				List<FNameFitValType> fname_Val_Temp= new ArrayList<FNameFitValType>();
				FNameFitValType temp_Fname_Val1 = new FNameFitValType(param1.toString(),Double.MAX_VALUE);
				FNameFitValType temp_Fname_Val2 = new FNameFitValType(param2.toString(),Double.MAX_VALUE);
				//temp_Fname_Val1.setfName(param1.toString());
				//temp_Fname_Val1.setFitnessVal(Double.MAX_VALUE);
				//FNameFitValType temp_Fname_Val2 = null;
				//temp_Fname_Val2.setfName(param2.toString());
				//temp_Fname_Val2.setFitnessVal(Double.MAX_VALUE);
				//temp_Fname_Val.add(param1.toString(),Double.MAX_VALUE);	
				//temp_Fname_Val.put(param2.toString(),Double.MAX_VALUE);	
				fname_Val_Temp.add(temp_Fname_Val1);
				fname_Val_Temp.add(temp_Fname_Val2);
				EFLType temp= new EFLType(false,false,"null", fname_Val_Temp);
				pathTCfitness.put(String.valueOf(i), temp);//put all the combination for backup
				FitType FitTypeTemp= new FitType(false, false, false, "null", true, fname_Val_Temp);
				String pairCom=param1.toString()+","+param2.toString();
				files_PC_PairCom_FitnessVals.put(pairCom, FitTypeTemp);// put all combination for calculate fitness 
				++i;
			}	
		}
		
		return i; 
	}

	public static void CheckTCisCovered(Map<String, EFLType> pathTCfitness2, Object[][][] args2) {
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
		for(Entry<String, EFLType> entry:pathTCfitness.entrySet()) {
			EFLType valueList= entry.getValue();
			
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
				/*for (Entry<String, FType> set : CalculateFitnessFromEvalPC.filesWithFitnessVals.entrySet()) {
					FType temp = new FType(set.getValue().getFitnessValue(), false, set.getValue().isTestGenerated(),set.getValue().isFirst());
					if(!set.getKey().contentEquals(fnameVals.get(0).getfName())){
						set.setValue(temp);
					}
					else if(!set.getKey().contentEquals(fnameVals.get(1).getfName())) {
						set.setValue(temp);
					}
				}*/
				
			}
		}
			/* if((fnameVals.get(0).getFitnessVal()==0)|(fnameVals.get(1).getFitnessVal()==0)) {
				//fnameVals.get(0).setFitnessVal(Double.MAX_VALUE);
				//fnameVals.get(1).setFitnessVal(Double.MAX_VALUE);
				//CalculateFitnessFromEvalPC.filesWithFitnessVals.
				for (Entry<String, FType> set : CalculateFitnessFromEvalPC.filesWithFitnessVals.entrySet()) {
					FType temp = new FType(set.getValue().getFitnessValue(), false, set.getValue().isTestGenerated(),set.getValue().isFirst());
					if(set.getValue().isTestCovered()) {
						
					}	
					if(set.getKey().contains(fnameVals.get(0).getfName())){
						set.setValue(temp);
					}
					else if(set.getKey().contains(fnameVals.get(1).getfName())) {
						set.setValue(temp);
					}
				}
			}
		}*/
	}
			
			//System.out.println(entry.getValue());
			//boolean isTCCovered=false;
			//for(FNameFitValType fnameVal: entry.getValue().getFname_Val()){
				//System.out.println(fnameVal.getValue());
				//if(fnameVal.getFitnessVal()!=0.0) {
					//isTCCovered= false;
					//break; // we need all fitness should be 0.0 else break
				//}
				//else {
				//	System.out.println(fnameVal.getKey());
				//	isTCCovered= true;
			//}
				
		//	}
			//if(isTCCovered) {
			//	System.out.println(entry.getKey());
			//	System.out.println(entry.getValue());
			//	entry.getValue().setTCcovered(isTCCovered); //set the TCCov if all fitness is 0.0
			//	System.out.println(entry.getValue());
		//	}
		//}
		//System.out.println(tempPathTCfitness);
	//}
	
	
	/*public static List genParameterListEFL(List<Parameter> parameterList) {
		
		for (Map.Entry<String, ArrayList<String>> entry : listofFunPaths.entrySet()) {
			String key = entry.getKey();
			ArrayList<String> values = entry.getValue();

			Parameter parameter = new Parameter(key, values);
			parameterList.add(parameter);
		}
		return parameterList;
		
	}
	*/
	public static AllPairs generateAllPairs(List<Parameter> parameterList) {
		AllPairs allPairs = new AllPairs.AllPairsBuilder()
				// .withParameter( Parameter )                            // specifies 1 Parameter
				.withParameters( parameterList)                     // alternative way to specify multiple Parameters as List
				//  .withConstraint( Predicate<ConstrainableCase> )        // specifies 1 Constraint, default is no Constraints
				// .withConstraints( List<Predicate<ConstrainableCase>> ) // alternative way to specify multiple Constraints as List
				.withTestCombinationSize(2)         // specifies test combination size, default is 2 (pair)
				
				//listofFunPaths.size()
				//  .printEachCaseDuringGeneration()                       // prints Cases during generation, useful for debug
				.build();
		return allPairs;
		//List<Case> generatedCases = allPairs.getGeneratedCases();      // work with resulting List of Cases
		//for (Case c : allPairs) { ... }     

	}
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





/*
 * Backuo of ALLPAIRS combination
 *      <List<Parameter> parameterList = new ArrayList<>();
		parameterList=genParameterListEFL(parameterList);
		
		AllPairs allPairs = generateAllPairs(parameterList);
		// or use Iterator

		System.out.println(allPairs);
		int size =allPairs.getTestCombinationSize();
		System.out.println(size);
		System.out.println(allPairs.getGeneratedCases());
		
		int i=0;
		for(Case genList :allPairs.getGeneratedCases()) {
			System.out.println(genList);
			System.out.println(genList.entrySet());
			Map<String,Double> temp_Fname_Val= new HashMap<>();
			for(Entry<String, Object> entry:genList.entrySet()) {
				//System.out.println(entry.getKey());
				//System.out.println(entry.getValue());
				temp_Fname_Val.put(entry.getValue().toString(),Double.MAX_VALUE);
			}
			EFLType temp= new EFLType(false, temp_Fname_Val);
			pathTCfitness.put(String.valueOf(i), temp);
			++i;
		}
		TILL HERE
 */
/*		        new Parameter("Browser", "Chrome", "Safari", "Edge"),
		                new Parameter("OS", "Windows", "Linux", "macOS"),
		                new Parameter("RAM", 2048, 4096, 8192, 16384),
		                new Parameter("Drive", "HDD", "SSD")
 * AllPairs allPairs = new AllPairs.AllPairsBuilder()

		        .withTestCombinationSize(3)
		        .withParameters(Arrays.asList(
		                new Parameter("Browser", "Chrome", "Safari", "Edge"),
		                new Parameter("OS", "Windows", "Linux", "macOS"),
		                new Parameter("RAM", 2048, 4096, 8192, 16384),
		                new Parameter("Drive", "HDD", "SSD")))
		        .build();

		System.out.println(allPairs);
 */

/*
		AllPairs allPairs = new AllPairs.AllPairsBuilder()
		        .withParameter( Parameter )                            // specifies 1 Parameter
		        .withParameters( List<Parameter> )                     // alternative way to specify multiple Parameters as List
		        .withConstraint( Predicate<ConstrainableCase> )        // specifies 1 Constraint, default is no Constraints
		        .withConstraints( List<Predicate<ConstrainableCase>> ) // alternative way to specify multiple Constraints as List
		        .withTestCombinationSize( int )                        // specifies test combination size, default is 2 (pair)
		        .printEachCaseDuringGeneration()                       // prints Cases during generation, useful for debug
		        .build();

		List<Case> generatedCases = allPairs.getGeneratedCases();      // work with resulting List of Cases
		for (Case c : allPairs) { ... }                                // or use Iterator

 */

