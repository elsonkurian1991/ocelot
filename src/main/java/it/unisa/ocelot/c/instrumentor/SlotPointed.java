package it.unisa.ocelot.c.instrumentor;

import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IType;

/**
 * A slot pointed by a pointer.
 */
final class SlotPointed extends Slot {
	/**
	 * Constructor.
	 * 
	 * @param pParent the parent {@link Slot}. It 
	 *        must not be {@code null} and must
	 *        have pointer type.
	 * @param pType the {@link IType} of the values
	 *        that can go in the slot.
	 */
	SlotPointed(Slot pParent, IType pType) {
		super(pParent, pType);
		if (pParent == null) {
			throw new RuntimeException("Attempted to create a pointed-by slot not pointed by a pointer (null pParent)");
		}
		if (!pParent.isTypePointer()) {
			throw new RuntimeException("Attempted to create a pointed-by slot not pointed by a pointer (pParent has not pointer type)");
		}
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
		return "*(" + this.parent.getParentAccessor() + ")";
	}
}
