package it.unisa.ocelot.c.cdg;

public class CDGEdge {
    private String label;

    public CDGEdge() {
        this.label = "";
    }

    public CDGEdge(String label) {
        this.label = label != null ? label : "";
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label.isEmpty() ? "" : "[" + label + "]";
    }
}
