package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ActorContextTest extends AbstractActorTest{
    @Test
    public void testResolvePathForRemoteActor(){
        ActorContext actorContext =
            new ActorContext(mock(ActorSystem.class), mock(ActorRef.class),mock(
                ClusterWrapper.class),
                mock(Configuration.class));

        String actual = actorContext.resolvePath(
            "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard",
            "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);
    }

    @Test
    public void testResolvePathForLocalActor(){
        ActorContext actorContext =
            new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class));

        String actual = actorContext.resolvePath(
            "akka://system/user/shardmanager/shard",
            "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka://system/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);

        System.out.println(actorContext
            .actorFor("akka://system/user/shardmanager/shard/transaction"));
    }
}
