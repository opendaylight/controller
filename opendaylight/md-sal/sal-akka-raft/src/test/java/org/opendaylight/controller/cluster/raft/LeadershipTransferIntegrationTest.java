/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static akka.pattern.Patterns.ask;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Tests leadership transfer end-to-end.
 *
 * @author Thomas Pantelis
 */
public class LeadershipTransferIntegrationTest extends AbstractRaftActorIntegrationTest {

    private TestActorRef<MessageCollectorActor> leaderNotifierActor;
    private TestActorRef<MessageCollectorActor> follower1NotifierActor;
    private TestActorRef<MessageCollectorActor> follower2NotifierActor;

    @Test
    public void test() throws Throwable {
        testLog.info("LeadershipTransferIntegrationTest starting");

        createRaftActors();

        sendPayloadWithFollower2Lagging();

        sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1();

        sendShutDownToFollower2();

        testLog.info("LeadershipTransferIntegrationTest ending");
    }

    private void sendShutDownToFollower2() throws Exception {
        testLog.info("sendShutDownToFollower2 starting");

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        Future<Boolean> stopFuture = Patterns.gracefulStop(follower2Actor, duration, new Shutdown());

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        testLog.info("sendShutDownToFollower2 ending");
    }

    private void sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1() throws Throwable {
        testLog.info("sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1 starting");

        clearMessages(leaderNotifierActor);
        clearMessages(follower1NotifierActor);
        clearMessages(follower2NotifierActor);

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        Future<Boolean> stopFuture = Patterns.gracefulStop(leaderActor, duration, new Shutdown());

        assertNullLeaderIdChange(leaderNotifierActor);
        assertNullLeaderIdChange(follower1NotifierActor);
        assertNullLeaderIdChange(follower2NotifierActor);

        verifyRaftState(follower1Actor, RaftState.Leader);

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        testLog.info("sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1 ending");
    }

    private void sendPayloadWithFollower2Lagging() {
        testLog.info("sendPayloadWithFollower2Lagging starting");

        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        sendPayloadData(leaderActor, "zero");

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        expectFirstMatching(follower1CollectorActor, ApplyState.class);

        testLog.info("sendPayloadWithFollower2Lagging ending");
    }

    private void createRaftActors() {
        testLog.info("createRaftActors starting");

        follower1NotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(follower1Id + "-notifier"));
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id))).
                    config(newFollowerConfigParams()).roleChangeNotifier(follower1NotifierActor));

        follower2NotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(follower2Id + "-notifier"));
        follower2Actor = newTestRaftActor(follower2Id,TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString())).
                    config(newFollowerConfigParams()).roleChangeNotifier(follower2NotifierActor));

        peerAddresses = ImmutableMap.<String, String>builder().
                put(follower1Id, follower1Actor.path().toString()).
                put(follower2Id, follower2Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderNotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(leaderId + "-notifier"));
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses).
                config(leaderConfigParams).roleChangeNotifier(leaderNotifierActor));

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        waitUntilLeader(leaderActor);

        testLog.info("createRaftActors starting");
    }

    private void verifyRaftState(ActorRef raftActor, final RaftState expState) throws Throwable {
        Timeout timeout = new Timeout(500, TimeUnit.MILLISECONDS);
        Throwable lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            try {
                OnDemandRaftState raftState = (OnDemandRaftState)Await.result(ask(raftActor,
                        GetOnDemandRaftState.INSTANCE, timeout), timeout.duration());
                assertEquals("getRaftState", expState.toString(), raftState.getRaftState());
                return;
            } catch (Exception | AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    private void assertNullLeaderIdChange(TestActorRef<MessageCollectorActor> notifierActor) {
        LeaderStateChanged change = expectFirstMatching(notifierActor, LeaderStateChanged.class);
        assertNull("Expected null leader Id", change.getLeaderId());
    }
}
