package it.unisa.ocelot.c.types;

/**
 * The C double type and all the types
 * *double, **double, ***double... More precisely:
 * the <emph>IEEE 754</emph>, <emph>64-bit</emph>, 
 * <emph>double precision</emph> floating point 
 * type, aka binary64 in IEEE 754 parlance.
 */
public class CTypeDouble extends CType {
	//just two instances
	private static final CTypeDouble C_TYPE_DOUBLE = new CTypeDouble(false);
	private static final CTypeDouble C_TYPE_DOUBLE_POINTER = new CTypeDouble(true);
	
	public static CTypeDouble type(boolean pPointer) {
		return (pPointer ? C_TYPE_DOUBLE_POINTER : C_TYPE_DOUBLE);
	}
	
	private CTypeDouble(boolean pPointer) {
		super(pPointer);
	}
	
	@Override
	public Object convert(Object pValue) {
		if (pValue instanceof Number) {
			return Double.valueOf((((Number) pValue).doubleValue()));
		} else {
			return null;
		}
	}
	
	@Override
	public double getMinValue() {
		return Double.MIN_VALUE;
	}
	
	@Override
	public double getMaxValue() {
		return Double.MAX_VALUE;
	}
	
	@Override
	public boolean isDiscrete() {
		return false;
	}
}
