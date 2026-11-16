package it.unisa.ocelot.genetic.objectives;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import it.unisa.ocelot.util.Utils;

public class BranchManager {
	private static Set<? extends GenericObjective> generatedObjectives;

	public static Set<? extends GenericObjective> loadObjectives() throws IOException { 
		if (generatedObjectives == null) {
			// Reads the list of branches from branchObjective.txt
			String[] listOfBranches = Utils.readFile("branchObjectives.txt").split(",");
	
			Set<BranchObjective> objectives = new HashSet<>();
			
			int objectiveId = 0;
			for (String branchId : listOfBranches) {
				BranchObjective branchObjective = new BranchObjective(objectiveId, branchId);
				objectives.add(branchObjective);
				objectiveId++;
			}
			
			// For every objective finds its triggering pair
			for (BranchObjective objective : objectives) {
				findTriggeredPair(objective, objectives);
			}					
			
			generatedObjectives = objectives;
		}
		
		return generatedObjectives;
	}

	private static void findTriggeredPair(BranchObjective objective, Set<BranchObjective> objectives) {
		String objectiveBranchId = objective.getBranchId();
		String triggeredBranchId;
		if (objectiveBranchId.indexOf("-false") != -1) {
			triggeredBranchId = objectiveBranchId.replaceAll("-false", "-true");
		} else if (objectiveBranchId.indexOf("-true") != -1) {
			triggeredBranchId = objectiveBranchId.replaceAll("-true", "-false");
		} else {
			triggeredBranchId = "Error";
			System.out.println("Error in reading objectPairs");
		}
		
		for (BranchObjective objectiveTriggered : objectives) {
			if (objectiveTriggered.getBranchId().equals(triggeredBranchId)) {
				// Sets it as a companion objective
				objective.setCompanion(objectiveTriggered);
			}
		}
	}
}
