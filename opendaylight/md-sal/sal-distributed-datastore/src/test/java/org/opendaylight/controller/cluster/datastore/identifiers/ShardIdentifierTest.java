package org.opendaylight.controller.cluster.datastore.identifiers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ShardIdentifierTest {

    @Test
    public void testBasic(){
        ShardIdentifier id = ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config").build();

        assertEquals("member-1-shard-inventory-config", id.toString());
    }


}
