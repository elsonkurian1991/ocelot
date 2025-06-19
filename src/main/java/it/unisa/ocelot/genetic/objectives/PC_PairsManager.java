package it.unisa.ocelot.genetic.objectives;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class PC_PairsManager {
	public static HashSet<EvalFunPathType> listofFunPaths = new HashSet<>();// ok
	public static HashSet<FunctionPair> listofIntRelationKeys = new HashSet<>();// ok
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();// ok
	public ArrayList<TestObjStateMachine> files_SM_PC_FitVals = new ArrayList<>();// ok make to list todo
	
	public static List<List<TestObjStateMachine>> ListOfSMs;
	
	public static List<GenericObjective> generatedObjectives;

	@SuppressWarnings("unchecked")
	public static List<GenericObjective> loadObjectives() throws IOException { // RunEFLfilesforPairCombination
		if (generatedObjectives == null) {
			try
	        {   
	            // Reading the object from a file
	            FileInputStream file = new FileInputStream("PairsData");
	            ObjectInputStream in = new ObjectInputStream(file);
	            
	            ListOfSMs = (List<List<TestObjStateMachine>>)in.readObject();
	            
	            in.close();
	            file.close();
	        }
	        
	        catch(Exception ex)
	        {
	        	System.err.println("Error reading testObjectives.to file: " + ex.getMessage());
	        }
	
			List<GenericObjective> objectives = new ArrayList<GenericObjective>();
			int j = 0;
			int objectiveID = 0;
			for (List<TestObjStateMachine> ListOfSMsindirectionLevel : ListOfSMs) {
				int indirectionLevel = 1;
				for (TestObjStateMachine sm : ListOfSMsindirectionLevel) {
					PC_PairObjective PC_Pair = new PC_PairObjective(false, objectiveID, sm, "Forward", indirectionLevel);
					objectives.add(PC_Pair);
					objectiveID++;
				}
				indirectionLevel++;
			}
			
			generatedObjectives = objectives;
			
			// For every objective find it's triggering pair
			for (GenericObjective obj : generatedObjectives) {
				findTriggeredPair((PC_PairObjective) obj, generatedObjectives);
			}
			
			return generatedObjectives;
		}
		else 
			return generatedObjectives;
	}

	private static void findTriggeredPair(PC_PairObjective pC_Pair, List<GenericObjective> objectives) {
		String firstBranch = pC_Pair.sm.getTestObjOne();
		String secondBranch = pC_Pair.sm.getTestObjTwo();
		
		
		String secondBranchTriggered;
		if (secondBranch.indexOf("-false") != -1) {
			secondBranchTriggered = secondBranch.replaceAll("-false", "-true");
			
		}
		else if (secondBranch.indexOf("-true") != -1) {
			secondBranchTriggered = secondBranch.replaceAll("-true", "-false");
		}
		else {
			secondBranchTriggered = "Error";
			System.out.println("Error in reading objectPairs");
		}
		//System.out.println(firstBranch);
		//System.out.println(secondBranch);
		//System.out.println(secondBranchTriggered);
		
		for (GenericObjective obj : objectives) {
			PC_PairObjective pairObjective = (PC_PairObjective) obj;
			//boolean test = pairObjective.sm.getTestObjOne().equals(secondBranchTriggered);
			if (pairObjective.sm.getTestObjOne().equals(firstBranch) && pairObjective.sm.getTestObjTwo().equals(secondBranchTriggered)) {
				// Insert in triggerd fiels of pc_pair
				pC_Pair.TriggeredPair =  obj;
			}
		}
	}
}
