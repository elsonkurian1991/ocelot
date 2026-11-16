package it.unisa.ocelot.c.types;

import java.util.ArrayList;
import java.util.List;

public class CTypeHandler {
	private List<CType> pointers;
	private List<CType> values;


	public CTypeHandler(CType[] pTypes) {
		this.pointers = new ArrayList<>();
		this.values = new ArrayList<>();
		generateInfo(pTypes);
	}

	private void generateInfo(CType[] pTypes) {
		for (CType type : pTypes) {
			if (type.isPointer()) {
				this.pointers.add(type);
			} else {
				this.values.add(type);
			}
		}
	}

	public List<CType> getPointers() {
		return this.pointers;
	}

	public List<CType> getValues() {
		return this.values;
	}
}
