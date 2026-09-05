package it.unisa.ocelot.genetic.objectives.branch_chains;
/**
 * EVINT_TOOL_MARKER
 * This class is used by the EvInT (Evolutionary Integration Testing) tool.
 */
/**
 * Represents a pair of components that should be analyzed together.
 * Parsed from configuration like "fun1,fun3;fun2,fun4".
 */
public class ComponentPair {

	private String component1;
    private String component2;
    
    public ComponentPair(String component1, String component2) {
        this.component1 = component1;
        this.component2 = component2;
    }
    
    public String getComponent1() {
        return component1;
    }
    
    public String getComponent2() {
        return component2;
    }
    
    @Override
    public String toString() {
        return component1 + " <-> " + component2;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentPair)) return false;
        ComponentPair other = (ComponentPair) obj;
        return (this.component1.equals(other.component1) && this.component2.equals(other.component2)) ||
               (this.component1.equals(other.component2) && this.component2.equals(other.component1));
    }
    
    @Override
    public int hashCode() {
        return component1.hashCode() + component2.hashCode();
    }

}
