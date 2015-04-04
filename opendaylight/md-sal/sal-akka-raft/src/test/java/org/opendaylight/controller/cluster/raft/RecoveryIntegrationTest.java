/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext.MockPayload;
import org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
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

        peerAddresses = ImmutableMap.<String, String>builder().
                put(follower1Id, follower1Actor.path().toString()).build();

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
        leaderActor.underlyingActor().startDropMessages(CaptureSnapshot.class);
        leaderActor.underlyingActor().startDropMessages(CaptureSnapshotReply.class);
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(
                leaderCollectorActor, CaptureSnapshot.class);

        // First, deliver the CaptureSnapshot to the leader.
        leaderActor.underlyingActor().stopDropMessages(CaptureSnapshot.class);
        leaderActor.tell(captureSnapshot, leaderActor);

        // Send another payload.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");

        // Now deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 3);

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
    public void testStatePersistedBetweenInitiateSnapshotAndCapture() {

        send2InitialPayloads();

        // Block these messages initially so we can control the sequence.
        leaderActor.underlyingActor().startDropMessages(CaptureSnapshot.class);
        follower1Actor.underlyingActor().startDropMessages(AppendEntries.class);

        MockPayload payload2 = sendPayloadData(leaderActor, "two");

        // This should trigger a snapshot.
        MockPayload payload3 = sendPayloadData(leaderActor, "three");

        // Send another payload.
        MockPayload payload4 = sendPayloadData(leaderActor, "four");

        MessageCollectorActor.expectMatching(follower1CollectorActor, AppendEntries.class, 3);

        CaptureSnapshot captureSnapshot = MessageCollectorActor.expectFirstMatching(
                leaderCollectorActor, CaptureSnapshot.class);

        // First, deliver the AppendEntries to the follower
        follower1Actor.underlyingActor().stopDropMessages(AppendEntries.class);

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 3);

        // Now deliver the CaptureSnapshot to the leader.
        leaderActor.underlyingActor().stopDropMessages(CaptureSnapshot.class);
        leaderActor.tell(captureSnapshot, leaderActor);

        // Wait for snapshot complete.
        MessageCollectorActor.expectFirstMatching(leaderCollectorActor, SaveSnapshotSuccess.class);

        reinstateLeaderActor();

        assertEquals("Leader snapshot term", currentTerm, leaderContext.getReplicatedLog().getSnapshotTerm());
        assertEquals("Leader snapshot index", 1, leaderContext.getReplicatedLog().getSnapshotIndex());
        assertEquals("Leader journal log size", 3, leaderContext.getReplicatedLog().size());
        assertEquals("Leader journal last index", 4, leaderContext.getReplicatedLog().lastIndex());
        assertEquals("Leader commit index", 4, leaderContext.getCommitIndex());
        assertEquals("Leader last applied", 4, leaderContext.getLastApplied());

        // payloads 2, 3, and 4 were applied after the snapshot was initiated and before it was captured so
        // were included in the snapshot. They were also included as unapplied entries in the snapshot as
        // they weren't yet applied to the state at the time the snapshot was initiated. They were applied to the
        // state on recovery by the ApplyJournalEntries messages which remained in the persisted log.
        // This is a side effect of trimming the persisted log to the sequence number captured at the time
        // the snapshot was initiated.
        assertEquals("Leader state", Arrays.asList(payload0, payload1, payload2, payload3, payload4, payload2,
                payload3, payload4), leaderActor.underlyingActor().getState());
    }

    @Test
    public void testApplyJournalEntriesPersistedAfterSnapshotPersisted() {

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

        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 3);

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
        payload1 = sendPayloadData(leaderActor, "one");

        // Verify the leader applies the states.
        MessageCollectorActor.expectMatching(leaderCollectorActor, ApplyJournalEntries.class, 2);

        assertEquals("Leader last applied", 1, leaderContext.getLastApplied());

        MessageCollectorActor.clearMessages(leaderCollectorActor);
        MessageCollectorActor.clearMessages(follower1CollectorActor);
    }
}
