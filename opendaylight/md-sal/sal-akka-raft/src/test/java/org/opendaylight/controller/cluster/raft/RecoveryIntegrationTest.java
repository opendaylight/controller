/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

/**
 * Tests raft actor persistence recovery end-to-end using real RaftActors and behavior communication.
 *
 * @author Thomas Pantelis
 */
public class RecoveryIntegrationTest extends AbstractRaftActorIntegrationTest {

    private MockPayload payload0;
    private MockPayload payload1;

    @Before
    public void setup() {
        follower1Actor = newTestRaftActor(follower1Id, ImmutableMap.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());

        Map<String, String> peerAddresses = new HashMap<>();
        peerAddresses.put(follower1Id, follower1Actor.path().toString());
        peerAddresses.put(follower2Id, "");

        leaderConfigParams = newLeaderConfigParams();
        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        follower1CollectorActor = follower1Actor.underlyingActor().collectorActor();
        leaderCollectorActor = leaderActor.underlyingActor().collectorActor();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
    }

    @Test
    public void testStatePersistedBetweenSnapshotCaptureAndPersist() {

        send2InitialPayloads();

        // Block these messages initially so we can control the sequence.
        leaderActor.underlyingActor().startDropMessages(CaptureSnapshotReply.class);
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        // Send another payload.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");

        // Now deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 1);

        // Now deliver the CaptureSnapshotReply to the leader.
        CaptureSnapshotReply captureSnapshotReply = MessageCollectorActor.expectFirstMatching(
                leaderCollectorActor, CaptureSnapshotReply.class);
        leaderActor.underlyingActor().stopDropMessages(CaptureSnapshotReply.class);
        leaderActor.tell(captureSnapshotReply, leaderActor);

        // Wait for snapshot complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        reinstateLeaderActor();

        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 4, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 4, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 4, leaderContext.getLastApplied());

        assertEquals("Leader state", Arrays.asList(payload0, payload1, payload2, payload3, payload4),
                leaderActor.underlyingActor().getState());
    }

    @Test
    public void testStatePersistedAfterSnapshotPersisted() {

        send2InitialPayloads();

        // Block these messages initially so we can control the sequence.
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        // Send another payload.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        // Wait for snapshot complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        // Now deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 1);

        reinstateLeaderActor();

        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 4, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 4, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 4, leaderContext.getLastApplied());

        assertEquals("Leader state", Arrays.asList(payload0, payload1, payload2, payload3, payload4),
                leaderActor.underlyingActor().getState());
    }

    @Test
    public void testFollowerRecoveryAfterInstallSnapshot() throws Exception {

        send2InitialPayloads();

        leader = leaderActor.underlyingActor().getCurrentBehavior();

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());
        follower2CollectorActor = follower2Actor.underlyingActor().collectorActor();

        leaderActor.tell(new SetPeerAddress(follower2Id, follower2Actor.path().toString()), ActorRef.noSender());

        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // Verify the leader applies the 3rd payload state.
        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 1);

        MessageCollectorActor.expectMatching(follower2CollectorActor, ApplyJournalEntries.class, 1);

        assertEquals("Leader commit index", 2, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 2, leaderContext.getLastApplied());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader replicatedToAllIndex", 1, leader.getReplicatedToAllIndex());

        killActor(follower2Actor);

        InMemoryJournal.clear();

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());
        TestRaftActor follower2Underlying = follower2Actor.underlyingActor();
        follower2CollectorActor = follower2Underlying.collectorActor();
        follower2Context = follower2Underlying.getRaftActorContext();

        leaderActor.tell(new SetPeerAddress(follower2Id, follower2Actor.path().toString()), ActorRef.noSender());

        // The leader should install a snapshot so wait for the follower to receive ApplySnapshot.
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, ApplySnapshot.class);

        // Wait for the follower to persist the snapshot.
        MessageCollectorActor.expectFirstMatching(follower2CollectorActor, SaveSnapshotSuccess.class);

        List<MockPayload> expFollowerState = Arrays.asList(payload0, payload1, payload2);

        assertEquals("Follower commit index", 2, follower2Context.getCommitIndex());
        assertEquals("Follower last applied", 2, follower2Context.getLastApplied());
        assertEquals("Follower snapshot index", 2, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower state", expFollowerState, follower2Underlying.getState());

        killActor(follower2Actor);

        follower2Actor = newTestRaftActor(follower2Id, ImmutableMap.of(leaderId, testActorPath(leaderId)),
                newFollowerConfigParams());

        follower2Underlying = follower2Actor.underlyingActor();
        follower2Underlying.waitForRecoveryComplete();
        follower2Context = follower2Underlying.getRaftActorContext();

        assertEquals("Follower commit index", 2, follower2Context.getCommitIndex());
        assertEquals("Follower last applied", 2, follower2Context.getLastApplied());
        assertEquals("Follower snapshot index", 2, follower2Context.getReplicatedLog().getSnapshotIndex());
        assertEquals("Follower state", expFollowerState, follower2Underlying.getState());
    }

    private void reinstateLeaderActor() {
        killActor(leaderActor);

        leaderActor = newTestRaftActor(leaderId, peerAddresses, leaderConfigParams);

        leaderActor.underlyingActor().waitForRecoveryComplete();

        leaderContext = leaderActor.underlyingActor().getRaftActorContext();
    }

    private void send2InitialPayloads() {
        waitUntilLeader(leaderActor);
        currentTerm = leaderContext.getTermInformation().getCurrentTerm();

        payload0 = sendPayloadData(leaderActor, "zero");

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 1);

        payload1 = sendPayloadData(leaderActor, "one");

        // Verify the leader applies the states.
        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 2);

        assertEquals("Leader last applied", 1, leaderContext.getLastApplied());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
    }
}
