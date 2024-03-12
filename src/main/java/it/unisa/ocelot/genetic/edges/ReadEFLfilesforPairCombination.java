package it.unisa.ocelot.genetic.edges;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
	public static Map<String, EFLType> pathTCfitness = new HashMap<>();
	/*public static void main(String[] args) throws IOException {
		RunEFLfilesforPairCombination();
	}*/
	public static void RunEFLfilesforPairCombination() throws IOException{ //RunEFLfilesforPairCombination
		// TODO Auto-generated method stub

		try (BufferedReader br = new BufferedReader(new FileReader("./evalFunList.efl"))) { // TODO this file should written while parseing KQuery files.
			String line;
			while ((line = br.readLine()) != null) {
				System.out.println(line);
				AddFunPath(listofFunPaths,line);
				// You can process the line here as needed
			}
		} catch (IOException e) {
			System.err.println("Error reading the file: " + e.getMessage());
		}

		System.out.println(listofFunPaths);
		List<Parameter> parameterList = new ArrayList<>();
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

		System.out.println(pathTCfitness);
		//CheckTCisCovered(pathTCfitness);
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
				.withTestCombinationSize( listofFunPaths.size())         // specifies test combination size, default is 2 (pair)
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
		key=key.replaceAll("\\{","").trim();
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

