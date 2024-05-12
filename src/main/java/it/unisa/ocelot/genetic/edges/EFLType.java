package it.unisa.ocelot.genetic.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EFLType {
    private boolean isTCcovered;
    private boolean isGenerated;
    private String argumentList;
    private List<FNameFitValType> Fname_Val= new ArrayList<FNameFitValType>();
    
	
    public EFLType(boolean isTCcovered,boolean isGenerated,String argumentList, List<FNameFitValType> Fname_Val) {
    	this.isTCcovered=isTCcovered;
    	this.isGenerated=isGenerated;
    	this.argumentList=argumentList;
    	this.Fname_Val=Fname_Val;
	}


	public String getArgumentList() {
		return argumentList;
	}


	public void setArgumentList(String argumentList) {
		this.argumentList = argumentList;
	}


	public boolean isGenerated() {
		return isGenerated;
	}


	public void setGenerated(boolean isGenerated) {
		this.isGenerated = isGenerated;
	}


	public boolean isTCcovered() {
		return isTCcovered;
	}


	public void setTCcovered(boolean isTCcovered) {
		this.isTCcovered = isTCcovered;
	}


	public List<FNameFitValType> getFname_Val() {
		return Fname_Val;
	}


	public void setFname_Val(List<FNameFitValType> fname_Val) {
		Fname_Val = fname_Val;
	}


	@Override
	public int hashCode() {
		return Objects.hash(Fname_Val, argumentList, isGenerated, isTCcovered);
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
		return Objects.equals(Fname_Val, other.Fname_Val) && Objects.equals(argumentList, other.argumentList)
				&& isGenerated == other.isGenerated && isTCcovered == other.isTCcovered;
	}


	@Override
	public String toString() {
		return "EFLType [isTCcovered=" + isTCcovered + ", isGenerated=" + isGenerated + ", argumentList=" + argumentList
				+ ", Fname_Val=" + Fname_Val + "]"+System.lineSeparator();
	}



}
