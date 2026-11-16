package it.unisa.ocelot.genetic.objectives;

import it.unisa.ocelot.util.Utils;
import jmetal.core.Solution;

public abstract class GenericObjective {
	/**
	 * A unique id. Two {@link GenericObjective}s
	 * shall not have same id.
	 */
	private final int id;
	
	/**
	 * Is this objective covered?
	 * I.e. has this objective been
	 * sent to zero by some solution?
	 */
	private boolean isCovered;
	
	/**
	 * Shall we try to optimize the
	 * value of this objective?
	 * In MOSA we may deactivate
	 * some objectives from 
	 * time to time.
	 */
	private boolean isActive;
	
	/** 
	 * If this objective is associated to one of 
	 * the two branches of an 'if' statement, 
	 * this field stores the objective associated 
	 * to the other branch of the same 'if' statement. 
	 */
	private GenericObjective companion;  

	public GenericObjective(int id) {
		this.id = id;
		this.isCovered = false; //initially we assume that the objective is not covered
		this.isActive = true;   //initially we assume that the objective is active
	}

	public final int getId() {
		return this.id;
	}
	
	public final boolean isCovered() {
		return this.isCovered;
	}
	
	public final void setCovered() {
		this.isCovered = true;
		this.isActive = false; //ensures that a covered objective is also inactive
	}
	
	public final boolean isCovered(Solution pSolution) {
		return (pSolution.getObjective(getId()) == 0d);
	}

	public final void setCovered(Solution pSolution) {
		if (isCovered(pSolution)) {
			setCovered();
		}
	}
	
	public final boolean isActive() {
		return this.isActive;
	}

	public final void setActive() {
		if (this.isCovered) { 
			return; //a covered objective stays inactive forever
		}
		this.isActive = true;
	}

	public final void setInactive() {
		this.isActive = false;
	}
	
	public final boolean hasCompanion() {
		return (this.companion != null);
	}
	
	public final GenericObjective getCompanion() {
		return this.companion;
	}

	public final void setCompanion(GenericObjective pCompanion) {
		this.companion = pCompanion;
	}

	/**
	 * Evaluates a {@link Solution} for this objective, 
	 * and updates it with the value.
	 * 
	 * @param pSolution a {@link Solution}.
	 * @return the value of this objective for 
	 *         the {@code pSolution}.
	 */
	public final void evaluate(Solution pSolution) {
		double value = Utils.safeDouble(calculateValue(pSolution));
		pSolution.setObjective(getId(), value);
	}
		
	protected abstract double calculateValue(Solution pSolution); 
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		GenericObjective other = (GenericObjective) obj;
		if (this.id != other.id) {
			return false;
		}
		return true;
	}
}
