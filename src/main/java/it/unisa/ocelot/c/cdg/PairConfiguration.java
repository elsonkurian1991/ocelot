package it.unisa.ocelot.c.cdg;

import java.util.ArrayList;
import java.util.List;

public class PairConfiguration {

private List<ComponentPair> componentPairs;
    
    public PairConfiguration() {
        this.componentPairs = new ArrayList<>();
    }
    
    /**
     * Parses a configuration string and extracts component pairs.
     * 
     * Format: "fun1,fun3;fun2,fun4;fun5,fun6"
     * Each pair is separated by semicolon, components within a pair by comma.
     * 
     * @param configString Configuration string
     */
    public void parseConfiguration(String configString) {
        componentPairs.clear();
        
        if (configString == null || configString.trim().isEmpty()) {
            return;
        }
        
        String[] pairs = configString.split(";");
        
        for (String pair : pairs) {
            String[] components = pair.trim().split(",");
            
            if (components.length == 2) {
                String comp1 = components[0].trim();
                String comp2 = components[1].trim();
                componentPairs.add(new ComponentPair(comp1, comp2));
            } else {
                System.err.println("WARNING: Invalid pair format: " + pair);
            }
        }
    }
    
    /**
     * Adds a component pair programmatically.
     * 
     * @param component1 First component name
     * @param component2 Second component name
     */
    public void addPair(String component1, String component2) {
        componentPairs.add(new ComponentPair(component1, component2));
    }
    
    /**
     * Returns all configured component pairs.
     * 
     * @return List of ComponentPair objects
     */
    public List<ComponentPair> getPairComponents() {
        return new ArrayList<>(componentPairs);
    }
    
    /**
     * Returns the number of configured pairs.
     */
    public int getPairCount() {
        return componentPairs.size();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Component Pairs Configuration (").append(componentPairs.size()).append(" pairs):\n");
        for (ComponentPair pair : componentPairs) {
            sb.append("  - ").append(pair.toString()).append("\n");
        }
        return sb.toString();
    }
}
