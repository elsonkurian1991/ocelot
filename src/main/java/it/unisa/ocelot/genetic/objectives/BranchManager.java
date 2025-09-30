package it.unisa.ocelot.genetic.objectives;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
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
	public static List<GenericObjective> generatedSyntheticObjectives;
	public static List<String> SyntheticBranches;
	public static List<GenericObjective> loadObjectives(int objectiveID) throws IOException { 
		if (generatedObjectives == null) {
			// Reading the object from a file
			ListOfBranches = Arrays.asList(Utils.readFile("branchObjectives.txt").split(","));
	
			List<GenericObjective> objectives = new ArrayList<GenericObjective>();
			try
	        {   
	            // Reading the object from a file
	            FileInputStream file = new FileInputStream("SyntheticBranches");
	            ObjectInputStream in = new ObjectInputStream(file);
	            
	            SyntheticBranches = (List<String>)in.readObject();
	            
	            in.close();
	            file.close();
	        } catch(Exception ex) {
	        	System.err.println("Error reading SyntheticBranches file: " + ex.getMessage());
	        }
			
		
			for (String Branch : ListOfBranches) {
				BranchObjective branchObj = new BranchObjective(false, objectiveID, Branch);
				objectives.add(branchObj);
				objectiveID++;
				}
			
			
			generatedObjectives = objectives;
			
			// For every objective find it's triggering pair
			
			for (GenericObjective obj : generatedObjectives) {
				BranchObjective BranchObj = (BranchObjective) obj;
				findTriggeredPair(BranchObj, generatedObjectives);
				for(String branch : SyntheticBranches) {
					if (branch.equals(BranchObj.testObj))
						BranchObj.isSynthetic = true;
					}
				
			}
			
			return generatedObjectives;
		}
		else 
			return generatedObjectives;
	}
	
	public static List<GenericObjective> loadObjectivesSynthetics(int objectiveID) throws IOException {
		if (generatedSyntheticObjectives == null) {
			// Reading the object from a file
			ListOfBranches = Arrays.asList(Utils.readFile("branchObjectives.txt").split(","));
	
			List<GenericObjective> objectives = new ArrayList<GenericObjective>();
			try
	        {   
	            // Reading the object from a file
	            FileInputStream file = new FileInputStream("SyntheticBranches");
	            ObjectInputStream in = new ObjectInputStream(file);
	            
	            SyntheticBranches = (List<String>)in.readObject();
	            
	            in.close();
	            file.close();
	        } catch(Exception ex) {
	        	System.err.println("Error reading SyntheticBranches file: " + ex.getMessage());
	        }
			
		
			for (String Branch : ListOfBranches) {
				for(String branch : SyntheticBranches) {
					if (branch.equals(Branch)) {
						BranchObjective branchObj = new BranchObjective(false, objectiveID, Branch);
						objectives.add(branchObj);
						objectiveID++;
					}
				}
			}
			
			
			generatedSyntheticObjectives = objectives;
			
			// For every objective find it's triggering pair
			for (GenericObjective obj : generatedObjectives) {
				BranchObjective BranchObj = (BranchObjective) obj;
				findTriggeredPair(BranchObj, generatedObjectives);
				for(String branch : SyntheticBranches) {
					if (branch.equals(BranchObj.testObj))
						BranchObj.isSynthetic = true;
					}
				
			}
			
			return generatedSyntheticObjectives;
		}
		else 
			return generatedSyntheticObjectives;
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
