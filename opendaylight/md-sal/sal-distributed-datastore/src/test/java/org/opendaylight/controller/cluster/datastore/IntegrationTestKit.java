/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.shardstrategy.ShardStrategyFactory;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

class IntegrationTestKit extends ShardTestKit {

    DatastoreContext.Builder datastoreContextBuilder;

    IntegrationTestKit(ActorSystem actorSystem, Builder datastoreContextBuilder) {
        super(actorSystem);
        this.datastoreContextBuilder = datastoreContextBuilder;
    }

    DistributedDataStore setupDistributedDataStore(String typeName, String... shardNames) {
        return setupDistributedDataStore(typeName, true, shardNames);
    }

    DistributedDataStore setupDistributedDataStore(String typeName, boolean waitUntilLeader,
            String... shardNames) {
        return setupDistributedDataStore(typeName, "module-shards.conf", waitUntilLeader, shardNames);
    }

    DistributedDataStore setupDistributedDataStore(String typeName, String moduleShardsConfig, boolean waitUntilLeader,
            String... shardNames) {
        ClusterWrapper cluster = new ClusterWrapperImpl(getSystem());
        Configuration config = new ConfigurationImpl(moduleShardsConfig, "modules.conf");
        ShardStrategyFactory.setConfiguration(config);

        datastoreContextBuilder.dataStoreType(typeName);

        DatastoreContext datastoreContext = datastoreContextBuilder.build();

        DistributedDataStore dataStore = new DistributedDataStore(getSystem(), cluster, config, datastoreContext);

        SchemaContext schemaContext = SchemaContextHelper.full();
        dataStore.onGlobalContextUpdated(schemaContext);

        if(waitUntilLeader) {
            waitUntilLeader(dataStore.getActorContext(), shardNames);
        }

        return dataStore;
    }

    void waitUntilLeader(ActorContext actorContext, String... shardNames) {
        for(String shardName: shardNames) {
            ActorRef shard = findLocalShard(actorContext, shardName);

            assertNotNull("Shard was not created", shard);

            waitUntilLeader(shard);
        }
    }

    void waitUntilNoLeader(ActorContext actorContext, String... shardNames) {
        for(String shardName: shardNames) {
            ActorRef shard = findLocalShard(actorContext, shardName);
            assertNotNull("No local shard found", shard);

            waitUntilNoLeader(shard);
        }
    }

    private ActorRef findLocalShard(ActorContext actorContext, String shardName) {
        ActorRef shard = null;
        for(int i = 0; i < 20 * 5 && shard == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            Optional<ActorRef> shardReply = actorContext.findLocalShard(shardName);
            if(shardReply.isPresent()) {
                shard = shardReply.get();
            }
        }
        return shard;
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

        doCommit(cohort);

        // 5. Verify the data in the store

        DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

        Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", nodeToWrite, optional.get());
    }

    void doCommit(final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
        Boolean canCommit = cohort.canCommit().get(7, TimeUnit.SECONDS);
        assertEquals("canCommit", true, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void doCommit(final ListenableFuture<Boolean> canCommitFuture, final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
        Boolean canCommit = canCommitFuture.get(7, TimeUnit.SECONDS);
        assertEquals("canCommit", true, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void cleanup(DistributedDataStore dataStore) {
        if(dataStore != null) {
            dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), null);
        }
    }

    void assertExceptionOnCall(Callable<Void> callable, Class<? extends Exception> expType)
            throws Exception {
        try {
            callable.call();
            fail("Expected " + expType.getSimpleName());
        } catch(Exception e) {
            assertEquals("Exception type", expType, e.getClass());
        }
    }

    void assertExceptionOnTxChainCreates(final DOMStoreTransactionChain txChain,
            Class<? extends Exception> expType) throws Exception {
        assertExceptionOnCall(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                txChain.newWriteOnlyTransaction();
                return null;
            }
        }, expType);

        assertExceptionOnCall(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                txChain.newReadWriteTransaction();
                return null;
            }
        }, expType);

        assertExceptionOnCall(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                txChain.newReadOnlyTransaction();
                return null;
            }
        }, expType);
    }
}