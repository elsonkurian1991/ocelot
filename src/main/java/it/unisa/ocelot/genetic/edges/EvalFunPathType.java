package it.unisa.ocelot.genetic.edges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EvalFunPathType {
	private final String evalFunName;
	private final ArrayList<String> evalFunPathList;

	public EvalFunPathType(String evalFunName, List<String> evalFunPathList) {
		this.evalFunName = evalFunName;
		this.evalFunPathList = new ArrayList<>(evalFunPathList);
	}

	public String getEvalFunName() {
		return this.evalFunName;
	}

	public List<String> getEvalFunPathList() {
		return this.evalFunPathList;
	}

	@Override
	public String toString() {
		return "EvalFunPathType [evalFunName=" + this.evalFunName + ", evalFunPathList=" + this.evalFunPathList + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.evalFunPathList, this.evalFunName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EvalFunPathType other = (EvalFunPathType) obj;
		return Objects.equals(this.evalFunPathList, other.evalFunPathList) && Objects.equals(this.evalFunName, other.evalFunName);
	}
}
