package it.unisa.ocelot.suites.budget;

public class BudgetManagerHandler {	
	public static final String BASIC_MANAGER = "Basic";
	public static final String ADAPTIVE_MANAGER = "Adaptive"; //not yet implemented...
	public static final String LEGACY_MANAGER = "Legacy";
	
	public static BudgetManager getInstance(String pName, int pTotalBudget, int pNumberOfExperiments) {
		if (pName.equalsIgnoreCase(BASIC_MANAGER)) {
			return new BasicBudgetManager(pTotalBudget, pNumberOfExperiments);
		} else if (pName.equalsIgnoreCase(ADAPTIVE_MANAGER)) {
			//TODO implement it or remove it
			throw new RuntimeException("ERROR: Adaptative budget manager not yet implemented, use basic budget manager");
		} else if (pName.equalsIgnoreCase(LEGACY_MANAGER)) {
			return new LegacyBudgetManager(pTotalBudget, pNumberOfExperiments);
		} else {
			throw new RuntimeException("ERROR: Budget manager with name " + pName + " not recognized");
		}
	}
}
