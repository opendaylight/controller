/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotFinished;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class SnapshotInstallStalemateTest extends AbstractRaftActorIntegrationTest {

    private final TestRaftActor.Builder follower1Builder = TestRaftActor.newBuilder();
    private TestRaftActor followerInstance;

    /**
     * Simulate the situation leading to bug CONTROLLER-2067, when the Leader tries to install Snapshot on Follower,
     * while the Follower captures and begins to persist his own Snapshot. The Leader can transfer all chunks without
     * any issue, however the Follower can't apply this Snapshot, since he is persisting his own at this time. In this
     * case the Follower needs to reply with failed InstallSnapshotFinished message, which prompts the Leader to
     * reinstall his Snapshot again.
     */
    @Test
    public void testSnapshotStalemate() {
        testLog.info("testSnapshotStalemate starting");

        setupLeaderAndOneNonVotingFollower();

        MessageCollectorActor.clearMessages(follower1Actor);

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        killActor(leaderActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);
        // dropping CaptureSnapshotReply means the Follower can't proceed from CAPTURING state
        followerInstance.startDropMessages(CaptureSnapshotReply.class);
        // set Follower to CAPTURING state
        followerInstance.getRaftActorContext().getSnapshotManager()
            .capture(new SimpleReplicatedLogEntry(2, 2,
                new MockRaftActorContext.MockPayload("two")), 2);

        createNewLeaderActor();

        currentTerm++;

        // Add new log entries to the leader - one less than the prior log entries
        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));

        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);

        final InstallSnapshotFinished installSnapshotFinishedDuringCapturingState =
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, InstallSnapshotFinished.class);
        // verify the InstallSnapshotFinished failed because of the CAPTURING state
        assertFalse(installSnapshotFinishedDuringCapturingState.isSuccess());

        // allow Follower to proceed from CAPTURING state
        followerInstance.stopDropMessages(CaptureSnapshotReply.class);
        // finish CAPTURING state
        followerInstance.getRaftActorSnapshotCohort().createSnapshot(follower1Actor, Optional.empty());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        // Leader should be able to install the Snapshot now, since the Follower finished capturing and persisting his
        // own snapshot and his SnapshotManager is back in IDLE state.
        final InstallSnapshotFinished installSnapshotFinishedAfterReset =
            MessageCollectorActor.expectFirstMatching(leaderCollectorActor, InstallSnapshotFinished.class,
                (finished) -> finished.isSuccess());
        assertTrue(installSnapshotFinishedAfterReset.isSuccess());

        testLog.info("testSnapshotStalemate ending");
    }

    private void createNewLeaderActor() {
        expSnapshotState.clear();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
            .config(leaderConfigParams).persistent(Optional.of(false)));
        TestRaftActor leaderInstance = leaderActor.underlyingActor();
        leaderCollectorActor = leaderInstance.collectorActor();
        waitUntilLeader(leaderActor);
        leaderContext = leaderInstance.getRaftActorContext();
    }

    private void setupLeaderAndOneNonVotingFollower() {
        snapshotBatchCount = 100;
        int persistedTerm = 1;

        // Set up a persisted ServerConfigurationPayload with the leader voting and the follower non-voting.

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(Arrays.asList(
            new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false)));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, persistedTerm,
            persistedServerConfig);

        InMemoryJournal.addEntry(leaderId, 1, new UpdateElectionTerm(persistedTerm, leaderId));
        InMemoryJournal.addEntry(leaderId, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(follower1Id, 1, new UpdateElectionTerm(persistedTerm, leaderId));
        InMemoryJournal.addEntry(follower1Id, 2, persistedServerConfigEntry);

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        follower1Actor = newTestRaftActor(follower1Id, follower1Builder.peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId))).config(followerConfigParams)
            .persistent(Optional.of(false)));

        peerAddresses = ImmutableMap.<String, String>builder()
            .put(follower1Id, follower1Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
            .config(leaderConfigParams).persistent(Optional.of(false)));

        followerInstance = follower1Actor.underlyingActor();
        follower1CollectorActor = followerInstance.collectorActor();

        TestRaftActor leaderInstance = leaderActor.underlyingActor();
        leaderCollectorActor = leaderInstance.collectorActor();

        leaderContext = leaderInstance.getRaftActorContext();
        follower1Context = followerInstance.getRaftActorContext();

        waitUntilLeader(leaderActor);

        // Verify leader's context after startup

        currentTerm = persistedTerm + 1;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader server config", Sets.newHashSet(persistedServerConfig.getServerConfig()),
            Sets.newHashSet(leaderContext.getPeerServerInfo(true).getServerConfig()));
        assertEquals("Leader isVotingMember", true, leaderContext.isVotingMember());

        // Verify follower's context after startup

        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower server config", Sets.newHashSet(persistedServerConfig.getServerConfig()),
            Sets.newHashSet(follower1Context.getPeerServerInfo(true).getServerConfig()));
        assertEquals("FollowerisVotingMember", false, follower1Context.isVotingMember());
    }
}
