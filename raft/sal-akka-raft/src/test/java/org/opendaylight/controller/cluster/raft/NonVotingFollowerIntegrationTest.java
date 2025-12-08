/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestRaftActor.Builder;
import org.opendaylight.controller.cluster.raft.SnapshotManager.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.PropertiesTermInfoStore;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorage;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFileFormat;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;

/**
 * Integration test for various scenarios involving non-voting followers.
 *
 * @author Thomas Pantelis
 */
class NonVotingFollowerIntegrationTest extends AbstractRaftActorIntegrationTest {
    private TestRaftActor followerInstance;
    private TestRaftActor leaderInstance;
    private final Builder follower1Builder = TestRaftActor.newBuilder();

    /**
     * Tests non-voting follower re-sync after the non-persistent leader restarts with an empty log. In this
     * case the follower's log will be ahead of the leader's log as the follower retains the previous
     * data in memory. The leader must force an install snapshot to re-sync the follower's state.
     */
    @Test
    void testFollowerResyncWithEmptyLeaderLogAfterNonPersistentLeaderRestart() throws Exception {
        testLog.info("testFollowerResyncWithEmptyLeaderLogAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        leaderCollector.expectMatching(ApplyState.class, 3);
        follower1Collector.expectMatching(ApplyState.class, 3);

        var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader journal lastIndex", 2, leaderLog.lastIndex());
        assertEquals("Leader commit index", 2, leaderLog.getCommitIndex());

        final var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower journal lastIndex", 2, follower1log.lastIndex());
        assertEquals("Follower commit index", 2, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Persisted journal should only contain the ServerConfigurationPayload and the original UpdateElectionTerm
        final var leaderSnapshot = leaderInstance.lastSnapshot();
        assertNotNull(leaderSnapshot);
        assertEquals(EntryInfo.of(-1, -1), leaderSnapshot.lastIncluded());
        assertNull(leaderSnapshot.source());
        final var leaderRaftSnapshot = leaderSnapshot.readRaftSnapshot(OBJECT_STREAMS);
        assertEquals(List.of(), leaderRaftSnapshot.unappliedEntries());
        assertEquals(new VotingConfig(new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false)),
            leaderRaftSnapshot.votingConfig());

        // Restart the leader

        killActor(leaderActor);
        follower1Collector.clearMessages();

        createNewLeaderActor();

        //follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        currentTerm++;
        leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader term", currentTerm, leaderContext.currentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderLog.lastIndex());
        assertEquals("Leader commit index", -1, leaderLog.getCommitIndex());

        // After restart, the leader's log and the follower's log will be ahead so the leader should force an
        // install snapshot to re-sync the follower's log and state.

        follower1Collector.expectFirstMatching(SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.currentTerm());
        assertEquals("Follower journal lastIndex", -1, follower1log.lastIndex());
        assertEquals("Follower commit index", -1, follower1log.getCommitIndex());

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));

        follower1Collector.expectFirstMatching(ApplyState.class);

        assertEquals("Follower journal lastIndex", 0, follower1log.lastIndex());
        assertEquals("Follower commit index", 0, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        testLog.info("testFollowerResyncWithEmptyLeaderLogAfterNonPersistentLeaderRestart ending");
    }

    /**
     * Tests non-voting follower re-sync after the non-persistent leader restarts and commits new log
     * entries prior to re-connecting to the follower. The leader's last index will still be less than the
     * follower's last index corresponding to the previous data retained in memory. So the follower's log
     * will be ahead of the leader's log and the leader must force an install snapshot to re-sync the
     * follower's state.
     */
    @Test
    void testFollowerResyncWithLessLeaderLogEntriesAfterNonPersistentLeaderRestart() throws Exception {
        testLog.info("testFollowerResyncWithLessLeaderLogEntriesAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        leaderCollector.expectMatching(ApplyState.class, 3);
        follower1Collector.expectMatching(ApplyState.class, 3);

        var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader journal lastIndex", 2, leaderLog.lastIndex());
        assertEquals("Leader commit index", 2, leaderLog.getCommitIndex());
        var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower journal lastIndex", 2, follower1log.lastIndex());
        assertEquals("Follower commit index", 2, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Restart the leader

        killActor(leaderActor);
        follower1Collector.clearMessages();

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);

        createNewLeaderActor();

        currentTerm++;
        leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader term", currentTerm, leaderContext.currentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderLog.lastIndex());
        assertEquals("Leader commit index", -1, leaderLog.getCommitIndex());

        // Add new log entries to the leader - one less than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));

        leaderCollector.expectMatching(ApplyState.class, 2);
        assertEquals("Leader journal lastIndex", 1, leaderLog.lastIndex());
        assertEquals("Leader commit index", 1, leaderLog.getCommitIndex());

        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        follower1Collector.expectFirstMatching(SnapshotComplete.class);

        follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower term", currentTerm, follower1Context.currentTerm());
        assertEquals("Follower journal lastIndex", 1, follower1log.lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1log.lastTerm());
        assertEquals("Follower commit index", 1, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        testLog.info("testFollowerResyncWithLessLeaderLogEntriesAfterNonPersistentLeaderRestart ending");
    }

    /**
     * Tests non-voting follower re-sync after the non-persistent leader restarts and commits new log
     * entries prior to re-connecting to the follower. The leader's last index will be 1 greater than the
     * follower's last index corresponding to the previous data retained in memory. So the follower's log
     * will be behind the leader's log but the leader's log entries will have a higher term. In this case the
     * leader should force an install snapshot to re-sync the follower's state.
     */
    @Test
    void testFollowerResyncWithOneMoreLeaderLogEntryAfterNonPersistentLeaderRestart() throws Exception {
        testLog.info("testFollowerResyncWithOneMoreLeaderLogEntryAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));

        leaderCollector.expectMatching(ApplyState.class, 2);
        follower1Collector.expectMatching(ApplyState.class, 2);

        var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader journal lastIndex", 1, leaderLog.lastIndex());
        assertEquals("Leader commit index", 1, leaderLog.getCommitIndex());
        final var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower journal lastIndex", 1, follower1log.lastIndex());
        assertEquals("Follower commit index", 1, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Restart the leader

        killActor(leaderActor);
        follower1Collector.clearMessages();

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);

        createNewLeaderActor();

        currentTerm++;
        leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader term", currentTerm, leaderContext.currentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderLog.lastIndex());
        assertEquals("Leader commit index", -1, leaderLog.getCommitIndex());

        // Add new log entries to the leader - one more than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two-1"));

        leaderCollector.expectMatching(ApplyState.class, 3);
        assertEquals("Leader journal lastIndex", 2, leaderLog.lastIndex());
        assertEquals("Leader commit index", 2, leaderLog.getCommitIndex());
        assertEquals("Leader replicatedToAllIndex", -1, leaderInstance.getCurrentBehavior().getReplicatedToAllIndex());

        // Re-enable AppendEntries to the follower. The follower's log will be out of sync and it should
        // should force the leader to install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        follower1Collector.expectFirstMatching(SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.currentTerm());
        assertEquals("Follower journal lastIndex", 2, follower1log.lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1log.lastTerm());
        assertEquals("Follower commit index", 2, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        testLog.info("testFollowerResyncWithOneMoreLeaderLogEntryAfterNonPersistentLeaderRestart ending");
    }

    /**
     * Tests non-voting follower re-sync after the non-persistent leader restarts and commits new log
     * entries prior to re-connecting to the follower. The leader's last index will be greater than the
     * follower's last index corresponding to the previous data retained in memory. So the follower's log
     * will be behind the leader's log but the leader's log entries will have a higher term. It also adds a
     * "down" peer on restart so the leader doesn't trim its log as it's trying to resync the follower.
     * Eventually the follower should force the leader to install snapshot to re-sync its state.
     */
    @Test
    void testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart() throws Exception {
        testLog.info("testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        leaderCollector.expectMatching(ApplyState.class, expSnapshotState.size());
        follower1Collector.expectMatching(ApplyState.class, expSnapshotState.size());

        long lastIndex = 2;
        var leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader journal lastIndex", lastIndex, leaderLog.lastIndex());
        assertEquals("Leader commit index", lastIndex, leaderLog.getCommitIndex());
        var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower journal lastIndex", lastIndex, follower1log.lastIndex());
        assertEquals("Follower commit index", lastIndex, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        follower1Collector.clearMessages();
        follower1Collector.expectFirstMatching(AppendEntries.class);
        assertEquals("Follower snapshot index", lastIndex - 1, follower1log.getSnapshotIndex());
        assertEquals("Follower journal size", 1, leaderLog.size());

        // Restart the leader
        final var leaderSnapshot = leaderInstance.lastSnapshot();
        assertNotNull(leaderSnapshot);

        killActor(leaderActor);
        follower1Collector.clearMessages();

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);

        // Add a "down" peer so the leader doesn't trim its log as it's trying to resync the follower. The
        // leader will keep decrementing the follower's nextIndex to try to find a matching index. Since
        // there is no matching index it will eventually hit index 0 which should cause the follower to
        // force an install snapshot upon failure to remove the conflicting indexes due to indexes 0 and 1
        // being in the prior snapshot and not the log.
        //
        // We also add another voting follower actor into the mix even though it shoildn't affect the
        // outcome.
        final var persistedServerConfig = new VotingConfig(
                new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false),
                new ServerInfo(follower2Id, true), new ServerInfo("downPeer", false));

        // Leader operates in non-persistent mode, hence serverConfig is stored in a snapshot. We store a newer
        // snapshot, which should be picked up during recovery.
        RaftStorage.saveSnapshot(leaderId, stateDir().resolve(leaderId), SnapshotFileFormat.latest(),
            CompressionType.NONE, new RaftSnapshot(persistedServerConfig), EntryInfo.of(-1, -1), null, Instant.now());
        RaftStorage.saveSnapshot(follower2Id, stateDir().resolve(leaderId), SnapshotFileFormat.latest(),
            CompressionType.NONE, new RaftSnapshot(persistedServerConfig), EntryInfo.of(-1, -1), null, Instant.now());

        DefaultConfigParamsImpl follower2ConfigParams = newFollowerConfigParams();
        follower2ConfigParams.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);
        follower2Actor = newTestRaftActor(follower2Id, TestRaftActor.newBuilder().peerAddresses(
                Map.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString()))
                    .config(follower2ConfigParams).persistent(Optional.of(Boolean.FALSE)));
        TestRaftActor follower2Instance = follower2Actor.underlyingActor();
        follower2Instance.waitForRecoveryComplete();
        follower2Collector = follower2Instance.collector();

        peerAddresses = Map.of(follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString());

        createNewLeaderActor();

        currentTerm++;
        leaderLog = leaderContext.getReplicatedLog();
        assertEquals("Leader term", currentTerm, leaderContext.currentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderLog.lastIndex());
        assertEquals("Leader commit index", -1, leaderLog.getCommitIndex());

        // Add new log entries to the leader - several more than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "three-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "four-1"));

        leaderCollector.expectMatching(ApplyState.class, expSnapshotState.size());
        follower2Collector.expectMatching(ApplyState.class, expSnapshotState.size());

        lastIndex = 4;
        assertEquals("Leader journal lastIndex", lastIndex, leaderLog.lastIndex());
        assertEquals("Leader commit index", lastIndex, leaderLog.getCommitIndex());
        assertEquals("Leader snapshot index", -1, leaderLog.getSnapshotIndex());
        assertEquals("Leader replicatedToAllIndex", -1, leaderInstance.getCurrentBehavior().getReplicatedToAllIndex());

        // Re-enable AppendEntries to the follower. The follower's log will be out of sync and it should
        // should force the leader to install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        follower1Collector.expectFirstMatching(SnapshotComplete.class);

        follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower term", currentTerm, follower1Context.currentTerm());
        assertEquals("Follower journal lastIndex", lastIndex, follower1log.lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1log.lastTerm());
        assertEquals("Follower commit index", lastIndex, follower1log.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        testLog.info("testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart ending");
    }

    @Test
    void testFollowerLeaderStateChanges() throws Exception {
        testLog.info("testFollowerLeaderStateChanges");

        MessageCollector roleChangeNotifier = MessageCollector.ofPrefix(factory, "roleChangeNotifier");
        follower1Builder.roleChangeNotifier(roleChangeNotifier.actor());

        setupLeaderAndNonVotingFollower();

        ((DefaultConfigParamsImpl) follower1Context.getConfigParams()).setElectionTimeoutFactor(2);
        ((DefaultConfigParamsImpl) follower1Context.getConfigParams()).setHeartBeatInterval(Duration.ofMillis(100));

        roleChangeNotifier.clearMessages();
        follower1Actor.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());
        followerInstance.startDropMessages(AppendEntries.class);

        LeaderStateChanged leaderStateChanged = roleChangeNotifier.expectFirstMatching(LeaderStateChanged.class);
        assertEquals("leaderId", null, leaderStateChanged.leaderId());

        roleChangeNotifier.clearMessages();
        followerInstance.stopDropMessages(AppendEntries.class);

        leaderStateChanged = roleChangeNotifier.expectFirstMatching(LeaderStateChanged.class);
        assertEquals("leaderId", leaderId, leaderStateChanged.leaderId());
    }

    private void createNewLeaderActor() {
        expSnapshotState.clear();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
            .config(leaderConfigParams).persistent(Optional.of(Boolean.FALSE)));
        leaderInstance = leaderActor.underlyingActor();
        leaderCollector = leaderInstance.collector();
        waitUntilLeader(leaderActor);
        leaderContext = leaderInstance.getRaftActorContext();
    }

    private void setupLeaderAndNonVotingFollower() throws Exception {
        snapshotBatchCount = 100;
        int persistedTerm = 1;

        // Set up a persisted ServerConfigurationPayload with the leader voting and the follower non-voting.
        final var leaderDir = stateDir().resolve(leaderId);
        final var followerDir = stateDir().resolve(follower1Id);

        // TermInfo
        final var termInfo = new TermInfo(persistedTerm, leaderId);
        new PropertiesTermInfoStore(leaderId, leaderDir).storeAndSetTerm(termInfo);
        new PropertiesTermInfoStore(follower1Id, followerDir).storeAndSetTerm(termInfo);

        final var persistedServerConfig = new VotingConfig(
                new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false));

        // Note: non-persistent, hence we use snapshot store
        RaftStorage.saveSnapshot(leaderId, leaderDir, SnapshotFileFormat.latest(), CompressionType.NONE,
            new RaftSnapshot(persistedServerConfig), EntryInfo.of(-1, -1), null, Instant.now());
        RaftStorage.saveSnapshot(follower1Id, followerDir, SnapshotFileFormat.latest(), CompressionType.NONE,
            new RaftSnapshot(persistedServerConfig), EntryInfo.of(-1, -1), null, Instant.now());

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        follower1Actor = newTestRaftActor(follower1Id, follower1Builder.peerAddresses(
                Map.of(leaderId, testActorPath(leaderId))).config(followerConfigParams)
                    .persistent(Optional.of(Boolean.FALSE)));

        peerAddresses = Map.of(follower1Id, follower1Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
            .config(leaderConfigParams).persistent(Optional.of(Boolean.FALSE)));

        followerInstance = follower1Actor.underlyingActor();
        follower1Collector = followerInstance.collector();

        leaderInstance = leaderActor.underlyingActor();
        leaderCollector = leaderInstance.collector();

        leaderContext = leaderInstance.getRaftActorContext();
        follower1Context = followerInstance.getRaftActorContext();

        waitUntilLeader(leaderActor);

        // Verify leader's context after startup

        currentTerm = persistedTerm + 1;
        assertEquals("Leader term", currentTerm, leaderContext.currentTerm());
        assertEquals("Leader server config", Set.copyOf(persistedServerConfig.serverInfo()),
                Set.copyOf(leaderContext.getPeerServerInfo(true).serverInfo()));
        assertTrue("Leader isVotingMember", leaderContext.isVotingMember());

        // Verify follower's context after startup

        follower1Collector.expectFirstMatching(AppendEntries.class);
        assertEquals("Follower term", currentTerm, follower1Context.currentTerm());
        assertEquals("Follower server config", Set.copyOf(persistedServerConfig.serverInfo()),
                Set.copyOf(follower1Context.getPeerServerInfo(true).serverInfo()));
        assertFalse("FollowerisVotingMember", follower1Context.isVotingMember());
    }
}
