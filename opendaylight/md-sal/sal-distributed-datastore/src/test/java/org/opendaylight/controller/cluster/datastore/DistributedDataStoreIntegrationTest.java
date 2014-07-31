package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.CarsModel;
import org.opendaylight.controller.md.cluster.datastore.model.PeopleModel;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

import java.util.concurrent.ExecutionException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class DistributedDataStoreIntegrationTest{

    private static ActorSystem system;

    @Before
    public void setUp() {
        System.setProperty("shard.persistent", "false");
        system = ActorSystem.create("test");
    }

    @After
    public void tearDown() {
        JavaTestKit.shutdownActorSystem(system);
        system = null;
    }

    protected ActorSystem getSystem() {
        return system;
    }

    @Test
    public void integrationTest() throws Exception {
        Configuration configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
        ShardStrategyFactory.setConfiguration(configuration);
        DistributedDataStore distributedDataStore =
            new DistributedDataStore(getSystem(), "config", new MockClusterWrapper(), configuration);

        distributedDataStore.onGlobalContextUpdated(TestModel.createTestContext());

        Thread.sleep(1000);

        DOMStoreReadWriteTransaction transaction =
            distributedDataStore.newReadWriteTransaction();

        transaction.write(TestModel.TEST_PATH, ImmutableNodes.containerNode(TestModel.TEST_QNAME));

        ListenableFuture<Optional<NormalizedNode<?, ?>>> future =
            transaction.read(TestModel.TEST_PATH);

        Optional<NormalizedNode<?, ?>> optional = future.get();

        NormalizedNode<?, ?> normalizedNode = optional.get();

        assertEquals(TestModel.TEST_QNAME, normalizedNode.getNodeType());

        DOMStoreThreePhaseCommitCohort ready = transaction.ready();

        ListenableFuture<Boolean> canCommit = ready.canCommit();

        assertTrue(canCommit.get());

        ListenableFuture<Void> preCommit = ready.preCommit();

        preCommit.get();

        ListenableFuture<Void> commit = ready.commit();

        commit.get();

    }


    @Test
    public void integrationTestWithMultiShardConfiguration()
        throws ExecutionException, InterruptedException {
        Configuration configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");

        ShardStrategyFactory.setConfiguration(configuration);
        DistributedDataStore distributedDataStore =
            new DistributedDataStore(getSystem(), "config", new MockClusterWrapper(), configuration);


        distributedDataStore.onGlobalContextUpdated(SchemaContextHelper.full());

        Thread.sleep(1000);

        DOMStoreReadWriteTransaction transaction =
            distributedDataStore.newReadWriteTransaction();

        transaction.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
        transaction.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

        DOMStoreThreePhaseCommitCohort ready = transaction.ready();

        ListenableFuture<Boolean> canCommit = ready.canCommit();

        assertTrue(canCommit.get());

        ListenableFuture<Void> preCommit = ready.preCommit();

        preCommit.get();

        ListenableFuture<Void> commit = ready.commit();

        commit.get();

    }

}
