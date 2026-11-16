package it.unisa.ocelot.c.types;

/**
 * The C long long type and all the types
 * *(long long), **(long long), ***(long long)... 
 * More precisely: the <emph>signed (two-complement)</emph>, 
 * <emph>64-bit</emph> integer type, aka int64_t 
 * in C99 parlance.
 */
public final class CTypeInteger extends CType {
	//just two instances
	private static final CTypeInteger C_TYPE_INTEGER = new CTypeInteger(false);
	private static final CTypeInteger C_TYPE_INTEGER_POINTER = new CTypeInteger(true);
	
	public static CTypeInteger type(boolean pPointer) {
		return (pPointer ? C_TYPE_INTEGER_POINTER : C_TYPE_INTEGER);
	}
	
	private CTypeInteger(boolean pPointer) {
		super(pPointer);
	}
	
	@Override
	public Object convert(Object pValue) {
		if (pValue instanceof Number) {
			return Long.valueOf(((Number) pValue).longValue());
		} else {
			return null;
		}
	}
	
	@Override
	public double getMinValue() {
		return Integer.MIN_VALUE;
	}
	
	@Override
	public double getMaxValue() {
		return Integer.MAX_VALUE;
	}
	
	@Override
	public boolean isDiscrete() {
		return true;
	}
}
