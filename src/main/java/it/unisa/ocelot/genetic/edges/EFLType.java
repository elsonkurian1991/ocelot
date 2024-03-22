package it.unisa.ocelot.genetic.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EFLType {
    private boolean isTCcovered;
    private Map<String,Double> Fname_Val= new HashMap<>();
    
	
    public EFLType(boolean isTCcovered, Map<String,Double> Fname_Val) {
    	this.isTCcovered=isTCcovered;
    	this.Fname_Val=Fname_Val;
	}


	@Override
	public int hashCode() {
		return Objects.hash(Fname_Val, isTCcovered);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EFLType other = (EFLType) obj;
		return Objects.equals(Fname_Val, other.Fname_Val) && isTCcovered == other.isTCcovered;
	}


	@Override
	public String toString() {
		return "EFLType [isTCcovered=" + isTCcovered + ", Fname_Val=" + Fname_Val + "]" + System.lineSeparator();
	}


	public boolean isTCcovered() {
		return isTCcovered;
	}


	public void setTCcovered(boolean isTCcovered) {
		this.isTCcovered = isTCcovered;
	}


	public Map<String, Double> getFname_Val() {
		return Fname_Val;
	}


	public void setFname_Val(Map<String, Double> fname_Val) {
		Fname_Val = fname_Val;
	}

}