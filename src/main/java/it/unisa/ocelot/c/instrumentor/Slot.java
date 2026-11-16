package it.unisa.ocelot.c.instrumentor;

import static it.unisa.ocelot.c.instrumentor.Utils.strip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;

import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IType;

abstract class Slot {
	protected final Slot parent;
	protected final IType type;
	protected String variableName;
	protected List<? extends Slot> childrenSyntactic;
	
	/**
	 * Factory method (parameters).
	 * 
	 * @param pType an {@link IType}.
	 * @return a (root) {@link Slot}; it will have 
	 *         {@code pType} as type and 
	 *         it will have no associated 
	 *         variable name (yet).
	 */
	static Slot make(IType pType) {
		return new SlotVariable(pType);
	}

	
	/**
	 * Factory method (externs).
	 * 
	 * @param pType an {@link IType}.
	 * @param pName a {@link String}.
	 * @return a (root) {@link Slot};
	 *         it will have {@code pType} as
	 *         type and {@code pName} as
	 *         associated variable name.
	 */	
	static Slot make(IType pType, String pName) {
		return new SlotVariable(pType, pName);
	}

	/**
	 * Constructor.
	 * 
	 * @param pParent the parent {@link Slot}. It 
	 *        may be {@code null} in the case
	 *        the slot is root (i.e., it is not
	 *        referred by/contained in another slot).
	 * @param pType the {@link IType} of the values
	 *        that can go in the slot. It must not
	 *        be {@code null}.
	 */
	protected Slot(Slot pParent, IType pType) {
		if (pType == null) {
			throw new RuntimeException("Attempted to create a slot with null pType");
		}
		this.parent = pParent;
		this.type = pType;
		this.variableName = null;
		this.childrenSyntactic = null; //initialized lazily
	}

	private List<? extends Slot> childrenSyntactic() {
		if (this.childrenSyntactic == null) {
			if (isTypeArray()) {
				this.childrenSyntactic = Collections.singletonList(slotArray(getComponent()));
			} else if (isTypePointer()) {
				this.childrenSyntactic = Collections.singletonList(slotPointed(getComponent()));
			} else if (isTypeStructUnion()) {
				this.childrenSyntactic = Arrays.stream(getFields()).map(this::slotField).toList();
			} else { 
				//this slot is primitive, or is an enum, 
				//or is a function type: It has no children
				this.childrenSyntactic = Collections.emptyList();
			}
		}
		return this.childrenSyntactic;
	}
	
	protected abstract Stream<? extends Slot> siblings();

	private SlotArray slotArray(IType pType) {
		return new SlotArray(this, pType);	
	}

	private SlotPointed slotPointed(IType pType) {
		return new SlotPointed(this, pType);
	}

	private SlotField slotField(IField pField) {
		return new SlotField(this, pField);
	}

	public final boolean isRoot() {
		return (this.parent == null);
	}

	public final boolean isDescendant() {
		return !isRoot();
	}

	public final boolean isLeaf() {
		return childrenSyntactic().isEmpty();
	}
	
	protected final boolean isSyntactic() {
		return !isSemantic();
	}

	protected abstract boolean isSemantic();
	
	public final void addSibling(String pName) {
		if (isSemantic()) {
			throw new RuntimeException("Attempted to add a sibling to a slot that is semantic");
		}
		doAddSibling(pName);
	}

	protected abstract void doAddSibling(String pName);

	public final Stream<? extends Slot> getChildren() {
		return childrenSyntactic().stream().flatMap(Slot::siblings);
	}
	
	public final List<Slot> getLeaves() {
		ArrayList<Slot> leaves = new ArrayList<>();

		Queue<Slot> queue = new LinkedList<>();
		queue.add(this);		
		while (!queue.isEmpty()) {
			Slot current = queue.poll();
			if (current.isLeaf()) {
				leaves.add(current);
			} else {
				queue.addAll(current.childrenSyntactic());
			}
		}

		return leaves;
	}

	public final IType getType() {
		return this.type;
	}
	
	public final String getTypeString() {
		return (isTypeStruct() ? "struct " : isTypeUnion() ? "union " : isTypeEnum() ? "enum " : "") +
				(isTypeVoid() ? "char" : strip(this.type).toString());
	}

	public final IType getComponent() {
		IType typeStripped = strip(getType());
		return Utils.getComponent(typeStripped);
	}
	
	public final boolean hasVariableName() {
		return !hasNoVariableName();
	}
	
	public final boolean hasNoVariableName() {
		return this.variableName == null;
	}
	
	public final void setVariableName(String pVariableName) {
		if (isSyntactic()) {
			throw new RuntimeException("Tried to set the name of a slot that is syntactic");
		}
		if (hasVariableName()) {
			throw new RuntimeException("Tried to modify the variable name associated to a slot");
		}
		this.variableName = pVariableName;
	}
	
	public final String getVariableName() {
		if (isSyntactic()) {
			throw new RuntimeException("Tried to get the variable name from a slot that is syntactic");
		}
		if (hasNoVariableName()) {
			throw new RuntimeException("Tried to get the variable name associated to a slot but the slot is not yet associated to a variable name");
		}
		return this.variableName;
	}

	public abstract String getParentAccessor();
	
	public final String getValueForParentAccessor() {
		if (isSyntactic()) {
			throw new RuntimeException("Tried to get the value for the parent's accessor from a slot that is syntactic");
		}
		if (hasNoVariableName()) {
			throw new RuntimeException("Tried to get the value for the parent's accessor from a slot but the slot is not yet associated to a variable name");
		}
		return (isDescendant() && this.parent.isTypePointer() ? "& " : "") +
				getVariableName();
	}
	
	public final IField[] getFields() {
		IType typeStripped = strip(getType());
		return Utils.getFields(typeStripped);
	}
	
	public final boolean isTypeArray() {
		IType typeStripped = strip(getType());
		return Utils.isArray(typeStripped);
	}

	public final boolean isTypePointer() {
		IType typeStripped = strip(getType());
		return Utils.isPointer(typeStripped);
	}

	public final boolean isTypeStructUnion() {
		IType typeStripped = strip(getType());
		return Utils.isStructUnion(typeStripped);
	}

	public final boolean isTypeStruct() {
		IType typeStripped = strip(getType());
		return Utils.isStruct(typeStripped);
	}

	public final boolean isTypeUnion() {
		IType typeStripped = strip(getType());
		return Utils.isUnion(typeStripped);
	}

	public final boolean isTypeComposite() {
		return isTypeArray() || isTypePointer() || isTypeStructUnion();
	}
	
	public final boolean isTypeVoid() {
		IType typeStripped = strip(getType());
		return Utils.isVoid(typeStripped);
	}
	
	public final boolean isTypeNumeric() {
		return !isTypeComposite() && !isTypeVoid();
	}

	public final boolean isTypeEnum() {
		IType typeStripped = strip(getType());
		return Utils.isEnum(typeStripped);
	}
}
