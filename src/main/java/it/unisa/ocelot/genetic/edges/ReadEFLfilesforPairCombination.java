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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.pavelicii.allpairs4j.AllPairs;
import io.github.pavelicii.allpairs4j.AllPairs.AllPairsBuilder;
import io.github.pavelicii.allpairs4j.Case;
import io.github.pavelicii.allpairs4j.Parameter;
import javassist.expr.NewArray;

public class ReadEFLfilesforPairCombination {
	public static Map<String, ArrayList<String>> listofFunPaths = new HashMap<>();
	public static Map<String, String> listofIntRelationKeys = new HashMap<>();
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();
	public static Map<String, EFLType> pathTCfitness = new HashMap<>();
	/*public static void main(String[] args) throws IOException {
		RunEFLfilesforPairCombination();
	}*/
	public static void RunEFLfilesforPairCombination() throws IOException{ //RunEFLfilesforPairCombination
		// TODO Auto-generated method stub

		try (BufferedReader br1 = new BufferedReader(new FileReader("./evalFunList.efl"))) { // TODO this file should written while parseing KQuery files.
			String lineBr1;
			while ((lineBr1 = br1.readLine()) != null) {
				System.out.println(lineBr1);
				AddFunPath(listofFunPaths,lineBr1);
				// You can process the line here as needed
			}
		} catch (IOException e) {
			System.err.println("Error reading evalFunList.efl file: " + e.getMessage());
		}
		try (BufferedReader br2 = new BufferedReader(new FileReader("./IntRelKeyList.kl"))) { // TODO somehow .
			String lineBr2;
			while ((lineBr2 = br2.readLine()) != null) {
				System.out.println(lineBr2);
				//AddFunPath(listofFunPaths,lineBr2);
				AddIntegrationKeyList(listofIntRelationKeys,lineBr2);
				// You can process the line here as needed
			}
		} catch (IOException e) {
			System.err.println("Error reading the file: " + e.getMessage());
		}
		System.out.println(listofIntRelationKeys);
		System.out.println(listofFunPaths);
		int i=0;
		int j=0;
		for(String keySet1: listofFunPaths.keySet()) {
			for(String keySet2: listofFunPaths.keySet()) {
				if(!keySet1.equals(keySet2)) {
					boolean check=checkForKeySets(keySet1,keySet2);
					boolean isIntRel=checkHaveIntegrationRelation(keySet1,keySet2);
					if(!check&isIntRel) {
						ArrayList<String> keySets=new ArrayList<>();
						keySets.add(keySet1);
						keySets.add(keySet2);
						listofKeys.put(String.valueOf(j), keySets);
						j++;
						i=generatePairWiseCombinations(listofFunPaths,keySet1,keySet2,i);
					}

				}
			}
		}
		System.out.println(pathTCfitness);
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
		System.out.println(pathTCfitness);
		//CheckTCisCovered(pathTCfitness);
	}
	
	private static boolean checkHaveIntegrationRelation(String keySet1, String keySet2) {
		 for(Entry <String,String> relationkeyLists: listofIntRelationKeys.entrySet()){
			 if(relationkeyLists.getKey().equals(keySet1.toString())){
				 if(relationkeyLists.getValue().equals(keySet2.toString())) {
					 return true;
				 }
			 }
		 }
	
		return false;
	}

	private static void AddIntegrationKeyList(Map<String, String> listofIntRelKeys, String lineBr2) {
		
		lineBr2 = lineBr2.replaceAll("[{};]", "");		
		String values[]=lineBr2.split(",");
		listofIntRelKeys.put(values[0], values[1]);		
	}

	private static boolean checkForKeySets(String keySet1, String keySet2) {
		 if(listofKeys.isEmpty()) {
			 return false;
		 }
		 else {
			 for(Entry <String,ArrayList<String>> keyLists: listofKeys.entrySet()){
				 if(keyLists.getValue().contains(keySet2.toString())&&keyLists.getValue().contains(keySet1.toString())) {
					 return true;
				 }
				 for(String listVal:keyLists.getValue()) {
					 if(listVal.contains(keySet1)&&listVal.contains(keySet2)) {
						 return true;
					 }
				 }
			 }
			
		 }
		 return false;
	}

	private static int generatePairWiseCombinations(Map<String, ArrayList<String>> listofFunPaths2, String keySet1,
			String keySet2, int i) {
		ArrayList<String> params1= listofFunPaths2.get(keySet1);
		ArrayList<String> params2= listofFunPaths2.get(keySet2);
		//EFLType temp= new EFLType(null, null);
		for(String param1: params1) {
			for(String param2: params2) {
				Map<String,Double> temp_Fname_Val= new HashMap<>();
				temp_Fname_Val.put(param1.toString(),Double.MAX_VALUE);	
				temp_Fname_Val.put(param2.toString(),Double.MAX_VALUE);	
				EFLType temp= new EFLType(false, temp_Fname_Val);
				pathTCfitness.put(String.valueOf(i), temp);
				++i;
			}	
		}
		return i;
	}

	public static void CheckTCisCovered(Map<String, EFLType> tempPathTCfitness) {
		//System.out.println(tempPathTCfitness);
		for(Entry<String, EFLType> entry:tempPathTCfitness.entrySet()) {
			//System.out.println(entry.getValue());
			boolean isTCCovered=false;
			for(Entry<String, Double> fnameVal: entry.getValue().getFname_Val().entrySet()){
				//System.out.println(fnameVal.getValue());
				if(fnameVal.getValue()!=0.0) {
					isTCCovered= false;
					break; // we need all fitness should be 0.0 else break
				}
				else {
				//	System.out.println(fnameVal.getKey());
					isTCCovered= true;
				}
				
			}
			if(isTCCovered) {
			//	System.out.println(entry.getKey());
			//	System.out.println(entry.getValue());
				entry.getValue().setTCcovered(isTCCovered); //set the TCCov if all fitness is 0.0
			//	System.out.println(entry.getValue());
			}
		}
		//System.out.println(tempPathTCfitness);
	}
	
	
	public static List genParameterListEFL(List<Parameter> parameterList) {
		
		for (Map.Entry<String, ArrayList<String>> entry : listofFunPaths.entrySet()) {
			String key = entry.getKey();
			ArrayList<String> values = entry.getValue();

			Parameter parameter = new Parameter(key, values);
			parameterList.add(parameter);
		}
		return parameterList;
		
	}
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
	public static void AddFunPath(Map<String, ArrayList<String>> funPaths, String line) {
		// TODO Auto-generated method stub
		String keyVal[]=line.split("=");
		String key=keyVal[0];
		key=key.replaceAll("\\{","").trim(); // line = line.replaceAll("[{};]", "");
		String val=keyVal[1].replaceAll("\\}","").trim();
		val=val.replaceAll(";", "").trim();
		String values[]=val.split(",");
		ArrayList<String> valuesList= new ArrayList<String>();
		for(int i=0;i<values.length;i++) {
			valuesList.add(values[i]);
		}
		funPaths.put(key, valuesList);
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

