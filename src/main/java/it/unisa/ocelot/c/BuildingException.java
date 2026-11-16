package it.unisa.ocelot.c;

public class BuildingException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5412131394361814653L;

	public BuildingException(String pMessage) {
		super(pMessage);
	}
	
	public BuildingException(Throwable pThrowable) {
		super(pThrowable);
	}
	
	public BuildingException() {
		super();
	}
}
