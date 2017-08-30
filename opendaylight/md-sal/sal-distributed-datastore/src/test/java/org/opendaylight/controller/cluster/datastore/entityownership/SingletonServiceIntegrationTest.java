/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.datastore.entityownership.DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.MemberNode;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceProvider;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple integration test to check if the Entity Ownership Service implementation can support the Cluster Singleton
 * Service.
 *
 * @author Robert Varga
 */
public class SingletonServiceIntegrationTest {
    private static final class Service implements ClusterSingletonService {
        private final ClusterSingletonServiceProvider provider;
        private final ServiceGroupIdentifier identifier;

        private ClusterSingletonServiceRegistration reg;
        private long startCount;
        private long stopCount;

        Service(final ClusterSingletonServiceProvider provider, final String identifier) {
            this.provider = requireNonNull(provider);
            this.identifier = ServiceGroupIdentifier.create(identifier);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return identifier;
        }

        @Override
        public ListenableFuture<Void> closeServiceInstance() {
            LOG.debug("{} stopping", identifier);
            stopCount++;
            start();
            return Futures.immediateFuture(null);
        }

        @SuppressWarnings("checkstyle:illegalCatch")
        @Override
        public void instantiateServiceInstance() {
            LOG.debug("{} starting", identifier);
            startCount++;
            try {
                reg.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void start() {
            reg = provider.registerClusterSingletonService(this);
        }

        long getStartCount() {
            return startCount;
        }

        long getStopCount() {
            return stopCount;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SingletonServiceIntegrationTest.class);
    private static final String MODULE_SHARDS_CONFIG = "module-shards-default.conf";
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();
    private static final long TEST_TIME_SECONDS = 10;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5)
                    .shardIsolatedLeaderCheckIntervalInMillis(1000000);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10000);

    private final List<MemberNode> memberNodes = new ArrayList<>();

    private ClusterSingletonServiceProvider leaderSingleton;
    private ClusterSingletonServiceProvider follower1Singleton;
    private ClusterSingletonServiceProvider follower2Singleton;

    private static DistributedEntityOwnershipService newOwnershipService(final AbstractDataStore datastore) {
        return DistributedEntityOwnershipService.start(datastore.getActorContext(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());
    }

    @Before
    public void setup() throws Exception {
        final String name = "SingletonServiceIntegrationTest";
        MemberNode leaderNode = MemberNode.builder(memberNodes).akkaConfig("Member1").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(leaderDatastoreContextBuilder).build();

        MemberNode follower1Node = MemberNode.builder(memberNodes).akkaConfig("Member2").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        MemberNode follower2Node = MemberNode.builder(memberNodes).akkaConfig("Member3").testName(name)
                .moduleShardsConfig(MODULE_SHARDS_CONFIG).schemaContext(SCHEMA_CONTEXT).createOperDatastore(false)
                .datastoreContextBuilder(followerDatastoreContextBuilder).build();

        AbstractDataStore leaderDistributedDataStore = leaderNode.configDataStore();

        leaderDistributedDataStore.waitTillReady();
        follower1Node.configDataStore().waitTillReady();
        follower2Node.configDataStore().waitTillReady();

        final DOMEntityOwnershipService leaderEntityOwnershipService = newOwnershipService(leaderDistributedDataStore);
        final DOMEntityOwnershipService follower1EntityOwnershipService = newOwnershipService(
            follower1Node.configDataStore());
        final DOMEntityOwnershipService follower2EntityOwnershipService = newOwnershipService(
            follower2Node.configDataStore());

        leaderNode.kit().waitUntilLeader(leaderNode.configDataStore().getActorContext(), ENTITY_OWNERSHIP_SHARD_NAME);

        DOMClusterSingletonServiceProviderImpl impl = new DOMClusterSingletonServiceProviderImpl(
            leaderEntityOwnershipService);
        impl.initializeProvider();
        leaderSingleton = impl;

        impl = new DOMClusterSingletonServiceProviderImpl(follower1EntityOwnershipService);
        impl.initializeProvider();
        follower1Singleton = impl;

        impl = new DOMClusterSingletonServiceProviderImpl(follower2EntityOwnershipService);
        impl.initializeProvider();
        follower2Singleton = impl;
    }

    @After
    public void tearDown() throws Exception {
        follower2Singleton.close();
        follower1Singleton.close();
        leaderSingleton.close();

        for (MemberNode m : Lists.reverse(memberNodes)) {
            m.cleanup();
        }
        memberNodes.clear();
    }

    @Test
    public void testSingletonServiceIntegration() throws InterruptedException {
        List<Service> services = ImmutableList.of(new Service(leaderSingleton, "leader"),
            new Service(follower1Singleton, "follower1"),
            new Service(follower2Singleton, "follower2"));

        services.forEach(Service::start);

        LOG.info("Waiting {} seconds...", TEST_TIME_SECONDS);
        Thread.sleep(TimeUnit.SECONDS.toMillis(TEST_TIME_SECONDS));

        long stops = 0;
        long starts = 0;
        for (Service service : services) {
            starts += service.getStartCount();
            stops += service.getStopCount();
        }

        LOG.info("Services saw {} starts and {} stops in {} seconds", starts, stops, TEST_TIME_SECONDS);
        assertTrue(starts > 0);
        assertTrue(stops <= starts);
    }
}
