/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;

/**
 * Recovery Integration Test for single node.
 */
public class RecoveryIntegrationSingleNodeTest extends AbstractRaftActorIntegrationTest {
    @Before
    public void setup() {
        leaderConfigParams = newLeaderConfigParams();
    }

    @Test
    public void testJournalReplayAfterSnapshotWithSingleNode() throws Exception {
        String persistenceId = factory.generateActorId("singleNode");
        TestActorRef<AbstractRaftActorIntegrationTest.TestRaftActor> singleNodeActorRef =
                newTestRaftActor(persistenceId, Map.of(), leaderConfigParams);

        waitUntilLeader(singleNodeActorRef);

        final var singleNodeContext = singleNodeActorRef.underlyingActor().getRaftActorContext();

        InMemoryJournal.addWriteMessagesCompleteLatch(persistenceId, 6, ApplyJournalEntries.class);

        final MockCommand payload0 = sendPayloadData(singleNodeActorRef, "zero");
        final MockCommand payload1 = sendPayloadData(singleNodeActorRef, "one");
        final MockCommand payload2 = sendPayloadData(singleNodeActorRef, "two");

        verifyApplyIndex(singleNodeActorRef, 2);

        // this should trigger a snapshot
        final MockCommand payload3 = sendPayloadData(singleNodeActorRef, "three");

        verifyApplyIndex(singleNodeActorRef, 3);

        //add 2 more
        final MockCommand payload4 = sendPayloadData(singleNodeActorRef, "four");
        final MockCommand payload5 = sendPayloadData(singleNodeActorRef, "five");


        // Wait for snapshot complete.
        awaitSnapshot(singleNodeActorRef);

        verifyApplyIndex(singleNodeActorRef, 5);

        assertEquals("Last applied", 5, singleNodeContext.getReplicatedLog().getLastApplied());

        assertEquals("Incorrect State after snapshot success is received ",
                List.of(payload0, payload1, payload2, payload3, payload4, payload5),
                singleNodeActorRef.underlyingActor().getState());

        InMemoryJournal.waitForWriteMessagesComplete(persistenceId);

        // we get 2 log entries (4 and 5 indexes) and 3 ApplyJournalEntries (for 3, 4, and 5 indexes)
        assertEquals(5, InMemoryJournal.get(persistenceId).size());

        final var snapshotFile = singleNodeActorRef.underlyingActor().snapshots().lastSnapshot();
        assertNotNull(snapshotFile);

        assertEquals(List.of(payload0, payload1, payload2, payload3), MockRaftActor.fromState(
            snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader())));

        // recovery logic starts
        killActor(singleNodeActorRef);

        singleNodeActorRef = newTestRaftActor(persistenceId, Map.of(), leaderConfigParams);

        singleNodeActorRef.underlyingActor().waitForRecoveryComplete();

        assertEquals("Incorrect State after Recovery ",
                List.of(payload0, payload1, payload2, payload3, payload4, payload5),
                singleNodeActorRef.underlyingActor().getState());
    }
}
