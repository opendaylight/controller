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
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStats;
import org.opendaylight.controller.cluster.datastore.messages.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class IntegrationTestKit extends ShardTestKit {

    protected DatastoreContext.Builder datastoreContextBuilder;
    protected DatastoreSnapshot restoreFromSnapshot;

    public IntegrationTestKit(final ActorSystem actorSystem, final Builder datastoreContextBuilder) {
        super(actorSystem);
        this.datastoreContextBuilder = datastoreContextBuilder;
    }

    public DatastoreContext.Builder getDatastoreContextBuilder() {
        return datastoreContextBuilder;
    }

    public DistributedDataStore setupDistributedDataStore(final String typeName, final String... shardNames) {
        return setupDistributedDataStore(typeName, "module-shards.conf", true, SchemaContextHelper.full(), shardNames);
    }

    public DistributedDataStore setupDistributedDataStore(final String typeName, final boolean waitUntilLeader,
            final String... shardNames) {
        return setupDistributedDataStore(typeName, "module-shards.conf", waitUntilLeader,
                SchemaContextHelper.full(), shardNames);
    }

    public DistributedDataStore setupDistributedDataStore(final String typeName, final String moduleShardsConfig,
            final boolean waitUntilLeader, final String... shardNames) {
        return setupDistributedDataStore(typeName, moduleShardsConfig, waitUntilLeader,
                SchemaContextHelper.full(), shardNames);
    }

    public DistributedDataStore setupDistributedDataStoreWithoutConfig(final String typeName, final SchemaContext schemaContext) {
        final ClusterWrapper cluster = new ClusterWrapperImpl(getSystem());
        final ConfigurationImpl configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider());

        getDatastoreContextBuilder().dataStoreName(typeName);

        final DatastoreContext datastoreContext = getDatastoreContextBuilder().build();

        final DatastoreContextFactory mockContextFactory = Mockito.mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        final DistributedDataStore dataStore = new DistributedDataStore(getSystem(), cluster, configuration, mockContextFactory, restoreFromSnapshot);

        dataStore.onGlobalContextUpdated(schemaContext);

        datastoreContextBuilder = DatastoreContext.newBuilderFrom(datastoreContext);
        return dataStore;
    }

    public DistributedDataStore setupDistributedDataStore(final String typeName, final String moduleShardsConfig,
            final boolean waitUntilLeader, final SchemaContext schemaContext, final String... shardNames) {
        final ClusterWrapper cluster = new ClusterWrapperImpl(getSystem());
        final Configuration config = new ConfigurationImpl(moduleShardsConfig, "modules.conf");

        datastoreContextBuilder.dataStoreName(typeName);

        final DatastoreContext datastoreContext = datastoreContextBuilder.build();
        final DatastoreContextFactory mockContextFactory = Mockito.mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        final DistributedDataStore dataStore = new DistributedDataStore(getSystem(), cluster, config, mockContextFactory,
                restoreFromSnapshot);

        dataStore.onGlobalContextUpdated(schemaContext);

        if(waitUntilLeader) {
            waitUntilLeader(dataStore.getActorContext(), shardNames);
        }

        datastoreContextBuilder = DatastoreContext.newBuilderFrom(datastoreContext);
        return dataStore;
    }

    public void waitUntilLeader(final ActorContext actorContext, final String... shardNames) {
        for(final String shardName: shardNames) {
            final ActorRef shard = findLocalShard(actorContext, shardName);

            assertNotNull("Shard was not created for " + shardName, shard);

            waitUntilLeader(shard);
        }
    }

    public void waitUntilNoLeader(final ActorContext actorContext, final String... shardNames) {
        for(final String shardName: shardNames) {
            final ActorRef shard = findLocalShard(actorContext, shardName);
            assertNotNull("No local shard found for " + shardName, shard);

            waitUntilNoLeader(shard);
        }
    }

    public void waitForMembersUp(final String... otherMembers) {
        final Set<String> otherMembersSet = Sets.newHashSet(otherMembers);
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 10) {
            final CurrentClusterState state = Cluster.get(getSystem()).state();
            for(final Member m: state.getMembers()) {
                if(m.status() == MemberStatus.up() && otherMembersSet.remove(m.getRoles().iterator().next()) &&
                        otherMembersSet.isEmpty()) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member(s) " + otherMembersSet + " are not Up");
    }

    public static ActorRef findLocalShard(final ActorContext actorContext, final String shardName) {
        ActorRef shard = null;
        for(int i = 0; i < 20 * 5 && shard == null; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            final Optional<ActorRef> shardReply = actorContext.findLocalShard(shardName);
            if(shardReply.isPresent()) {
                shard = shardReply.get();
            }
        }
        return shard;
    }

    public static void verifyShardStats(final DistributedDataStore datastore, final String shardName, final ShardStatsVerifier verifier)
            throws Exception {
        final ActorContext actorContext = datastore.getActorContext();

        final Future<ActorRef> future = actorContext.findLocalShardAsync(shardName);
        final ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));

        AssertionError lastError = null;
        final Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.SECONDS) <= 5) {
            final ShardStats shardStats = (ShardStats)actorContext.
                    executeOperation(shardActor, Shard.GET_SHARD_MBEAN_MESSAGE);

            try {
                verifier.verify(shardStats);
                return;
            } catch (final AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    void testWriteTransaction(final DistributedDataStore dataStore, final YangInstanceIdentifier nodePath,
            final NormalizedNode<?, ?> nodeToWrite) throws Exception {

        // 1. Create a write-only Tx

        final DOMStoreWriteTransaction writeTx = dataStore.newWriteOnlyTransaction();
        assertNotNull("newWriteOnlyTransaction returned null", writeTx);

        // 2. Write some data

        writeTx.write(nodePath, nodeToWrite);

        // 3. Ready the Tx for commit

        final DOMStoreThreePhaseCommitCohort cohort = writeTx.ready();

        // 4. Commit the Tx

        doCommit(cohort);

        // 5. Verify the data in the store

        final DOMStoreReadTransaction readTx = dataStore.newReadOnlyTransaction();

        final Optional<NormalizedNode<?, ?>> optional = readTx.read(nodePath).get(5, TimeUnit.SECONDS);
        assertEquals("isPresent", true, optional.isPresent());
        assertEquals("Data node", nodeToWrite, optional.get());
    }

    public void doCommit(final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
        final Boolean canCommit = cohort.canCommit().get(7, TimeUnit.SECONDS);
        assertEquals("canCommit", true, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void doCommit(final ListenableFuture<Boolean> canCommitFuture, final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
        final Boolean canCommit = canCommitFuture.get(7, TimeUnit.SECONDS);
        assertEquals("canCommit", true, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void assertExceptionOnCall(final Callable<Void> callable, final Class<? extends Exception> expType)
            throws Exception {
        try {
            callable.call();
            fail("Expected " + expType.getSimpleName());
        } catch(final Exception e) {
            assertEquals("Exception type", expType, e.getClass());
        }
    }

    void assertExceptionOnTxChainCreates(final DOMStoreTransactionChain txChain,
            final Class<? extends Exception> expType) throws Exception {
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

    public interface ShardStatsVerifier {
        void verify(ShardStats stats);
    }
}
