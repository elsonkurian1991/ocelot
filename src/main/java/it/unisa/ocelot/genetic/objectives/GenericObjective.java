package it.unisa.ocelot.genetic.objectives;

import java.util.Objects;

public abstract class GenericObjective {
	private boolean isCovered;
	private int objectiveID;
	// Used in DynaMOSA to know if we are currenlty optimizing for this objective
	private boolean isActive;

	public GenericObjective(boolean isCovered, int objectiveID) {
		super();
		this.isCovered = isCovered;
		this.isActive = true;
		this.objectiveID = objectiveID;
	}

	public boolean isCovered() {
		return isCovered;
	}

	public void setCovered(boolean isCovered) {
		this.isCovered = isCovered;
	}
	
	public boolean isActive() {
		//return isCovered;
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public int getObjectiveID() {
		return objectiveID;
	}

	public void setObjectiveID(int objectiveID) {
		this.objectiveID = objectiveID;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + objectiveID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericObjective other = (GenericObjective) obj;
		if (objectiveID != other.objectiveID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GenericObjective [isCovered=" + isCovered + ", objectiveID=" + objectiveID + "]";
	}
	
	public abstract double getFitness(Object[][][] arguments);

}
