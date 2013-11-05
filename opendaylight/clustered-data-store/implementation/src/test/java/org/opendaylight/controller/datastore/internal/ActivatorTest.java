package org.opendaylight.controller.datastore.internal;

import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class ActivatorTest {

    @Test
    public void construct(){
        assertNotNull(new Activator());
    }
}
