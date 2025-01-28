/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent.CurrentClusterState;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.cluster.MemberStatus;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.databroker.ClientBackedDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;
import org.opendaylight.controller.cluster.datastore.messages.OnDemandShardState;
import org.opendaylight.controller.cluster.datastore.persisted.DatastoreSnapshot;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.controller.cluster.raft.client.messages.GetOnDemandRaftState;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

public class IntegrationTestKit extends ShardTestKit {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestKit.class);

    protected DatastoreContext.Builder datastoreContextBuilder;
    protected DatastoreSnapshot restoreFromSnapshot;
    private final int commitTimeout;

    private final @NonNull Path stateDir;

    public IntegrationTestKit(final Path stateDir, final ActorSystem actorSystem,
            final Builder datastoreContextBuilder) {
        this(stateDir, actorSystem, datastoreContextBuilder, 7);
    }

    public IntegrationTestKit(final Path stateDir, final ActorSystem actorSystem, final Builder datastoreContextBuilder,
            final int commitTimeout) {
        super(actorSystem);
        this.stateDir = requireNonNull(stateDir);
        this.datastoreContextBuilder = datastoreContextBuilder;
        this.commitTimeout = commitTimeout;
    }

    public DatastoreContext.Builder getDatastoreContextBuilder() {
        return datastoreContextBuilder;
    }

    public ClientBackedDataStore setupDataStore(final Class<? extends ClientBackedDataStore> implementation,
            final String typeName, final String... shardNames) throws Exception {
        return setupDataStore(implementation, typeName, "module-shards.conf", true, SchemaContextHelper.full(),
            shardNames);
    }

    public ClientBackedDataStore setupDataStore(final Class<? extends ClientBackedDataStore> implementation,
            final String typeName, final boolean waitUntilLeader, final String... shardNames) throws Exception {
        return setupDataStore(implementation, typeName, "module-shards.conf", waitUntilLeader,
            SchemaContextHelper.full(), shardNames);
    }

    public ClientBackedDataStore setupDataStore(final Class<? extends ClientBackedDataStore> implementation,
            final String typeName, final String moduleShardsConfig, final boolean waitUntilLeader,
            final String... shardNames) throws Exception {
        return setupDataStore(implementation, typeName, moduleShardsConfig, waitUntilLeader,
            SchemaContextHelper.full(), shardNames);
    }

    public ClientBackedDataStore setupDataStore(final Class<? extends ClientBackedDataStore> implementation,
            final String typeName, final String moduleShardsConfig, final boolean waitUntilLeader,
            final EffectiveModelContext schemaContext, final String... shardNames) throws Exception {
        return setupDataStore(implementation, typeName, moduleShardsConfig, "modules.conf", waitUntilLeader,
            schemaContext, shardNames);
    }

    private ClientBackedDataStore setupDataStore(final Class<? extends ClientBackedDataStore> implementation,
            final String typeName, final String moduleShardsConfig, final String modulesConfig,
            final boolean waitUntilLeader, final EffectiveModelContext schemaContext, final String... shardNames)
                throws Exception {
        final ClusterWrapper cluster = new ClusterWrapperImpl(getSystem());
        final Configuration config = new ConfigurationImpl(moduleShardsConfig, modulesConfig);

        setDataStoreName(typeName);

        final DatastoreContext datastoreContext = datastoreContextBuilder.build();
        final DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(anyString());

        final var constructor = implementation.getDeclaredConstructor(Path.class, ActorSystem.class,
            ClusterWrapper.class, Configuration.class, DatastoreContextFactory.class, DatastoreSnapshot.class);

        final var dataStore = constructor.newInstance(stateDir, getSystem(), cluster, config, mockContextFactory,
            restoreFromSnapshot);

        dataStore.onModelContextUpdated(schemaContext);

        if (waitUntilLeader) {
            waitUntilLeader(dataStore.getActorUtils(), shardNames);
        }

        datastoreContextBuilder = DatastoreContext.newBuilderFrom(datastoreContext);
        return dataStore;
    }

    private void setDataStoreName(final String typeName) {
        if ("config".equals(typeName)) {
            datastoreContextBuilder.logicalStoreType(LogicalDatastoreType.CONFIGURATION);
        } else if ("operational".equals(typeName)) {
            datastoreContextBuilder.logicalStoreType(LogicalDatastoreType.OPERATIONAL);
        } else {
            datastoreContextBuilder.dataStoreName(typeName);
        }
    }

    public void waitUntilLeader(final ActorUtils actorUtils, final String... shardNames) {
        for (String shardName: shardNames) {
            ActorRef shard = findLocalShard(actorUtils, shardName);

            assertNotNull("Shard was not created for " + shardName, shard);

            waitUntilLeader(shard);
        }
    }

    public void waitUntilNoLeader(final ActorUtils actorUtils, final String... shardNames) {
        for (String shardName: shardNames) {
            ActorRef shard = findLocalShard(actorUtils, shardName);
            assertNotNull("No local shard found for " + shardName, shard);

            waitUntilNoLeader(shard);
        }
    }

    public void waitForMembersUp(final String... otherMembers) {
        Set<String> otherMembersSet = Sets.newHashSet(otherMembers);
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 10) {
            CurrentClusterState state = Cluster.get(getSystem()).state();
            for (Member m: state.getMembers()) {
                if (m.status() == MemberStatus.up() && otherMembersSet.remove(m.getRoles().iterator().next())
                        && otherMembersSet.isEmpty()) {
                    return;
                }
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }

        fail("Member(s) " + otherMembersSet + " are not Up");
    }

    public static ActorRef findLocalShard(final ActorUtils actorUtils, final String shardName) {
        for (int i = 0; i < 20 * 5; i++) {
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            Optional<ActorRef> shardReply = actorUtils.findLocalShard(shardName);
            if (shardReply.isPresent()) {
                return shardReply.orElseThrow();
            }
        }
        return null;
    }

    public static void waitUntilShardIsDown(final ActorUtils actorUtils, final String shardName) {
        for (int i = 0; i < 20 * 5 ; i++) {
            LOG.debug("Waiting for shard down {}", shardName);
            Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            Optional<ActorRef> shardReply = actorUtils.findLocalShard(shardName);
            if (!shardReply.isPresent()) {
                return;
            }
        }

        throw new IllegalStateException("Shard[" + shardName + " did not shutdown in time");
    }

    public static void verifyShardStats(final ClientBackedDataStore datastore, final String shardName,
            final ShardStatsVerifier verifier) throws Exception {
        ActorUtils actorUtils = datastore.getActorUtils();

        Future<ActorRef> future = actorUtils.findLocalShardAsync(shardName);
        ActorRef shardActor = Await.result(future, FiniteDuration.create(10, TimeUnit.SECONDS));

        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            final var shardStats = (ShardStatsMXBean) actorUtils
                    .executeOperation(shardActor, Shard.GET_SHARD_MBEAN_MESSAGE);

            try {
                verifier.verify(shardStats);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    public static void verifyShardState(final ClientBackedDataStore datastore, final String shardName,
            final Consumer<OnDemandShardState> verifier) throws Exception {
        ActorUtils actorUtils = datastore.getActorUtils();

        Future<ActorRef> future = actorUtils.findLocalShardAsync(shardName);
        ActorRef shardActor = Await.result(future, FiniteDuration.create(10, TimeUnit.SECONDS));

        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 5) {
            OnDemandShardState shardState = (OnDemandShardState)actorUtils
                    .executeOperation(shardActor, GetOnDemandRaftState.INSTANCE);

            try {
                verifier.accept(shardState);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    void testWriteTransaction(final ClientBackedDataStore dataStore, final YangInstanceIdentifier nodePath,
            final NormalizedNode nodeToWrite) throws Exception {

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
        assertEquals(Optional.of(nodeToWrite), readTx.read(nodePath).get(5, TimeUnit.SECONDS));
    }

    public void doCommit(final DOMStoreThreePhaseCommitCohort cohort) throws Exception {
        Boolean canCommit = cohort.canCommit().get(commitTimeout, TimeUnit.SECONDS);
        assertEquals("canCommit", Boolean.TRUE, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void doCommit(final ListenableFuture<Boolean> canCommitFuture, final DOMStoreThreePhaseCommitCohort cohort)
            throws Exception {
        Boolean canCommit = canCommitFuture.get(commitTimeout, TimeUnit.SECONDS);
        assertEquals("canCommit", Boolean.TRUE, canCommit);
        cohort.preCommit().get(5, TimeUnit.SECONDS);
        cohort.commit().get(5, TimeUnit.SECONDS);
    }

    void assertExceptionOnTxChainCreates(final DOMStoreTransactionChain txChain,
            final Class<? extends Exception> expType) {
        assertThrows(expType, () -> txChain.newWriteOnlyTransaction());
        assertThrows(expType, () -> txChain.newReadWriteTransaction());
        assertThrows(expType, () -> txChain.newReadOnlyTransaction());
    }

    public interface ShardStatsVerifier {
        void verify(ShardStatsMXBean stats);
    }
}
