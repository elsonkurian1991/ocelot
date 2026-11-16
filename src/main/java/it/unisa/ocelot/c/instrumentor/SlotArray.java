package it.unisa.ocelot.c.instrumentor;

import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IType;

/**
 * A slot inside an array.
 */
final class SlotArray extends Slot {
	private final Integer index;
	private final ArrayList<String> names;

	/**
	 * Constructor (syntactic slot).
	 * 
	 * @param pParent the parent {@link Slot}. It 
	 *        must not be {@code null} and must
	 *        have array type.
	 * @param pType the {@link IType} of the values
	 *        that can go in the slot.
	 */
	SlotArray(Slot pParent, IType pType) {
		super(pParent, pType);
		if (pParent == null) {
			throw new RuntimeException("Attempted to create an array slot not contained in an array (null pParent)");
		}
		if (!pParent.isTypeArray()) {
			throw new RuntimeException("Attempted to create an array slot not contained in an array (pParent has not array type)");
		}
		this.index = null;
		this.names = new ArrayList<>();
	}

	/**
	 * Constructor (semantic slot).
	 * 
	 * @param pParent the parent {@link Slot}. It 
	 *        must not be {@code null} and must
	 *        have array type.
	 * @param pType the {@link IType} of the values
	 *        that can go in the slot.
	 * @param pIndex an {@code int}, the array index 
	 *        of this slot.
	 * @param pName the name of this slot.
	 */
	private SlotArray(Slot pParent, IType pType, int pIndex, String pName) {
		super(pParent, pType);
		if (!pParent.isTypeArray()) {
			throw new RuntimeException("Attempted to create an array slot not contained in an array");
		}
		this.index = Integer.valueOf(pIndex);
		this.names = null;
		setVariableName(pName);
	}

	@Override
	protected Stream<? extends Slot> siblings() {
		if (isSemantic() || this.names.isEmpty()) {
			return Stream.of(this);
		} else {
			return IntStream.range(0, this.names.size())
					.mapToObj(i -> new SlotArray(this.parent, this.type, i, this.names.get(i)));
		}
	}

	@Override
	protected boolean isSemantic() {
		return (this.names == null);
	}

	@Override
	protected void doAddSibling(String pName) {
		this.names.add(pName);
	}

	@Override
	public String getParentAccessor() {
		if (isSyntactic()) {
			throw new RuntimeException("Attempted to get the parent accessor from an array slot that is syntactic");
		}
		return this.parent.getParentAccessor() + "[" + this.index + "]";
	}
}

