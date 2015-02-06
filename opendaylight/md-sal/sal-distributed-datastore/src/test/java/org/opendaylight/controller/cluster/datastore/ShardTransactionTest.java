package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.ShardWriteTransaction.GetCompositeModificationReply;
import org.opendaylight.controller.cluster.datastore.exceptions.UnknownMessageException;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CreateSnapshot;
import org.opendaylight.controller.cluster.datastore.messages.DataExists;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;
import org.opendaylight.controller.cluster.datastore.messages.DeleteData;
import org.opendaylight.controller.cluster.datastore.messages.DeleteDataReply;
import org.opendaylight.controller.cluster.datastore.messages.MergeData;
import org.opendaylight.controller.cluster.datastore.messages.MergeDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransaction;
import org.opendaylight.controller.cluster.datastore.messages.ReadyTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.WriteData;
import org.opendaylight.controller.cluster.datastore.messages.WriteDataReply;
import org.opendaylight.controller.cluster.datastore.modification.CompositeModification;
import org.opendaylight.controller.cluster.datastore.modification.DeleteModification;
import org.opendaylight.controller.cluster.datastore.modification.MergeModification;
import org.opendaylight.controller.cluster.datastore.modification.Modification;
import org.opendaylight.controller.cluster.datastore.modification.WriteModification;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Encoded;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.duration.Duration;

public class ShardTransactionTest extends AbstractActorTest {

    private static final SchemaContext testSchemaContext = TestModel.createTestContext();

    private static final ShardIdentifier SHARD_IDENTIFIER =
        ShardIdentifier.builder().memberName("member-1")
            .shardName("inventory").type("config").build();

    private DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();

    private final ShardStats shardStats = new ShardStats(SHARD_IDENTIFIER.toString(), "DataStore");

    private final InMemoryDOMDataStore store =
            new InMemoryDOMDataStore("OPER", MoreExecutors.sameThreadExecutor());

    @Before
    public void setup() {
        store.onGlobalContextUpdated(testSchemaContext);
    }

    private ActorRef createShard(){
        return getSystem().actorOf(Shard.props(SHARD_IDENTIFIER,
            Collections.<ShardIdentifier, String>emptyMap(), datastoreContext, TestModel.createTestContext()));
    }

    private ActorRef newTransactionActor(DOMStoreTransaction transaction, String name) {
        return newTransactionActor(transaction, name, DataStoreVersions.CURRENT_VERSION);
    }

    private ActorRef newTransactionActor(DOMStoreTransaction transaction, String name, short version) {
        return newTransactionActor(transaction, null, name, version);
    }

    private ActorRef newTransactionActor(DOMStoreTransaction transaction, ActorRef shard, String name) {
        return newTransactionActor(transaction, null, name, DataStoreVersions.CURRENT_VERSION);
    }

    private ActorRef newTransactionActor(DOMStoreTransaction transaction, ActorRef shard, String name,
            short version) {
        Props props = ShardTransaction.props(transaction, shard != null ? shard : createShard(),
                testSchemaContext, datastoreContext, shardStats, "txn", version);
        return getSystem().actorOf(props, name);
    }

    @Test
    public void testOnReceiveReadData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();

            testOnReceiveReadData(newTransactionActor(store.newReadOnlyTransaction(), shard, "testReadDataRO"));

            testOnReceiveReadData(newTransactionActor(store.newReadWriteTransaction(), shard, "testReadDataRW"));
        }

        private void testOnReceiveReadData(final ActorRef transaction) {
            //serialized read
            transaction.tell(new ReadData(YangInstanceIdentifier.builder().build()).toSerializable(),
                getRef());

            Object replySerialized =
                    expectMsgClass(duration("5 seconds"), ReadDataReply.SERIALIZABLE_CLASS);

            assertNotNull(ReadDataReply.fromSerializable(replySerialized).getNormalizedNode());

            // unserialized read
            transaction.tell(new ReadData(YangInstanceIdentifier.builder().build()),getRef());

            ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

            assertNotNull(reply.getNormalizedNode());
        }};
    }

    @Test
    public void testOnReceiveReadDataWhenDataNotFound() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();

            testOnReceiveReadDataWhenDataNotFound(newTransactionActor(
                    store.newReadOnlyTransaction(), shard, "testReadDataWhenDataNotFoundRO"));

            testOnReceiveReadDataWhenDataNotFound(newTransactionActor(
                    store.newReadWriteTransaction(), shard, "testReadDataWhenDataNotFoundRW"));
        }

        private void testOnReceiveReadDataWhenDataNotFound(final ActorRef transaction) {
            // serialized read
            transaction.tell(new ReadData(TestModel.TEST_PATH).toSerializable(), getRef());

            Object replySerialized =
                    expectMsgClass(duration("5 seconds"), ReadDataReply.SERIALIZABLE_CLASS);

            assertTrue(ReadDataReply.fromSerializable(replySerialized).getNormalizedNode() == null);

            // unserialized read
            transaction.tell(new ReadData(TestModel.TEST_PATH),getRef());

            ReadDataReply reply = expectMsgClass(duration("5 seconds"), ReadDataReply.class);

            assertTrue(reply.getNormalizedNode() == null);
        }};
    }

    @Test
    public void testOnReceiveReadDataHeliumR1() throws Exception {
        new JavaTestKit(getSystem()) {{
            ActorRef transaction = newTransactionActor(store.newReadOnlyTransaction(),
                    "testOnReceiveReadDataHeliumR1", DataStoreVersions.HELIUM_1_VERSION);

            transaction.tell(new ReadData(YangInstanceIdentifier.builder().build()).toSerializable(),
                    getRef());

            ShardTransactionMessages.ReadDataReply replySerialized =
                    expectMsgClass(duration("5 seconds"), ShardTransactionMessages.ReadDataReply.class);

            assertNotNull(ReadDataReply.fromSerializable(replySerialized).getNormalizedNode());
        }};
    }

    @Test
    public void testOnReceiveDataExistsPositive() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();

            testOnReceiveDataExistsPositive(newTransactionActor(store.newReadOnlyTransaction(), shard,
                    "testDataExistsPositiveRO"));

            testOnReceiveDataExistsPositive(newTransactionActor(store.newReadWriteTransaction(), shard,
                    "testDataExistsPositiveRW"));
        }

        private void testOnReceiveDataExistsPositive(final ActorRef transaction) {
            transaction.tell(new DataExists(YangInstanceIdentifier.builder().build()).toSerializable(),
                getRef());

            ShardTransactionMessages.DataExistsReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DataExistsReply.class);

            assertTrue(DataExistsReply.fromSerializable(replySerialized).exists());

            // unserialized read
            transaction.tell(new DataExists(YangInstanceIdentifier.builder().build()),getRef());

            DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

            assertTrue(reply.exists());
        }};
    }

    @Test
    public void testOnReceiveDataExistsNegative() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef shard = createShard();

            testOnReceiveDataExistsNegative(newTransactionActor(store.newReadOnlyTransaction(), shard,
                    "testDataExistsNegativeRO"));

            testOnReceiveDataExistsNegative(newTransactionActor(store.newReadWriteTransaction(), shard,
                    "testDataExistsNegativeRW"));
        }

        private void testOnReceiveDataExistsNegative(final ActorRef transaction) {
            transaction.tell(new DataExists(TestModel.TEST_PATH).toSerializable(), getRef());

            ShardTransactionMessages.DataExistsReply replySerialized =
                expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DataExistsReply.class);

            assertFalse(DataExistsReply.fromSerializable(replySerialized).exists());

            // unserialized read
            transaction.tell(new DataExists(TestModel.TEST_PATH),getRef());

            DataExistsReply reply = expectMsgClass(duration("5 seconds"), DataExistsReply.class);

            assertFalse(reply.exists());
        }};
    }

    private void assertModification(final ActorRef subject,
        final Class<? extends Modification> modificationType) {
        new JavaTestKit(getSystem()) {{
            subject.tell(new ShardWriteTransaction.GetCompositedModification(), getRef());

            CompositeModification compositeModification = expectMsgClass(duration("3 seconds"),
                    GetCompositeModificationReply.class).getModification();

            assertTrue(compositeModification.getModifications().size() == 1);
            assertEquals(modificationType, compositeModification.getModifications().get(0).getClass());
        }};
    }

    @Test
    public void testOnReceiveWriteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newWriteOnlyTransaction(),
                    "testOnReceiveWriteData");

            transaction.tell(new WriteData(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME)).toSerializable(
                            DataStoreVersions.HELIUM_2_VERSION), getRef());

            expectMsgClass(duration("5 seconds"), ShardTransactionMessages.WriteDataReply.class);

            assertModification(transaction, WriteModification.class);

            // unserialized write
            transaction.tell(new WriteData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME)),
                getRef());

            expectMsgClass(duration("5 seconds"), WriteDataReply.class);
        }};
    }

    @Test
    public void testOnReceiveHeliumR1WriteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newWriteOnlyTransaction(),
                    "testOnReceiveHeliumR1WriteData", DataStoreVersions.HELIUM_1_VERSION);

            Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            ShardTransactionMessages.WriteData serialized = ShardTransactionMessages.WriteData.newBuilder()
                    .setInstanceIdentifierPathArguments(encoded.getEncodedPath())
                    .setNormalizedNode(encoded.getEncodedNode().getNormalizedNode()).build();

            transaction.tell(serialized, getRef());

            expectMsgClass(duration("5 seconds"), ShardTransactionMessages.WriteDataReply.class);

            assertModification(transaction, WriteModification.class);
        }};
    }

    @Test
    public void testOnReceiveMergeData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadWriteTransaction(),
                    "testMergeData");

            transaction.tell(new MergeData(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME)).toSerializable(
                            DataStoreVersions.HELIUM_2_VERSION), getRef());

            expectMsgClass(duration("5 seconds"), ShardTransactionMessages.MergeDataReply.class);

            assertModification(transaction, MergeModification.class);

            //unserialized merge
            transaction.tell(new MergeData(TestModel.TEST_PATH,
                ImmutableNodes.containerNode(TestModel.TEST_QNAME)),
                getRef());

            expectMsgClass(duration("5 seconds"), MergeDataReply.class);
        }};
    }

    @Test
    public void testOnReceiveHeliumR1MergeData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newWriteOnlyTransaction(),
                    "testOnReceiveHeliumR1MergeData", DataStoreVersions.HELIUM_1_VERSION);

            Encoded encoded = new NormalizedNodeToNodeCodec(null).encode(TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));
            ShardTransactionMessages.MergeData serialized = ShardTransactionMessages.MergeData.newBuilder()
                    .setInstanceIdentifierPathArguments(encoded.getEncodedPath())
                    .setNormalizedNode(encoded.getEncodedNode().getNormalizedNode()).build();

            transaction.tell(serialized, getRef());

            expectMsgClass(duration("5 seconds"), ShardTransactionMessages.MergeDataReply.class);

            assertModification(transaction, MergeModification.class);
        }};
    }

    @Test
    public void testOnReceiveDeleteData() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newWriteOnlyTransaction(),
                    "testDeleteData");

            transaction.tell(new DeleteData(TestModel.TEST_PATH).toSerializable(
                    DataStoreVersions.HELIUM_2_VERSION), getRef());

            expectMsgClass(duration("5 seconds"), ShardTransactionMessages.DeleteDataReply.class);

            assertModification(transaction, DeleteModification.class);

            //unserialized
            transaction.tell(new DeleteData(TestModel.TEST_PATH), getRef());

            expectMsgClass(duration("5 seconds"), DeleteDataReply.class);
        }};
    }


    @Test
    public void testOnReceiveReadyTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadWriteTransaction(),
                    "testReadyTransaction");

            watch(transaction);

            transaction.tell(new ReadyTransaction().toSerializable(), getRef());

            expectMsgAnyClassOf(duration("5 seconds"), ReadyTransactionReply.SERIALIZABLE_CLASS,
                    Terminated.class);
            expectMsgAnyClassOf(duration("5 seconds"), ReadyTransactionReply.SERIALIZABLE_CLASS,
                    Terminated.class);
        }};

        // test
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadWriteTransaction(),
                    "testReadyTransaction2");

            watch(transaction);

            transaction.tell(new ReadyTransaction(), getRef());

            expectMsgAnyClassOf(duration("5 seconds"), ReadyTransactionReply.class,
                    Terminated.class);
            expectMsgAnyClassOf(duration("5 seconds"), ReadyTransactionReply.class,
                    Terminated.class);
        }};
    }

    @Test
    public void testOnReceiveCreateSnapshot() throws Exception {
        new JavaTestKit(getSystem()) {{
            ShardTest.writeToStore(store, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            NormalizedNode<?,?> expectedRoot = ShardTest.readStore(store,
                    YangInstanceIdentifier.builder().build());

            final ActorRef transaction = newTransactionActor(store.newReadOnlyTransaction(),
                    "testOnReceiveCreateSnapshot");

            watch(transaction);

            transaction.tell(CreateSnapshot.INSTANCE, getRef());

            CaptureSnapshotReply reply = expectMsgClass(duration("3 seconds"), CaptureSnapshotReply.class);

            assertNotNull("getSnapshot is null", reply.getSnapshot());

            NormalizedNode<?,?> actualRoot = SerializationUtils.deserializeNormalizedNode(
                    reply.getSnapshot());

            assertEquals("Root node", expectedRoot, actualRoot);

            expectTerminated(duration("3 seconds"), transaction);
        }};
    }

    @Test
    public void testReadWriteTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadWriteTransaction(),
                    "testReadWriteTxOnReceiveCloseTransaction");

            watch(transaction);

            transaction.tell(new CloseTransaction().toSerializable(), getRef());

            expectMsgClass(duration("3 seconds"), CloseTransactionReply.SERIALIZABLE_CLASS);
            expectTerminated(duration("3 seconds"), transaction);
        }};
    }

    @Test
    public void testWriteOnlyTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newWriteOnlyTransaction(),
                    "testWriteTxOnReceiveCloseTransaction");

            watch(transaction);

            transaction.tell(new CloseTransaction().toSerializable(), getRef());

            expectMsgClass(duration("3 seconds"), CloseTransactionReply.SERIALIZABLE_CLASS);
            expectTerminated(duration("3 seconds"), transaction);
        }};
    }

    @Test
    public void testReadOnlyTxOnReceiveCloseTransaction() throws Exception {
        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadOnlyTransaction(),
                    "testReadOnlyTxOnReceiveCloseTransaction");

            watch(transaction);

            transaction.tell(new CloseTransaction().toSerializable(), getRef());

            expectMsgClass(duration("3 seconds"), Terminated.class);
        }};
    }

    @Test(expected=UnknownMessageException.class)
    public void testNegativePerformingWriteOperationOnReadTransaction() throws Exception {
        final ActorRef shard = createShard();
        final Props props = ShardTransaction.props(store.newReadOnlyTransaction(), shard,
                testSchemaContext, datastoreContext, shardStats, "txn",
                DataStoreVersions.CURRENT_VERSION);
        final TestActorRef<ShardTransaction> transaction = TestActorRef.apply(props,getSystem());

        transaction.receive(new DeleteData(TestModel.TEST_PATH).toSerializable(
                DataStoreVersions.CURRENT_VERSION), ActorRef.noSender());
    }

    @Test
    public void testShardTransactionInactivity() {

        datastoreContext = DatastoreContext.newBuilder().shardTransactionIdleTimeout(
                Duration.create(500, TimeUnit.MILLISECONDS)).build();

        new JavaTestKit(getSystem()) {{
            final ActorRef transaction = newTransactionActor(store.newReadWriteTransaction(),
                    "testShardTransactionInactivity");

            watch(transaction);

            expectMsgClass(duration("3 seconds"), Terminated.class);
        }};
    }
}
