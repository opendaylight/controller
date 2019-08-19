/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.behaviors.Leader;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class LastChunkTimeoutTest extends AbstractRaftActorIntegrationTest {
    private final char[] data = new char[1024];
    private final TestRaftActor.Builder follower1Builder = TestRaftActor.newBuilder();
    private TestRaftActor followerInstance;

    @Test
    public void testLongerResponseToLastChunk() {
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

        createNewLeaderActor();
        currentTerm++;

        // Drop reply to last chunk, so we can simulate long reply time.
        leaderActor.underlyingActor()
                .startDropMessages(InstallSnapshotReply.class, reply -> reply.getChunkIndex() == 4);

        // Add new log entries to the leader - one less than the prior log entries
        expSnapshotState.add(sendPayloadData(leaderActor, new String(data)));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.
        followerInstance.stopDropMessages(AppendEntries.class);

        // Wait for all 4 chunks to be sent.
        MessageCollectorActor.expectMatching(follower1CollectorActor, InstallSnapshot.class, 4);

        // Wait more than normal timeout.
        var waitTime = leaderContext.getConfigParams().getHeartBeatInterval()
                .$times(leaderContext.getConfigParams().getElectionTimeoutFactor() * 3);
        Uninterruptibles.sleepUninterruptibly(waitTime.toMillis() * 3, TimeUnit.MILLISECONDS);

        // asset still installing
        assertNotNull(((Leader) leaderContext.getCurrentBehavior()).getFollower(follower1Id).getInstallSnapshotState());

        // asset leader didn't send more
        assertEquals(4, MessageCollectorActor.getAllMatching(follower1CollectorActor, InstallSnapshot.class).size());

        // Now we send reply to last chunk.
        leaderActor.underlyingActor().stopDropMessages(InstallSnapshotReply.class);
        leaderActor.tell(new InstallSnapshotReply(currentTerm, follower1Id, 4, true), follower1Actor);

        // Give leader some time to finish and assert InstallSnapshot is finished.
        Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
        assertNull(((Leader) leaderContext.getCurrentBehavior()).getFollower(follower1Id).getInstallSnapshotState());
        testLog.info("testSnapshotStalemate ending");
    }

    @Test
    public void testLastChunkTimeout() {
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

        createNewLeaderActor();
        currentTerm++;

        // Drop last chunk, so snapshot won't finish installing and no InstallSnapshotReply is sent.
        followerInstance.startDropMessages(InstallSnapshot.class, reply -> reply.getChunkIndex() == 4);

        // Add new log entries to the leader - one less than the prior log entries
        expSnapshotState.add(sendPayloadData(leaderActor, new String(data)));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.
        followerInstance.stopDropMessages(AppendEntries.class);

        // Wait for all 4 chunks to be sent.
        var chunks = MessageCollectorActor.expectMatching(follower1CollectorActor, InstallSnapshot.class, 4);
        assertEquals(4, chunks.get(3).getChunkIndex());

        // Wait more than last chunk timeout.
        var waitTime = leaderContext.getConfigParams().getHeartBeatInterval()
                .$times(leaderContext.getConfigParams().getElectionTimeoutFactor() * 3);
        Uninterruptibles.sleepUninterruptibly(waitTime.toMillis() * 9, TimeUnit.MILLISECONDS);

        // asset still installing
        assertNotNull(((Leader) leaderContext.getCurrentBehavior()).getFollower(follower1Id).getInstallSnapshotState());

        // Leader tried to resend last chunk after timeout, but because of it being dropped no response was sent.
        var messages = MessageCollectorActor.getAllMatching(follower1CollectorActor, InstallSnapshot.class);
        assertEquals(5, messages.size());
        assertEquals(4, messages.get(4).getChunkIndex());

        // We send last chunk and let the follower answer with success.
        followerInstance.stopDropMessages(InstallSnapshot.class);
        follower1Actor.tell(chunks.get(3), leaderActor);

        // Leader successfully finish InstallSnapshot and don't sand more InstallSnapshot messages.
        MessageCollectorActor.expectMatching(leaderCollectorActor, InstallSnapshotReply.class, 4);
        assertNull(((Leader) leaderContext.getCurrentBehavior()).getFollower(follower1Id).getInstallSnapshotState());
        messages = MessageCollectorActor.getAllMatching(follower1CollectorActor, InstallSnapshot.class);
        // One more because of the one we sent.
        assertEquals(6, messages.size());
        testLog.info("testSnapshotStalemate ending");
    }

    @Test
    public void testLastChunkTimeout2() {
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

        createNewLeaderActor();
        currentTerm++;

        // want to control InstallSnapshotReply
        leaderActor.underlyingActor()
                .startDropMessages(InstallSnapshotReply.class, reply -> reply.getChunkIndex() == 4);

        // Add new log entries to the leader - one less than the prior log entries
        expSnapshotState.add(sendPayloadData(leaderActor, new String(data)));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.
        followerInstance.stopDropMessages(AppendEntries.class);

        // Wait for all 4 chunks to be sent.
        var chunks = MessageCollectorActor.expectMatching(follower1CollectorActor, InstallSnapshot.class, 4);
        assertEquals(4, chunks.get(3).getChunkIndex());
        // Wait for last reply to be dropped and let the process finish naturally.
        MessageCollectorActor.expectMatching(leaderCollectorActor, InstallSnapshotReply.class, 4);
        leaderActor.underlyingActor().stopDropMessages(InstallSnapshotReply.class);

        // wait more than normal timeout
        var waitTime = leaderContext.getConfigParams().getHeartBeatInterval()
                .$times(leaderContext.getConfigParams().getElectionTimeoutFactor() * 3);
        Uninterruptibles.sleepUninterruptibly(waitTime.toMillis() * 9, TimeUnit.MILLISECONDS);

        // assert install finished
        assertNull(((Leader) leaderContext.getCurrentBehavior()).getFollower(follower1Id).getInstallSnapshotState());

        // Leader sent last chunk again after timeout but follower already finished snapshot install
        // Follower answers failure to resent last chunk
        // Leader initiate snapshot form the start and successfully finish since it can now receive InstallSnapshotReply
        // of last chunk.
        var messages = MessageCollectorActor.getAllMatching(follower1CollectorActor, InstallSnapshot.class);
        assertEquals(9, messages.size());
        assertEquals(4, messages.get(4).getChunkIndex());
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
