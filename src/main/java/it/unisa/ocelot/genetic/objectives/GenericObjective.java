package it.unisa.ocelot.genetic.objectives;

import java.util.Objects;

public abstract class GenericObjective {
	private boolean isCovered;
	private int objectiveID;

	public GenericObjective(boolean isCovered, int objectiveID) {
		super();
		this.isCovered = isCovered;
		this.objectiveID = objectiveID;
	}

	public boolean isCovered() {
		return isCovered;
	}

	public void setCovered(boolean isCovered) {
		this.isCovered = isCovered;
	}

	public int getObjectiveID() {
		return objectiveID;
	}

	public void setObjectiveID(int objectiveID) {
		this.objectiveID = objectiveID;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isCovered, objectiveID);
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
		return isCovered == other.isCovered && objectiveID == other.objectiveID;
	}

	@Override
	public String toString() {
		return "GenericObjective [isCovered=" + isCovered + ", objectiveID=" + objectiveID + "]";
	}
	
	public abstract double getFitness(Object[][][] arguments);

}
