/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor.expectMatching;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Status;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.LeaderTransitioning;
import org.opendaylight.controller.cluster.raft.client.messages.Shutdown;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.messages.RequestLeadership;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.raft.api.RaftRole;
import org.opendaylight.raft.api.TermInfo;
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
    private ActorRef leaderNotifierActor;
    private ActorRef follower1NotifierActor;
    private ActorRef follower2NotifierActor;
    private ActorRef follower3NotifierActor;
    private TestActorRef<TestRaftActor> follower3Actor;
    private ActorRef follower3CollectorActor;
    private ActorRef requestLeadershipResultCollectorActor;

    @Test
    public void testLeaderTransferOnShutDown() throws Exception {
        testLog.info("testLeaderTransferOnShutDown starting");

        createRaftActors();

        sendPayloadWithFollower2Lagging();

        sendShutDownToLeaderAndVerifyLeadershipTransferToFollower1();

        sendShutDown(follower2Actor);

        testLog.info("testLeaderTransferOnShutDown ending");
    }

    private void sendShutDown(final ActorRef actor) throws Exception {
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

        verifyRaftState(follower1Actor, RaftRole.Leader);

        Boolean stopped = Await.result(stopFuture, duration);
        assertEquals("Stopped", Boolean.TRUE, stopped);

        // Re-enable LeaderTransitioning messages to follower2.
        final LeaderTransitioning leaderTransitioning = expectFirstMatching(follower2CollectorActor,
                LeaderTransitioning.class);
        follower2Instance.stopDropMessages(LeaderTransitioning.class);

        follower2Instance.stopDropMessages(AppendEntries.class);
        ApplyState applyState = expectFirstMatching(follower2CollectorActor, ApplyState.class);
        assertEquals("Apply sate index", 0, applyState.getReplicatedLogEntry().index());

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

        final Snapshot snapshot = Snapshot.create(EmptyState.INSTANCE, List.of(), -1, -1, -1, -1,
            new TermInfo(1), new ClusterConfig(
                new ServerInfo(leaderId, true), new ServerInfo(follower1Id, true),
                new ServerInfo(follower2Id, true), new ServerInfo(follower3Id, false)));

        InMemorySnapshotStore.addSnapshot(leaderId, snapshot);
        InMemorySnapshotStore.addSnapshot(follower1Id, snapshot);
        InMemorySnapshotStore.addSnapshot(follower2Id, snapshot);
        InMemorySnapshotStore.addSnapshot(follower3Id, snapshot);

        follower1NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower1Id + "-notifier"));
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                Map.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id),
                        follower3Id, testActorPath(follower3Id)))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower1NotifierActor));

        follower2NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower2Id + "-notifier"));
        follower2Actor = newTestRaftActor(follower2Id,TestRaftActor.newBuilder().peerAddresses(
                Map.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString(),
                        follower3Id, testActorPath(follower3Id)))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower2NotifierActor));

        follower3NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower3Id + "-notifier"));
        follower3Actor = newTestRaftActor(follower3Id,TestRaftActor.newBuilder().peerAddresses(
                Map.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString(),
                        follower2Id, follower2Actor.path().toString()))
                .config(newFollowerConfigParams()).roleChangeNotifier(follower3NotifierActor));

        peerAddresses = Map.of(
                follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString(),
                follower3Id, follower3Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderConfigParams.setElectionTimeoutFactor(3);
        leaderNotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(leaderId + "-notifier"));
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
                .config(leaderConfigParams).roleChangeNotifier(leaderNotifierActor));

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        follower3CollectorActor = follower3Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();

        waitUntilLeader(leaderActor);

        testLog.info("createRaftActors starting");
    }

    private static void verifyRaftState(final ActorRef raftActor, final RaftRole expState) {
        verifyRaftState(raftActor, rs -> assertEquals(expState, rs.getRaftState()));
    }

    private static void verifyLeaderStateChangedMessages(final ActorRef notifierActor,
            final String... expLeaderIds) {
        final var leaderStateChanges = expectMatching(notifierActor, LeaderStateChanged.class, expLeaderIds.length);

        Collections.reverse(leaderStateChanges);
        final var actual = leaderStateChanges.iterator();
        for (int i = expLeaderIds.length - 1; i >= 0; i--) {
            assertEquals("getLeaderId", expLeaderIds[i], actual.next().leaderId());
        }
    }

    @Test
    public void testLeaderTransferAborted() throws Exception {
        testLog.info("testLeaderTransferAborted starting");

        createRaftActors();

        leaderActor.underlyingActor().startDropMessages(AppendEntriesReply.class);

        sendShutDown(leaderActor);

        verifyRaftState(follower1Actor, RaftRole.Follower);
        verifyRaftState(follower2Actor, RaftRole.Follower);
        verifyRaftState(follower3Actor, RaftRole.Follower);

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

    private void sendFollower2RequestLeadershipTransferToLeader() {
        testLog.info("sendFollower2RequestLeadershipTransferToLeader starting");

        leaderActor.tell(
                new RequestLeadership(follower2Id, requestLeadershipResultCollectorActor), ActorRef.noSender());

        testLog.info("sendFollower2RequestLeadershipTransferToLeader ending");
    }

    private void createRequestLeadershipResultCollectorActor() {
        testLog.info("createRequestLeadershipResultCollectorActor starting");

        requestLeadershipResultCollectorActor = factory.createActor(MessageCollectorActor.props());

        testLog.info("createRequestLeadershipResultCollectorActor ending");
    }

    @Test
    public void testSuccessfulRequestLeadershipTransferToFollower2() {
        testLog.info("testSuccessfulRequestLeadershipTransferToFollower2 starting");

        createRaftActors();
        createRequestLeadershipResultCollectorActor();

        sendFollower2RequestLeadershipTransferToLeader();

        verifyRaftState(follower2Actor, RaftRole.Leader);

        expectMatching(requestLeadershipResultCollectorActor, Status.Success.class, 1);

        testLog.info("testSuccessfulRequestLeadershipTransferToFollower2 ending");
    }

    @Test
    public void testRequestLeadershipTransferToFollower2WithFollower2Lagging() {
        testLog.info("testRequestLeadershipTransferToFollower2WithFollower2Lagging starting");

        createRaftActors();
        createRequestLeadershipResultCollectorActor();

        sendPayloadWithFollower2Lagging();

        sendFollower2RequestLeadershipTransferToLeader();

        verifyRaftState(follower1Actor, RaftRole.Follower);
        verifyRaftState(follower2Actor, RaftRole.Follower);
        verifyRaftState(follower3Actor, RaftRole.Follower);

        Status.Failure failure = expectFirstMatching(requestLeadershipResultCollectorActor, Status.Failure.class);
        assertTrue(failure.cause() instanceof LeadershipTransferFailedException);

        testLog.info("testRequestLeadershipTransferToFollower2WithFollower2Lagging ending");
    }


    @Test
    public void testRequestLeadershipTransferToFollower2WithFollower2Shutdown() throws Exception {
        testLog.info("testRequestLeadershipTransferToFollower2WithFollower2Shutdown starting");

        createRaftActors();
        createRequestLeadershipResultCollectorActor();

        sendShutDown(follower2Actor);

        sendFollower2RequestLeadershipTransferToLeader();

        verifyRaftState(follower1Actor, RaftRole.Follower);
        verifyRaftState(follower3Actor, RaftRole.Follower);

        Status.Failure failure = expectFirstMatching(requestLeadershipResultCollectorActor, Status.Failure.class);
        assertTrue(failure.cause() instanceof LeadershipTransferFailedException);

        testLog.info("testRequestLeadershipTransferToFollower2WithFollower2Shutdown ending");
    }

    @Test
    public void testRequestLeadershipTransferToFollower2WithOtherFollowersDown() {
        testLog.info("testRequestLeadershipTransferToFollower2WithOtherFollowersDown starting");

        createRaftActors();
        createRequestLeadershipResultCollectorActor();

        factory.killActor(follower1Actor, new TestKit(getSystem()));
        factory.killActor(follower3Actor, new TestKit(getSystem()));

        sendFollower2RequestLeadershipTransferToLeader();

        expectFirstMatching(requestLeadershipResultCollectorActor, Status.Success.class);

        verifyRaftState(follower2Actor, RaftRole.Leader);
        verifyRaftState(leaderActor, RaftRole.Follower);

        testLog.info("testRequestLeadershipTransferToFollower2WithOtherFollowersDown ending");
    }
}
