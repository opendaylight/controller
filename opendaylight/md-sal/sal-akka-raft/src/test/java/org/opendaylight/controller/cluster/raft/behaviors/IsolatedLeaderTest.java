/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.apache.pekko.actor.ActorRef;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.raft.api.RaftRole;

public class IsolatedLeaderTest extends AbstractLeaderTest<IsolatedLeader> {

    private final ActorRef leaderActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("leader"));

    private final ActorRef senderActor = actorFactory.createActor(
            MessageCollectorActor.props(), actorFactory.generateActorId("sender"));

    private AbstractLeader isolatedLeader;

    @Override
    @After
    public void tearDown() {
        if (isolatedLeader != null) {
            isolatedLeader.close();
        }

        super.tearDown();
    }

    @Override
    protected IsolatedLeader createBehavior(final RaftActorContext actorContext) {
        return new IsolatedLeader(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext(final int payloadVersion) {
        return createActorContext(leaderActor, payloadVersion);
    }

    @Override
    protected MockRaftActorContext createActorContext(final ActorRef actor, final int payloadVersion) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setElectionTimeoutFactor(100000);
        MockRaftActorContext context = new MockRaftActorContext("isolated-leader", getSystem(), actor, payloadVersion);
        context.setConfigParams(configParams);
        return context;
    }

    @Test
    public void testHandleMessageWithThreeMembers() {
        String followerAddress1 = "pekko://test/user/$a";
        String followerAddress2 = "pekko://test/user/$b";

        MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(isolatedLeader);
        assertEquals("Raft state", RaftRole.IsolatedLeader, isolatedLeader.raftRole());

        // in a 3 node cluster, even if 1 follower is returns a reply, the isolatedLeader is not isolated
        RaftActorBehavior newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short)0));

        assertEquals("Raft state", RaftRole.Leader, newBehavior.raftRole());

        isolatedLeader.close();
        isolatedLeader = (AbstractLeader) newBehavior;

        newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-2", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short) 0));

        assertEquals("Raft state", RaftRole.Leader, newBehavior.raftRole());
    }

    @Test
    public void testHandleMessageWithFiveMembers() {
        String followerAddress1 = "pekko://test/user/$a";
        String followerAddress2 = "pekko://test/user/$b";
        String followerAddress3 = "pekko://test/user/$c";
        String followerAddress4 = "pekko://test/user/$d";

        final MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        peerAddresses.put("follower-3", followerAddress3);
        peerAddresses.put("follower-4", followerAddress4);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(isolatedLeader);
        assertEquals("Raft state", RaftRole.IsolatedLeader, isolatedLeader.raftRole());

        // in a 5 member cluster, atleast 2 followers need to be active and return a reply
        RaftActorBehavior newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short) 0));

        assertEquals("Raft state", RaftRole.IsolatedLeader, newBehavior.raftRole());

        newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-2", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short) 0));

        assertEquals("Raft state", RaftRole.Leader, newBehavior.raftRole());

        isolatedLeader.close();
        isolatedLeader = (AbstractLeader) newBehavior;

        newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-3", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short) 0));

        assertEquals("Raft state", RaftRole.Leader, newBehavior.raftRole());
    }

    @Test
    public void testHandleMessageFromAnotherLeader() {
        String followerAddress1 = "pekko://test/user/$a";
        String followerAddress2 = "pekko://test/user/$b";

        MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        assertEquals("Raft state", RaftRole.IsolatedLeader, isolatedLeader.raftRole());

        // if an append-entries reply is received by the isolated-leader, and that reply
        // has a term  > than its own term, then IsolatedLeader switches to Follower
        // bowing itself to another leader in the cluster
        RaftActorBehavior newBehavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() + 1, true,
                        isolatedLeader.lastIndex() + 1, isolatedLeader.lastTerm() + 1, (short)0));

        assertEquals("Raft state", RaftRole.Follower, newBehavior.raftRole());

        newBehavior.close();
    }
}
