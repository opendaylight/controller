/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.clearMessages;
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.expectFirstMatching;
import static org.opendaylight.controller.cluster.raft.MessageCollectorActor.expectMatching;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.List;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.opendaylight.raft.api.RaftRole;

/**
 * Tests PreLeader raft state functionality end-to-end.
 *
 * @author Thomas Pantelis
 */
public class PreLeaderScenarioTest extends AbstractRaftActorIntegrationTest {

    private ActorRef follower1NotifierActor;
    private DefaultConfigParamsImpl followerConfigParams;

    @Test
    public void testUnComittedEntryOnLeaderChange() {
        testLog.info("testUnComittedEntryOnLeaderChange starting");

        createRaftActors();

        // Drop AppendEntriesReply to the leader so it doesn't commit the payload entry.
        leaderActor.underlyingActor().startDropMessages(AppendEntriesReply.class);
        follower2Actor.underlyingActor().startDropMessages(AppendEntries.class);

        // Send a payload and verify AppendEntries is received in follower1.
        MockCommand payload0 = sendPayloadData(leaderActor, "zero");

        AppendEntries appendEntries = expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("AppendEntries - # entries", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 0, payload0);

        // Kill the leader actor.
        killActor(leaderActor);

        // At this point, the payload entry is in follower1's log but is uncommitted. follower2 has not
        // received the payload entry yet.
        final var follower1Log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal log size", 1, follower1Log.size());
        assertEquals("Follower 1 journal last index", 0, follower1Log.lastIndex());
        assertEquals("Follower 1 commit index", -1, follower1Log.getCommitIndex());
        assertEquals("Follower 1 last applied index", -1, follower1Log.getLastApplied());

        assertEquals("Follower 2 journal log size", 0, follower2Context.getReplicatedLog().size());

        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        clearMessages(follower1NotifierActor);

        // Force follower1 to start an election. It should win since it's journal is more up-to-date than
        // follower2's journal.
        follower1Actor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        // Verify the expected raft state changes. It should go to PreLeader since it has an uncommitted entry.
        List<RoleChanged> roleChange = expectMatching(follower1NotifierActor, RoleChanged.class, 3);
        assertEquals("Role change 1", RaftRole.Candidate, roleChange.get(0).newRole());
        assertEquals("Role change 2", RaftRole.PreLeader, roleChange.get(1).newRole());
        assertEquals("Role change 3", RaftRole.Leader, roleChange.get(2).newRole());

        final long previousTerm = currentTerm;
        currentTerm = follower1Context.currentTerm();

        // Since it went to Leader, it should've appended and successfully replicated a NoopPaylod with the
        // new term to follower2 and committed both entries, including the first payload from the previous term.
        var follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal log size", 2, follower1log.size());
        assertEquals("Follower 1 journal last index", 1, follower1log.lastIndex());
        assertEquals("Follower 1 commit index", 1, follower1log.getCommitIndex());
        verifyReplicatedLogEntry(follower1log.get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower1log.get(1), currentTerm, 1, NoopPayload.INSTANCE);

        // Both entries should be applied to the state.
        expectMatching(follower1CollectorActor, ApplyState.class, 2);
        expectMatching(follower2CollectorActor, ApplyState.class, 2);

        assertEquals("Follower 1 last applied index", 1, follower1log.getLastApplied());

        // Verify follower2's journal matches follower1's.
        final var follower2log = follower2Context.getReplicatedLog();
        assertEquals("Follower 2 journal log size", 2, follower2log.size());
        assertEquals("Follower 2 journal last index", 1, follower2log.lastIndex());
        assertEquals("Follower 2 commit index", 1, follower2log.getCommitIndex());
        assertEquals("Follower 2 last applied index", 1, follower2log.getLastApplied());
        verifyReplicatedLogEntry(follower2log.get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower2log.get(1), currentTerm, 1, NoopPayload.INSTANCE);

        // Reinstate follower1.
        killActor(follower1Actor);

        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id)))
                .config(followerConfigParams));
        follower1Actor.underlyingActor().waitForRecoveryComplete();
        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();

        // Verify follower1's journal was persisted and recovered correctly.
        follower1log = follower1Context.getReplicatedLog();
        assertEquals("Follower 1 journal log size", 2, follower1log.size());
        assertEquals("Follower 1 journal last index", 1, follower1log.lastIndex());
        assertEquals("Follower 1 commit index", 1, follower1log.getCommitIndex());
        assertEquals("Follower 1 last applied index", 1, follower1log.getLastApplied());
        verifyReplicatedLogEntry(follower1log.get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower1log.get(1), currentTerm, 1, NoopPayload.INSTANCE);

        testLog.info("testUnComittedEntryOnLeaderChange ending");
    }

    private void createRaftActors() {
        testLog.info("createRaftActors starting");

        follower1NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower1Id + "-notifier"));

        followerConfigParams = newFollowerConfigParams();
        followerConfigParams.setHeartBeatInterval(Duration.ofMillis(100));
        followerConfigParams.setSnapshotBatchCount(snapshotBatchCount);
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id)))
                .config(followerConfigParams).roleChangeNotifier(follower1NotifierActor));

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), followerConfigParams);

        peerAddresses = ImmutableMap.<String, String>builder()
                .put(follower1Id, follower1Actor.path().toString())
                .put(follower2Id, follower2Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderConfigParams.setHeartBeatInterval(Duration.ofDays(1));
        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderActor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());
        waitUntilLeader(leaderActor);

        expectMatching(leaderCollectorActor, AppendEntriesReply.class, 2);
        expectFirstMatching(follower1CollectorActor, AppendEntries.class);

        clearMessages(leaderCollectorActor);
        clearMessages(follower1CollectorActor);
        clearMessages(follower2CollectorActor);

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
        currentTerm = leaderContext.currentTerm();

        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();
        follower2Context = follower2Actor.underlyingActor().getRaftActorContext();

        testLog.info("createRaftActors ending - follower1: {}, follower2: {}", follower1Id, follower2Id);
    }
}
