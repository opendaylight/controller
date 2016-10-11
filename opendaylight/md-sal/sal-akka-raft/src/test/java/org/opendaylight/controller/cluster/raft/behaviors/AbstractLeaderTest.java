/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertTrue;

import akka.actor.ActorRef;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.utils.ForwardMessageToBehaviorActor;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.duration.FiniteDuration;

public abstract class AbstractLeaderTest<T extends AbstractLeader> extends AbstractRaftActorBehaviorTest<T> {

    /**
     * When we removed scheduling of heartbeat in the AbstractLeader constructor we ended up with a situation where
     * if no follower responded to an initial AppendEntries heartbeats would not be sent to it. This test verifies
     * that regardless of whether followers respond or not we schedule heartbeats.
     */
    @Test
    public void testLeaderSchedulesHeartbeatsEvenWhenNoFollowersRespondToInitialAppendEntries() throws Exception {
        logStart("testLeaderSchedulesHeartbeatsEvenWhenNoFollowersRespondToInitialAppendEntries");

        String leaderActorId = actorFactory.generateActorId("leader");
        String follower1ActorId = actorFactory.generateActorId("follower");
        String follower2ActorId = actorFactory.generateActorId("follower");

        TestActorRef<ForwardMessageToBehaviorActor> leaderActor =
                actorFactory.createTestActor(ForwardMessageToBehaviorActor.props(), leaderActorId);
        final ActorRef follower1Actor = actorFactory.createActor(MessageCollectorActor.props(), follower1ActorId);
        final ActorRef follower2Actor = actorFactory.createActor(MessageCollectorActor.props(), follower2ActorId);

        MockRaftActorContext leaderActorContext =
                new MockRaftActorContext(leaderActorId, getSystem(), leaderActor);

        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(200, TimeUnit.MILLISECONDS));
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(10, TimeUnit.SECONDS));

        leaderActorContext.setConfigParams(configParams);

        leaderActorContext.setReplicatedLog(
                new MockRaftActorContext.MockReplicatedLogBuilder().createEntries(1,5,1).build());

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(follower1ActorId,
                follower1Actor.path().toString());
        peerAddresses.put(follower2ActorId,
                follower2Actor.path().toString());


        leaderActorContext.setPeerAddresses(peerAddresses);

        RaftActorBehavior leader = createBehavior(leaderActorContext);

        leaderActor.underlyingActor().setBehavior(leader);

        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        List<SendHeartBeat> allMessages = MessageCollectorActor.getAllMatching(leaderActor, SendHeartBeat.class);

        // Need more than 1 heartbeat to be delivered because we waited for 1 second with heartbeat interval 200ms
        assertTrue(String.format("%s messages is less than expected", allMessages.size()),
                allMessages.size() > 1);
    }
}
