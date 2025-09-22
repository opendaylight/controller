/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.opendaylight.controller.cluster.raft.RaftActorTestKit.awaitSnapshot;

import com.google.common.util.concurrent.Uninterruptibles;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.testkit.TestActorRef;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.cluster.raft.spi.PropertiesTermInfoStore;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.WellKnownRaftPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for migrated messages on recovery.
 *
 * @author Thomas Pantelis
 */
class MigratedMessagesTest extends AbstractActorTest {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(MigratedMessagesTest.class);

    private TestActorFactory factory;

    @BeforeEach
    void beforeEach() {
        factory = new TestActorFactory(getSystem());
    }

    @AfterEach
    void afterEach() {
        factory.close();
    }

    @Test
    void testNoSnapshotAfterStartupWithNoMigratedMessages() throws Exception {
        TEST_LOG.info("testNoSnapshotAfterStartupWithNoMigratedMessages starting");
        String id = factory.generateActorId("test-actor-");

        final var actorDir = stateDir().resolve(id);
        new PropertiesTermInfoStore(id, actorDir).storeAndSetTerm(new TermInfo(1, id));

        try (var journal = new EntryJournalV1(id, actorDir, CompressionType.NONE, true)) {
            journal.appendEntry(new DefaultLogEntry(0, 1, new MockCommand("A")));
            journal.setApplyTo(1);
        }

        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder()
            .id(id)
            .config(config)
            .snapshotCohort((MockRaftActorSnapshotCohort) () -> new MockSnapshotState(List.of()))
            .persistent(Optional.of(Boolean.TRUE))
            .props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(750, TimeUnit.MILLISECONDS);

        TEST_LOG.info("testNoSnapshotAfterStartupWithNoMigratedMessages ending");
    }

    private TestActorRef<MockRaftActor> doTestSnapshotAfterStartupWithMigratedMessage(final String persistenceId,
            final boolean persistent, final Consumer<SnapshotFile> snapshotVerifier,
            final @NonNull MockSnapshotState snapshotState) {
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setRaftPolicy(WellKnownRaftPolicy.DISABLE_ELECTIONS);

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder()
            .id(persistenceId)
            .config(config)
            .snapshotCohort((MockRaftActorSnapshotCohort) () -> snapshotState)
            .persistent(Optional.of(persistent))
            .peerAddresses(Map.of("peer", ""))
            .props(stateDir())
            .withDispatcher(Dispatchers.DefaultDispatcherId()), persistenceId);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        snapshotVerifier.accept(awaitSnapshot(mockRaftActor));

        return raftActorRef;
    }
}
