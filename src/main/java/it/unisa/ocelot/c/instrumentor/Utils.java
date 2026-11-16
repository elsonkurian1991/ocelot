package it.unisa.ocelot.c.instrumentor;

import org.eclipse.cdt.core.dom.ast.IArrayType;
import org.eclipse.cdt.core.dom.ast.IBasicType;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.IEnumeration;
import org.eclipse.cdt.core.dom.ast.IField;
import org.eclipse.cdt.core.dom.ast.IPointerType;
import org.eclipse.cdt.core.dom.ast.IQualifierType;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.ITypedef;
import org.eclipse.cdt.core.dom.ast.IBasicType.Kind;

public final class Utils {
	/**
	 * Resolves all the typedefs and drops all 
	 * qualifiers (const, volatile...)
	 * to a type.
	 * 
	 * @param type an {@link IType}.
	 * @return {@code type} if it is not a typedef and has
	 *         no qualifiers, otherwise the 
	 *         type to which the typedefs resolve (recursively),
	 *         stripped of all the C qualifiers.
	 */
	public static IType strip(IType type) {
		IType result = type;
		while (true) {
			if (isQualified(result)) {
				result = ((IQualifierType) result).getType();
			} else if (isTypedef(result)) {
				result = ((ITypedef) result).getType();
			} else {
				return result;
			}
		}
	}
	
	/**
	 * Gets the component type of an 
	 * array or pointer type.
	 * 
	 * @param type an {@link IType}.
	 * @return the component type if {@code type} is
	 *         an array or pointer type.
	 * @throws RuntimeException if type is neither an array
	 *         nor a pointer type.
	 */
	public static IType getComponent(IType type) {
		if (isArray(type)) {
			return ((IArrayType) type).getType();
		} else if (isPointer(type)) {
			return ((IPointerType) type).getType();
		} else {
			throw new RuntimeException("Utils.getComponent() invoked with a IType that is neither an array nor a pointer type");
		}
	}
	
	/**
	 * Gets the name of a struct, union, or enum type.
	 * 
	 * @param type an {@link IType}.
	 * @return the component type if {@code type} is
	 *         an array or pointer type.
	 * @throws RuntimeException if type is neither an array
	 *         nor a pointer type.
	 */
	public static String getName(IType type) {
		if (isStructUnion(type)) {
			return ((ICompositeType) type).getName();
		} else if (isEnum(type)) {
			return ((IEnumeration) type).getName();
		} else {
			throw new RuntimeException("Utils.getName() invoked with a IType that is neither an struct nor a union nor an enum type");
		}
	}
	
	/**
	 * Gets the fields of a
	 * struct or union type.
	 * 
	 * @param type an {@link IType}.
	 * @return the {@link IField}{@code []} of all the fields
	 *         of {@code type} if it is
	 *         a struct or union type.
	 * @throws RuntimeException if type is neither a struct
	 *         nor a union type.
	 */
	public static IField[] getFields(IType type) {
		return ((ICompositeType) type).getFields();
	}

	public static boolean isArray(IType type) {
		return (type instanceof IArrayType);
	}

	public static boolean isEnum(IType type) {
		return (type instanceof IEnumeration);
	}

	public static boolean isPointer(IType type) {
		return (type instanceof IPointerType);
	}

	public static boolean isQualified(IType type) {
		return (type instanceof IQualifierType);
	}

	public static boolean isStructUnion(IType type) {
		return (type instanceof ICompositeType);
	}

	public static boolean isStruct(IType type) {
		return (isStructUnion(type) &&
				((ICompositeType) type).getKey() == ICompositeType.k_struct);
	}

	public static boolean isUnion(IType type) {
		return (isStructUnion(type) &&
				((ICompositeType) type).getKey() == ICompositeType.k_union);
	}

	public static boolean isTypedef(IType type) {
		return (type instanceof ITypedef);
	}

	public static boolean isVoid(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eVoid);
	}
	
	public static boolean isInt(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				!((IBasicType) type).isShort() &&
				!((IBasicType) type).isLong() &&
				!((IBasicType) type).isLongLong() &&
				!((IBasicType) type).isSigned() &&
				!((IBasicType) type).isUnsigned());
	}

	public static boolean isSignedInt(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				!((IBasicType) type).isShort() &&
				!((IBasicType) type).isLong() &&
				!((IBasicType) type).isLongLong() &&
				((IBasicType) type).isSigned());
	}

	public static boolean isUnsignedInt(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				!((IBasicType) type).isShort() &&
				!((IBasicType) type).isLong() &&
				!((IBasicType) type).isLongLong() &&
				((IBasicType) type).isUnsigned());
	}
	
	public static boolean isShort(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isShort() &&
				!((IBasicType) type).isSigned() &&
				!((IBasicType) type).isUnsigned());
	}

	public static boolean isSignedShort(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isShort() &&
				((IBasicType) type).isSigned());
	}

	public static boolean isUnsignedShort(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isShort() &&
				((IBasicType) type).isUnsigned());
	}
	
	public static boolean isLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLong() &&
				!((IBasicType) type).isSigned() &&
				!((IBasicType) type).isUnsigned());
	}

	public static boolean isSignedLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLong() &&
				((IBasicType) type).isSigned());
	}

	public static boolean isUnsignedLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLong() &&
				((IBasicType) type).isUnsigned());
	}
	
	public static boolean isLongLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLongLong() &&
				!((IBasicType) type).isSigned() &&
				!((IBasicType) type).isUnsigned());
	}

	public static boolean isSignedLongLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLongLong() &&
				((IBasicType) type).isSigned());
	}

	public static boolean isUnsignedLongLong(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eInt &&
				((IBasicType) type).isLongLong() &&
				((IBasicType) type).isUnsigned());
	}
	
	public static boolean isChar(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eChar &&
				!((IBasicType) type).isSigned() &&
				!((IBasicType) type).isUnsigned());
	}

	public static boolean isSignedChar(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eChar &&
				((IBasicType) type).isSigned());
	}
	
	public static boolean isUnsignedChar(IType type) {
		return (type instanceof IBasicType &&
				((IBasicType) type).getKind() == Kind.eChar &&
				((IBasicType) type).isUnsigned());
	}

	public static boolean isBool(IType type) {
		return (type instanceof IBasicType && 
				((IBasicType) type).getKind() == Kind.eBoolean);
	}

	public static boolean isFloat(IType type) {
		return (type instanceof IBasicType && 
				((IBasicType) type).getKind() == Kind.eFloat);
	}

	public static boolean isDouble(IType type) {
		return (type instanceof IBasicType && 
				((IBasicType) type).getKind() == Kind.eDouble) &&
				!((IBasicType) type).isLong();
	}

	public static boolean isLongDouble(IType type) {
		return (type instanceof IBasicType && 
				((IBasicType) type).getKind() == Kind.eDouble) &&
				((IBasicType) type).isLong();
	}

	public static int numPointerIndirections(IType type) {
		int result = 0;
		IType t = strip(type);
		while (isPointer(t)) {
			++result;
			t = strip(((IPointerType) t).getType());
		}

		return result;
	}

	private Utils() {
		throw new AssertionError();
	}
}
