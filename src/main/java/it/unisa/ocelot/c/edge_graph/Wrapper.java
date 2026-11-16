package it.unisa.ocelot.c.edge_graph;

import java.util.Objects;

/**
 * A silly generic wrapper class.
 * 
 * @param <E> The wrapped object
 */
public class Wrapper<E> {
	private final E wrapped;
	
	public Wrapper(E pWrapped) {
		this.wrapped = pWrapped;
	}
	
	public Wrapper() {
		this(null); //nothing wrapped
	}
	
	public E getWrapped() {
		return this.wrapped;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		
		@SuppressWarnings("rawtypes")
		Wrapper other = (Wrapper) obj;		
		return Objects.equals(this.wrapped, other.wrapped);
	}
}
