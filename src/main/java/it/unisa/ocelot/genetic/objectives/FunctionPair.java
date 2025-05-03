package it.unisa.ocelot.genetic.objectives;

import java.util.Objects;

public class FunctionPair {

	@Override
	public String toString() {
		return "FunctionPair [fun1=" + fun1 + ", fun2=" + fun2 + "]";
	}
	String fun1;
	String fun2;
	public String getFun1() {
		return fun1;
	}
	public void setFun1(String fun1) {
		this.fun1 = fun1;
	}
	public String getFun2() {
		return fun2;
	}
	public void setFun2(String fun2) {
		this.fun2 = fun2;
	}
	@Override
	public int hashCode() {
		return Objects.hash(fun1, fun2);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionPair other = (FunctionPair) obj;
		return Objects.equals(fun1, other.fun1) && Objects.equals(fun2, other.fun2);
	}
	public FunctionPair(String fun1, String fun2) {
		super();
		this.fun1 = fun1;
		this.fun2 = fun2;
	}
}
