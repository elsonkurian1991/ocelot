package it.unisa.ocelot.genetic.objectives;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import it.unisa.ocelot.util.Utils;


public class BranchManager {
	public static HashSet<EvalFunPathType> listofFunPaths = new HashSet<>();// ok
	public static HashSet<FunctionPair> listofIntRelationKeys = new HashSet<>();// ok
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();// ok
	
	public static List<String> ListOfBranches;
	
	public static List<GenericObjective> generatedObjectives;

	public static List<GenericObjective> loadObjectives(int objectiveID) throws IOException { 
		if (generatedObjectives == null) {
			// Reading the object from a file
			ListOfBranches = Arrays.asList(Utils.readFile("branchObjectives.txt").split(","));
	
			List<GenericObjective> objectives = new ArrayList<GenericObjective>();

			for (String Branch : ListOfBranches) {
				BranchObjective branchObj = new BranchObjective(false, objectiveID, Branch);
				objectives.add(branchObj);
				objectiveID++;
				}
			
			
			generatedObjectives = objectives;
			
			// For every objective find it's triggering pair
			for (GenericObjective obj : generatedObjectives) {
				findTriggeredPair((BranchObjective) obj, generatedObjectives);
			}
			
			return generatedObjectives;
		}
		else 
			return generatedObjectives;
	}

	private static void findTriggeredPair(BranchObjective branch, List<GenericObjective> objectives) {
		
		String branchString = branch.testObj;
		String secondBranchTriggered;
		if (branchString.indexOf("-false") != -1) {
			secondBranchTriggered = branchString.replaceAll("-false", "-true");
			
		}
		else if (branchString.indexOf("-true") != -1) {
			secondBranchTriggered = branchString.replaceAll("-true", "-false");
		}
		else {
			secondBranchTriggered = "Error";
			System.out.println("Error in reading objectPairs");
		}
		
		for (GenericObjective obj : objectives) {
			BranchObjective branchObjective = (BranchObjective) obj;
			
			if (branchObjective.testObj.equals(secondBranchTriggered)) {
				// Insert in triggerd fiels of branchObj
				branch.TriggeredPair =  obj;
			}
		}
	}
}
