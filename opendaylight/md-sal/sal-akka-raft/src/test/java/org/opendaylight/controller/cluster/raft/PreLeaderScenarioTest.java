/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
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

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyState;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.persisted.NoopPayload;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import scala.concurrent.duration.FiniteDuration;

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
        MockPayload payload0 = sendPayloadData(leaderActor, "zero");

        AppendEntries appendEntries = expectFirstMatching(follower1CollectorActor, AppendEntries.class);
        assertEquals("AppendEntries - # entries", 1, appendEntries.getEntries().size());
        verifyReplicatedLogEntry(appendEntries.getEntries().get(0), currentTerm, 0, payload0);

        // Kill the leader actor.
        killActor(leaderActor);

        // At this point, the payload entry is in follower1's log but is uncommitted. follower2 has not
        // received the payload entry yet.
        assertEquals("Follower 1 journal log size", 1, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 0, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", -1, follower1Context.getCommitIndex());
        assertEquals("Follower 1 last applied index", -1, follower1Context.getLastApplied());

        assertEquals("Follower 2 journal log size", 0, follower2Context.getReplicatedLog().size());

        follower2Actor.underlyingActor().stopDropMessages(AppendEntries.class);
        clearMessages(follower1NotifierActor);

        // Force follower1 to start an election. It should win since it's journal is more up-to-date than
        // follower2's journal.
        follower1Actor.tell(TimeoutNow.INSTANCE, ActorRef.noSender());

        // Verify the expected raft state changes. It should go to PreLeader since it has an uncommitted entry.
        List<RoleChanged> roleChange = expectMatching(follower1NotifierActor, RoleChanged.class, 3);
        assertEquals("Role change 1", RaftState.Candidate.name(), roleChange.get(0).getNewRole());
        assertEquals("Role change 2", RaftState.PreLeader.name(), roleChange.get(1).getNewRole());
        assertEquals("Role change 3", RaftState.Leader.name(), roleChange.get(2).getNewRole());

        final long previousTerm = currentTerm;
        currentTerm = follower1Context.currentTerm();

        // Since it went to Leader, it should've appended and successfully replicated a NoopPaylod with the
        // new term to follower2 and committed both entries, including the first payload from the previous term.
        assertEquals("Follower 1 journal log size", 2, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 1, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 1, follower1Context.getCommitIndex());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(1), currentTerm, 1, NoopPayload.INSTANCE);

        // Both entries should be applied to the state.
        expectMatching(follower1CollectorActor, ApplyState.class, 2);
        expectMatching(follower2CollectorActor, ApplyState.class, 2);

        assertEquals("Follower 1 last applied index", 1, follower1Context.getLastApplied());

        // Verify follower2's journal matches follower1's.
        assertEquals("Follower 2 journal log size", 2, follower2Context.getReplicatedLog().size());
        assertEquals("Follower 2 journal last index", 1, follower2Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 2 commit index", 1, follower2Context.getCommitIndex());
        assertEquals("Follower 2 last applied index", 1, follower2Context.getLastApplied());
        verifyReplicatedLogEntry(follower2Context.getReplicatedLog().get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower2Context.getReplicatedLog().get(1), currentTerm, 1, NoopPayload.INSTANCE);

        // Reinstate follower1.
        killActor(follower1Actor);

        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id)))
                .baseDir(baseDir()).config(followerConfigParams));
        follower1Actor.underlyingActor().waitForRecoveryComplete();
        follower1Context = follower1Actor.underlyingActor().getRaftActorContext();

        // Verify follower1's journal was persisted and recovered correctly.
        assertEquals("Follower 1 journal log size", 2, follower1Context.getReplicatedLog().size());
        assertEquals("Follower 1 journal last index", 1, follower1Context.getReplicatedLog().lastIndex());
        assertEquals("Follower 1 commit index", 1, follower1Context.getCommitIndex());
        assertEquals("Follower 1 last applied index", 1, follower1Context.getLastApplied());
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(0), previousTerm, 0, payload0);
        verifyReplicatedLogEntry(follower1Context.getReplicatedLog().get(1), currentTerm, 1, NoopPayload.INSTANCE);

        testLog.info("testUnComittedEntryOnLeaderChange ending");
    }

    private void createRaftActors() {
        testLog.info("createRaftActors starting");

        follower1NotifierActor = factory.createActor(MessageCollectorActor.props(),
                factory.generateActorId(follower1Id + "-notifier"));

        followerConfigParams = newFollowerConfigParams();
        followerConfigParams.setHeartBeatInterval(new FiniteDuration(100, TimeUnit.MILLISECONDS));
        followerConfigParams.setSnapshotBatchCount(snapshotBatchCount);
        follower1Actor = newTestRaftActor(follower1Id, TestRaftActor.newBuilder().peerAddresses(
                ImmutableMap.of(leaderId, testActorPath(leaderId), follower2Id, testActorPath(follower2Id)))
                .baseDir(baseDir()).config(followerConfigParams).roleChangeNotifier(follower1NotifierActor));

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId),
                follower1Id, testActorPath(follower1Id)), followerConfigParams);

        peerAddresses = ImmutableMap.<String, String>builder()
                .put(follower1Id, follower1Actor.path().toString())
                .put(follower2Id, follower2Actor.path().toString()).build();

        leaderConfigParams = newLeaderConfigParams();
        leaderConfigParams.setHeartBeatInterval(new FiniteDuration(1, TimeUnit.DAYS));
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
