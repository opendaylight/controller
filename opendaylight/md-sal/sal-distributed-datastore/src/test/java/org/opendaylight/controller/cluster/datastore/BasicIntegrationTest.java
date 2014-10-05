/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;

public class BasicIntegrationTest extends AbstractActorTest {

    @Test
    public void transactionIntegrationTest() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore = setupDistributedDataStore("transactionIntegrationTest");

            // 1. Create a write-only Tx

            DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
            assertNotNull("newWriteOnlyTransaction returned null", writeTx);

            // 2. Write some data

            NormalizedNode<?, ?> containerNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);
            writeTx.write(TestModel.TEST_PATH, containerNode);

            // 3. Ready the Tx for commit

            DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

            // 4. Commit the Tx

            Boolean canCommit = cohort.canCommit().get();
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get();
            cohort.commit().get();

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", containerNode, optional.get());
        }};
    }

    @Test
    public void transactionChainIntegrationTest() throws Exception{
        System.setProperty("shard.persistent", "true");
        new IntegrationTestKit(getSystem()) {{
            DistributedDataStore dataStore = setupDistributedDataStore("transactionChainIntegrationTest");

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

            Boolean canCommit = cohort.canCommit().get();
            assertEquals("canCommit", true, canCommit);
            cohort.preCommit().get();
            cohort.commit().get();

            // 5. Verify the data in the store

            DOMStoreReadTransaction readTx = txChain.newReadOnlyTransaction();

            Optional<NormalizedNode<?, ?>> optional = readTx.read(TestModel.TEST_PATH).get();
            assertEquals("isPresent", true, optional.isPresent());
            assertEquals("Data node", containerNode, optional.get());
        }};
    }

    class IntegrationTestKit extends ShardTestKit {

        IntegrationTestKit(ActorSystem actorSystem) {
            super(actorSystem);
        }

        DistributedDataStore setupDistributedDataStore(String typeName) {
            MockClusterWrapper cluster = new MockClusterWrapper();
            Configuration mockConfig = mock(Configuration.class);
            String shardName = "default";
            doReturn(Lists.newArrayList(shardName)).when(mockConfig).getMemberShardNames(
                    cluster.getCurrentMemberName());
            doReturn(Collections.emptyList()).when(mockConfig).getMembersFromShardName(shardName);
            doReturn(Collections.emptyMap()).when(mockConfig).getModuleNameToShardStrategyMap();
            doReturn(Optional.of("odl-datastore-test")).when(mockConfig).getModuleNameFromNameSpace(
                    TestModel.TEST_QNAME.getNamespace().toASCIIString());

            ShardStrategyFactory.setConfiguration(mockConfig);

            DatastoreContext datastoreContext = new DatastoreContext();
            DistributedDataStore dataStore = new DistributedDataStore(getSystem(), typeName, cluster,
                    mockConfig, datastoreContext);

            SchemaContext schemaContext = TestModel.createTestContext();
            dataStore.onGlobalContextUpdated(schemaContext);

            ActorRef shard = null;
            for(int i = 0; i < 20 * 5 && shard == null; i++) {
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
                shard = dataStore.getActorContext().findLocalShard(shardName);
            }

            assertNotNull("Shard was not created", shard);

            waitUntilLeader(shard);
            return dataStore;
        }
    }
}
