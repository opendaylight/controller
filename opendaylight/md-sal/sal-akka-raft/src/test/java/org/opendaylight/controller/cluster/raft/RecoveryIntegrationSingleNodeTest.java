/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import akka.actor.ActorRef;
import akka.persistence.SaveSnapshotSuccess;
import akka.testkit.TestActorRef;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recovery Integration Test for single node
 */
public class RecoveryIntegrationSingleNodeTest extends AbstractRaftActorIntegrationTest {

    static final Logger LOG = LoggerFactory.getLogger(RecoveryIntegrationSingleNodeTest.class);

    @Before
    public void setup() {
        leaderConfigParams = newLeaderConfigParams();
    }


    @Test
    public void testJournalReplayAfterSnapshotWithSingleNode() throws Exception {

        String persistenceId = factory.generateActorId("singleNode");
        TestActorRef<AbstractRaftActorIntegrationTest.TestRaftActor> singleNodeActorRef = newTestRaftActor(persistenceId,
                ImmutableMap.<String, String>builder().build(), leaderConfigParams);

        waitUntilLeader(singleNodeActorRef);

        ActorRef singleNodeCollectorActor = singleNodeActorRef.underlyingActor().collectorActor();
        RaftActorContext singleNodeContext = singleNodeActorRef.underlyingActor().getRaftActorContext();


        MockRaftActorContext.MockPayload payload0 = sendPayloadData(singleNodeActorRef, "zero");
        MockRaftActorContext.MockPayload payload1 = sendPayloadData(singleNodeActorRef, "one");
        MockRaftActorContext.MockPayload payload2 = sendPayloadData(singleNodeActorRef, "two");

        MessageCollectorActor.expectMatching(singleNodeCollectorActor, ApplyJournalEntries.class, 3);

        // this should trigger a snapshot
        MockRaftActorContext.MockPayload payload3 = sendPayloadData(singleNodeActorRef, "three");

        MessageCollectorActor.expectMatching(singleNodeCollectorActor, ApplyJournalEntries.class, 4);

        //add 2 more
        MockRaftActorContext.MockPayload payload4 = sendPayloadData(singleNodeActorRef, "four");
        MockRaftActorContext.MockPayload payload5 = sendPayloadData(singleNodeActorRef, "five");


        // Wait for snapshot complete.
        MessageCollectorActor.expectFirstMatching(singleNodeCollectorActor, SaveSnapshotSuccess.class);

        MessageCollectorActor.expectMatching(singleNodeCollectorActor, ApplyJournalEntries.class, 6);

        assertEquals("Last applied", 5, singleNodeContext.getLastApplied());

        assertEquals("Incorrect State after snapshot success is received ",
                Lists.newArrayList(payload0, payload1, payload2, payload3, payload4, payload5), singleNodeActorRef.underlyingActor().getState());

        // we get 2 log entries (4 and 5 indexes) and 3 ApplyJournalEntries (for 3, 4, and 5 indexes)
        assertEquals(5, InMemoryJournal.get(persistenceId).size());

        List<Snapshot> persistedSnapshots = InMemorySnapshotStore.getSnapshots(persistenceId, Snapshot.class);
        assertEquals(1, persistedSnapshots.size());

        List<Object> snapshottedState = (List<Object>)MockRaftActor.toObject(persistedSnapshots.get(0).getState());
        assertEquals("Incorrect Snapshot", Lists.newArrayList(payload0, payload1, payload2, payload3), snapshottedState);

        //recovery logic starts
        killActor(singleNodeActorRef);

        singleNodeActorRef = newTestRaftActor(persistenceId,
                ImmutableMap.<String, String>builder().build(), leaderConfigParams);

        singleNodeActorRef.underlyingActor().waitForRecoveryComplete();

        assertEquals("Incorrect State after Recovery ",
                Lists.newArrayList(payload0, payload1, payload2, payload3, payload4, payload5), singleNodeActorRef.underlyingActor().getState());

    }
}
