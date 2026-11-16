package it.unisa.ocelot.util;

import java.util.ArrayList;

import jmetal.core.SolutionSet;

/**
 * Represent a non-dominated ranking assignment of front F
 * allFronts at position 0 represent the proposed preference criterion
 * 
 * @author giograno
 *
 */
public class Fronts {
	private final ArrayList<SolutionSet> fronts;
	
	public Fronts() {
		this.fronts = new ArrayList<>();
	}
	
	public SolutionSet getFront(int index){
		return this.fronts.get(index);
	}
	
	public void addFront(SolutionSet front){
		this.fronts.add(front);
	}
}
