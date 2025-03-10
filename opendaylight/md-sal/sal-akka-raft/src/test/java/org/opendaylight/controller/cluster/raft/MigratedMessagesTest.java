/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.ByteState;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for migrated messages on recovery.
 *
 * @author Thomas Pantelis
 */
public class MigratedMessagesTest extends AbstractActorTest {
    static final Logger TEST_LOG = LoggerFactory.getLogger(MigratedMessagesTest.class);

    private TestActorFactory factory;

    @Before
    public void setUp() {
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void tearDown() {
        factory.close();
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @Test
    public void testNoSnapshotAfterStartupWithNoMigratedMessages() {
        TEST_LOG.info("testNoSnapshotAfterStartupWithNoMigratedMessages starting");
        String id = factory.generateActorId("test-actor-");

        InMemoryJournal.addEntry(id, 1, new UpdateElectionTerm(1, id));
        InMemoryJournal.addEntry(id, 2, new SimpleReplicatedLogEntry(0, 1, new MockRaftActorContext.MockPayload("A")));
        InMemoryJournal.addEntry(id, 3, new ApplyJournalEntries(0));

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        RaftActorSnapshotCohort snapshotCohort = new RaftActorSnapshotCohort() {
            @Override
            public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
                actorRef.tell(new CaptureSnapshotReply(ByteState.empty(), installSnapshotStream.orElse(null)),
                    actorRef);
            }

            @Override
            public void applySnapshot(final Snapshot.State snapshotState) {
                // Nothing
            }

            @Override
            public State deserializeSnapshot(final ByteSource snapshotBytes) {
                throw new UnsupportedOperationException();
            }
        };

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder()
            .id(id).config(config).snapshotCohort(snapshotCohort)
            .persistent(Optional.of(Boolean.TRUE)).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(750, TimeUnit.MILLISECONDS);

        List<Snapshot> snapshots = InMemorySnapshotStore.getSnapshots(id, Snapshot.class);
        assertEquals("Snapshots", 0, snapshots.size());

        TEST_LOG.info("testNoSnapshotAfterStartupWithNoMigratedMessages ending");
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private TestActorRef<MockRaftActor> doTestSnapshotAfterStartupWithMigratedMessage(final String id,
            final boolean persistent, final Consumer<Snapshot> snapshotVerifier, final State snapshotState) {
        InMemorySnapshotStore.addSnapshotSavedLatch(id);
        InMemoryJournal.addDeleteMessagesCompleteLatch(id);
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        RaftActorSnapshotCohort snapshotCohort = new RaftActorSnapshotCohort() {
            @Override
            public void createSnapshot(final ActorRef actorRef, final Optional<OutputStream> installSnapshotStream) {
                actorRef.tell(new CaptureSnapshotReply(snapshotState, installSnapshotStream.orElse(null)), actorRef);
            }

            @Override
            public void applySnapshot(final State newState) {
                // Nothing
            }

            @Override
            public State deserializeSnapshot(final ByteSource snapshotBytes) {
                throw new UnsupportedOperationException();
            }
        };

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(id)
            .config(config).snapshotCohort(snapshotCohort).persistent(Optional.of(persistent))
            .peerAddresses(ImmutableMap.of("peer", "")).props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Snapshot snapshot = InMemorySnapshotStore.waitForSavedSnapshot(id, Snapshot.class);
        snapshotVerifier.accept(snapshot);

        InMemoryJournal.waitForDeleteMessagesComplete(id);

        assertEquals("InMemoryJournal size", 0, InMemoryJournal.get(id).size());

        return raftActorRef;
    }
}
