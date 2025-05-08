package it.unisa.ocelot.genetic.objectives;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unisa.ocelot.genetic.edges.TestObjStateMachine;

public class PC_PairsManager {
	public static HashSet<EvalFunPathType> listofFunPaths = new HashSet<>();// ok
	public static HashSet<FunctionPair> listofIntRelationKeys = new HashSet<>();// ok
	public static Map<String, ArrayList<String>> listofKeys = new HashMap<>();// ok
	public ArrayList<TestObjStateMachine> files_SM_PC_FitVals = new ArrayList<>();// ok make to list todo

	public static List<GenericObjective> loadObjectives() throws IOException { // RunEFLfilesforPairCombination
		// read the test objectives file
		try (BufferedReader br1 = new BufferedReader(new FileReader("./testObjectives.to"))) {
			String lineBr1;
			while ((lineBr1 = br1.readLine()) != null) {
				AddFunPath(lineBr1); // add to the listofFunPaths??
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

		List<GenericObjective> objectives = new ArrayList<GenericObjective>();
		int j = 0;
		int objectiveID = 0;
		for (EvalFunPathType keySet1 : listofFunPaths) {
			for (EvalFunPathType keySet2 : listofFunPaths) {
				if (!keySet1.getEvalFunName().equals(keySet2.getEvalFunName())) {
					boolean check = checkForKeySets(keySet1, keySet2);
					boolean isIntRel = checkHaveIntegrationRelation(keySet1, keySet2);
					if (!check & isIntRel) {
						ArrayList<String> keySets = new ArrayList<>();
						keySets.add(keySet1.getEvalFunName());
						keySets.add(keySet2.getEvalFunName());
						listofKeys.put(String.valueOf(j), keySets);
						objectiveID = generatePairWiseCombinations(objectives, keySet1, keySet2, objectiveID);
						j++;
					}
				}
			}
		}
		return objectives;
	}

	private static boolean checkHaveIntegrationRelation(EvalFunPathType keySet1, EvalFunPathType keySet2) {
		String fun1 = keySet1.getEvalFunName();
		String fun2 = keySet2.getEvalFunName();
		// System.out.println("fun1:"+fun1+ " fun2:"+fun2);
		for (FunctionPair relationkeyLists : listofIntRelationKeys) {
			if ((relationkeyLists.getFun1().equals(fun1)) && (relationkeyLists.getFun2().equals(fun2))) {
				// System.out.println(relationkeyLists.toString());
				return true;
			}
		}
		return false;
	}

	private static void AddIntegrationKeyList(String lineBr2) {

		lineBr2 = lineBr2.replaceAll("[{};]", "");
		String values[] = lineBr2.split(",");
		FunctionPair inner = new FunctionPair(values[0], values[1]);
		listofIntRelationKeys.add(inner);
		// System.out.println(listofIntRelationKeys);
	}

	private static boolean checkForKeySets(EvalFunPathType keySet1, EvalFunPathType keySet2) {
		if (listofKeys.isEmpty()) {
			return false;
		} else {
			for (Entry<String, ArrayList<String>> keyLists : listofKeys.entrySet()) {
				ArrayList<String> lists = keyLists.getValue();
				String Chk1 = lists.get(0);
				String Chk2 = lists.get(1);
				if (Chk1.equals(keySet2.toString()) && Chk2.equals(keySet1.toString())) {
					return true;
				}
			}

		}
		return false;
	}

	public static int generatePairWiseCombinations(List<GenericObjective> objectives, EvalFunPathType keySet1,
			EvalFunPathType keySet2, int objectiveID) {
		ArrayList<String> params1 = keySet1.getEvalFunPathList(); // TODO add the getName and add to params
		ArrayList<String> params2 = keySet2.getEvalFunPathList();
		for (String param1 : params1) {
			for (String param2 : params2) {
				String testObj1 = param1.toString();
				String testObj2 = param2.toString();
				PC_PairObjective PC_Pair = new PC_PairObjective(false, objectiveID, testObj1, testObj2);
				objectives.add(PC_Pair);
				objectiveID++;
			}
		}
		return objectiveID;
	}

	private static void AddFunPath(String line) {
		String keyVal[] = line.split("=");
		String key = keyVal[0];
		key = key.replaceAll("\\{", "").trim(); // line = line.replaceAll("[{};]", "");
		String val = keyVal[1].replaceAll("\\}", "").trim();
		val = val.replaceAll(";", "").trim();
		String values[] = val.split(",");
		ArrayList<String> valuesList = new ArrayList<String>();
		for (int i = 0; i < values.length; i++) {
			String tempValList = values[i];
			valuesList.add(tempValList);
		}
		EvalFunPathType evalFunPathType = new EvalFunPathType(key, valuesList);
		listofFunPaths.add(evalFunPathType);
	}

}
