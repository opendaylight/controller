package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;

public class FixedLeaderElectionStrategyTest {

    @Test
    public void testIsAllowedToBecomeLeader(){
        Dictionary<String, Object> leaders = new Hashtable<>();

        leaders.put("inventory-config", "member-3");
        FixedLeaderElectionStrategy fixedLeaderElectionStrategy = new FixedLeaderElectionStrategy(leaders);

        assertFalse(fixedLeaderElectionStrategy.isAllowedToBecomeLeader(toShardIdentifier("member-2", "inventory", "operational")));
        assertTrue(fixedLeaderElectionStrategy.isAllowedToBecomeLeader(toShardIdentifier("member-1", "inventory", "operational")));
        assertTrue(fixedLeaderElectionStrategy.isAllowedToBecomeLeader(toShardIdentifier("member-3", "inventory", "config")));

    }

    String toShardIdentifier(String memberName, String shardName, String type){
        return ShardIdentifier.builder().memberName(memberName).shardName(shardName).type(type).build().toString();
    }
}