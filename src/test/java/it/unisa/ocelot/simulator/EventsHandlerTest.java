package it.unisa.ocelot.simulator;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test case for the class it.unisa.ocelot.simulator.EventsHandler
 * @author Giovanni Grano
 */
public class EventsHandlerTest {
    private EventsHandler eventsHandler;

    @Before
    public void setUp() {
        this.eventsHandler = new EventsHandler();
    }

    @Test
    public void testGetEvents() throws Exception {
    	this.eventsHandler.add(0, 0.12, 0);
    	this.eventsHandler.addCase(0, 0.2323, false);
        assertEquals(this.eventsHandler.getEvents().size(), 2);
        IfExecutionEvent event0 = this.eventsHandler.getEvents().get(0);
        assertEquals(0, event0.id);
        assertEquals(0.12, event0.distanceThen, 0.0);
        assertEquals(0, event0.distanceElse, 0.0);
        assertEquals(CaseExecutionEvent.class, this.eventsHandler.getEvents().get(1).getClass());
        CaseExecutionEvent event1 = (CaseExecutionEvent) this.eventsHandler.getEvents().get(1);
        assertEquals(0, event1.id);
        assertEquals(0.2323, event1.distanceThen, 0.0);
        assertEquals(0, event1.distanceElse, 0.0);
    }

    @Test
    public void testAdd() throws Exception {
        this.eventsHandler.add(0, 0.7571, 0);
        IfExecutionEvent event = this.eventsHandler.getEvents().get(0);
        assertEquals(event.id, 0);
        assertEquals(event.distanceThen, 0.7571, 0.0);
        assertEquals(event.distanceElse, 0.0, 0.0);
    }

    @Test
    public void testAddCase() throws Exception {
        this.eventsHandler.addCase(0, 0.7571, false);
        CaseExecutionEvent event = (CaseExecutionEvent) this.eventsHandler.getEvents().get(0);
        assertEquals(event.id, 0);
        assertEquals(event.distanceThen, 0.7571, 0.0);
        assertEquals(event.chosen, false);
    }
}