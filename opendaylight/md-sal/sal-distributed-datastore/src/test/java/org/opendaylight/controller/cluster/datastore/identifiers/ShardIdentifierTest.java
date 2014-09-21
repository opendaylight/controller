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

    @Test
    public void testFromShardIdString(){
        String shardIdStr = "member-1-shard-inventory-config";

        ShardIdentifier id = ShardIdentifier.builder().fromShardIdString(shardIdStr).build();

        assertEquals("member-1", id.getMemberName());
        assertEquals("inventory", id.getShardName());
        assertEquals("config", id.getType());
    }
}
