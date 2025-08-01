/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opendaylight.controller.cluster.raft.RaftActorTestKit.awaitLastApplied;
import static org.opendaylight.controller.cluster.raft.RaftActorTestKit.awaitSnapshot;

import java.util.List;
import java.util.Map;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;

/**
 * Recovery Integration Test for single node.
 */
class RecoveryIntegrationSingleNodeTest extends AbstractRaftActorIntegrationTest {
    @BeforeEach
    void beforeEach() {
        leaderConfigParams = newLeaderConfigParams();
    }

    @Test
    void testJournalReplayAfterSnapshotWithSingleNode() throws Exception {
        String persistenceId = factory.generateActorId("singleNode");
        TestActorRef<AbstractRaftActorIntegrationTest.TestRaftActor> singleNodeActorRef =
                newTestRaftActor(persistenceId, Map.of(), leaderConfigParams);

        waitUntilLeader(singleNodeActorRef);

        final var singleNodeContext = singleNodeActorRef.underlyingActor().getRaftActorContext();

        final MockCommand payload0 = sendPayloadData(singleNodeActorRef, "zero");
        final MockCommand payload1 = sendPayloadData(singleNodeActorRef, "one");
        final MockCommand payload2 = sendPayloadData(singleNodeActorRef, "two");

        awaitLastApplied(singleNodeActorRef, 2);

        // this should trigger a snapshot
        final MockCommand payload3 = sendPayloadData(singleNodeActorRef, "three");

        awaitLastApplied(singleNodeActorRef, 3);

        //add 2 more
        final MockCommand payload4 = sendPayloadData(singleNodeActorRef, "four");
        final MockCommand payload5 = sendPayloadData(singleNodeActorRef, "five");


        // Wait for snapshot complete.
        awaitSnapshot(singleNodeActorRef);

        awaitLastApplied(singleNodeActorRef, 5);

        assertEquals(5, singleNodeContext.getReplicatedLog().getLastApplied());

        assertEquals(
                List.of(payload0, payload1, payload2, payload3, payload4, payload5),
                singleNodeActorRef.underlyingActor().getState());

        final var journal = assertInstanceOf(EnabledRaftStorage.class, singleNodeContext.entryStore()).journal();
        assertEquals(6, journal.applyToJournalIndex());

        try (var reader = journal.openReader()) {
            assertEquals(5, reader.nextJournalIndex());
            var journalEntry = reader.nextEntry();
            assertNotNull(journalEntry);
            var logEntry = journalEntry.toLogEntry(OBJECT_STREAMS);
            assertEquals(4, logEntry.index());
            assertEquals(1, logEntry.term());
            assertEquals(payload4, logEntry.command());

            assertEquals(6, reader.nextJournalIndex());
            journalEntry = reader.nextEntry();
            assertNotNull(journalEntry);
            logEntry = journalEntry.toLogEntry(OBJECT_STREAMS);
            assertEquals(5, logEntry.index());
            assertEquals(1, logEntry.term());
            assertEquals(payload5, logEntry.command());

            assertEquals(7, reader.nextJournalIndex());
            assertNull(reader.nextEntry());
        }

        final var snapshotFile = singleNodeActorRef.underlyingActor().lastSnapshot();
        assertNotNull(snapshotFile);

        assertEquals(List.of(payload0, payload1, payload2, payload3), MockRaftActor.fromState(
            snapshotFile.readSnapshot(MockSnapshotState.SUPPORT.reader())));

        // recovery logic starts
        killActor(singleNodeActorRef);

        singleNodeActorRef = newTestRaftActor(persistenceId, Map.of(), leaderConfigParams);

        singleNodeActorRef.underlyingActor().waitForRecoveryComplete();

        assertEquals(List.of(payload0, payload1, payload2, payload3, payload4, payload5),
                singleNodeActorRef.underlyingActor().getState());
    }
}
