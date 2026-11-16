package it.unisa.ocelot.genetic.objectives;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class PC_PairsManager {
	public static List<List<TestObjStateMachine>> listOfSMs;
	public static List<String> syntheticBranches;
	public static Set<? extends GenericObjective> generatedObjectives;

	@SuppressWarnings("unchecked")
	public static Set<? extends GenericObjective> loadObjectives() throws IOException {
		if (generatedObjectives == null) {
			try {   
	            // Reads the serialized PairsData file and deserializes into listOfSMs
	            FileInputStream file = new FileInputStream("PairsData");
	            ObjectInputStream in = new ObjectInputStream(file);
	            listOfSMs = (List<List<TestObjStateMachine>>) in.readObject();
	            in.close();
	            file.close();
	        } catch (Exception ex) {
	        	System.err.println("Error reading PairsData file: " + ex.getMessage());
	        }
			
			try {   
	            // Reads the serialized SyntheticBranches file and deserializes into syntheticBranches
	            FileInputStream file = new FileInputStream("SyntheticBranches");
	            ObjectInputStream in = new ObjectInputStream(file);
	            syntheticBranches = (List<String>)in.readObject();
	            in.close();
	            file.close();
	        } catch (Exception ex) {
	        	System.err.println("Error reading SyntheticBranches file: " + ex.getMessage());
	        }
	
			// Builds the objectives
			Set<PCPairObjective> objectives = new HashSet<>();
			int objectiveId = 0;
			for (List<TestObjStateMachine> listOfSMsIndirectionLevel : listOfSMs) {
				int indirectionLevel = 1;
				for (TestObjStateMachine sm : listOfSMsIndirectionLevel) {
					PCPairObjective pcPair = new PCPairObjective(objectiveId, sm, indirectionLevel);
					objectives.add(pcPair);
					objectiveId++;
				}
				indirectionLevel++;
			}
			
			// Detects all the synthetic objectives based 
			// the list of synthetic branches; also, sets 
			// the associated triggered pair of each 
			//objective
			for (PCPairObjective objective : objectives) {
				setIsSynthetic(objective);
				findAndSetTriggeredPair(objective, objectives);
			}
			
			// Stores the generated objectives into generatedObjectives
			generatedObjectives = objectives;
			
		}
		
		return generatedObjectives;
	}

	private static void findAndSetTriggeredPair(PCPairObjective objective, Set<PCPairObjective> objectives) {
		String firstBranch = objective.sm.getTestObjOne();
		String secondBranch = objective.sm.getTestObjTwo();
		
		String secondBranchTriggered;
		if (secondBranch.indexOf("-false") != -1) {
			secondBranchTriggered = secondBranch.replaceAll("-false", "-true");
		} else if (secondBranch.indexOf("-true") != -1) {
			secondBranchTriggered = secondBranch.replaceAll("-true", "-false");
		} else {
			secondBranchTriggered = "Error";
			System.out.println("Error: branch data seems ill-formed. Are there incorrect data in the PairsData file?");
		}
		
		for (GenericObjective objectiveOther : objectives) {
			PCPairObjective pairObjective = (PCPairObjective) objectiveOther;
			//boolean test = pairObjective.sm.getTestObjOne().equals(secondBranchTriggered);
			if (pairObjective.sm.getTestObjOne().equals(firstBranch) && pairObjective.sm.getTestObjTwo().equals(secondBranchTriggered)) {
				// Insert in triggerd fiels of pc_pair
				objective.setCompanion(objectiveOther);
			}
		}
	}
	
	private static void setIsSynthetic(PCPairObjective objective) {
		for (String branch : syntheticBranches) {
			if (branch.equals(objective.sm.getTestObjOne()) || branch.equals(objective.sm.getTestObjTwo())) {
				objective.isSynthetic = true;
				break;
			}
		}
	}
}
