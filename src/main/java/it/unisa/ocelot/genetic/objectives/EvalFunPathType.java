package it.unisa.ocelot.genetic.objectives;

import java.util.ArrayList;
import java.util.Objects;

public class EvalFunPathType {
	
	 String evalFunName;
     ArrayList<String> EvalFunPathList= new ArrayList<String>();

	@Override
	public int hashCode() {
		return Objects.hash(EvalFunPathList, evalFunName);
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
		return Objects.equals(EvalFunPathList, other.EvalFunPathList) && Objects.equals(evalFunName, other.evalFunName);
	}

	@Override
	public String toString() {
		return "EvalFunPathType [evalFunName=" + evalFunName + ", EvalFunPathList=" + EvalFunPathList + "]";
	}

	public String getEvalFunName() {
		return evalFunName;
	}

	public void setEvalFunName(String evalFunName) {
		this.evalFunName = evalFunName;
	}

	public ArrayList<String> getEvalFunPathList() {
		return EvalFunPathList;
	}

	public void setEvalFunPathList(ArrayList<String> evalFunPathList) {
		EvalFunPathList = evalFunPathList;
	}

	public EvalFunPathType(String evalFunName,ArrayList<String> EvalFunPathList) {
		// TODO Auto-generated constructor stub
		this.evalFunName=evalFunName;
		this.EvalFunPathList=EvalFunPathList;
	}

}
