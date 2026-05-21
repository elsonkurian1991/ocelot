package it.unisa.ocelot.c.cdg;

import it.unisa.ocelot.c.cfg.edges.LabeledEdge;
import it.unisa.ocelot.simulator.ExecutionEvent;

/**
 * Synthetic CFG edge used when augmenting the CFG for CDG construction
 * (entry/exit wiring in legacy {@code CDGBuilder*} classes).
 */
public class MyLabeledEdge extends LabeledEdge {

    private static final long serialVersionUID = 1L;

    public MyLabeledEdge() {
        super("");
    }

    @Override
    public boolean matchesExecution(ExecutionEvent pEvent) {
        return true;
    }

    @Override
    public boolean needsEvent() {
        return false;
    }

    @Override
    public String toString() {
        return "MyLabeledEdge";
    }
}
