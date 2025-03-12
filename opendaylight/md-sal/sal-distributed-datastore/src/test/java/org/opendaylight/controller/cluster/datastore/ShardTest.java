/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;
import static org.apache.pekko.actor.ActorRef.noSender;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.japi.Creator;
import org.apache.pekko.japi.Procedure;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.apache.pekko.testkit.TestActorRef;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ClosedTransactionException;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ConnectClientRequest;
import org.opendaylight.controller.cluster.access.commands.CreateLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.DestroyLocalHistoryRequest;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.NotLeaderException;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionFailure;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.api.SchemaValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;

public class ShardTest extends AbstractShardTest {
    private static final String DUMMY_DATA = "Dummy data as snapshot sequence number is set to 0 in "
            + "InMemorySnapshotStore and journal recovery seq number will start from 1";

    @Test
    public void testRegisterDataTreeChangeListener() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testRegisterDataTreeChangeListener");

        ShardTestKit.waitUntilLeader(shard);

        shard.tell(new UpdateSchemaContext(SchemaContextHelper.full()), noSender());

        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener,
            TestModel.TEST_PATH), "testRegisterDataTreeChangeListener-DataTreeChangeListener");

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, false), testKit.getRef());

        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(3),
            RegisterDataTreeNotificationListenerReply.class);
        final String replyPath = reply.getListenerRegistrationPath().toString();
        assertTrue("Incorrect reply path: " + replyPath,
            replyPath.matches("pekko:\\/\\/test\\/user\\/testRegisterDataTreeChangeListener\\/\\$.*"));

        final YangInstanceIdentifier path = TestModel.TEST_PATH;
        writeToStore(shard, path, TestModel.EMPTY_TEST);

        listener.waitForChangeEvents();
        listener.verifyOnInitialDataEvent();

        final MockDataTreeChangeListener listener2 = new MockDataTreeChangeListener(1);
        final ActorRef dclActor2 = actorFactory.createActor(DataTreeChangeListenerActor.props(listener2,
            TestModel.TEST_PATH), "testRegisterDataTreeChangeListener-DataTreeChangeListener2");

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor2, false), testKit.getRef());

        testKit.expectMsgClass(Duration.ofSeconds(3), RegisterDataTreeNotificationListenerReply.class);

        listener2.waitForChangeEvents();
        listener2.verifyNoOnInitialDataEvent();
    }

    @Test
    public void testDataTreeChangeListenerNotifiedWhenNotTheLeaderOnRegistration() throws Exception {
        final CountDownLatch onFirstElectionTimeout = new CountDownLatch(1);
        final CountDownLatch onChangeListenerRegistered = new CountDownLatch(1);
        final Creator<Shard> creator = new Creator<>() {
            @java.io.Serial
            private static final long serialVersionUID = 1L;

            boolean firstElectionTimeout = true;

            @Override
            public Shard create() {
                return new Shard(stateDir(), newShardBuilder()) {
                    @Override
                    public void handleCommand(final Object message) {
                        if (message instanceof ElectionTimeout && firstElectionTimeout) {
                            firstElectionTimeout = false;
                            final ActorRef self = self();
                            new Thread(() -> {
                                Uninterruptibles.awaitUninterruptibly(onChangeListenerRegistered, 5, TimeUnit.SECONDS);
                                self.tell(message, self);
                            }).start();

                            onFirstElectionTimeout.countDown();
                        } else {
                            super.handleCommand(message);
                        }
                    }
                };
            }
        };

        setupInMemorySnapshotStore();

        final YangInstanceIdentifier path = TestModel.TEST_PATH;
        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener, path),
                "testDataTreeChangeListenerNotifiedWhenNotTheLeaderOnRegistration-DataChangeListener");

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
                new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testDataTreeChangeListenerNotifiedWhenNotTheLeaderOnRegistration");

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        assertTrue("Got first ElectionTimeout", onFirstElectionTimeout.await(5, TimeUnit.SECONDS));

        shard.tell(new RegisterDataTreeChangeListener(path, dclActor, false), testKit.getRef());
        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(5),
            RegisterDataTreeNotificationListenerReply.class);
        assertNotNull("getListenerRegistratioznPath", reply.getListenerRegistrationPath());

        shard.tell(FindLeader.INSTANCE, testKit.getRef());
        final FindLeaderReply findLeadeReply = testKit.expectMsgClass(Duration.ofSeconds(5), FindLeaderReply.class);
        assertFalse("Expected the shard not to be the leader", findLeadeReply.getLeaderActor().isPresent());

        onChangeListenerRegistered.countDown();

        // TODO: investigate why we do not receive data chage events
        listener.waitForChangeEvents();
    }

    @Test
    public void testPeerAddressResolved() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final ShardIdentifier peerID = ShardIdentifier.create("inventory", MemberName.forName("member-2"),
                "config");
        final TestActorRef<Shard> shard = actorFactory.createTestActor(newShardBuilder()
            .peerAddresses(Collections.singletonMap(peerID.toString(), null))
            .props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()), "testPeerAddressResolved");

        final String address = "pekko://foobar";
        shard.tell(new PeerAddressResolved(peerID.toString(), address), noSender());

        shard.tell(GetOnDemandRaftState.INSTANCE, testKit.getRef());
        final OnDemandRaftState state = testKit.expectMsgClass(OnDemandRaftState.class);
        assertEquals("getPeerAddress", address, state.getPeerAddresses().get(peerID.toString()));
    }

    @Test
    public void testApplySnapshot() throws Exception {

        final TestActorRef<Shard> shard = actorFactory.createTestActor(newShardProps()
                .withDispatcher(Dispatchers.DefaultDispatcherId()), "testApplySnapshot");

        ShardTestKit.waitUntilLeader(shard);

        final DataTree store = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SCHEMA_CONTEXT);

        final ContainerNode container = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(TestModel.outerNode(1))
            .build();

        writeToStore(store, TestModel.TEST_PATH, container);

        final YangInstanceIdentifier root = YangInstanceIdentifier.of();
        final NormalizedNode expected = readStore(store, root);

        final Snapshot snapshot = Snapshot.create(new ShardSnapshotState(new MetadataShardDataTreeSnapshot(expected)),
                List.of(), 1, 2, 3, 4, new TermInfo(-1, null), null);

        shard.underlyingActor().getRaftActorContext().getSnapshotManager().applyFromRecovery(snapshot);

        await().atMost(Duration.ofSeconds(5)).pollDelay(Duration.ofMillis(75))
            .untilAsserted(() -> assertEquals("Root node", expected, readStore(shard, root)));
    }

    @Test
    public void testApplyState() throws Exception {
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testApplyState");

        ShardTestKit.waitUntilLeader(shard);

        final DataTree store = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SCHEMA_CONTEXT);

        final DataTreeModification writeMod = store.takeSnapshot().newModification();
        final ContainerNode node = TestModel.EMPTY_TEST;
        writeMod.write(TestModel.TEST_PATH, node);
        writeMod.ready();

        shard.underlyingActor().applyState(null, null, payloadForModification(store, writeMod, nextTransactionId()));

        await().atMost(Duration.ofSeconds(5)).pollDelay(Duration.ofMillis(75))
            .untilAsserted(() -> assertEquals("Applied state", node, readStore(shard, TestModel.TEST_PATH)));
    }

    @Test
    public void testDataTreeCandidateRecovery() throws Exception {
        // Set up the InMemorySnapshotStore.
        final DataTree source = setupInMemorySnapshotStore();

        final DataTreeModification writeMod = source.takeSnapshot().newModification();
        writeMod.write(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST);
        writeMod.ready();
        InMemoryJournal.addEntry(shardID.toString(), 0, DUMMY_DATA);

        // Set up the InMemoryJournal.
        InMemoryJournal.addEntry(shardID.toString(), 1, new SimpleReplicatedLogEntry(0, 1,
            payloadForModification(source, writeMod, nextTransactionId())));

        final int nListEntries = 16;
        final var listEntryKeys = new HashSet<Integer>();

        // Add some ModificationPayload entries
        for (int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(i);

            final var mod = source.takeSnapshot().newModification();
            mod.merge(TestModel.outerEntryPath(i), TestModel.outerEntry(i));
            mod.ready();

            InMemoryJournal.addEntry(shardID.toString(), i + 1, new SimpleReplicatedLogEntry(i, 1,
                payloadForModification(source, mod, nextTransactionId())));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 2,
            new ApplyJournalEntries(nListEntries));

        testRecovery(listEntryKeys, true);
    }

    @Test
    public void testConcurrentThreePhaseCommits() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testConcurrentThreePhaseCommits");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // asyncRequest()s only start
        final var tx1 = nextTransactionId();
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // start canCommit for the first Tx
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // ready 2 more Tx's
        final var tx2 = nextTransactionId();
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.outerEntryPath(1), TestModel.outerEntry(1))
            .setReady()
            .build());
        final var tx3 = nextTransactionId();
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx3, self)
            .setSequence(0)
            .addWrite(TestModel.outerEntryPath(2), TestModel.outerEntry(2))
            .setReady()
            .build());

        // preCommit tx1, which starts the queue
        connection.asyncRequest(self -> new TransactionPreCommitRequest(tx1, 2, self));

        // canCommit tx2/tx3 in order
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(true)
            .build());
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx3, self)
            .setSequence(1)
            .setCommit(true)
            .build());
        connection.asyncRequest(self -> new TransactionPreCommitRequest(tx2, 2, self));
        connection.asyncRequest(self -> new TransactionPreCommitRequest(tx3, 2, self));

        // commit the first Tx. After it completes, it should trigger the 2nd Tx to proceed which should in turn then
        // trigger the 3rd.
        connection.asyncRequest(self -> new TransactionDoCommitRequest(tx1, 3, self));
        connection.asyncRequest(self -> new TransactionDoCommitRequest(tx2, 3, self));
        connection.asyncRequest(self -> new TransactionDoCommitRequest(tx3, 3, self));

        // no more requests past this point, verify responses
        final var ready1 = connection.assertResponse(ModifyTransactionSuccess.class);
        assertEquals(tx1, ready1.getTarget());
        final var can1 = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx1, can1.getTarget());
        final var ready2 = connection.assertResponse(ModifyTransactionSuccess.class);
        assertEquals(tx2, ready2.getTarget());
        final var ready3 = connection.assertResponse(ModifyTransactionSuccess.class);
        assertEquals(tx3, ready3.getTarget());
        final var pre1 = connection.assertResponse(TransactionPreCommitSuccess.class);
        assertEquals(tx1, pre1.getTarget());
        final var can2 = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx2, can2.getTarget());
        final var pre2 = connection.assertResponse(TransactionPreCommitSuccess.class);
        assertEquals(tx2, pre2.getTarget());
        final var can3 = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx3, can3.getTarget());
        final var pre3 = connection.assertResponse(TransactionPreCommitSuccess.class);
        assertEquals(tx3, pre3.getTarget());
        final var commit1 = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx1, commit1.getTarget());
        final var commit2 = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx2, commit2.getTarget());
        final var commit3 = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx3, commit3.getTarget());

        // Verify data in the data store.
        verifyOuterListEntry(shard, 1);
        verifyOuterListEntry(shard, 2);
        verifyLastApplied(shard, 2);
    }

    @Test
    public void testBatchedModificationsWithNoCommitOnReady() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsWithNoCommitOnReady");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // send a modification to start a transaction
        final var tx = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .build());

        // send a couple more modifications
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(1)
            .addWrite(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST)
            .build());
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(2)
            .addWrite(TestModel.outerEntryPath(1), TestModel.outerEntry(1))
            .setReady()
            .build());

        // start canCommit
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(3)
            .setCommit(true)
            .build());

        // Send the (Pre)CommitTransaction messages.
        connection.request(TransactionPreCommitSuccess.class, self -> new TransactionPreCommitRequest(tx, 4, self));
        connection.request(TransactionCommitSuccess.class, self -> new TransactionDoCommitRequest(tx, 5, self));

        // Verify data in the data store.
        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());
        verifyOuterListEntry(shard, 1);
    }

    @Test
    public void testBatchedModificationsWithCommitOnReady() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsWithCommitOnReady");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Send a BatchedModifications to start a transaction.
        final var tx = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .build());

        // Send a couple more BatchedModifications.
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(1)
            .addWrite(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST)
            .build());
        connection.request(TransactionCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(2)
            .addWrite(TestModel.outerEntryPath(1), TestModel.outerEntry(1))
            .setCommit(false)
            .build());

        // Verify data in the data store.
        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());
        verifyOuterListEntry(shard, 1);
    }

    @Test
    public void testFirstModificationWithOperationFailure() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testFirstModificationWithOperationFailure");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Test merge with invalid data. An exception should occur when the merge is applied. Note that write will not
        // validate the children for performance reasons.

        final var tx = nextTransactionId();
        final var invalidData = ImmutableNodes.newContainerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            // invalid leaf
            .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
            .build();

        // TODO: this test case tests failure on the first message, we should have a duplicate doing the same on the
        //       second message, when the transaction is established on the backend
        //       (but see testCanCommitPhaseFailure(), which fails on the second message via a different mechanism)
        final var first = connection.request(TransactionFailure.class,
            self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(0)
                .addMerge(TestModel.TEST_PATH, invalidData)
                .build());
        assertTrue(first.isHardFailure());
        final var cause = assertInstanceOf(SchemaValidationFailedException.class, first.getCause().unwrap());
        assertEquals("""
            Node (urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:junk?revision=2014-03-13)junk is not \
            a valid child of \
            (urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test according \
            to the schema.""", cause.getMessage());

        // FIXME: invalid operation should be properly accounted for:
        //      assertEquals(1, shard.underlyingActor().getShardMBean().getFailedTransactionsCount());

        // Failure occurred on the first message, hence no state has been established -- if we have pipelined, we will
        // should get a sequencing smack
        final var second = connection.request(TransactionFailure.class,
            self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(1)
                .setReady()
                .build());
        final var ex = assertInstanceOf(OutOfOrderRequestException.class, second.getCause());
        assertEquals("Expecting request 0", ex.getMessage());
    }

    @Test
    public void testBatchedModificationsOnTransactionChain() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsOnTransactionChain");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // create a new history
        final var historyId = nextHistoryId();
        connection.request(LocalHistorySuccess.class, self -> new CreateLocalHistoryRequest(historyId, self));

        // start a chained write transaction and ready it.
        final var tx1 = new TransactionIdentifier(historyId, 0);
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addMerge(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // the next read transaction should observe the state being present
        final var tx2 = new TransactionIdentifier(historyId, 1);
        final var read = connection.request(ReadTransactionSuccess.class,
            self -> new ReadTransactionRequest(tx2, 0, self, TestModel.TEST_PATH, true));
        assertEquals(Optional.of(TestModel.EMPTY_TEST), read.getData());

        // Commit the write transaction.
        connection.request(TransactionCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(false)
            .build());

        // Abort the read transaction
        connection.request(ModifyTransactionSuccess.class,
            self -> ModifyTransactionRequest.builder(tx2, self).setSequence(1).setAbort().build());

        // Close the history
        connection.request(LocalHistorySuccess.class, self -> new DestroyLocalHistoryRequest(historyId, 1, self));

        // Verify data in the data store.
        assertEquals(TestModel.EMPTY_TEST, readStore(shard, TestModel.TEST_PATH));
    }

    @Test
    public void testConnectToNotLeader() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        dataStoreContextBuilder
            .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName())
            .shardHeartbeatIntervalInMillis(50)
            .shardElectionTimeoutFactor(1);
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionMessagesWithNoLeader");

        testKit.waitUntilNoLeader(shard);

        shard.tell(new ConnectClientRequest(CLIENT_ID, testKit.getRef(), ABIVersion.current(), ABIVersion.current()),
            noSender());
        var failure = testKit.expectMsgClass(Failure.class);
        assertInstanceOf(NotLeaderException.class, failure.cause());
    }

    @Test
    public void testReadyWithImmediateCommitLocal() {
        testReadyWithImmediateCommit(true);
    }

    @Test
    public void testReadyWithImmediateCommitRemote() {
        testReadyWithImmediateCommit(false);
    }

    private void testReadyWithImmediateCommit(final boolean local) {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyWithImmediateCommit-" + local);

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx = nextTransactionId();
        if (local) {
            final var mod = shard.underlyingActor().getDataStore().newModification();
            mod.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            mod.ready();
            connection.asyncRequest(self -> new CommitLocalTransactionRequest(tx, 0, self, mod, null, false));
        } else {
            connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());
        }

        connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        assertEquals(TestModel.EMPTY_TEST, readStore(shard, TestModel.TEST_PATH));
    }

    @Test
    public void testReadyLocalTransactionWithImmediateCommit() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyLocalTransactionWithImmediateCommit");

        ShardTestKit.waitUntilLeader(shard);

        final var modification = shard.underlyingActor().getDataStore().newModification();
        final var writeData = TestModel.EMPTY_TEST;
        modification.write(TestModel.TEST_PATH, writeData);
        final var mergeData = TestModel.outerNode(42);
        modification.merge(TestModel.OUTER_LIST_PATH, mergeData);
        modification.ready();

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx = nextTransactionId();
        connection.request(TransactionCommitSuccess.class,
            self -> new CommitLocalTransactionRequest(tx, 0, self, modification, null, false));

        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        assertEquals(mergeData, readStore(shard, TestModel.OUTER_LIST_PATH));
    }

    @Test
    public void testReadyLocalTransactionWithThreePhaseCommit() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyLocalTransactionWithThreePhaseCommit");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        final var tx = nextTransactionId();
        final var modification = shard.underlyingActor().getDataStore().newModification();
        final var writeData = TestModel.EMPTY_TEST;
        modification.write(TestModel.TEST_PATH, writeData);
        final var mergeData = TestModel.outerNode(42);
        modification.merge(TestModel.OUTER_LIST_PATH, mergeData);
        modification.ready();

        connection.request(TransactionCanCommitSuccess.class,
            self -> new CommitLocalTransactionRequest(tx, 0, self, modification, null, true));

        // Send preCommit message.
        connection.request(TransactionPreCommitSuccess.class, self -> new TransactionPreCommitRequest(tx, 1, self));

        // Send the doCommit message.
        connection.request(TransactionCommitSuccess.class, self -> new TransactionDoCommitRequest(tx, 2, self));

        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        assertEquals(mergeData, readStore(shard, TestModel.OUTER_LIST_PATH));
    }

    @Test
    public void testReadWriteCommitWithPersistenceDisabled() {
        dataStoreContextBuilder.persistent(false);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitWithPersistenceDisabled");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Setup a simulated transactions with a mock cohort.
        final var tx = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class,
            self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setReady()
                .build());

        // initiate three-phase commit: can-commit
        connection.request(TransactionCanCommitSuccess.class,
            self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(1)
                .setCommit(true)
                .build());
        // pre-commit
        connection.request(TransactionPreCommitSuccess.class,
            self -> new TransactionPreCommitRequest(tx, 2, self));
        // commit
        connection.request(TransactionCommitSuccess.class,
            self -> new TransactionDoCommitRequest(tx, 3, self));

        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());
        assertEquals(TestModel.EMPTY_TEST, readStore(shard, TestModel.TEST_PATH));
    }

    @Test
    public void testLocalCommitWhenTransactionHasModifications() throws Exception {
        testCommitWhenTransactionHasModifications(true);
    }

    @Test
    public void testRemoteCommitWhenTransactionHasModifications() throws Exception {
        testCommitWhenTransactionHasModifications(false);
    }

    // FIXME: @ParameterizedTest when on JUnit5
    private void testCommitWhenTransactionHasModifications(final boolean local) throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitWhenTransactionHasModifications-" + local);

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx = nextTransactionId();
        if (local) {
            final var mod = shard.underlyingActor().getDataStore().newModification();
            mod.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            mod.ready();
            connection.asyncRequest(self -> new CommitLocalTransactionRequest(tx, 0, self, mod, null, false));
        } else {
            connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());
        }

        connection.assertResponse(TransactionCommitSuccess.class);

        final var inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));
        inOrder.verify(dataTree).commit(any(DataTreeCandidate.class));

        // Use MBean for verification
        shard.tell(Shard.GET_SHARD_MBEAN_MESSAGE, testKit.getRef());
        final var shardStats = testKit.expectMsgClass(Duration.ofSeconds(5), ShardStatsMXBean.class);
        // Committed transaction count should increase as usual
        assertEquals(1, shardStats.getCommittedTransactionsCount());
        // Commit index should not move
        assertEquals(0, shardStats.getCommitIndex());
    }

    @Test
    public void testCommitPhaseFailure() throws Exception {
        final var dataTree = createDelegatingMockDataTree();
        final var commitEx = new RuntimeException("mock commit failure");
        doThrow(commitEx).when(dataTree).commit(any(DataTreeCandidate.class));

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Setup 2 simulated transactions with mock cohorts. The first one fails in the commit phase.
        final var tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());
        final var tx2 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            // Note: different subtree, otherwise canCommit would fail after tx1 preCommit on conflict
            .addWrite(TestModel.TEST2_PATH, TestModel.EMPTY_TEST2)
            .setReady()
            .build());

        // start canCommit of first Tx, it should be enqueued into the commit queue
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // start canCommit of second Tx, this should get queued and processed after the first Tx completes
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // Send the CommitTransaction message for the first Tx. This should send back an error and trigger the 2nd Tx to
        // proceed.
        final var success1 = connection.request(TransactionPreCommitSuccess.class,
            self -> new TransactionPreCommitRequest(tx1, 2, self));
        assertEquals(tx1, success1.getTarget());

        // 2nd tx succeeds canCommit
        final var success = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx2, success.getTarget());

        final var failure = connection.request(TransactionFailure.class,
            self -> new TransactionDoCommitRequest(tx1, 3, self));
        assertEquals(tx1, failure.getTarget());
        assertSame(commitEx, failure.getCause().unwrap());

        assertEquals(1, shard.underlyingActor().getShardMBean().getFailedTransactionsCount());
        assertEquals(1, shard.underlyingActor().getShardMBean().getPendingTxCommitQueueSize());
        assertEquals(0, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        final var inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));

        // FIXME: this invocation is done on the result of validate(). To test it, we need to make sure mock
        //        validate performs wrapping and we capture that mock
        // inOrder.verify(dataTree).validate(any(DataTreeModification.class));

        inOrder.verify(dataTree).commit(any(DataTreeCandidate.class));
    }

    @Test
    public void testPreCommitPhaseFailure() throws Exception {
        final var dataTree = createDelegatingMockDataTree();
        final var prepareEx = new RuntimeException("mock preCommit failure");
        doThrow(prepareEx).when(dataTree).prepare(any(DataTreeModification.class));

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testPreCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        final var tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());
        final var tx2 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // start canCommit for the first Tx.
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // start canCommit for the 2nd Tx. This should get queued and processed after the first Tx completes.
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // Send the CommitTransaction message for the first Tx. This should send back an error and trigger the 2nd Tx
        // to proceed.
        final var failure = connection.request(TransactionFailure.class,
            self -> new TransactionPreCommitRequest(tx1, 2, self));
        assertEquals(tx1, failure.getTarget());
        assertSame(prepareEx, failure.getCause().unwrap());

        final var success = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx2, success.getTarget());

        final InOrder inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
    }

    @Test
    public void testCanCommitPhaseFailure() throws Exception {
        final var dataTree = createDelegatingMockDataTree();
        final var validateEx = new DataValidationFailedException(YangInstanceIdentifier.of(), "mock canCommit failure");
        doThrow(validateEx).doNothing().when(dataTree).validate(any(DataTreeModification.class));

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCanCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // TODO: this tests the failure on second message, we should have the equivalent for the first one as well
        //       (but see testFirstModificationWithOperationFailure(), which fails on the first message via a different
        //       mechanism)
        final var tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // start canCommiit
        final var failure = connection.request(TransactionFailure.class,
            self -> ModifyTransactionRequest.builder(tx1, self)
                .setSequence(1)
                .setCommit(true)
                .build());
        assertTrue(failure.isHardFailure());
        final var cause = assertInstanceOf(TransactionCommitFailedException.class, failure.getCause().unwrap());
        assertEquals("Data did not pass validation for path /", cause.getMessage());
        assertSame(validateEx, cause.getCause());

        // next transaction should be just fine
        connection.request(TransactionCommitSuccess.class,
            self -> ModifyTransactionRequest.builder(nextTransactionId(), self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());

        assertEquals(TestModel.EMPTY_TEST, readStore(shard, TestModel.TEST_PATH));
    }

    @Test
    public void testImmediateCommitWithCanCommitPhaseFailureLocal() throws Exception {
        testImmediateCommitWithCanCommitPhaseFailure(true);
    }

    @Test
    public void testImmediateCommitWithCanCommitPhaseFailureRemote() throws Exception {
        testImmediateCommitWithCanCommitPhaseFailure(false);
    }

    // FIXME: @ParameterizedTest when we have JUnit5
    private void testImmediateCommitWithCanCommitPhaseFailure(final boolean local) throws Exception {
        final var dataTree = createDelegatingMockDataTree();
        final var validateEx = new DataValidationFailedException(YangInstanceIdentifier.of(), "mock canCommit failure");
        doThrow(validateEx).doNothing().when(dataTree).validate(any(DataTreeModification.class));

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testImmediateCommitWithCanCommitPhaseFailure-" + local);

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx1 = nextTransactionId();
        if (local) {
            final var mod = shard.underlyingActor().getDataStore().newModification();
            mod.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            mod.ready();
            connection.asyncRequest(self -> new CommitLocalTransactionRequest(tx1, 0, self, mod, null, false));
        } else {
            connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx1, self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());
        }

        final var failure = connection.assertResponse(TransactionFailure.class);
        final var ex = assertInstanceOf(RuntimeRequestException.class, failure.getCause());
        assertEquals("CanCommit failed", ex.getMessage());
        final var cause = assertInstanceOf(TransactionCommitFailedException.class, ex.getCause());
        assertSame(validateEx, cause.getCause());

        // Send another can commit to ensure the failed one got cleaned up.
        final var tx2 = nextTransactionId();
        if (local) {
            final var mod = shard.underlyingActor().getDataStore().newModification();
            mod.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
            mod.ready();
            connection.asyncRequest(self -> new CommitLocalTransactionRequest(tx2, 0, self, mod, null, false));
        } else {
            connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());
        }

        final var success = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx2, success.getTarget());
    }

    @Test
    public void testAbortWithCommitPending() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final Creator<Shard> creator = () -> new Shard(stateDir(), newShardBuilder());

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
            new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testAbortWithCommitPending");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(1)
            .setCommit(true)
            .build());
        connection.request(TransactionPreCommitSuccess.class,
            self -> new TransactionPreCommitRequest(tx, 2, self));

        // Abort after pre-commit ...
        connection.asyncRequest(self -> new TransactionAbortRequest(tx, 3, self));
        // ... and also try co-commit before pre-commit returns
        connection.asyncRequest(self -> new TransactionDoCommitRequest(tx, 4, self));

        // wait for TransactionAbortSuccess
        connection.assertResponse(TransactionAbortSuccess.class);
        // wait for do-commit, it will fail
        final var commit = connection.assertResponse(TransactionFailure.class);
        assertInstanceOf(ClosedTransactionException.class, commit.getCause());
        assertTrue(commit.isHardFailure());

        assertEquals(0, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());
        assertEquals(1, shard.underlyingActor().getShardMBean().getAbortTransactionsCount());
        assertNull(readStore(shard, TestModel.TEST_PATH));
    }

    @Test
    public void testTransactionCommitTimeout() throws Exception {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitTimeout");

        ShardTestKit.waitUntilLeader(shard);

        writeToStore(shard, TestModel.TEST_PATH, TestModel.EMPTY_TEST);
        writeToStore(shard, TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST);

        // Ready 2 Tx's - the first will timeout
        final var connection = testKit.connect(shard, CLIENT_ID);

        final TransactionIdentifier tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.outerEntryPath(1), TestModel.outerEntry(1))
            .setReady()
            .build());

        final TransactionIdentifier tx2 = nextTransactionId();
        final var listNodePath = TestModel.outerEntryPath(2);
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(listNodePath, TestModel.outerEntry(2))
            .setReady()
            .build());

        // canCommit 1st Tx. We don't send the commit so it should timeout.
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // uncoordinated commit the 2nd Tx - it should complete after the 1st Tx times out.
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(false)
            .build());

        assertEquals(tx1, connection.assertResponse(TransactionCanCommitSuccess.class).getTarget());
        assertEquals(tx2, connection.assertResponse(TransactionCommitSuccess.class).getTarget());
        assertEquals(0, shard.underlyingActor().getShardMBean().getFailedTransactionsCount());
        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        final var failure = connection.request(TransactionFailure.class,
            self -> new TransactionPreCommitRequest(tx1, 2, self));
        assertEquals(tx1, failure.getTarget());
        assertTrue(failure.isHardFailure());
        final var cause = assertInstanceOf(RuntimeRequestException.class, failure.getCause());
        assertEquals("Precommit failed", cause.getMessage());
        final var unwrapped = assertInstanceOf(TimeoutException.class, cause.unwrap());
        assertThat(unwrapped.getMessage(), startsWith("Backend timeout in state CAN_COMMIT_COMPLETE after "));
        assertEquals(1, shard.underlyingActor().getShardMBean().getFailedTransactionsCount());
        assertEquals(1, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());

        assertEquals(TestModel.outerEntry(2), readStore(shard, listNodePath));
    }

    @Test
    @Ignore
    public void testTransactionCommitQueueCapacityExceeded() {
        dataStoreContextBuilder.shardTransactionCommitQueueCapacity(2);

//        new ShardTestKit(getSystem()) {{
//            final TestActorRef<Shard> shard = actorFactory.createTestActor(
//                    newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
//                    "testTransactionCommitQueueCapacityExceeded");
//
//            waitUntilLeader(shard);
//
//            final FiniteDuration duration = duration("5 seconds");
//
//            final ShardDataTree dataStore = shard.underlyingActor().getDataStore();
//
//            final TransactionIdentifier transactionID1 = nextTransactionId();
//            final MutableCompositeModification modification1 = new MutableCompositeModification();
//            final ShardDataTreeCohort cohort1 = setupMockWriteTransaction("cohort1", dataStore,
//                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), transactionID1,
//                    modification1);
//
//            final TransactionIdentifier transactionID2 = nextTransactionId();
//            final MutableCompositeModification modification2 = new MutableCompositeModification();
//            final ShardDataTreeCohort cohort2 = setupMockWriteTransaction("cohort2", dataStore,
//                    TestModel.OUTER_LIST_PATH,
//                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), transactionID2,
//                    modification2);
//
//            final TransactionIdentifier transactionID3 = nextTransactionId();
//            final MutableCompositeModification modification3 = new MutableCompositeModification();
//            final ShardDataTreeCohort cohort3 = setupMockWriteTransaction("cohort3", dataStore,
//                    TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME), transactionID3,
//                    modification3);
//
//            // Ready the Tx's
//
//            shard.tell(prepareReadyTransactionMessage(false, shard.underlyingActor(), cohort1, transactionID1,
//                    modification1), getRef());
//            expectMsgClass(duration, ReadyTransactionReply.class);
//
//            shard.tell(prepareReadyTransactionMessage(false, shard.underlyingActor(), cohort2, transactionID2,
//                    modification2), getRef());
//            expectMsgClass(duration, ReadyTransactionReply.class);
//
//            // The 3rd Tx should exceed queue capacity and fail.
//
//            shard.tell(prepareReadyTransactionMessage(false, shard.underlyingActor(), cohort3, transactionID3,
//                    modification3), getRef());
//            expectMsgClass(duration, Failure.class);
//
//            // canCommit 1st Tx.
//
//            shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), getRef());
//            expectMsgClass(duration, CanCommitTransactionReply.class);
//
//            // canCommit the 2nd Tx - it should get queued.
//
//            shard.tell(new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), getRef());
//
//            // canCommit the 3rd Tx - should exceed queue capacity and fail.
//
//            shard.tell(new CanCommitTransaction(transactionID3, CURRENT_VERSION).toSerializable(), getRef());
//            expectMsgClass(duration, Failure.class);
//        }};
    }

    @Test
    public void testTransactionCommitWithPriorExpiredCohortEntries() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitWithPriorExpiredCohortEntries");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        final var tx1 = nextTransactionId();
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setCommit(true)
            .build());
        final var tx2 = nextTransactionId();
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setCommit(true)
            .build());
        final var tx3 = nextTransactionId();
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx3, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setCommit(true)
            .build());

        // All Tx's are readied. We'll send preCommit for the last one but not the others. The others should expire from
        // the queue and the last one should be processed.
        // TODO: we really should do more assertions here -- are we sure those transactions needed to time out so we get
        //       here?
        final var success = connection.request(TransactionPreCommitSuccess.class,
            self -> new TransactionPreCommitRequest(tx3, 1, self));
        assertEquals(tx3, success.getTarget());
    }

    @Test
    public void testTransactionCommitWithSubsequentExpiredCohortEntry() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitWithSubsequentExpiredCohortEntry");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // CanCommit + PreCommit the first Tx so it's the current in-progress Tx.
        final var tx1 = nextTransactionId();
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setCommit(true)
            .build());
        connection.request(TransactionPreCommitSuccess.class, self -> new TransactionPreCommitRequest(tx1, 1, self));

        // Ready the second Tx.
        final var tx2 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // Ready the third Tx.
        final var tx3 = nextTransactionId();
        final var mod3 = shard.underlyingActor().getDataStore().newModification();
        mod3.write(TestModel.TEST2_PATH, TestModel.EMPTY_TEST2);
        mod3.ready();
        connection.asyncRequest(self -> new CommitLocalTransactionRequest(tx3, 0, self, mod3, null, false));
        // Commit the first Tx. After completing, the second should expire from the queue and the third Tx committed.
        connection.asyncRequest(self -> new TransactionDoCommitRequest(tx1, 2, self));

        // Expect commit reply from the first and the third Tx.
        final var success1 = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx1, success1.getTarget());
        final var success3 = connection.assertResponse(TransactionCommitSuccess.class);
        assertEquals(tx3, success3.getTarget());

        assertEquals(TestModel.EMPTY_TEST2, readStore(shard, TestModel.TEST2_PATH));
    }

    @Test
    public void testPreCommitBeforeReadyFailure() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCanCommitBeforeReadyFailure");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);
        final var tx = nextTransactionId();
        final var failure = connection.request(TransactionFailure.class,
            self -> new TransactionPreCommitRequest(tx, 0, self));
        assertTrue(failure.isHardFailure());
        final var cause = assertInstanceOf(IllegalStateException.class, failure.getCause().unwrap());
        assertEquals(tx + " cannot preCommit in state OPEN", cause.getMessage());
    }

    @Test
    public void testAbortAfterCanCommit() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterCanCommit");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Ready 2 transactions - the first one will be aborted.
        final var tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        final var tx2 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // Issue canCommit for the first Tx.
        connection.request(TransactionCanCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // Send the CanCommitTransaction message for the 2nd Tx.
        // This should get queued and processed after the first Tx completes.
        connection.asyncRequest(self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(true)
            .build());

        // Send the AbortTransaction message for the first Tx. This should trigger the 2nd Tx to proceed.
        connection.asyncRequest(self -> new TransactionAbortRequest(tx1, 2, self));

        final var canCommit = connection.assertResponse(TransactionCanCommitSuccess.class);
        assertEquals(tx2, canCommit.getTarget());
        final var abort = connection.assertResponse(TransactionAbortSuccess.class);
        assertEquals(tx1, abort.getTarget());
    }

    @Test
    public void testAbortAfterReady() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterReady");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Ready a tx.
        final var tx = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());

        // Send the AbortTransaction message.
        connection.request(TransactionAbortSuccess.class, self -> new TransactionAbortRequest(tx, 1, self));
        assertEquals(0, shard.underlyingActor().getPendingTxCommitQueueSize());
        assertEquals(1, shard.underlyingActor().getShardMBean().getAbortTransactionsCount());

        // Now send CanCommitTransaction - should fail.
        final var failure = connection.request(TransactionFailure.class,
            self -> ModifyTransactionRequest.builder(tx, self)
                .setSequence(2)
                .setCommit(true)
                .build());
        final var cause = assertInstanceOf(IllegalStateException.class, failure.getCause().unwrap());
        assertEquals(tx + " cannot become ready in state ABORTED", cause.getMessage());

        // Ready and CanCommit another and verify success.
        connection.request(TransactionCommitSuccess.class,
            self -> ModifyTransactionRequest.builder(nextTransactionId(), self)
                .setSequence(0)
                .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
                .setCommit(false)
                .build());
    }

    @Test
    public void testAbortQueuedTransaction() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterReady");

        ShardTestKit.waitUntilLeader(shard);

        final var connection = testKit.connect(shard, CLIENT_ID);

        // Ready 3 tx's.
        final var tx1 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx1, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());
        final var tx2 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(0)
            .addWrite(TestModel.TEST_PATH, TestModel.EMPTY_TEST)
            .setReady()
            .build());
        final var tx3 = nextTransactionId();
        connection.request(ModifyTransactionSuccess.class, self -> ModifyTransactionRequest.builder(tx3, self)
            .setSequence(0)
            .addWrite(TestModel.OUTER_LIST_PATH, TestModel.EMPTY_OUTER_LIST)
            .setReady()
            .build());

        // Abort the second tx while it's queued.
        connection.request(TransactionAbortSuccess.class, self -> new TransactionAbortRequest(tx1, 1, self));

        // Commit the other 2.
        connection.request(TransactionCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx2, self)
            .setSequence(1)
            .setCommit(false)
            .build());
        connection.request(TransactionCommitSuccess.class, self -> ModifyTransactionRequest.builder(tx3, self)
            .setSequence(1)
            .setCommit(false)
            .build());

        assertEquals(0, shard.underlyingActor().getPendingTxCommitQueueSize());
        assertEquals(1, shard.underlyingActor().getShardMBean().getAbortTransactionsCount());
        assertEquals(2, shard.underlyingActor().getShardMBean().getCommittedTransactionsCount());
    }

    @Test
    public void testCreateSnapshotWithNonPersistentData() throws Exception {
        testCreateSnapshot(false, "testCreateSnapshotWithNonPersistentData");
    }

    @Test
    public void testCreateSnapshot() throws Exception {
        testCreateSnapshot(true, "testCreateSnapshot");
    }

    private void testCreateSnapshot(final boolean persistent, final String shardActorName) throws Exception {
        final var latch = new AtomicReference<>(new CountDownLatch(1));
        final var savedSnapshot = new AtomicReference<>();

        final class TestDataPersistenceProvider implements DataPersistenceProvider {
            private final DataPersistenceProvider delegate;

            TestDataPersistenceProvider(final DataPersistenceProvider delegate) {
                this.delegate = requireNonNull(delegate);
            }

            @Override
            public boolean isRecoveryApplicable() {
                return delegate.isRecoveryApplicable();
            }

            @Override
            public <T> void persist(final T entry, final Procedure<T> procedure) {
                delegate.persist(entry, procedure);
            }

            @Override
            public <T> void persistAsync(final T entry, final Procedure<T> procedure) {
                delegate.persistAsync(entry, procedure);
            }

            @Override
            public void saveSnapshot(final Object entry) {
                savedSnapshot.set(entry);
                delegate.saveSnapshot(entry);
            }

            @Override
            public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
                delegate.deleteSnapshots(criteria);
            }

            @Override
            public void deleteMessages(final long sequenceNumber) {
                delegate.deleteMessages(sequenceNumber);
            }

            @Override
            public long getLastSequenceNumber() {
                return delegate.getLastSequenceNumber();
            }

            @Override
            public boolean handleJournalResponse(final JournalProtocol.Response response) {
                return delegate.handleJournalResponse(response);
            }

            @Override
            public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
                return delegate.handleSnapshotResponse(response);
            }

        }

        dataStoreContextBuilder.persistent(persistent);

        final class TestShard extends Shard {
            TestShard(final Path stateDir, final AbstractBuilder<?, ?> builder) {
                super(stateDir, builder);
                setPersistence(new TestDataPersistenceProvider(super.persistence()));
            }

            @Override
            public void handleCommand(final Object message) {
                super.handleCommand(message);

                // XXX:  commit_snapshot equality check references RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT
                if (message instanceof SaveSnapshotSuccess || "commit_snapshot".equals(message.toString())) {
                    latch.get().countDown();
                }
            }
        }

        final Creator<Shard> creator = () -> new TestShard(stateDir(), newShardBuilder());

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
            new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()), shardActorName);

        ShardTestKit.waitUntilLeader(shard);
        writeToStore(shard, TestModel.TEST_PATH, TestModel.EMPTY_TEST);

        final NormalizedNode expectedRoot = readStore(shard, YangInstanceIdentifier.of());

        // Trigger creation of a snapshot by ensuring
        final RaftActorContext raftActorContext = shard.underlyingActor().getRaftActorContext();
        raftActorContext.getSnapshotManager().capture(mock(ReplicatedLogEntry.class), -1);
        awaitAndValidateSnapshot(latch, savedSnapshot, expectedRoot);

        raftActorContext.getSnapshotManager().capture(mock(ReplicatedLogEntry.class), -1);
        awaitAndValidateSnapshot(latch, savedSnapshot, expectedRoot);
    }

    private static void awaitAndValidateSnapshot(final AtomicReference<CountDownLatch> latch,
            final AtomicReference<Object> savedSnapshot, final NormalizedNode expectedRoot)
                    throws InterruptedException {
        assertTrue("Snapshot saved", latch.get().await(5, TimeUnit.SECONDS));

        assertTrue("Invalid saved snapshot " + savedSnapshot.get(), savedSnapshot.get() instanceof Snapshot);

        verifySnapshot((Snapshot) savedSnapshot.get(), expectedRoot);

        latch.set(new CountDownLatch(1));
        savedSnapshot.set(null);
    }

    private static void verifySnapshot(final Snapshot snapshot, final NormalizedNode expectedRoot) {
        assertEquals("Root node", expectedRoot,
            ((ShardSnapshotState)snapshot.getState()).getSnapshot().getRootNode().orElseThrow());
    }

    /**
     * This test simply verifies that the applySnapShot logic will work.
     */
    @Test
    public void testInMemoryDataTreeRestore() throws DataValidationFailedException {
        final DataTree store = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SCHEMA_CONTEXT);

        final DataTreeModification putTransaction = store.takeSnapshot().newModification();
        putTransaction.write(TestModel.TEST_PATH, TestModel.EMPTY_TEST);
        commitTransaction(store, putTransaction);


        final NormalizedNode expected = readStore(store, YangInstanceIdentifier.of());

        final DataTreeModification writeTransaction = store.takeSnapshot().newModification();

        writeTransaction.delete(YangInstanceIdentifier.of());
        writeTransaction.write(YangInstanceIdentifier.of(), expected);

        commitTransaction(store, writeTransaction);

        assertEquals(expected, readStore(store, YangInstanceIdentifier.of()));
    }

    @Test
    public void testRecoveryApplicable() {

        final DatastoreContext persistentContext = DatastoreContext.newBuilder()
                .shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(true).build();

        final Props persistentProps = Shard.builder().id(shardID).datastoreContext(persistentContext)
                .schemaContextProvider(() -> SCHEMA_CONTEXT).props(stateDir());

        final DatastoreContext nonPersistentContext = DatastoreContext.newBuilder()
                .shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(false).build();

        final Props nonPersistentProps = Shard.builder().id(shardID).datastoreContext(nonPersistentContext)
                .schemaContextProvider(() -> SCHEMA_CONTEXT).props(stateDir());

        final TestActorRef<Shard> shard1 = actorFactory.createTestActor(persistentProps, "testPersistence1");

        assertTrue("Recovery Applicable", shard1.underlyingActor().isRecoveryApplicable());

        final TestActorRef<Shard> shard2 = actorFactory.createTestActor(nonPersistentProps, "testPersistence2");

        assertFalse("Recovery Not Applicable", shard2.underlyingActor().isRecoveryApplicable());
    }

    @Test
    public void testOnDatastoreContext() {
        dataStoreContextBuilder.persistent(true);

        final TestActorRef<Shard> shard = actorFactory.createTestActor(newShardProps(), "testOnDatastoreContext");

        assertTrue("isRecoveryApplicable", shard.underlyingActor().isRecoveryApplicable());

        ShardTestKit.waitUntilLeader(shard);

        shard.tell(dataStoreContextBuilder.persistent(false).build(), noSender());

        assertFalse("isRecoveryApplicable", shard.underlyingActor().isRecoveryApplicable());

        shard.tell(dataStoreContextBuilder.persistent(true).build(), noSender());

        assertTrue("isRecoveryApplicable", shard.underlyingActor().isRecoveryApplicable());
    }

    @Test
    public void testRegisterRoleChangeListener() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testRegisterRoleChangeListener");

        ShardTestKit.waitUntilLeader(shard);

        final ActorRef listener = getSystem().actorOf(MessageCollectorActor.props());

        shard.tell(new RegisterRoleChangeListener(), listener);

        MessageCollectorActor.expectFirstMatching(listener, RegisterRoleChangeListenerReply.class);

        ShardLeaderStateChanged leaderStateChanged = MessageCollectorActor.expectFirstMatching(listener,
            ShardLeaderStateChanged.class);
        final var dataTree = leaderStateChanged.localShardDataTree();
        assertNotNull("getLocalShardDataTree present", dataTree);
        assertSame("getLocalShardDataTree", shard.underlyingActor().getDataStore().getDataTree(), dataTree);

        MessageCollectorActor.clearMessages(listener);

        // Force a leader change

        shard.tell(new RequestVote(10000, "member2", 50, 50), testKit.getRef());

        leaderStateChanged = MessageCollectorActor.expectFirstMatching(listener, ShardLeaderStateChanged.class);
        assertNull("getLocalShardDataTree present", leaderStateChanged.localShardDataTree());
    }

    @Test
    public void testFollowerInitialSyncStatus() {
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testFollowerInitialSyncStatus");

        shard.underlyingActor().handleNonRaftCommand(new FollowerInitialSyncUpStatus(false,
                "member-1-shard-inventory-operational"));

        assertFalse(shard.underlyingActor().getShardMBean().getFollowerInitialSyncStatus());

        shard.underlyingActor().handleNonRaftCommand(new FollowerInitialSyncUpStatus(true,
                "member-1-shard-inventory-operational"));

        assertTrue(shard.underlyingActor().getShardMBean().getFollowerInitialSyncStatus());
    }

    @Test
    public void testClusteredDataTreeChangeListenerWithDelayedRegistration() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final String testName = "testClusteredDataTreeChangeListenerWithDelayedRegistration";
        dataStoreContextBuilder.shardElectionTimeoutFactor(1000)
            .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener,
            TestModel.TEST_PATH), actorFactory.generateActorId(testName + "-DataTreeChangeListener"));

        setupInMemorySnapshotStore();

        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(testName + "-shard"));

        testKit.waitUntilNoLeader(shard);

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, true), testKit.getRef());
        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(5),
            RegisterDataTreeNotificationListenerReply.class);
        assertNotNull("getListenerRegistrationPath", reply.getListenerRegistrationPath());

        shard.tell(DatastoreContext.newBuilderFrom(dataStoreContextBuilder.build())
            .customRaftPolicyImplementation(null).build(), noSender());

        listener.waitForChangeEvents();
    }

    @Test
    public void testClusteredDataTreeChangeListenerWithDelayedRegistrationClosed() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final String testName = "testClusteredDataTreeChangeListenerWithDelayedRegistrationClosed";
        dataStoreContextBuilder.shardElectionTimeoutFactor(1000)
            .customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName());

        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(0);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener,
            TestModel.TEST_PATH), actorFactory.generateActorId(testName + "-DataTreeChangeListener"));

        setupInMemorySnapshotStore();

        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(testName + "-shard"));

        testKit.waitUntilNoLeader(shard);

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, true), testKit.getRef());
        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(5),
            RegisterDataTreeNotificationListenerReply.class);
        assertNotNull("getListenerRegistrationPath", reply.getListenerRegistrationPath());

        final ActorSelection regActor = getSystem().actorSelection(reply.getListenerRegistrationPath());
        regActor.tell(CloseDataTreeNotificationListenerRegistration.getInstance(), testKit.getRef());
        testKit.expectMsgClass(CloseDataTreeNotificationListenerRegistrationReply.class);

        shard.tell(DatastoreContext.newBuilderFrom(dataStoreContextBuilder.build())
            .customRaftPolicyImplementation(null).build(), noSender());

        listener.expectNoMoreChanges("Received unexpected change after close");
    }

    @Test
    public void testClusteredDataTreeChangeListenerRegistration() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final String testName = "testClusteredDataTreeChangeListenerRegistration";
        final ShardIdentifier followerShardID = ShardIdentifier.create("inventory",
            MemberName.forName(actorFactory.generateActorId(testName + "-follower")), "config");

        final ShardIdentifier leaderShardID = ShardIdentifier.create("inventory",
            MemberName.forName(actorFactory.generateActorId(testName + "-leader")), "config");

        final TestActorRef<Shard> followerShard = actorFactory
                .createTestActor(Shard.builder().id(followerShardID)
                    .datastoreContext(dataStoreContextBuilder.shardElectionTimeoutFactor(1000).build())
                    .peerAddresses(Collections.singletonMap(leaderShardID.toString(),
                        "pekko://test/user/" + leaderShardID.toString()))
                    .schemaContextProvider(() -> SCHEMA_CONTEXT).props(stateDir())
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), followerShardID.toString());

        final TestActorRef<Shard> leaderShard = actorFactory
                .createTestActor(Shard.builder().id(leaderShardID).datastoreContext(newDatastoreContext())
                    .peerAddresses(Collections.singletonMap(followerShardID.toString(),
                        "pekko://test/user/" + followerShardID.toString()))
                    .schemaContextProvider(() -> SCHEMA_CONTEXT).props(stateDir())
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), leaderShardID.toString());

        leaderShard.tell(TimeoutNow.INSTANCE, noSender());
        final String leaderPath = ShardTestKit.waitUntilLeader(followerShard);
        assertEquals("Shard leader path", leaderShard.path().toString(), leaderPath);

        final YangInstanceIdentifier path = TestModel.TEST_PATH;
        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener, path),
            actorFactory.generateActorId(testName + "-DataTreeChangeListener"));

        followerShard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, true), testKit.getRef());
        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(5),
            RegisterDataTreeNotificationListenerReply.class);
        assertNotNull("getListenerRegistrationPath", reply.getListenerRegistrationPath());

        writeToStore(followerShard, path, TestModel.EMPTY_TEST);

        listener.waitForChangeEvents();
    }

    @Test
    public void testServerRemoved() {
        final TestActorRef<MessageCollectorActor> parent = actorFactory.createTestActor(MessageCollectorActor.props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()));

        final ActorRef shard = parent.underlyingActor().context().actorOf(
                newShardBuilder().props(stateDir()).withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testServerRemoved");

        shard.tell(new ServerRemoved("test"), noSender());

        MessageCollectorActor.expectFirstMatching(parent, ServerRemoved.class);
    }
}
