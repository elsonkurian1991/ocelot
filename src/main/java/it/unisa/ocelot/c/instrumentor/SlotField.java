package it.unisa.ocelot.c.instrumentor;

import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IField;

/**
 * A field in a struct/union.
 */
final class SlotField extends Slot {
	private final String fieldName;
	
	SlotField(Slot pParent, IField pField) {
		super(pParent, pField.getType());
		if (pParent == null) {
			throw new RuntimeException("Attempted to create a field slot not contained in a struct/union (null pParent)");
		}
		if (!pParent.isTypeStructUnion()) {
			throw new RuntimeException("Attempted to create a field slot not contained in a struct/union (pParent has not struct/union type)");
		}
		this.fieldName = pField.getName();
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
		return this.parent.getParentAccessor() + "." + this.fieldName;
	}
}
