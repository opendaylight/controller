/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static org.junit.Assert.assertEquals;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.ServerInfo;
import org.opendaylight.controller.cluster.raft.persisted.UpdateElectionTerm;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.InMemorySnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import akka.actor.ActorRef;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

/**
 * Unit tests for migrated messages on recovery.
 *
 * @author Thomas Pantelis
 */
public class MigratedMessagesTest extends AbstractActorTest {
    static final Logger TEST_LOG = LoggerFactory.getLogger(MigratedMessagesTest.class);

    private TestActorFactory factory;

    @Before
    public void setUp(){
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void tearDown() throws Exception {
        factory.close();
        InMemoryJournal.clear();
        InMemorySnapshotStore.clear();
    }

    @Test
    public void testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceEnabled() {
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceEnabled starting");
        doTestSnapshotAfterStartupWithMigratedServerConfigPayload(true);
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceEnabled ending");
    }

    @Test
    public void testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceDisabled() {
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceDisabled starting");

        TestActorRef<MockRaftActor> actor = doTestSnapshotAfterStartupWithMigratedServerConfigPayload(false);
        MockRaftActor mockRaftActor = actor.underlyingActor();
        String id = mockRaftActor.persistenceId();
        ConfigParams config = mockRaftActor.getRaftActorContext().getConfigParams();

        factory.killActor(actor, new JavaTestKit(getSystem()));

        actor = factory.createTestActor(MockRaftActor.builder().id(id).config(config).persistent(Optional.of(false)).props().
                    withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        mockRaftActor = actor.underlyingActor();
        mockRaftActor.waitForRecoveryComplete();

        assertEquals("electionTerm", 1,
                mockRaftActor.getRaftActorContext().getTermInformation().getCurrentTerm());
        assertEquals("votedFor", id,
                mockRaftActor.getRaftActorContext().getTermInformation().getVotedFor());

        TEST_LOG.info("testSnapshotAfterStartupWithMigratedServerConfigPayloadAndPersistenceDisabled ending");
    }

    @Test
    public void testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceEnabled() {
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceEnabled starting");

        String persistenceId = factory.generateActorId("test-actor-");

        org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm updateElectionTerm =
                new org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm(5, persistenceId);

        InMemoryJournal.addEntry(persistenceId, 1, updateElectionTerm);

        doTestSnapshotAfterStartupWithMigratedMessage(persistenceId, true, snapshot -> {
            assertEquals("getElectionVotedFor", persistenceId, snapshot.getElectionVotedFor());
            assertEquals("getElectionTerm", 5, snapshot.getElectionTerm());
        });

        TEST_LOG.info("testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceEnabled ending");
    }

    @Test
    public void testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceDisabled() {
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceDisabled starting");

        String persistenceId = factory.generateActorId("test-actor-");

        org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm updateElectionTerm =
                new org.opendaylight.controller.cluster.raft.base.messages.UpdateElectionTerm(5, persistenceId);

        InMemoryJournal.addEntry(persistenceId, 1, updateElectionTerm);

        doTestSnapshotAfterStartupWithMigratedMessage(persistenceId, false, snapshot -> {
            assertEquals("getElectionVotedFor", persistenceId, snapshot.getElectionVotedFor());
            assertEquals("getElectionTerm", 5, snapshot.getElectionTerm());
        });

        TEST_LOG.info("testSnapshotAfterStartupWithMigratedUpdateElectionTermAndPersistenceDisabled ending");
    }

    @Test
    public void testSnapshotAfterStartupWithMigratedApplyJournalEntries() {
        TEST_LOG.info("testSnapshotAfterStartupWithMigratedApplyJournalEntries starting");

        String persistenceId = factory.generateActorId("test-actor-");

        InMemoryJournal.addEntry(persistenceId, 1, new UpdateElectionTerm(1, persistenceId));
        InMemoryJournal.addEntry(persistenceId, 2, new ReplicatedLogImplEntry(0, 1,
                new MockRaftActorContext.MockPayload("A")));
        InMemoryJournal.addEntry(persistenceId, 3,
                new org.opendaylight.controller.cluster.raft.base.messages.ApplyJournalEntries(0));


        doTestSnapshotAfterStartupWithMigratedMessage(persistenceId, true, snapshot -> {
            assertEquals("getLastAppliedIndex", 0, snapshot.getLastAppliedIndex());
            assertEquals("getLastAppliedTerm", 1, snapshot.getLastAppliedTerm());
            assertEquals("getLastIndex", 0, snapshot.getLastIndex());
            assertEquals("getLastTerm", 1, snapshot.getLastTerm());
        });

        TEST_LOG.info("testSnapshotAfterStartupWithMigratedApplyJournalEntries ending");
    }

    @Test
    public void testNoSnapshotAfterStartupWithNoMigratedMessages() {
        String id = factory.generateActorId("test-actor-");
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        RaftActorSnapshotCohort snapshotCohort = new RaftActorSnapshotCohort() {
            @Override
            public void createSnapshot(ActorRef actorRef) {
                actorRef.tell(new CaptureSnapshotReply(new byte[0]), actorRef);
            }

            @Override
            public void applySnapshot(byte[] snapshotBytes) {
            }
        };

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(id).
                config(config).snapshotCohort(snapshotCohort).persistent(Optional.of(true)).props().
                    withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Uninterruptibles.sleepUninterruptibly(750, TimeUnit.MILLISECONDS);

        List<Snapshot> snapshots = InMemorySnapshotStore.getSnapshots(id, Snapshot.class);
        assertEquals("Snapshots", 0, snapshots.size());
    }

    private TestActorRef<MockRaftActor> doTestSnapshotAfterStartupWithMigratedServerConfigPayload(boolean persistent) {
        String persistenceId = factory.generateActorId("test-actor-");

        org.opendaylight.controller.cluster.raft.ServerConfigurationPayload persistedServerConfig =
                new org.opendaylight.controller.cluster.raft.ServerConfigurationPayload(Arrays.asList(
                    new org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo(
                            persistenceId, true),
                    new org.opendaylight.controller.cluster.raft.ServerConfigurationPayload.ServerInfo(
                            "downNode", true)));

        ServerConfigurationPayload expectedServerConfig = new ServerConfigurationPayload(Arrays.asList(
                new ServerInfo(persistenceId, true), new ServerInfo("downNode", true)));

        InMemoryJournal.addEntry(persistenceId, 1, new UpdateElectionTerm(1, persistenceId));
        InMemoryJournal.addEntry(persistenceId, 3, new ReplicatedLogImplEntry(0, 1, persistedServerConfig));

        TestActorRef<MockRaftActor> actor = doTestSnapshotAfterStartupWithMigratedMessage(persistenceId,
                persistent, snapshot -> {
            assertEquals("getElectionVotedFor", persistenceId, snapshot.getElectionVotedFor());
            assertEquals("getElectionTerm", 1, snapshot.getElectionTerm());
            assertEquals("getServerConfiguration", new HashSet<>(expectedServerConfig.getServerConfig()),
                    new HashSet<>(snapshot.getServerConfiguration().getServerConfig()));
        });

        return actor;
    }


    private TestActorRef<MockRaftActor> doTestSnapshotAfterStartupWithMigratedMessage(String id, boolean persistent,
            Consumer<Snapshot> snapshotVerifier) {
        InMemorySnapshotStore.addSnapshotSavedLatch(id);
        InMemoryJournal.addDeleteMessagesCompleteLatch(id);
        DefaultConfigParamsImpl config = new DefaultConfigParamsImpl();
        config.setCustomRaftPolicyImplementationClass(DisableElectionsRaftPolicy.class.getName());

        RaftActorSnapshotCohort snapshotCohort = new RaftActorSnapshotCohort() {
            @Override
            public void createSnapshot(ActorRef actorRef) {
                actorRef.tell(new CaptureSnapshotReply(new byte[0]), actorRef);
            }

            @Override
            public void applySnapshot(byte[] snapshotBytes) {
            }
        };

        TestActorRef<MockRaftActor> raftActorRef = factory.createTestActor(MockRaftActor.builder().id(id).
                config(config).snapshotCohort(snapshotCohort).persistent(Optional.of(persistent)).props().
                    withDispatcher(Dispatchers.DefaultDispatcherId()), id);
        MockRaftActor mockRaftActor = raftActorRef.underlyingActor();

        mockRaftActor.waitForRecoveryComplete();

        Snapshot snapshot = InMemorySnapshotStore.waitForSavedSnapshot(id, Snapshot.class);
        snapshotVerifier.accept(snapshot);

        InMemoryJournal.waitForDeleteMessagesComplete(id);

        assertEquals("InMemoryJournal size", 0, InMemoryJournal.get(id).size());

        return raftActorRef;
    }

}
