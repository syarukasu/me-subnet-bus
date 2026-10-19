package com.syaru.mesubnetbus.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.syaru.mesubnetbus.core.ExposureGraph.Result.*;

class ExposureGraphTest {
    @Test void modePermissionsAndWrap() {
        assertFalse(Mode.POWER.storage());
        assertTrue(Mode.SHARED.insert && Mode.SHARED.extract);
        assertTrue(Mode.EXTRACT.extract && !Mode.EXTRACT.insert);
        assertTrue(Mode.INSERT.insert && !Mode.INSERT.extract);
        for (Mode m : Mode.values()) assertEquals(m, m.next(1).next(-1));
        assertEquals(Mode.INSERT, Mode.POWER.next(-1));
        assertEquals(Mode.SHARED, Mode.decode(999));
    }
    @Test void chainsCyclesAndDiamonds() {
        var g = new ExposureGraph<Object>();
        var a = new Object(); var b = new Object(); var c = new Object(); var d = new Object();
        assertEquals(SAME_NETWORK, g.add(a,a));
        assertEquals(ACCEPTED, g.add(a,b));
        assertEquals(ACCEPTED, g.add(b,c));
        assertEquals(CYCLE, g.add(c,a));
        assertEquals(DUPLICATE_PATH, g.add(a,c));
        assertEquals(ACCEPTED, g.add(a,d));
        assertEquals(DUPLICATE_PATH, g.add(d,c));
        assertEquals(ACCEPTED, g.add(new Object(),c));
    }
    @Test void duplicateRejectedWithoutRemovingExistingEdge() {
        var g = new ExposureGraph<Object>(); var a = new Object(); var b = new Object();
        assertEquals(ACCEPTED, g.add(a,b));
        assertEquals(DUPLICATE_PATH, g.add(a,b));
        assertEquals(CYCLE, g.add(b,a));
    }
}
