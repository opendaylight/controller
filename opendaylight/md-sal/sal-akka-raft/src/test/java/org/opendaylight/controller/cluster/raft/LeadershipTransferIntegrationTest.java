/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
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

    private final String follower3Id = factory.generateActorId("follower");
    private TestActorRef<MessageCollectorActor> leaderNotifierActor;
    private TestActorRef<MessageCollectorActor> follower1NotifierActor;
    private TestActorRef<MessageCollectorActor> follower2NotifierActor;
    private TestActorRef<MessageCollectorActor> follower3NotifierActor;
    private TestActorRef<TestRaftActor> follower3Actor;
    private ActorRef follower3CollectorActor;

    @Test
    public void testLeaderTransferOnShutDown() throws Exception {
        testLog.info("testLeaderTransferOnShutDown starting");

        createRaftActors();

        sendPayloadWithFollower2Lagging();

        sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1();

        sendShutDown(follower2Actor);

        testLog.info("testLeaderTransferOnShutDown ending");
    }

    private void sendShutDown(ActorRef actor) throws Exception {
        testLog.info("sendShutDown for {} starting", actor.path());

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        Future<Boolean> stopFuture = Patterns.gracefulStop(actor, duration, Shutdown.INSTANCE);

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        testLog.info("sendShutDown for {} ending", actor.path());
    }

    private void sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1() throws Exception {
        testLog.info("sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1 starting");

        clearMessages(leaderNotifierActor);
        clearMessages(follower1NotifierActor);
        clearMessages(follower2NotifierActor);
        clearMessages(follower3NotifierActor);

        // Simulate a delay for follower2 in receiving the LeaderTransitioning message with null leader id.
        final TestRaftActor follower2Instance = follower2Actor.underlyingActor();
        follower2Instance.startDropMessages(LeaderTransitioning.class);

        FiniteDuration duration = FiniteDuration.create(5, TimeUnit.SECONDS);
        final Future<Boolean> stopFuture = Patterns.gracefulStop(leaderActor, duration, Shutdown.INSTANCE);

        verifyRaftState(follower1Actor, RaftState.Leader);

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        // Re-enable LeaderTransitioning messages to follower2.
        final LeaderTransitioning leaderTransitioning = expectFirstMatching(follower2CollectorActor,
                LeaderTransitioning.class);
        follower2Instance.stopDropMessages(LeaderTransitioning.class);

        follower2Instance.stopDropMessages(AppendEntries.class);
        ApplyState applyState = expectFirstMatching(follower2CollectorActor, ApplyState.class);
        assertEquals("Apply sate index", 0, applyState.getReplicatedLogEntry().getIndex());

        // Now send the LeaderTransitioning to follower2 after it has received AppendEntries from the new leader.
        follower2Actor.tell(leaderTransitioning, ActorRef.noSender());

        verifyLeaderStateChangedMessages(leaderNotifierActor, null, follower1Id);
        verifyLeaderStateChangedMessages(follower1NotifierActor, null, follower1Id);
        // follower2 should only get 1 LeaderStateChanged with the new leaderId - the LeaderTransitioning message
        // should not generate a LeaderStateChanged with null leaderId since it arrived after the new leaderId was set.
        verifyLeaderStateChangedMessages(follower2NotifierActor, follower1Id);
        verifyLeaderStateChangedMessages(follower3NotifierActor, null, follower1Id);

        testLog.info("sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1 ending");
    }

    private void sendPayloadWithFollower2Lagging() {
        testLog.info("sendPayloadWithFollower2Lagging starting");

        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        sendPayloadData(leaderActor, "zero");

        expectFirstMatching(leaderCollectorActor, ApplyState.class);
        expectFirstMatching(follower1CollectorActor, ApplyState.class);
        expectFirstMatching(follower3CollectorActor, ApplyState.class);

        testLog.info("sendPayloadWithFollower2Lagging ending");
    }

    private void createRaftActors() {
        testLog.info("createRaftActors starting");

        follower1NotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(follower1Id + "-notifier"));
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id),
                        follower3Id, testActorPath(follower3Id)))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower1NotifierActor));

        follower2NotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(follower2Id + "-notifier"));
        follower2Actor = newTestRaftActor(follower2Id,TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString(),
                        follower3Id, testActorPath(follower3Id)))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower2NotifierActor));

        follower3NotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(follower3Id + "-notifier"));
        follower3Actor = newTestRaftActor(follower3Id,TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString(),
                        follower2Id, follower2Actor.path().toString()))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower3NotifierActor));

        peerAddresses = ImmutableMap.<String, String>builder()
                .put(follower1Id, follower1Actor.path().toString())
                .put(follower2Id, follower2Actor.path().toString())
                .put(follower3Id, follower3Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderConfigParams.setElectionTimeoutFactor(3);
        leaderNotifierActor = factory.createTestActor(Props.create(MessageCollectorActor.class),
                factory.generateActorId(leaderId + "-notifier"));
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
                .config(leaderConfigParams).roleChangeNotifier(leaderNotifierActor));

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        follower3CollectorActor = follower3Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
        leaderContext.getPeerInfo(follower3Id).setVotingState(VotingState.NON_VOTING);

        waitUntilLeader(leaderActor);

        testLog.info("createRaftActors starting");
    }

    private static void verifyRaftState(ActorRef raftActor, final RaftState expState) {
        verifyRaftState(raftActor, rs -> assertEquals("getRaftState", expState.toString(), rs.getRaftState()));
    }

    private void verifyLeaderStateChangedMessages(TestActorRef<MessageCollectorActor> notifierActor,
            String... expLeaderIds) {
        List<LeaderStateChanged> leaderStateChanges = expectMatching(notifierActor, LeaderStateChanged.class,
                expLeaderIds.length);

        Collections.reverse(leaderStateChanges);
        Iterator<LeaderStateChanged> actual = leaderStateChanges.iterator();
        for (int i = expLeaderIds.length - 1; i >= 0; i--) {
            assertEquals("getLeaderId", expLeaderIds[i], actual.next().getLeaderId());
        }
    }

    @Test
    public void testLeaderTransferAborted() throws Exception {
        testLog.info("testLeaderTransferAborted starting");

        createRaftActors();

        leaderActor.underlyingActor().startDropMessages(AppendEntriesReply.class);

        sendShutDown(leaderActor);

        verifyRaftState(follower1Actor, RaftState.Follower);
        verifyRaftState(follower2Actor, RaftState.Follower);
        verifyRaftState(follower3Actor, RaftState.Follower);

        testLog.info("testLeaderTransferOnShutDown ending");
    }

    @Test
    public void testLeaderTransferSkippedOnShutdownWithNoFollowers() throws Exception {
        testLog.info("testLeaderTransferSkippedOnShutdownWithNoFollowers starting");

        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().config(newLeaderConfigParams()));
        waitUntilLeader(leaderActor);

        sendShutDown(leaderActor);

        testLog.info("testLeaderTransferSkippedOnShutdownWithNoFollowers ending");
    }
}
