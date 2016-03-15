/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.TestActorRef;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class IsolatedLeaderTest extends AbstractLeaderTest<IsolatedLeader> {

    private final TestActorRef<MessageCollectorActor> leaderActor = actorFactory.createTestActor(
            Props.create(MessageCollectorActor.class), actorFactory.generateActorId("leader"));

    private final TestActorRef<MessageCollectorActor> senderActor = actorFactory.createTestActor(
            Props.create(MessageCollectorActor.class), actorFactory.generateActorId("sender"));

    private AbstractLeader isolatedLeader;

    @Override
    @After
    public void tearDown() throws Exception {
        if(isolatedLeader != null) {
            isolatedLeader.close();
        }

        super.tearDown();
    }

    @Override
    protected IsolatedLeader createBehavior(RaftActorContext actorContext) {
        return new IsolatedLeader(actorContext);
    }

    @Override
    protected MockRaftActorContext createActorContext() {
        return createActorContext(leaderActor);
    }

    @Override
    protected MockRaftActorContext createActorContext(ActorRef actor) {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setElectionTimeoutFactor(100000);
        MockRaftActorContext context = new MockRaftActorContext("isolated-leader", getSystem(), actor);
        context.setConfigParams(configParams);
        return context;
    }

    @Test
    public void testHandleMessageWithThreeMembers() throws Exception {
        String followerAddress1 = "akka://test/user/$a";
        String followerAddress2 = "akka://test/user/$b";

        MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(isolatedLeader);
        assertEquals("Raft state", RaftState.IsolatedLeader, isolatedLeader.state());

        // in a 3 node cluster, even if 1 follower is returns a reply, the isolatedLeader is not isolated
        RaftActorBehavior behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() - 1, isolatedLeader.lastTerm() - 1, (short)0));

        assertEquals("Raft state", RaftState.Leader, behavior.state());

        isolatedLeader.close();
        isolatedLeader = (AbstractLeader) behavior;

        behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-2", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() -1, isolatedLeader.lastTerm() -1, (short)0 ));

        assertEquals("Raft state", RaftState.Leader, behavior.state());
    }

    @Test
    public void testHandleMessageWithFiveMembers() throws Exception {
        String followerAddress1 = "akka://test/user/$a";
        String followerAddress2 = "akka://test/user/$b";
        String followerAddress3 = "akka://test/user/$c";
        String followerAddress4 = "akka://test/user/$d";

        MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        peerAddresses.put("follower-3", followerAddress3);
        peerAddresses.put("follower-4", followerAddress4);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        leaderActorContext.setCurrentBehavior(isolatedLeader);
        assertEquals("Raft state", RaftState.IsolatedLeader, isolatedLeader.state());

        // in a 5 member cluster, atleast 2 followers need to be active and return a reply
        RaftActorBehavior behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() -1, isolatedLeader.lastTerm() -1, (short)0 ));

        assertEquals("Raft state", RaftState.IsolatedLeader, behavior.state());

        behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-2", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() -1, isolatedLeader.lastTerm() -1, (short)0 ));

        assertEquals("Raft state", RaftState.Leader, behavior.state());

        isolatedLeader.close();
        isolatedLeader = (AbstractLeader) behavior;

        behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-3", isolatedLeader.lastTerm() - 1, true,
                        isolatedLeader.lastIndex() -1, isolatedLeader.lastTerm() -1, (short)0 ));

        assertEquals("Raft state", RaftState.Leader, behavior.state());
    }

    @Test
    public void testHandleMessageFromAnotherLeader() throws Exception {
        String followerAddress1 = "akka://test/user/$a";
        String followerAddress2 = "akka://test/user/$b";

        MockRaftActorContext leaderActorContext = createActorContext();
        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put("follower-1", followerAddress1);
        peerAddresses.put("follower-2", followerAddress2);
        leaderActorContext.setPeerAddresses(peerAddresses);

        isolatedLeader = new IsolatedLeader(leaderActorContext);
        assertEquals("Raft state", RaftState.IsolatedLeader, isolatedLeader.state());

        // if an append-entries reply is received by the isolated-leader, and that reply
        // has a term  > than its own term, then IsolatedLeader switches to Follower
        // bowing itself to another leader in the cluster
        RaftActorBehavior behavior = isolatedLeader.handleMessage(senderActor,
                new AppendEntriesReply("follower-1", isolatedLeader.lastTerm() + 1, true,
                        isolatedLeader.lastIndex() + 1, isolatedLeader.lastTerm() + 1, (short)0));

        assertEquals("Raft state", RaftState.Follower, behavior.state());

        behavior.close();
    }
}
