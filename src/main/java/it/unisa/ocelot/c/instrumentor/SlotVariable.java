package it.unisa.ocelot.c.instrumentor;

import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IType;

/**
 * A variable.
 */
final class SlotVariable extends Slot {
	SlotVariable(IType pType, String pName) {
		this(pType);
		setVariableName(pName);
	}

	SlotVariable(IType pType) {
		super(null, pType);
	}

	@Override
	protected Stream<? extends Slot> siblings() {
		return Stream.of(this);
	}

	@Override
	protected boolean isSemantic() {
		return true;
	}

	@Override
	protected void doAddSibling(String pName) {
		throw new RuntimeException("Attempted to add a sibling to a slot that is semantic");
	}
	
	@Override
	public String getParentAccessor() {
		throw new RuntimeException("Attempted to get the parent accessor from a root slot");
	}
}
