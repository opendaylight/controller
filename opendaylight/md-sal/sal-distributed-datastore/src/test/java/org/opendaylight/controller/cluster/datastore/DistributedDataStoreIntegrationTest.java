package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSystem;
import akka.event.Logging;
import akka.testkit.JavaTestKit;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class DistributedDataStoreIntegrationTest {

    private static ActorSystem system;

    @Before
    public void setUp() throws IOException {
        File journal = new File("journal");

        if(journal.exists()) {
            FileUtils.deleteDirectory(journal);
        }


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
        final Configuration configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
        ShardStrategyFactory.setConfiguration(configuration);



        new JavaTestKit(getSystem()) {
            {

                new Within(duration("10 seconds")) {
                    @Override
                    protected void run() {
                        try {
                            final DistributedDataStore distributedDataStore =
                                new DistributedDataStore(getSystem(), "config",
                                        new MockClusterWrapper(), configuration,
                                        new DistributedDataStoreProperties());

                            distributedDataStore.onGlobalContextUpdated(TestModel.createTestContext());

                            // Wait for a specific log message to show up
                            final boolean result =
                                new JavaTestKit.EventFilter<Boolean>(Logging.Info.class
                                    ) {
                                    @Override
                                    protected Boolean run() {
                                        return true;
                                    }
                                }.from("akka://test/user/shardmanager-config/member-1-shard-test-1-config")
                                    .message("Switching from state Candidate to Leader")
                                    .occurrences(1).exec();

                            assertEquals(true, result);

                            DOMStoreReadWriteTransaction transaction =
                                distributedDataStore.newReadWriteTransaction();

                            transaction
                                .write(TestModel.TEST_PATH, ImmutableNodes
                                    .containerNode(TestModel.TEST_QNAME));

                            ListenableFuture<Optional<NormalizedNode<?, ?>>>
                                future =
                                transaction.read(TestModel.TEST_PATH);

                            Optional<NormalizedNode<?, ?>> optional =
                                future.get();

                            Assert.assertTrue("Node not found", optional.isPresent());

                            NormalizedNode<?, ?> normalizedNode =
                                optional.get();

                            assertEquals(TestModel.TEST_QNAME,
                                normalizedNode.getNodeType());

                            DOMStoreThreePhaseCommitCohort ready =
                                transaction.ready();

                            ListenableFuture<Boolean> canCommit =
                                ready.canCommit();

                            assertTrue(canCommit.get(5, TimeUnit.SECONDS));

                            ListenableFuture<Void> preCommit =
                                ready.preCommit();

                            preCommit.get(5, TimeUnit.SECONDS);

                            ListenableFuture<Void> commit = ready.commit();

                            commit.get(5, TimeUnit.SECONDS);
                        } catch (ExecutionException | TimeoutException | InterruptedException e){
                            fail(e.getMessage());
                        }
                    }
                };
            }
        };

    }


    //FIXME : Disabling test because it's flaky
    //@Test
    public void integrationTestWithMultiShardConfiguration()
        throws ExecutionException, InterruptedException, TimeoutException {
        final Configuration configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");

        ShardStrategyFactory.setConfiguration(configuration);

        new JavaTestKit(getSystem()) {
            {

                new Within(duration("10 seconds")) {
                    @Override
                    protected void run() {
                        try {
                            final DistributedDataStore distributedDataStore =
                                new DistributedDataStore(getSystem(), "config",
                                    new MockClusterWrapper(), configuration, null);

                            distributedDataStore.onGlobalContextUpdated(
                                SchemaContextHelper.full());

                            // Wait for a specific log message to show up
                            final boolean result =
                                new JavaTestKit.EventFilter<Boolean>(
                                    Logging.Info.class
                                ) {
                                    @Override
                                    protected Boolean run() {
                                        return true;
                                    }
                                }.from(
                                    "akka://test/user/shardmanager-config/member-1-shard-cars-1-config")
                                    .message(
                                        "Switching from state Candidate to Leader")
                                    .occurrences(1)
                                    .exec();

                            Thread.sleep(1000);


                            DOMStoreReadWriteTransaction transaction =
                                distributedDataStore.newReadWriteTransaction();

                            transaction.write(CarsModel.BASE_PATH, CarsModel.emptyContainer());
                            transaction.write(PeopleModel.BASE_PATH, PeopleModel.emptyContainer());

                            DOMStoreThreePhaseCommitCohort ready = transaction.ready();

                            ListenableFuture<Boolean> canCommit = ready.canCommit();

                            assertTrue(canCommit.get(5, TimeUnit.SECONDS));

                            ListenableFuture<Void> preCommit = ready.preCommit();

                            preCommit.get(5, TimeUnit.SECONDS);

                            ListenableFuture<Void> commit = ready.commit();

                            commit.get(5, TimeUnit.SECONDS);

                            assertEquals(true, result);
                        } catch(ExecutionException | TimeoutException | InterruptedException e){
                            fail(e.getMessage());
                        }
                    }
                };
            }
        };


    }

}
