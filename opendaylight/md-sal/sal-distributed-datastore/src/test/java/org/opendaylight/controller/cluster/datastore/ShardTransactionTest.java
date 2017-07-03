/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status.Failure;
import akka.actor.Terminated;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModifications;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardTransactionTest extends AbstractActorTest {

    private static final TransactionType RO = TransactionType.READ_ONLY;
    private static final TransactionType RW = TransactionType.READ_WRITE;
    private static final TransactionType WO = TransactionType.WRITE_ONLY;

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.create("inventory", MEMBER_NAME, "config");
    private static final SchemaContext TEST_MODEL = TestModel.createTestContext();

    private DatastoreContext datastoreContext = DatastoreContext.newBuilder().persistent(false).build();

    private final TestActorFactory actorFactory = new TestActorFactory(getSystem());

    private TestActorRef<Shard> shard;
    private ShardDataTree store;

    @Before
    public void setUp() {
        shard = actorFactory.createTestActor(Shard.builder().id(SHARD_IDENTIFIER).datastoreContext(datastoreContext)
                .schemaContextProvider(() -> TEST_MODEL).props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()));
        ShardTestKit.waitUntilLeader(shard);
        store = shard.underlyingActor().getDataStore();
    }

    private ActorRef newTransactionActor(final TransactionType type,
            final AbstractShardDataTreeTransaction<?> transaction, final String name) {
        Props props = ShardTransaction.props(type, transaction, shard, datastoreContext,
                shard.underlyingActor().getShardMBean());
        return actorFactory.createActorNoVerify(props, name);
    }

    private ReadOnlyShardDataTreeTransaction readOnlyTransaction() {
        return store.newReadOnlyTransaction(nextTransactionId());
    }

    private ReadWriteShardDataTreeTransaction readWriteTransaction() {
        return store.newReadWriteTransaction(nextTransactionId());
    }

    @Test
    public void testOnReceiveReadData() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                testOnReceiveReadData(newTransactionActor(RO, readOnlyTransaction(), "testReadDataRO"));

                testOnReceiveReadData(newTransactionActor(RW, readWriteTransaction(), "testReadDataRW"));
            }

            private void testOnReceiveReadData(final ActorRef transaction) {
                transaction.tell(new ReadData(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION),
                        getRef());

                ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

                assertNotNull(reply.getNormalizedNode());
            }
        };
    }

    @Test
    public void testOnReceiveReadDataWhenDataNotFound() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                testOnReceiveReadDataWhenDataNotFound(
                        newTransactionActor(RO, readOnlyTransaction(), "testReadDataWhenDataNotFoundRO"));

                testOnReceiveReadDataWhenDataNotFound(
                        newTransactionActor(RW, readWriteTransaction(), "testReadDataWhenDataNotFoundRW"));
            }

            private void testOnReceiveReadDataWhenDataNotFound(final ActorRef transaction) {
                transaction.tell(new ReadData(TestModel.TEST_PATH, DataStoreVersions.CURRENT_VERSION), getRef());

                ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

                assertTrue(reply.getNormalizedNode() == null);
            }
        };
    }

    @Test
    public void testOnReceiveDataExistsPositive() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                testOnReceiveDataExistsPositive(
                        newTransactionActor(RO, readOnlyTransaction(), "testDataExistsPositiveRO"));

                testOnReceiveDataExistsPositive(
                        newTransactionActor(RW, readWriteTransaction(), "testDataExistsPositiveRW"));
            }

            private void testOnReceiveDataExistsPositive(final ActorRef transaction) {
                transaction.tell(new DataExists(YangInstanceIdentifier.EMPTY, DataStoreVersions.CURRENT_VERSION),
                        getRef());

                DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

                assertTrue(reply.exists());
            }
        };
    }

    @Test
    public void testOnReceiveDataExistsNegative() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                testOnReceiveDataExistsNegative(
                        newTransactionActor(RO, readOnlyTransaction(), "testDataExistsNegativeRO"));

                testOnReceiveDataExistsNegative(
                        newTransactionActor(RW, readWriteTransaction(), "testDataExistsNegativeRW"));
            }

            private void testOnReceiveDataExistsNegative(final ActorRef transaction) {
                transaction.tell(new DataExists(TestModel.TEST_PATH, DataStoreVersions.CURRENT_VERSION), getRef());

                DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

                assertFalse(reply.exists());
            }
        };
    }

    @Test
    public void testOnReceiveBatchedModifications() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                ShardDataTreeTransactionParent parent = Mockito.mock(ShardDataTreeTransactionParent.class);
                DataTreeModification mockModification = Mockito.mock(DataTreeModification.class);
                ReadWriteShardDataTreeTransaction mockWriteTx = new ReadWriteShardDataTreeTransaction(parent,
                        nextTransactionId(), mockModification);
                final ActorRef transaction = newTransactionActor(RW, mockWriteTx, "testOnReceiveBatchedModifications");

                YangInstanceIdentifier writePath = TestModel.TEST_PATH;
                NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                        .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

                YangInstanceIdentifier mergePath = TestModel.OUTER_LIST_PATH;
                NormalizedNode<?, ?> mergeData = ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.OUTER_LIST_QNAME))
                        .build();

                YangInstanceIdentifier deletePath = TestModel.TEST_PATH;

                BatchedModifications batched = new BatchedModifications(nextTransactionId(),
                        DataStoreVersions.CURRENT_VERSION);
                batched.addModification(new WriteModification(writePath, writeData));
                batched.addModification(new MergeModification(mergePath, mergeData));
                batched.addModification(new DeleteModification(deletePath));

                transaction.tell(batched, getRef());

                BatchedModificationsReply reply = expectMsgClass(duration("5 seconds"),
                        BatchedModificationsReply.class);
                assertEquals("getNumBatched", 3, reply.getNumBatched());

                InOrder inOrder = Mockito.inOrder(mockModification);
                inOrder.verify(mockModification).write(writePath, writeData);
                inOrder.verify(mockModification).merge(mergePath, mergeData);
                inOrder.verify(mockModification).delete(deletePath);
            }
        };
    }

    @Test
    public void testOnReceiveBatchedModificationsReadyWithoutImmediateCommit() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(WO, readWriteTransaction(),
                        "testOnReceiveBatchedModificationsReadyWithoutImmediateCommit");

                JavaTestKit watcher = new JavaTestKit(getSystem());
                watcher.watch(transaction);

                YangInstanceIdentifier writePath = TestModel.TEST_PATH;
                NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                        .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

                final TransactionIdentifier tx1 = nextTransactionId();
                BatchedModifications batched = new BatchedModifications(tx1, DataStoreVersions.CURRENT_VERSION);
                batched.addModification(new WriteModification(writePath, writeData));

                transaction.tell(batched, getRef());
                BatchedModificationsReply reply = expectMsgClass(duration("5 seconds"),
                        BatchedModificationsReply.class);
                assertEquals("getNumBatched", 1, reply.getNumBatched());

                batched = new BatchedModifications(tx1, DataStoreVersions.CURRENT_VERSION);
                batched.setReady(true);
                batched.setTotalMessagesSent(2);

                transaction.tell(batched, getRef());
                expectMsgClass(duration("5 seconds"), ReadyTransactionReply.class);
                watcher.expectMsgClass(duration("5 seconds"), Terminated.class);
            }
        };
    }

    @Test
    public void testOnReceiveBatchedModificationsReadyWithImmediateCommit() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(WO, readWriteTransaction(),
                        "testOnReceiveBatchedModificationsReadyWithImmediateCommit");

                JavaTestKit watcher = new JavaTestKit(getSystem());
                watcher.watch(transaction);

                YangInstanceIdentifier writePath = TestModel.TEST_PATH;
                NormalizedNode<?, ?> writeData = ImmutableContainerNodeBuilder.create()
                        .withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME))
                        .withChild(ImmutableNodes.leafNode(TestModel.DESC_QNAME, "foo")).build();

                BatchedModifications batched = new BatchedModifications(nextTransactionId(),
                        DataStoreVersions.CURRENT_VERSION);
                batched.addModification(new WriteModification(writePath, writeData));
                batched.setReady(true);
                batched.setDoCommitOnReady(true);
                batched.setTotalMessagesSent(1);

                transaction.tell(batched, getRef());
                expectMsgClass(duration("5 seconds"), CommitTransactionReply.class);
                watcher.expectMsgClass(duration("5 seconds"), Terminated.class);
            }
        };
    }

    @Test(expected = TestException.class)
    public void testOnReceiveBatchedModificationsFailure() throws Exception {
        new JavaTestKit(getSystem()) {
            {

                ShardDataTreeTransactionParent parent = Mockito.mock(ShardDataTreeTransactionParent.class);
                DataTreeModification mockModification = Mockito.mock(DataTreeModification.class);
                ReadWriteShardDataTreeTransaction mockWriteTx = new ReadWriteShardDataTreeTransaction(parent,
                        nextTransactionId(), mockModification);
                final ActorRef transaction = newTransactionActor(RW, mockWriteTx,
                        "testOnReceiveBatchedModificationsFailure");

                JavaTestKit watcher = new JavaTestKit(getSystem());
                watcher.watch(transaction);

                YangInstanceIdentifier path = TestModel.TEST_PATH;
                ContainerNode node = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

                doThrow(new TestException()).when(mockModification).write(path, node);

                final TransactionIdentifier tx1 = nextTransactionId();
                BatchedModifications batched = new BatchedModifications(tx1, DataStoreVersions.CURRENT_VERSION);
                batched.addModification(new WriteModification(path, node));

                transaction.tell(batched, getRef());
                expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);

                batched = new BatchedModifications(tx1, DataStoreVersions.CURRENT_VERSION);
                batched.setReady(true);
                batched.setTotalMessagesSent(2);

                transaction.tell(batched, getRef());
                Failure failure = expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);
                watcher.expectMsgClass(duration("5 seconds"), Terminated.class);

                if (failure != null) {
                    Throwables.propagateIfPossible(failure.cause(), Exception.class);
                    throw new RuntimeException(failure.cause());
                }
            }
        };
    }

    @Test(expected = IllegalStateException.class)
    public void testOnReceiveBatchedModificationsReadyWithIncorrectTotalMessageCount() throws Exception {
        new JavaTestKit(getSystem()) {
            {

                final ActorRef transaction = newTransactionActor(WO, readWriteTransaction(),
                        "testOnReceiveBatchedModificationsReadyWithIncorrectTotalMessageCount");

                JavaTestKit watcher = new JavaTestKit(getSystem());
                watcher.watch(transaction);

                BatchedModifications batched = new BatchedModifications(nextTransactionId(),
                        DataStoreVersions.CURRENT_VERSION);
                batched.setReady(true);
                batched.setTotalMessagesSent(2);

                transaction.tell(batched, getRef());

                Failure failure = expectMsgClass(duration("5 seconds"), akka.actor.Status.Failure.class);
                watcher.expectMsgClass(duration("5 seconds"), Terminated.class);

                if (failure != null) {
                    Throwables.throwIfInstanceOf(failure.cause(), Exception.class);
                    Throwables.throwIfUnchecked(failure.cause());
                    throw new RuntimeException(failure.cause());
                }
            }
        };
    }

    @Test
    public void testReadWriteTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(RW, readWriteTransaction(),
                        "testReadWriteTxOnReceiveCloseTransaction");

                watch(transaction);

                transaction.tell(new CloseTransaction().toSerializable(), getRef());

                expectMsgClass(duration("3 seconds"), CloseTransactionReply.class);
                expectTerminated(duration("3 seconds"), transaction);
            }
        };
    }

    @Test
    public void testWriteOnlyTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(WO, readWriteTransaction(),
                        "testWriteTxOnReceiveCloseTransaction");

                watch(transaction);

                transaction.tell(new CloseTransaction().toSerializable(), getRef());

                expectMsgClass(duration("3 seconds"), CloseTransactionReply.class);
                expectTerminated(duration("3 seconds"), transaction);
            }
        };
    }

    @Test
    public void testReadOnlyTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(TransactionType.READ_ONLY, readOnlyTransaction(),
                        "testReadOnlyTxOnReceiveCloseTransaction");

                watch(transaction);

                transaction.tell(new CloseTransaction().toSerializable(), getRef());

                expectMsgClass(duration("3 seconds"), Terminated.class);
            }
        };
    }

    @Test
    public void testShardTransactionInactivity() {
        datastoreContext = DatastoreContext.newBuilder().shardTransactionIdleTimeout(
                500, TimeUnit.MILLISECONDS).build();

        new JavaTestKit(getSystem()) {
            {
                final ActorRef transaction = newTransactionActor(RW, readWriteTransaction(),
                        "testShardTransactionInactivity");

                watch(transaction);

                expectMsgClass(duration("3 seconds"), Terminated.class);
            }
        };
    }

    public static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
