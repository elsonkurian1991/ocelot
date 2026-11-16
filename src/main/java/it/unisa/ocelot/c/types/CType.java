package it.unisa.ocelot.c.types;

/**
 * A (set of related) C types. 
 * Instances of a subclass 
 * of this class shall represent a
 * C non-pointer primitive type T 
 * (int, float...) or (the set of all) 
 * the pointer-to-T types *T, **T, ***T....
 */
public abstract class CType {
	protected final boolean pointer;
	
	/**
	 * Constructor.
	 * 
	 * @param pPointer {@code false} if
	 *        this object represents 
	 *        a non-pointer primitive
	 *        type T (e.g. int), {@code true}
	 *        if it represents the set of all 
	 *        the corresponding pointer-to-T
	 *        types *T, **T, ***T...
	 */
	protected CType(boolean pPointer) {
		this.pointer = pPointer;
	}
	
	/**
	 * Is this object representing a pointer type?
	 * 
	 * @return {@code false} if it is a
	 */
	public boolean isPointer() {
		return this.pointer;
	}
	
	/**
	 * Converts a value of some numeric C 
	 * type to a value of the C type this
	 * object represents (possibly with 
	 * semantic misalignments w.r.t. the
	 * C programming language semantics
	 * of type conversions).
	 * 
	 * @param pValue an arbitrary object representing 
	 *        a C value with (numeric) type S; {@code pValue}
	 *        may possibly represent its associated C value 
	 *        in a possibly approximate way, given that
	 *        this representation is "as faithfully 
	 *        as possible" to the associated C value.
	 * @return a Java object representing the conversion 
	 *         of the C value of type S 
	 *         represented by {@code pValue},
	 *         to a value of type T, given that this 
	 *         object represents the C type T or the set
	 *         of C types *T, **T, ***T...
	 *         The conversion should be "as faithful
	 *         as possible" to the S-to-T
	 *         conversion mandated by the C programming
	 *         language standard. The returned Java object
	 *         will represent the result of the
	 *         conversion "as faithfully as possible".
	 *         Note that this implies that three approximations
	 *         may come into play: the approximation due
	 *         to representing the C value to convert
	 *         by means of {@code pValue}; the approximation
	 *         potentially introduced if the S-to-T conversion
	 *         calculated by this method is not completely 
	 *         faithful to the semantics mandated by the C 
	 *         programming language standard; and finally, 
	 *         the approximation of representing the result
	 *         of the conversion by means of the returned 
	 *         Java object.
	 *         If {@code pValue} does not represent
	 *         any C value, or represents a C value that 
	 *         cannot be converted to T, or this method 
	 *         cannot convert or represent the result of 
	 *         the conversion in a "sufficiently faithful" 
	 *         way, the method returns {@code null}.
	 */
	public abstract Object convert(Object pValue);
	
	/**
	 * The minimum value representable by T,
	 * converted to a Java {@code double},
	 * if this object represents the C 
	 * type T or the set of C types 
	 * *T, **T, ***T...
	 * 
	 * @return a {@code double}.
	 */
	public abstract double getMinValue();

	/**
	 * The minimum value representable by T,
	 * converted to a Java {@code double},
	 * where this object represents
	 * T or *T, **T, ***T...
	 * 
	 * @return a {@code double}.
	 */
	public abstract double getMaxValue();
	
	/**
	 * Is this type discrete?
	 * 
	 * @return {@code true} if this object represents
	 *         T or *T, **T, ***T... where T is a integer
	 *         type, {@code false} otherwise (i.e., if
	 *         T is a floating point type).
	 */
	public abstract boolean isDiscrete();
}
