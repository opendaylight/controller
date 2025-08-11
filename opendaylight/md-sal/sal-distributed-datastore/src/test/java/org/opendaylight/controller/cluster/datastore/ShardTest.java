/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.DataStoreVersions.CURRENT_VERSION;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.dispatch.Dispatchers;
import akka.dispatch.OnComplete;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.persistence.SaveSnapshotSuccess;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Uninterruptibles;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.mockito.InOrder;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistration;
import org.opendaylight.controller.cluster.datastore.messages.CloseDataTreeNotificationListenerRegistrationReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PeerAddressResolved;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyLocalTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterDataTreeNotificationListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.ShardLeaderStateChanged;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.persisted.MetadataShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.datastore.utils.MockDataTreeChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.ElectionTimeout;
import org.opendaylight.controller.cluster.raft.base.messages.FollowerInitialSyncUpStatus;
import org.opendaylight.controller.cluster.raft.base.messages.TimeoutNow;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeader;
import org.opendaylight.controller.cluster.raft.client.messages.FindLeaderReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.cluster.raft.client.messages.OnDemandRaftState;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.RequestVote;
import org.opendaylight.controller.cluster.raft.messages.ServerRemoved;
import org.opendaylight.controller.cluster.raft.persisted.ApplyJournalEntries;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.policy.DisableElectionsRaftPolicy;
import org.opendaylight.controller.cluster.raft.utils.InMemoryJournal;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.tree.api.DataTree;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeConfiguration;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

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

        shard.tell(new UpdateSchemaContext(SchemaContextHelper.full()), ActorRef.noSender());

        final MockDataTreeChangeListener listener = new MockDataTreeChangeListener(1);
        final ActorRef dclActor = actorFactory.createActor(DataTreeChangeListenerActor.props(listener,
            TestModel.TEST_PATH), "testRegisterDataTreeChangeListener-DataTreeChangeListener");

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, false), testKit.getRef());

        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(3),
            RegisterDataTreeNotificationListenerReply.class);
        final String replyPath = reply.getListenerRegistrationPath().toString();
        assertTrue("Incorrect reply path: " + replyPath,
            replyPath.matches("akka:\\/\\/test\\/user\\/testRegisterDataTreeChangeListener\\/\\$.*"));

        final YangInstanceIdentifier path = TestModel.TEST_PATH;
        writeToStore(shard, path, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

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

    @SuppressWarnings("serial")
    @Test
    public void testDataTreeChangeListenerNotifiedWhenNotTheLeaderOnRegistration() throws Exception {
        final CountDownLatch onFirstElectionTimeout = new CountDownLatch(1);
        final CountDownLatch onChangeListenerRegistered = new CountDownLatch(1);
        final Creator<Shard> creator = new Creator<>() {
            boolean firstElectionTimeout = true;

            @Override
            public Shard create() {
                return new Shard(newShardBuilder()) {
                    @Override
                    public void handleCommandImpl(final Object message) {
                        if (message instanceof ElectionTimeout && firstElectionTimeout) {
                            firstElectionTimeout = false;
                            final ActorRef self = getSelf();
                            new Thread(() -> {
                                Uninterruptibles.awaitUninterruptibly(
                                        onChangeListenerRegistered, 5, TimeUnit.SECONDS);
                                self.tell(message, self);
                            }).start();

                            onFirstElectionTimeout.countDown();
                        } else {
                            super.handleCommandImpl(message);
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
    public void testCreateTransaction() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final ActorRef shard = actorFactory.createActor(newShardProps(), "testCreateTransaction");

        ShardTestKit.waitUntilLeader(shard);

        shard.tell(new UpdateSchemaContext(TestModel.createTestContext()), testKit.getRef());

        shard.tell(new CreateTransaction(nextTransactionId(), TransactionType.READ_ONLY.ordinal(),
            DataStoreVersions.CURRENT_VERSION).toSerializable(), testKit.getRef());

        final CreateTransactionReply reply = testKit.expectMsgClass(Duration.ofSeconds(3),
            CreateTransactionReply.class);

        final String path = reply.getTransactionPath().toString();
        assertThat(path, containsString(String.format("/user/testCreateTransaction/shard-%s-%s:ShardTransactionTest@0:",
            shardID.getShardName(), shardID.getMemberName().getName())));
    }

    @Test
    public void testCreateTransactionOnChain() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final ActorRef shard = actorFactory.createActor(newShardProps(), "testCreateTransactionOnChain");

        ShardTestKit.waitUntilLeader(shard);

        shard.tell(new CreateTransaction(nextTransactionId(), TransactionType.READ_ONLY.ordinal(),
            DataStoreVersions.CURRENT_VERSION).toSerializable(), testKit.getRef());

        final CreateTransactionReply reply = testKit.expectMsgClass(Duration.ofSeconds(3),
            CreateTransactionReply.class);

        final String path = reply.getTransactionPath().toString();
        assertThat(path, containsString(String.format(
            "/user/testCreateTransactionOnChain/shard-%s-%s:ShardTransactionTest@0:",
            shardID.getShardName(), shardID.getMemberName().getName())));
    }

    @Test
    public void testPeerAddressResolved() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final ShardIdentifier peerID = ShardIdentifier.create("inventory", MemberName.forName("member-2"),
                "config");
        final TestActorRef<Shard> shard = actorFactory.createTestActor(newShardBuilder()
            .peerAddresses(Collections.<String, String>singletonMap(peerID.toString(), null))
            .props().withDispatcher(Dispatchers.DefaultDispatcherId()), "testPeerAddressResolved");

        final String address = "akka://foobar";
        shard.tell(new PeerAddressResolved(peerID.toString(), address), ActorRef.noSender());

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

        final ContainerNode container = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1))
                .build())
            .build();

        writeToStore(store, TestModel.TEST_PATH, container);

        final YangInstanceIdentifier root = YangInstanceIdentifier.of();
        final NormalizedNode expected = readStore(store, root);

        final Snapshot snapshot = Snapshot.create(new ShardSnapshotState(new MetadataShardDataTreeSnapshot(expected)),
                Collections.emptyList(), 1, 2, 3, 4, -1, null, null);

        shard.tell(new ApplySnapshot(snapshot), ActorRef.noSender());

        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            Uninterruptibles.sleepUninterruptibly(75, TimeUnit.MILLISECONDS);

            try {
                assertEquals("Root node", expected, readStore(shard, root));
                return;
            } catch (final AssertionError e) {
                // try again
            }
        }

        fail("Snapshot was not applied");
    }

    @Test
    public void testApplyState() throws Exception {
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testApplyState");

        ShardTestKit.waitUntilLeader(shard);

        final DataTree store = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_OPERATIONAL,
            SCHEMA_CONTEXT);

        final DataTreeModification writeMod = store.takeSnapshot().newModification();
        final ContainerNode node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        writeMod.write(TestModel.TEST_PATH, node);
        writeMod.ready();

        final TransactionIdentifier tx = nextTransactionId();
        shard.underlyingActor().applyState(null, null, payloadForModification(store, writeMod, tx));

        final Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            Uninterruptibles.sleepUninterruptibly(75, TimeUnit.MILLISECONDS);

            final NormalizedNode actual = readStore(shard, TestModel.TEST_PATH);
            if (actual != null) {
                assertEquals("Applied state", node, actual);
                return;
            }
        }

        fail("State was not applied");
    }

    @Test
    public void testDataTreeCandidateRecovery() throws Exception {
        // Set up the InMemorySnapshotStore.
        final DataTree source = setupInMemorySnapshotStore();

        final DataTreeModification writeMod = source.takeSnapshot().newModification();
        writeMod.write(TestModel.OUTER_LIST_PATH, ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());
        writeMod.ready();
        InMemoryJournal.addEntry(shardID.toString(), 0, DUMMY_DATA);

        // Set up the InMemoryJournal.
        InMemoryJournal.addEntry(shardID.toString(), 1, new SimpleReplicatedLogEntry(0, 1,
            payloadForModification(source, writeMod, nextTransactionId())));

        final int nListEntries = 16;
        final Set<Integer> listEntryKeys = new HashSet<>();

        // Add some ModificationPayload entries
        for (int i = 1; i <= nListEntries; i++) {
            listEntryKeys.add(i);

            final YangInstanceIdentifier path = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                    .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i).build();

            final DataTreeModification mod = source.takeSnapshot().newModification();
            mod.merge(path, ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, i));
            mod.ready();

            InMemoryJournal.addEntry(shardID.toString(), i + 1, new SimpleReplicatedLogEntry(i, 1,
                payloadForModification(source, mod, nextTransactionId())));
        }

        InMemoryJournal.addEntry(shardID.toString(), nListEntries + 2,
            new ApplyJournalEntries(nListEntries));

        testRecovery(listEntryKeys, true);
    }

    @Test
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void testConcurrentThreePhaseCommits() throws Exception {
        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        final CountDownLatch commitLatch = new CountDownLatch(2);

        final long timeoutSec = 5;
        final Duration duration = Duration.ofSeconds(timeoutSec);
        final Timeout timeout = Timeout.create(duration);

        final TestActorRef<Shard> shard = actorFactory.createTestActor(
                newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testConcurrentThreePhaseCommits");

        class OnFutureComplete extends OnComplete<Object> {
            private final Class<?> expRespType;

            OnFutureComplete(final Class<?> expRespType) {
                this.expRespType = expRespType;
            }

            @Override
            public void onComplete(final Throwable error, final Object resp) {
                if (error != null) {
                    caughtEx.set(new AssertionError(getClass().getSimpleName() + " failure", error));
                } else {
                    try {
                        assertEquals("Commit response type", expRespType, resp.getClass());
                        onSuccess(resp);
                    } catch (final Exception e) {
                        caughtEx.set(e);
                    }
                }
            }

            void onSuccess(final Object resp) {
            }
        }

        class OnCommitFutureComplete extends OnFutureComplete {
            OnCommitFutureComplete() {
                super(CommitTransactionReply.class);
            }

            @Override
            public void onComplete(final Throwable error, final Object resp) {
                super.onComplete(error, resp);
                commitLatch.countDown();
            }
        }

        class OnCanCommitFutureComplete extends OnFutureComplete {
            private final TransactionIdentifier transactionID;

            OnCanCommitFutureComplete(final TransactionIdentifier transactionID) {
                super(CanCommitTransactionReply.class);
                this.transactionID = transactionID;
            }

            @Override
            void onSuccess(final Object resp) {
                final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply.fromSerializable(resp);
                assertTrue("Can commit", canCommitReply.getCanCommit());

                final Future<Object> commitFuture = Patterns.ask(shard,
                        new CommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), timeout);
                commitFuture.onComplete(new OnCommitFutureComplete(), getSystem().dispatcher());
            }
        }

        final ShardTestKit testKit = new ShardTestKit(getSystem());
        ShardTestKit.waitUntilLeader(shard);

        final TransactionIdentifier transactionID1 = nextTransactionId();
        final TransactionIdentifier transactionID2 = nextTransactionId();
        final TransactionIdentifier transactionID3 = nextTransactionId();

        final Map<TransactionIdentifier, CapturingShardDataTreeCohort> cohortMap = setupCohortDecorator(
            shard.underlyingActor(), transactionID1, transactionID2, transactionID3);
        final CapturingShardDataTreeCohort cohort1 = cohortMap.get(transactionID1);
        final CapturingShardDataTreeCohort cohort2 = cohortMap.get(transactionID2);
        final CapturingShardDataTreeCohort cohort3 = cohortMap.get(transactionID3);

        shard.tell(prepareBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        final ReadyTransactionReply readyReply = ReadyTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, ReadyTransactionReply.class));

        String pathSuffix = shard.path().toString().replaceFirst("akka://test", "");
        assertThat(readyReply.getCohortPath(), endsWith(pathSuffix));
        // Send the CanCommitTransaction message for the first Tx.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Ready 2 more Tx's.

        shard.tell(prepareBatchedModifications(transactionID2, TestModel.OUTER_LIST_PATH,
            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        shard.tell(
            prepareBatchedModifications(transactionID3,
                YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message for the next 2 Tx's.
        // These should get queued and
        // processed after the first Tx completes.

        final Future<Object> canCommitFuture1 = Patterns.ask(shard,
            new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), timeout);

        final Future<Object> canCommitFuture2 = Patterns.ask(shard,
            new CanCommitTransaction(transactionID3, CURRENT_VERSION).toSerializable(), timeout);

        // Send the CommitTransaction message for the first Tx. After it
        // completes, it should
        // trigger the 2nd Tx to proceed which should in turn then
        // trigger the 3rd.

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        // Wait for the next 2 Tx's to complete.

        canCommitFuture1.onComplete(new OnCanCommitFutureComplete(transactionID2), getSystem().dispatcher());

        canCommitFuture2.onComplete(new OnCanCommitFutureComplete(transactionID3), getSystem().dispatcher());

        final boolean done = commitLatch.await(timeoutSec, TimeUnit.SECONDS);

        final Throwable t = caughtEx.get();
        if (t != null) {
            Throwables.propagateIfPossible(t, Exception.class);
            throw new RuntimeException(t);
        }

        assertTrue("Commits complete", done);

//                final InOrder inOrder = inOrder(cohort1.getCanCommit(), cohort1.getPreCommit(), cohort1.getCommit(),
//                        cohort2.getCanCommit(), cohort2.getPreCommit(), cohort2.getCommit(), cohort3.getCanCommit(),
//                        cohort3.getPreCommit(), cohort3.getCommit());
//                inOrder.verify(cohort1.getCanCommit()).onSuccess(any(Void.class));
//                inOrder.verify(cohort1.getPreCommit()).onSuccess(any(DataTreeCandidate.class));
//                inOrder.verify(cohort2.getCanCommit()).onSuccess(any(Void.class));
//                inOrder.verify(cohort2.getPreCommit()).onSuccess(any(DataTreeCandidate.class));
//                inOrder.verify(cohort3.getCanCommit()).onSuccess(any(Void.class));
//                inOrder.verify(cohort3.getPreCommit()).onSuccess(any(DataTreeCandidate.class));
//                inOrder.verify(cohort1.getCommit()).onSuccess(any(UnsignedLong.class));
//                inOrder.verify(cohort2.getCommit()).onSuccess(any(UnsignedLong.class));
//                inOrder.verify(cohort3.getCommit()).onSuccess(any(UnsignedLong.class));

        // Verify data in the data store.

        verifyOuterListEntry(shard, 1);

        verifyLastApplied(shard, 3);
    }

    @Test
    public void testBatchedModificationsWithNoCommitOnReady() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsWithNoCommitOnReady");

        ShardTestKit.waitUntilLeader(shard);

        final TransactionIdentifier transactionID = nextTransactionId();
        final Duration duration = Duration.ofSeconds(5);

        // Send a BatchedModifications to start a transaction.

        shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, BatchedModificationsReply.class);

        // Send a couple more BatchedModifications.

        shard.tell(newBatchedModifications(transactionID, TestModel.OUTER_LIST_PATH,
                ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), false, false, 2),
            testKit.getRef());
        testKit.expectMsgClass(duration, BatchedModificationsReply.class);

        shard.tell(newBatchedModifications(transactionID,
            YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), true, false, 3),
            testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message.

        shard.tell(new CanCommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CommitTransaction message.

        shard.tell(new CommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        // Verify data in the data store.

        verifyOuterListEntry(shard, 1);
    }

    @Test
    public void testBatchedModificationsWithCommitOnReady() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsWithCommitOnReady");

        ShardTestKit.waitUntilLeader(shard);

        final TransactionIdentifier transactionID = nextTransactionId();
        final Duration duration = Duration.ofSeconds(5);

        // Send a BatchedModifications to start a transaction.

        shard.tell(newBatchedModifications(transactionID, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, BatchedModificationsReply.class);

        // Send a couple more BatchedModifications.

        shard.tell(newBatchedModifications(transactionID, TestModel.OUTER_LIST_PATH,
            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), false, false, 2),
            testKit.getRef());
        testKit.expectMsgClass(duration, BatchedModificationsReply.class);

        shard.tell(newBatchedModifications(transactionID,
            YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
            .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
            ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), true, true, 3),
            testKit.getRef());

        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        // Verify data in the data store.
        verifyOuterListEntry(shard, 1);
    }

    @Deprecated(since = "9.0.0", forRemoval = true)
    @Test(expected = IllegalStateException.class)
    public void testBatchedModificationsReadyWithIncorrectTotalMessageCount() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsReadyWithIncorrectTotalMessageCount");

        ShardTestKit.waitUntilLeader(shard);

        final TransactionIdentifier transactionID = nextTransactionId();
        final BatchedModifications batched = new BatchedModifications(transactionID,
            DataStoreVersions.CURRENT_VERSION);
        batched.setReady();
        batched.setTotalMessagesSent(2);

        shard.tell(batched, testKit.getRef());

        final Failure failure = testKit.expectMsgClass(Duration.ofSeconds(5), Failure.class);

        if (failure != null) {
            Throwables.propagateIfPossible(failure.cause(), Exception.class);
            throw new RuntimeException(failure.cause());
        }
    }

    @Test
    @Deprecated(since = "9.0.0", forRemoval = true)
    public void testBatchedModificationsWithOperationFailure() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsWithOperationFailure");

        ShardTestKit.waitUntilLeader(shard);

        // Test merge with invalid data. An exception should occur when
        // the merge is applied. Note that
        // write will not validate the children for performance reasons.

        final TransactionIdentifier transactionID = nextTransactionId();

        final ContainerNode invalidData = Builders.containerBuilder()
            .withNodeIdentifier(new NodeIdentifier(TestModel.TEST_QNAME))
            .withChild(ImmutableNodes.leafNode(TestModel.JUNK_QNAME, "junk"))
            .build();

        BatchedModifications batched = new BatchedModifications(transactionID, CURRENT_VERSION);
        batched.addModification(new MergeModification(TestModel.TEST_PATH, invalidData));
        shard.tell(batched, testKit.getRef());
        Failure failure = testKit.expectMsgClass(Duration.ofSeconds(5), akka.actor.Status.Failure.class);

        final Throwable cause = failure.cause();

        batched = new BatchedModifications(transactionID, DataStoreVersions.CURRENT_VERSION);
        batched.setReady();
        batched.setTotalMessagesSent(2);

        shard.tell(batched, testKit.getRef());

        failure = testKit.expectMsgClass(Duration.ofSeconds(5), akka.actor.Status.Failure.class);
        assertEquals("Failure cause", cause, failure.cause());
    }

    @Test
    public void testBatchedModificationsOnTransactionChain() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testBatchedModificationsOnTransactionChain");

        ShardTestKit.waitUntilLeader(shard);

        final LocalHistoryIdentifier historyId = nextHistoryId();
        final TransactionIdentifier transactionID1 = new TransactionIdentifier(historyId, 0);
        final TransactionIdentifier transactionID2 = new TransactionIdentifier(historyId, 1);

        final Duration duration = Duration.ofSeconds(5);

        // Send a BatchedModifications to start a chained write
        // transaction and ready it.

        final ContainerNode containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        final YangInstanceIdentifier path = TestModel.TEST_PATH;
        shard.tell(newBatchedModifications(transactionID1, path, containerNode, true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Create a read Tx on the same chain.

        shard.tell(new CreateTransaction(transactionID2, TransactionType.READ_ONLY.ordinal(),
            DataStoreVersions.CURRENT_VERSION).toSerializable(), testKit.getRef());

        final CreateTransactionReply createReply = testKit.expectMsgClass(Duration.ofSeconds(3),
            CreateTransactionReply.class);

        getSystem().actorSelection(createReply.getTransactionPath())
        .tell(new ReadData(path, DataStoreVersions.CURRENT_VERSION), testKit.getRef());
        final ReadDataReply readReply = testKit.expectMsgClass(Duration.ofSeconds(3), ReadDataReply.class);
        assertEquals("Read node", containerNode, readReply.getNormalizedNode());

        // Commit the write transaction.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        // Verify data in the data store.

        final NormalizedNode actualNode = readStore(shard, path);
        assertEquals("Stored node", containerNode, actualNode);
    }

    @Test
    @Deprecated(since = "9.0.0", forRemoval = true)
    public void testOnBatchedModificationsWhenNotLeader() {
        final AtomicBoolean overrideLeaderCalls = new AtomicBoolean();
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final Creator<Shard> creator = new Creator<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Shard create() {
                return new Shard(newShardBuilder()) {
                    @Override
                    protected boolean isLeader() {
                        return overrideLeaderCalls.get() ? false : super.isLeader();
                    }

                    @Override
                    public ActorSelection getLeader() {
                        return overrideLeaderCalls.get() ? getSystem().actorSelection(testKit.getRef().path())
                                : super.getLeader();
                    }
                };
            }
        };

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
            new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testOnBatchedModificationsWhenNotLeader");

        ShardTestKit.waitUntilLeader(shard);

        overrideLeaderCalls.set(true);

        final BatchedModifications batched = new BatchedModifications(nextTransactionId(),
            DataStoreVersions.CURRENT_VERSION);

        shard.tell(batched, ActorRef.noSender());

        testKit.expectMsgEquals(batched);
    }

    @Test
    @Deprecated(since = "9.0.0", forRemoval = true)
    public void testTransactionMessagesWithNoLeader() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        dataStoreContextBuilder.customRaftPolicyImplementation(DisableElectionsRaftPolicy.class.getName())
        .shardHeartbeatIntervalInMillis(50).shardElectionTimeoutFactor(1);
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionMessagesWithNoLeader");

        testKit.waitUntilNoLeader(shard);

        final TransactionIdentifier txId = nextTransactionId();
        shard.tell(new BatchedModifications(txId, DataStoreVersions.CURRENT_VERSION), testKit.getRef());
        Failure failure = testKit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", NoShardLeaderException.class, failure.cause().getClass());

        shard.tell(prepareForwardedReadyTransaction(shard, txId, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), testKit.getRef());
        failure = testKit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", NoShardLeaderException.class, failure.cause().getClass());

        shard.tell(new ReadyLocalTransaction(txId, mock(DataTreeModification.class), true, Optional.empty()),
            testKit.getRef());
        failure = testKit.expectMsgClass(Failure.class);
        assertEquals("Failure cause type", NoShardLeaderException.class, failure.cause().getClass());
    }

    @Test
    public void testReadyWithReadWriteImmediateCommit() {
        testReadyWithImmediateCommit(true);
    }

    @Test
    public void testReadyWithWriteOnlyImmediateCommit() {
        testReadyWithImmediateCommit(false);
    }

    private void testReadyWithImmediateCommit(final boolean readWrite) {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyWithImmediateCommit-" + readWrite);

        ShardTestKit.waitUntilLeader(shard);

        final TransactionIdentifier transactionID = nextTransactionId();
        final NormalizedNode containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        if (readWrite) {
            shard.tell(prepareForwardedReadyTransaction(shard, transactionID, TestModel.TEST_PATH, containerNode, true),
                testKit.getRef());
        } else {
            shard.tell(prepareBatchedModifications(transactionID, TestModel.TEST_PATH, containerNode, true),
                testKit.getRef());
        }

        testKit.expectMsgClass(Duration.ofSeconds(5), CommitTransactionReply.class);

        final NormalizedNode actualNode = readStore(shard, TestModel.TEST_PATH);
        assertEquals(TestModel.TEST_QNAME.getLocalName(), containerNode, actualNode);
    }

    @Test
    public void testReadyLocalTransactionWithImmediateCommit() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyLocalTransactionWithImmediateCommit");

        ShardTestKit.waitUntilLeader(shard);

        final ShardDataTree dataStore = shard.underlyingActor().getDataStore();

        final DataTreeModification modification = dataStore.newModification();

        final ContainerNode writeData = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        new WriteModification(TestModel.TEST_PATH, writeData).apply(modification);
        final MapNode mergeData = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 42))
                .build();
        new MergeModification(TestModel.OUTER_LIST_PATH, mergeData).apply(modification);

        final TransactionIdentifier txId = nextTransactionId();
        modification.ready();
        final ReadyLocalTransaction readyMessage =
                new ReadyLocalTransaction(txId, modification, true, Optional.empty());

        shard.tell(readyMessage, testKit.getRef());

        testKit.expectMsgClass(CommitTransactionReply.class);

        final NormalizedNode actualNode = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertEquals(TestModel.OUTER_LIST_QNAME.getLocalName(), mergeData, actualNode);
    }

    @Test
    public void testReadyLocalTransactionWithThreePhaseCommit() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testReadyLocalTransactionWithThreePhaseCommit");

        ShardTestKit.waitUntilLeader(shard);

        final ShardDataTree dataStore = shard.underlyingActor().getDataStore();

        final DataTreeModification modification = dataStore.newModification();

        final ContainerNode writeData = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        new WriteModification(TestModel.TEST_PATH, writeData).apply(modification);
        final MapNode mergeData = ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME)
                .addChild(ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 42))
                .build();
        new MergeModification(TestModel.OUTER_LIST_PATH, mergeData).apply(modification);

        final TransactionIdentifier txId = nextTransactionId();
        modification.ready();
        final ReadyLocalTransaction readyMessage =
                new ReadyLocalTransaction(txId, modification, false, Optional.empty());

        shard.tell(readyMessage, testKit.getRef());

        testKit.expectMsgClass(ReadyTransactionReply.class);

        // Send the CanCommitTransaction message.

        shard.tell(new CanCommitTransaction(txId, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CanCommitTransaction message.

        shard.tell(new CommitTransaction(txId, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(CommitTransactionReply.class);

        final NormalizedNode actualNode = readStore(shard, TestModel.OUTER_LIST_PATH);
        assertEquals(TestModel.OUTER_LIST_QNAME.getLocalName(), mergeData, actualNode);
    }

    @Test
    public void testReadWriteCommitWithPersistenceDisabled() {
        dataStoreContextBuilder.persistent(false);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitWithPersistenceDisabled");

        ShardTestKit.waitUntilLeader(shard);

        // Setup a simulated transactions with a mock cohort.

        final Duration duration = Duration.ofSeconds(5);

        final TransactionIdentifier transactionID = nextTransactionId();
        final NormalizedNode containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
        shard.tell(prepareBatchedModifications(transactionID, TestModel.TEST_PATH, containerNode, false),
            testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message.

        shard.tell(new CanCommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CanCommitTransaction message.

        shard.tell(new CommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        final NormalizedNode actualNode = readStore(shard, TestModel.TEST_PATH);
        assertEquals(TestModel.TEST_QNAME.getLocalName(), containerNode, actualNode);
    }

    @Test
    public void testReadWriteCommitWhenTransactionHasModifications() throws Exception {
        testCommitWhenTransactionHasModifications(true);
    }

    @Test
    public void testWriteOnlyCommitWhenTransactionHasModifications() throws Exception {
        testCommitWhenTransactionHasModifications(false);
    }

    private void testCommitWhenTransactionHasModifications(final boolean readWrite) throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitWhenTransactionHasModifications-" + readWrite);

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);
        final TransactionIdentifier transactionID = nextTransactionId();

        if (readWrite) {
            shard.tell(prepareForwardedReadyTransaction(shard, transactionID, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        } else {
            shard.tell(prepareBatchedModifications(transactionID, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        }

        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message.

        shard.tell(new CanCommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        shard.tell(new CommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        final InOrder inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));
        inOrder.verify(dataTree).commit(any(DataTreeCandidate.class));

        // Purge request is scheduled as asynchronous, wait for two heartbeats to let it propagate into
        // the journal
        Thread.sleep(HEARTBEAT_MILLIS * 2);

        shard.tell(Shard.GET_SHARD_MBEAN_MESSAGE, testKit.getRef());
        final ShardStats shardStats = testKit.expectMsgClass(duration, ShardStats.class);

        // Use MBean for verification
        // Committed transaction count should increase as usual
        assertEquals(1, shardStats.getCommittedTransactionsCount());

        // Commit index should advance 1 to account for disabling metadata
        assertEquals(1, shardStats.getCommitIndex());
    }

    @Test
    public void testCommitPhaseFailure() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);
        final Timeout timeout = Timeout.create(duration);

        // Setup 2 simulated transactions with mock cohorts. The first
        // one fails in the
        // commit phase.

        doThrow(new RuntimeException("mock commit failure")).when(dataTree)
        .commit(any(DataTreeCandidate.class));

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message for the first Tx.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CanCommitTransaction message for the 2nd Tx. This
        // should get queued and
        // processed after the first Tx completes.

        final Future<Object> canCommitFuture = Patterns.ask(shard,
            new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), timeout);

        // Send the CommitTransaction message for the first Tx. This
        // should send back an error
        // and trigger the 2nd Tx to proceed.

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, akka.actor.Status.Failure.class);

        // Wait for the 2nd Tx to complete the canCommit phase.

        final CountDownLatch latch = new CountDownLatch(1);
        canCommitFuture.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object resp) {
                latch.countDown();
            }
        }, getSystem().dispatcher());

        assertTrue("2nd CanCommit complete", latch.await(5, TimeUnit.SECONDS));

        final InOrder inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));

        // FIXME: this invocation is done on the result of validate(). To test it, we need to make sure mock
        //        validate performs wrapping and we capture that mock
        // inOrder.verify(dataTree).validate(any(DataTreeModification.class));

        inOrder.verify(dataTree).commit(any(DataTreeCandidate.class));
    }

    @Test
    public void testPreCommitPhaseFailure() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testPreCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);
        final Timeout timeout = Timeout.create(duration);

        doThrow(new RuntimeException("mock preCommit failure")).when(dataTree)
        .prepare(any(DataTreeModification.class));

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message for the first Tx.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CanCommitTransaction message for the 2nd Tx. This
        // should get queued and
        // processed after the first Tx completes.

        final Future<Object> canCommitFuture = Patterns.ask(shard,
            new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), timeout);

        // Send the CommitTransaction message for the first Tx. This
        // should send back an error
        // and trigger the 2nd Tx to proceed.

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, akka.actor.Status.Failure.class);

        // Wait for the 2nd Tx to complete the canCommit phase.

        final CountDownLatch latch = new CountDownLatch(1);
        canCommitFuture.onComplete(new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Object resp) {
                latch.countDown();
            }
        }, getSystem().dispatcher());

        assertTrue("2nd CanCommit complete", latch.await(5, TimeUnit.SECONDS));

        final InOrder inOrder = inOrder(dataTree);
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
        inOrder.verify(dataTree).prepare(any(DataTreeModification.class));
        inOrder.verify(dataTree).validate(any(DataTreeModification.class));
    }

    @Test
    public void testCanCommitPhaseFailure() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCanCommitPhaseFailure");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);
        final TransactionIdentifier transactionID1 = nextTransactionId();

        doThrow(new DataValidationFailedException(YangInstanceIdentifier.of(), "mock canCommit failure"))
        .doNothing().when(dataTree).validate(any(DataTreeModification.class));

        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, akka.actor.Status.Failure.class);

        // Send another can commit to ensure the failed one got cleaned
        // up.

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        shard.tell(new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final CanCommitTransactionReply reply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(CanCommitTransactionReply.class));
        assertTrue("getCanCommit", reply.getCanCommit());
    }

    @Test
    public void testImmediateCommitWithCanCommitPhaseFailure() throws Exception {
        testImmediateCommitWithCanCommitPhaseFailure(true);
        testImmediateCommitWithCanCommitPhaseFailure(false);
    }

    private void testImmediateCommitWithCanCommitPhaseFailure(final boolean readWrite) throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final DataTree dataTree = createDelegatingMockDataTree();
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardBuilder().dataTree(dataTree).props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testImmediateCommitWithCanCommitPhaseFailure-" + readWrite);

        ShardTestKit.waitUntilLeader(shard);

        doThrow(new DataValidationFailedException(YangInstanceIdentifier.of(), "mock canCommit failure"))
        .doNothing().when(dataTree).validate(any(DataTreeModification.class));

        final Duration duration = Duration.ofSeconds(5);

        final TransactionIdentifier transactionID1 = nextTransactionId();

        if (readWrite) {
            shard.tell(prepareForwardedReadyTransaction(shard, transactionID1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), testKit.getRef());
        } else {
            shard.tell(prepareBatchedModifications(transactionID1, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), testKit.getRef());
        }

        testKit.expectMsgClass(duration, akka.actor.Status.Failure.class);

        // Send another can commit to ensure the failed one got cleaned
        // up.

        final TransactionIdentifier transactionID2 = nextTransactionId();
        if (readWrite) {
            shard.tell(prepareForwardedReadyTransaction(shard, transactionID2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), testKit.getRef());
        } else {
            shard.tell(prepareBatchedModifications(transactionID2, TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME), true), testKit.getRef());
        }

        testKit.expectMsgClass(duration, CommitTransactionReply.class);
    }

    @Test
    public void testAbortWithCommitPending() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final Creator<Shard> creator = () -> new Shard(newShardBuilder()) {
            @Override
            void persistPayload(final Identifier id, final Payload payload, final boolean batchHint) {
                // Simulate an AbortTransaction message occurring during
                // replication, after
                // persisting and before finishing the commit to the
                // in-memory store.

                doAbortTransaction(id, null);
                super.persistPayload(id, payload, batchHint);
            }
        };

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
            new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testAbortWithCommitPending");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        final TransactionIdentifier transactionID = nextTransactionId();

        shard.tell(prepareBatchedModifications(transactionID, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        shard.tell(new CanCommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        shard.tell(new CommitTransaction(transactionID, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        final NormalizedNode node = readStore(shard, TestModel.TEST_PATH);

        // Since we're simulating an abort occurring during replication
        // and before finish commit,
        // the data should still get written to the in-memory store
        // since we've gotten past
        // canCommit and preCommit and persisted the data.
        assertNotNull(TestModel.TEST_QNAME.getLocalName() + " not found", node);
    }

    @Test
    public void testTransactionCommitTimeout() throws Exception {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitTimeout");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        writeToStore(shard, TestModel.OUTER_LIST_PATH,
            ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

        // Ready 2 Tx's - the first will timeout

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(
            prepareBatchedModifications(transactionID1,
                YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1).build(),
                ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 1), false),
            testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        final YangInstanceIdentifier listNodePath = YangInstanceIdentifier.builder(TestModel.OUTER_LIST_PATH)
                .nodeWithKey(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2).build();
        shard.tell(
            prepareBatchedModifications(transactionID2, listNodePath,
                ImmutableNodes.mapEntry(TestModel.OUTER_LIST_QNAME, TestModel.ID_QNAME, 2), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // canCommit 1st Tx. We don't send the commit so it should
        // timeout.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        // canCommit the 2nd Tx - it should complete after the 1st Tx
        // times out.

        shard.tell(new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        // Try to commit the 1st Tx - should fail as it's not the
        // current Tx.

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, akka.actor.Status.Failure.class);

        // Commit the 2nd Tx.

        shard.tell(new CommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        final NormalizedNode node = readStore(shard, listNodePath);
        assertNotNull(listNodePath + " not found", node);
    }

//    @Test
//    @Ignore
//    public void testTransactionCommitQueueCapacityExceeded() throws Throwable {
//        dataStoreContextBuilder.shardTransactionCommitQueueCapacity(2);
//
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
//            expectMsgClass(duration, akka.actor.Status.Failure.class);
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
//            expectMsgClass(duration, akka.actor.Status.Failure.class);
//        }};
//    }

    @Test
    public void testTransactionCommitWithPriorExpiredCohortEntries() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitWithPriorExpiredCohortEntries");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID3 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID3, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // All Tx's are readied. We'll send canCommit for the last one
        // but not the others. The others
        // should expire from the queue and the last one should be
        // processed.

        shard.tell(new CanCommitTransaction(transactionID3, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);
    }

    @Test
    public void testTransactionCommitWithSubsequentExpiredCohortEntry() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testTransactionCommitWithSubsequentExpiredCohortEntry");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        final ShardDataTree dataStore = shard.underlyingActor().getDataStore();

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(prepareBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // CanCommit the first Tx so it's the current in-progress Tx.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        // Ready the second Tx.

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(prepareBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), false), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Ready the third Tx.

        final TransactionIdentifier transactionID3 = nextTransactionId();
        final DataTreeModification modification3 = dataStore.newModification();
        new WriteModification(TestModel.TEST2_PATH, ImmutableNodes.containerNode(TestModel.TEST2_QNAME))
            .apply(modification3);
        modification3.ready();
        final ReadyLocalTransaction readyMessage = new ReadyLocalTransaction(transactionID3, modification3,
            true, Optional.empty());
        shard.tell(readyMessage, testKit.getRef());

        // Commit the first Tx. After completing, the second should
        // expire from the queue and the third
        // Tx committed.

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        // Expect commit reply from the third Tx.

        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        final NormalizedNode node = readStore(shard, TestModel.TEST2_PATH);
        assertNotNull(TestModel.TEST2_PATH + " not found", node);
    }

    @Test
    public void testCanCommitBeforeReadyFailure() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()),
            "testCanCommitBeforeReadyFailure");

        shard.tell(new CanCommitTransaction(nextTransactionId(), CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(Duration.ofSeconds(5), akka.actor.Status.Failure.class);
    }

    @Test
    public void testAbortAfterCanCommit() throws Exception {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterCanCommit");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);
        final Timeout timeout = Timeout.create(duration);

        // Ready 2 transactions - the first one will be aborted.

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the CanCommitTransaction message for the first Tx.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        CanCommitTransactionReply canCommitReply = CanCommitTransactionReply
                .fromSerializable(testKit.expectMsgClass(duration, CanCommitTransactionReply.class));
        assertTrue("Can commit", canCommitReply.getCanCommit());

        // Send the CanCommitTransaction message for the 2nd Tx. This
        // should get queued and
        // processed after the first Tx completes.

        final Future<Object> canCommitFuture = Patterns.ask(shard,
            new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), timeout);

        // Send the AbortTransaction message for the first Tx. This
        // should trigger the 2nd
        // Tx to proceed.

        shard.tell(new AbortTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, AbortTransactionReply.class);

        // Wait for the 2nd Tx to complete the canCommit phase.

        canCommitReply = (CanCommitTransactionReply) Await.result(canCommitFuture,
            FiniteDuration.create(5, TimeUnit.SECONDS));
        assertTrue("Can commit", canCommitReply.getCanCommit());
    }

    @Test
    public void testAbortAfterReady() {
        dataStoreContextBuilder.shardTransactionCommitTimeoutInSeconds(1);
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterReady");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        // Ready a tx.

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Send the AbortTransaction message.

        shard.tell(new AbortTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, AbortTransactionReply.class);

        assertEquals("getPendingTxCommitQueueSize", 0, shard.underlyingActor().getPendingTxCommitQueueSize());

        // Now send CanCommitTransaction - should fail.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        final Throwable failure = testKit.expectMsgClass(duration, akka.actor.Status.Failure.class).cause();
        assertTrue("Failure type", failure instanceof IllegalStateException);

        // Ready and CanCommit another and verify success.

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        shard.tell(new CanCommitTransaction(transactionID2, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);
    }

    @Test
    public void testAbortQueuedTransaction() {
        final ShardTestKit testKit = new ShardTestKit(getSystem());
        final TestActorRef<Shard> shard = actorFactory.createTestActor(
            newShardProps().withDispatcher(Dispatchers.DefaultDispatcherId()), "testAbortAfterReady");

        ShardTestKit.waitUntilLeader(shard);

        final Duration duration = Duration.ofSeconds(5);

        // Ready 3 tx's.

        final TransactionIdentifier transactionID1 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID1, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID2 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID2, TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        final TransactionIdentifier transactionID3 = nextTransactionId();
        shard.tell(newBatchedModifications(transactionID3, TestModel.OUTER_LIST_PATH,
                ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build(), true, false, 1), testKit.getRef());
        testKit.expectMsgClass(duration, ReadyTransactionReply.class);

        // Abort the second tx while it's queued.

        shard.tell(new AbortTransaction(transactionID2, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, AbortTransactionReply.class);

        // Commit the other 2.

        shard.tell(new CanCommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        shard.tell(new CommitTransaction(transactionID1, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        shard.tell(new CanCommitTransaction(transactionID3, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CanCommitTransactionReply.class);

        shard.tell(new CommitTransaction(transactionID3, CURRENT_VERSION).toSerializable(), testKit.getRef());
        testKit.expectMsgClass(duration, CommitTransactionReply.class);

        assertEquals("getPendingTxCommitQueueSize", 0, shard.underlyingActor().getPendingTxCommitQueueSize());
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
        final AtomicReference<CountDownLatch> latch = new AtomicReference<>(new CountDownLatch(1));

        final AtomicReference<Object> savedSnapshot = new AtomicReference<>();
        class TestPersistentDataProvider extends DelegatingPersistentDataProvider {
            TestPersistentDataProvider(final DataPersistenceProvider delegate) {
                super(delegate);
            }

            @Override
            public void saveSnapshot(final Object obj) {
                savedSnapshot.set(obj);
                super.saveSnapshot(obj);
            }
        }

        dataStoreContextBuilder.persistent(persistent);

        final class TestShard extends Shard {

            TestShard(final AbstractBuilder<?, ?> builder) {
                super(builder);
                setPersistence(new TestPersistentDataProvider(super.persistence()));
            }

            @Override
            public void handleCommandImpl(final Object message) {
                super.handleCommandImpl(message);

                // XXX:  commit_snapshot equality check references RaftActorSnapshotMessageSupport.COMMIT_SNAPSHOT
                if (message instanceof SaveSnapshotSuccess || "commit_snapshot".equals(message.toString())) {
                    latch.get().countDown();
                }
            }

            @Override
            public RaftActorContext getRaftActorContext() {
                return super.getRaftActorContext();
            }
        }

        final Creator<Shard> creator = () -> new TestShard(newShardBuilder());

        final TestActorRef<Shard> shard = actorFactory.createTestActor(Props.create(Shard.class,
            new DelegatingShardCreator(creator)).withDispatcher(Dispatchers.DefaultDispatcherId()), shardActorName);

        ShardTestKit.waitUntilLeader(shard);
        writeToStore(shard, TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        final NormalizedNode expectedRoot = readStore(shard, YangInstanceIdentifier.of());

        // Trigger creation of a snapshot by ensuring
        final RaftActorContext raftActorContext = ((TestShard) shard.underlyingActor()).getRaftActorContext();
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
        putTransaction.write(TestModel.TEST_PATH,
            ImmutableNodes.containerNode(TestModel.TEST_QNAME));
        commitTransaction(store, putTransaction);


        final NormalizedNode expected = readStore(store, YangInstanceIdentifier.of());

        final DataTreeModification writeTransaction = store.takeSnapshot().newModification();

        writeTransaction.delete(YangInstanceIdentifier.of());
        writeTransaction.write(YangInstanceIdentifier.of(), expected);

        commitTransaction(store, writeTransaction);

        final NormalizedNode actual = readStore(store, YangInstanceIdentifier.of());

        assertEquals(expected, actual);
    }

    @Test
    public void testRecoveryApplicable() {

        final DatastoreContext persistentContext = DatastoreContext.newBuilder()
                .shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(true).build();

        final Props persistentProps = Shard.builder().id(shardID).datastoreContext(persistentContext)
                .schemaContextProvider(() -> SCHEMA_CONTEXT).props();

        final DatastoreContext nonPersistentContext = DatastoreContext.newBuilder()
                .shardJournalRecoveryLogBatchSize(3).shardSnapshotBatchCount(5000).persistent(false).build();

        final Props nonPersistentProps = Shard.builder().id(shardID).datastoreContext(nonPersistentContext)
                .schemaContextProvider(() -> SCHEMA_CONTEXT).props();

        final TestActorRef<Shard> shard1 = actorFactory.createTestActor(persistentProps, "testPersistence1");

        assertTrue("Recovery Applicable", shard1.underlyingActor().persistence().isRecoveryApplicable());

        final TestActorRef<Shard> shard2 = actorFactory.createTestActor(nonPersistentProps, "testPersistence2");

        assertFalse("Recovery Not Applicable", shard2.underlyingActor().persistence().isRecoveryApplicable());
    }

    @Test
    public void testOnDatastoreContext() {
        dataStoreContextBuilder.persistent(true);

        final TestActorRef<Shard> shard = actorFactory.createTestActor(newShardProps(), "testOnDatastoreContext");

        assertTrue("isRecoveryApplicable", shard.underlyingActor().persistence().isRecoveryApplicable());

        ShardTestKit.waitUntilLeader(shard);

        shard.tell(dataStoreContextBuilder.persistent(false).build(), ActorRef.noSender());

        assertFalse("isRecoveryApplicable", shard.underlyingActor().persistence().isRecoveryApplicable());

        shard.tell(dataStoreContextBuilder.persistent(true).build(), ActorRef.noSender());

        assertTrue("isRecoveryApplicable", shard.underlyingActor().persistence().isRecoveryApplicable());
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
            newShardBuilder().props().withDispatcher(Dispatchers.DefaultDispatcherId()),
            actorFactory.generateActorId(testName + "-shard"));

        testKit.waitUntilNoLeader(shard);

        shard.tell(new RegisterDataTreeChangeListener(TestModel.TEST_PATH, dclActor, true), testKit.getRef());
        final RegisterDataTreeNotificationListenerReply reply = testKit.expectMsgClass(Duration.ofSeconds(5),
            RegisterDataTreeNotificationListenerReply.class);
        assertNotNull("getListenerRegistrationPath", reply.getListenerRegistrationPath());

        shard.tell(DatastoreContext.newBuilderFrom(dataStoreContextBuilder.build())
            .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

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
            newShardBuilder().props().withDispatcher(Dispatchers.DefaultDispatcherId()),
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
            .customRaftPolicyImplementation(null).build(), ActorRef.noSender());

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
                        "akka://test/user/" + leaderShardID.toString()))
                    .schemaContextProvider(() -> SCHEMA_CONTEXT).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), followerShardID.toString());

        final TestActorRef<Shard> leaderShard = actorFactory
                .createTestActor(Shard.builder().id(leaderShardID).datastoreContext(newDatastoreContext())
                    .peerAddresses(Collections.singletonMap(followerShardID.toString(),
                        "akka://test/user/" + followerShardID.toString()))
                    .schemaContextProvider(() -> SCHEMA_CONTEXT).props()
                    .withDispatcher(Dispatchers.DefaultDispatcherId()), leaderShardID.toString());

        leaderShard.tell(TimeoutNow.INSTANCE, ActorRef.noSender());
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

        writeToStore(followerShard, path, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        listener.waitForChangeEvents();
    }

    @Test
    public void testServerRemoved() {
        final TestActorRef<MessageCollectorActor> parent = actorFactory.createTestActor(MessageCollectorActor.props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()));

        final ActorRef shard = parent.underlyingActor().context().actorOf(
                newShardBuilder().props().withDispatcher(Dispatchers.DefaultDispatcherId()),
                "testServerRemoved");

        shard.tell(new ServerRemoved("test"), ActorRef.noSender());

        MessageCollectorActor.expectFirstMatching(parent, ServerRemoved.class);
    }
}
