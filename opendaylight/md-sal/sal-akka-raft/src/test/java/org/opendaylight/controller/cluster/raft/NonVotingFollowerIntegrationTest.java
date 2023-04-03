/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;

import akka.actor.ActorRef;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.raft.AbstractRaftActorIntegrationTest.TestRaftActor.Builder;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.duration.FiniteDuration;

/**
 * Integration test for various scenarios involving non-voting followers.
 *
 * @author Thomas Pantelis
 */
public class NonVotingFollowerIntegrationTest extends AbstractRaftActorIntegrationTest {
    private TestRaftActor followerInstance;
    private TestRaftActor leaderInstance;
    private final Builder follower1Builder = TestRaftActor.newBuilder();

    /**
     * Tests non-voting follower re-sync after the non-persistent leader restarts with an empty log. In this
     * case the follower's log will be ahead of the leader's log as the follower retains the previous
     * data in memory. The leader must force an install snapshot to re-sync the follower's state.
     */
    @Test
    public void testFollowerResyncWithEmptyLeaderLogAfterNonPersistentLeaderRestart() {
        testLog.info("testFollowerResyncWithEmptyLeaderLogAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);

        assertEquals("Leader journal lastIndex", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Follower journal lastIndex", 2, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", 2, follower1Context.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Persisted journal should only contain the ServerConfigurationPayload and 2 UpdateElectionTerm entries.
        assertEquals("Leader persisted journal size", 3, InMemoryJournal.get(leaderId).size());

        // Restart the leader

        killActor(leaderActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        createNewLeaderActor();

        //follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        currentTerm++;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", -1, leaderContext.getCommitIndex());

        // After restart, the leader's log and the follower's log will be ahead so the leader should force an
        // install snapshot to re-sync the follower's log and state.

        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower journal lastIndex", -1, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", -1, follower1Context.getCommitIndex());

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));

        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, ApplyState.class);

        assertEquals("Follower journal lastIndex", 0, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", 0, follower1Context.getCommitIndex());
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
    public void testFollowerResyncWithLessLeaderLogEntriesAfterNonPersistentLeaderRestart() {
        testLog.info("testFollowerResyncWithLessLeaderLogEntriesAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 3);

        assertEquals("Leader journal lastIndex", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Follower journal lastIndex", 2, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", 2, follower1Context.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Restart the leader

        killActor(leaderActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);

        createNewLeaderActor();

        currentTerm++;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", -1, leaderContext.getCommitIndex());

        // Add new log entries to the leader - one less than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 2);
        assertEquals("Leader journal lastIndex", 1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderContext.getCommitIndex());

        // Re-enable AppendEntries to the follower. The leaders previous index will be present in the
        // follower's but the terms won't match and the follower's log will be ahead of the leader's log
        // The leader should force an install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower journal lastIndex", 1, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower commit index", 1, follower1Context.getCommitIndex());
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
    public void testFollowerResyncWithOneMoreLeaderLogEntryAfterNonPersistentLeaderRestart() {
        testLog.info("testFollowerResyncWithOneMoreLeaderLogEntryAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 2);
        MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, 2);

        assertEquals("Leader journal lastIndex", 1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 1, leaderContext.getCommitIndex());
        assertEquals("Follower journal lastIndex", 1, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", 1, follower1Context.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        // Restart the leader

        killActor(leaderActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

        // Temporarily drop AppendEntries to simulate a disconnect when the leader restarts.
        followerInstance.startDropMessages(AppendEntries.class);

        createNewLeaderActor();

        currentTerm++;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", -1, leaderContext.getCommitIndex());

        // Add new log entries to the leader - one more than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two-1"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, 3);
        assertEquals("Leader journal lastIndex", 2, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Leader replicatedToAllIndex", -1, leaderInstance.getCurrentBehavior().getReplicatedToAllIndex());

        // Re-enable AppendEntries to the follower. The follower's log will be out of sync and it should
        // should force the leader to install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower journal lastIndex", 2, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower commit index", 2, follower1Context.getCommitIndex());
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
    public void testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart() {
        testLog.info("testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart starting");

        setupLeaderAndNonVotingFollower();

        // Add log entries and verify they are committed and applied by both nodes.

        expSnapshotState.add(sendPayloadData(leaderActor, "zero"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, expSnapshotState.size());
        MessageCollectorActor.expectMatching(follower1CollectorActor, ApplyState.class, expSnapshotState.size());

        long lastIndex = 2;
        assertEquals("Leader journal lastIndex", lastIndex, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", lastIndex, leaderContext.getCommitIndex());
        assertEquals("Follower journal lastIndex", lastIndex, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower commit index", lastIndex, follower1Context.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        MessageCollectorActor.clearMessages(follower1CollectorActor);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("Follower snapshot index", lastIndex - 1, follower1Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower journal size", 1, leaderContext.getReplicatedLog().size());

        // Restart the leader

        killActor(leaderActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);

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
        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(List.of(
                new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false),
                new ServerInfo(follower2Id, true), new ServerInfo("downPeer", false)));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, currentTerm,
                persistedServerConfig);

        InMemoryJournal.clear();
        InMemoryJournal.addEntry(leaderId, 1, new UpdateElectionTerm(currentTerm, leaderId));
        InMemoryJournal.addEntry(leaderId, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(follower2Id, 1, persistedServerConfigEntry);

        DefaultConfigParamsImpl follower2ConfigParams = newFollowerConfigParams();
        follower2ConfigParams.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());
        follower2Actor = newTestRaftActor(follower2Id, TestRaftActor.newBuilder().peerAddresses(
                Map.of(leaderId, testActorPath(leaderId), follower1Id, follower1Actor.path().toString()))
                    .config(follower2ConfigParams).persistent(Optional.of(false)));
        TestRaftActor follower2Instance = follower2Actor.underlyingActor();
        follower2Instance.waitForRecoveryComplete();
        follower2CollectorActor = follower2Instance.collectorActor();

        peerAddresses = Map.of(follower1Id, follower1Actor.path().toString(),
                follower2Id, follower2Actor.path().toString());

        createNewLeaderActor();

        currentTerm++;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader journal lastIndex", -1, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", -1, leaderContext.getCommitIndex());

        // Add new log entries to the leader - several more than the prior log entries

        expSnapshotState.add(sendPayloadData(leaderActor, "zero-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "one-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "two-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "three-1"));
        expSnapshotState.add(sendPayloadData(leaderActor, "four-1"));

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyState.class, expSnapshotState.size());
        MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyState.class, expSnapshotState.size());

        lastIndex = 4;
        assertEquals("Leader journal lastIndex", lastIndex, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", lastIndex, leaderContext.getCommitIndex());
        assertEquals("Leader snapshot index", -1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader replicatedToAllIndex", -1, leaderInstance.getCurrentBehavior().getReplicatedToAllIndex());

        // Re-enable AppendEntries to the follower. The follower's log will be out of sync and it should
        // should force the leader to install snapshot to re-sync the entire follower's log and state.

        followerInstance.stopDropMessages(AppendEntries.class);
        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, SnapshotComplete.class);

        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower journal lastIndex", lastIndex, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower journal lastTerm", currentTerm, follower1Context.getReplicatedLog().lastTerm());
        assertEquals("Follower commit index", lastIndex, follower1Context.getCommitIndex());
        assertEquals("Follower applied state", expSnapshotState, followerInstance.getState());

        testLog.info("testFollowerResyncWithMoreLeaderLogEntriesAndDownPeerAfterNonPersistentLeaderRestart ending");
    }

    @Test
    public void testFollowerLeaderStateChanges() {
        testLog.info("testFollowerLeaderStateChanges");

        ActorRef roleChangeNotifier = factory.createActor(
                MessageCollectorActor.props(), factory.generateActorId("roleChangeNotifier"));
        follower1Builder.roleChangeNotifier(roleChangeNotifier);

        setupLeaderAndNonVotingFollower();

        ((DefaultConfigParamsImpl)follower1Context.getConfigParams()).setElectionTimeoutFactor(2);
        ((DefaultConfigParamsImpl)follower1Context.getConfigParams())
                .setHeartBeatInterval(FiniteDuration.apply(100, TimeUnit.MILLISECONDS));

        MessageCollectorActor.clearMessages(roleChangeNotifier);
        follower1Actor.tell(ElectionTimeout.INSTANCE, ActorRef.noSender());
        followerInstance.startDropMessages(AppendEntries.class);

        LeaderStateChanged leaderStateChanged = MessageCollectorActor.expectFirstMatching(roleChangeNotifier,
                LeaderStateChanged.class);
        assertEquals("getLeaderId", null, leaderStateChanged.getLeaderId());

        MessageCollectorActor.clearMessages(roleChangeNotifier);
        followerInstance.stopDropMessages(AppendEntries.class);

        leaderStateChanged = MessageCollectorActor.expectFirstMatching(roleChangeNotifier,
                LeaderStateChanged.class);
        assertEquals("getLeaderId", leaderId, leaderStateChanged.getLeaderId());
    }

    private void createNewLeaderActor() {
        expSnapshotState.clear();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
                .config(leaderConfigParams).persistent(Optional.of(false)));
        leaderInstance = leaderActor.underlyingActor();
        leaderCollectorActor = leaderInstance.collectorActor();
        waitUntilLeader(leaderActor);
        leaderContext = leaderInstance.getRaftActorContext();
    }

    private void setupLeaderAndNonVotingFollower() {
        snapshotBatchCount = 100;
        int persistedTerm = 1;

        // Set up a persisted ServerConfigurationPayload with the leader voting and the follower non-voting.

        ServerConfigurationPayload persistedServerConfig = new ServerConfigurationPayload(List.of(
                new ServerInfo(leaderId, true), new ServerInfo(follower1Id, false)));
        SimpleReplicatedLogEntry persistedServerConfigEntry = new SimpleReplicatedLogEntry(0, persistedTerm,
                persistedServerConfig);

        InMemoryJournal.addEntry(leaderId, 1, new UpdateElectionTerm(persistedTerm, leaderId));
        InMemoryJournal.addEntry(leaderId, 2, persistedServerConfigEntry);
        InMemoryJournal.addEntry(follower1Id, 1, new UpdateElectionTerm(persistedTerm, leaderId));
        InMemoryJournal.addEntry(follower1Id, 2, persistedServerConfigEntry);

        DefaultConfigParamsImpl followerConfigParams = newFollowerConfigParams();
        follower1Actor = newTestRaftActor(follower1Id, follower1Builder.peerAddresses(
                Map.of(leaderId, testActorPath(leaderId))).config(followerConfigParams)
                    .persistent(Optional.of(false)));

        peerAddresses = Map.of(follower1Id, follower1Actor.path().toString());

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, TestRaftActor.newBuilder().peerAddresses(peerAddresses)
                .config(leaderConfigParams).persistent(Optional.of(false)));

        followerInstance = follower1Actor.underlyingActor();
        follower1CollectorActor = followerInstance.collectorActor();

        leaderInstance = leaderActor.underlyingActor();
        leaderCollectorActor = leaderInstance.collectorActor();

        leaderContext = leaderInstance.getRaftActorContext();
        follower1Context = followerInstance.getRaftActorContext();

        waitUntilLeader(leaderActor);

        // Verify leader's context after startup

        currentTerm = persistedTerm + 1;
        assertEquals("Leader term", currentTerm, leaderContext.getTermInformation().getCurrentTerm());
        assertEquals("Leader server config", Set.copyOf(persistedServerConfig.getServerConfig()),
                Set.copyOf(leaderContext.getPeerServerInfo(true).getServerConfig()));
        assertEquals("Leader isVotingMember", true, leaderContext.isVotingMember());

        // Verify follower's context after startup

        MessageCollectorActor.expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("Follower term", currentTerm, follower1Context.getTermInformation().getCurrentTerm());
        assertEquals("Follower server config", Set.copyOf(persistedServerConfig.getServerConfig()),
                Set.copyOf(follower1Context.getPeerServerInfo(true).getServerConfig()));
        assertEquals("FollowerisVotingMember", false, follower1Context.isVotingMember());
    }
}
