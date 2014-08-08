package org.opendaylight.controller.cluster.datastore.identifiers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

    public class ShardManagerIdentifierTest {

    @Test
    public void testIdentifier(){
        assertEquals("shardmanager-operational", ShardManagerIdentifier.builder().type("operational").build().toString());
    }

}
