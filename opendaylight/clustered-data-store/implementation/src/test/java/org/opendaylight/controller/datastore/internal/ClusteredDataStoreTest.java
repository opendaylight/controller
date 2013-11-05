package org.opendaylight.controller.datastore.internal;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ClusteredDataStoreTest {
    @Test
    public void construct(){
        assertNotNull(new ClusteredDataStore());
    }
}
