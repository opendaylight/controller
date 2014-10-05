package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import java.util.concurrent.TimeUnit;

public class DistributedDataStoreIntegrationTest extends AbstractActorTest {

    @Test
    public void testWriteTransactionWithSingleShard() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("transactionIntegrationTest", "test-1");

            testWriteTransaction(dataStore, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            testWriteTransaction(dataStore, TestModel.OUTER_LIST_PATH,
                    ImmutableNodes.mapNodeBuilder(TestModel.OUTER_LIST_QNAME).build());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testWriteTransactionWithMultipleShards() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testWriteTransactionWithMultipleShards", "cars-1", "people-1");

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            YangInstanceIdentifier nodePath1 = CarsModel.BASE_PATH;
            NormalizedNode<?, ?> nodeToWrite1 = CarsModel.emptyContainer();
            writeTx.write(nodePath1, nodeToWrite1);

            YangInstanceIdentifier nodePath2 = PeopleModel.BASE_PATH;
            NormalizedNode<?, ?> nodeToWrite2 = PeopleModel.emptyContainer();
            writeTx.write(nodePath2, nodeToWrite2);

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            Boolean canCommit = cohort.canCommit().get(5, TimeUnit.SECONDS);
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get(5, TimeUnit.SECONDS);
            cohort.commit().get(5, TimeUnit.SECONDS);

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath1).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite1, optional.get());

            optional = readTx.read(nodePath2).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite2, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testReadWriteTransaction() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("testReadWriteTransaction", "test-1");

         // 1. Create a read-write Tx

            DOMStoreReadWriteTransaction readWriteTx = dataStore.newReadWriteTransaction();
            assertNotNull("newReadWriteTransaction returned null", readWriteTx);

            // 2. Write some data

            YangInstanceIdentifier nodePath = TestModel.TEST_PATH;
            NormalizedNode<?, ?> nodeToWrite = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            readWriteTx.write(nodePath, nodeToWrite );

            // 3. Read the data from Tx

            Boolean exists = readWriteTx.exists(nodePath).checkedGet(5, TimeUnit.SECONDS);
            assertEquals("exists", true, exists);

            Optional<NormalizedNode<?, ?>> optional = readWriteTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());

            // 4. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = readWriteTx.ready();

            // 5. Commit the Tx

            Boolean canCommit = cohort.canCommit().get(5, TimeUnit.SECONDS);
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get(5, TimeUnit.SECONDS);
            cohort.commit().get(5, TimeUnit.SECONDS);

            // 6. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());

            cleanup(dataStore);
        }};
    }

    @Test
    public void testTransactionAbort() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("transactionAbortIntegrationTest", "test-1");

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            writeTx.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            cohort.canCommit().get(5, TimeUnit.SECONDS);

            cohort.abort().get(5, TimeUnit.SECONDS);

            testWriteTransaction(dataStore, TestModel.TEST_PATH,
                    ImmutableNodes.containerNode(TestModel.TEST_QNAME));

            cleanup(dataStore);
        }};
    }

    @Test
    public void testTransactionChain() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore =
                    setupDistributedDataStore("transactionChainIntegrationTest", "test-1");

            // 1. Create a Tx chain and write-only Tx

            DOMStoreTransactionChain txChain = dataStore.createTransactionChain();

            DOMStoreWriteTransaction writeTx = txChain.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data

            NormalizedNode<?, ?> containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx.write(TestModel.TEST_PATH, containerNode);

            // 3. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            // 4. Commit the Tx

            Boolean canCommit = cohort.canCommit().get(5, TimeUnit.SECONDS);
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get(5, TimeUnit.SECONDS);
            cohort.commit().get(5, TimeUnit.SECONDS);

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = txChain.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", containerNode, optional.get());

            txChain.close();

            cleanup(dataStore);
        }};
    }

    class IntegrationTestKit extends ShardTestKit {

        IntegrationTestKit(ActorSystem actorSystem) {
            super(actorSystem);
        }

        DistributedDataStore setupDistributedDataStore(String typeName, String... shardNames) {
            MockClusterWrapper cluster = new MockClusterWrapper();
            Configuration config = new ConfigurationImpl("module-shards.conf", "modules.conf");
            ShardStrategyFactory.setConfiguration(config);

            DatastoreContext datastoreContext = DatastoreContext.newBuilder().build();
            DistributedDataStore dataStore = new DistributedDataStore(getSystem(), typeName, cluster,
                    config, datastoreContext);

            SchemaContext schemaContext = SchemaContextHelper.full();
            dataStore.onGlobalContextUpdated(schemaContext);

            for(String shardName: shardNames) {
                ActorRef shard = null;
                for(int i = 0; i < 20 * 5 && shard == null; i++) {
                    Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
                    Optional<ActorRef> shardReply = dataStore.getActorContext().findLocalShard(shardName);
                    if(shardReply.isPresent()) {
                        shard = shardReply.get();
                    }
                }

                assertNotNull("Shard was not created", shard);

                System.out.println("!!!!!!shard: "+shard.path().toString());
                waitUntilLeader(shard);
            }

            return dataStore;
        }

        void testWriteTransaction(DistributedDataStore dataStore, YangInstanceIdentifier nodePath,
                NormalizedNode<?, ?> nodeToWrite) throws Exception {

            // 1. Create a write-only Tx

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data

            writeTx.write(nodePath, nodeToWrite);

            // 3. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            // 4. Commit the Tx

            Boolean canCommit = cohort.canCommit().get(5, TimeUnit.SECONDS);
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get(5, TimeUnit.SECONDS);
            cohort.commit().get(5, TimeUnit.SECONDS);

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", nodeToWrite, optional.get());
        }

        void cleanup(DistributedDataStore dataStore) {
            dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), null);
        }
    }

}
