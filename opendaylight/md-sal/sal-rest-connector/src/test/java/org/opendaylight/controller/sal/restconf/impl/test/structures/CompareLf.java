package org.opendaylight.controller.sal.restconf.impl.test.structures;

import static org.junit.Assert.*;

import org.junit.Test;

public class CompareLf {

    @Test
    public void test() {
        Lf lf1 = new Lf("name", "value");
        Lf lf2 = new Lf("name", "value");
        Lf lf3 = new Lf("name1", "value");
        Lf lf4 = new Lf("name", "value1");
        
        assertTrue(lf1.equals(lf2));
        assertFalse(lf1.equals(lf3));
        assertFalse(lf1.equals(lf4));
    }

}
