/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.CANDIDATE_NAME_NODE_ID;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import akka.actor.ActorSystem;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.IntegrationTestKit;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.entity.owners.rev150804.entity.owners.entity.type.entity.Candidate;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * End-to-end integration tests for the entity ownership functionality.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipIntegrationTest {
    private static final Address MEMBER_1_ADDRESS = AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558");
    private static final String MODULE_SHARDS_CONFIG = "module-shards-default.conf";
    private static final String ENTITY_TYPE1 = "entityType1";
    private static final String ENTITY_TYPE2 = "entityType2";
    private static final Entity ENTITY1 = new Entity(ENTITY_TYPE1, "entity1");
    private static final Entity ENTITY1_2 = new Entity(ENTITY_TYPE2, "entity1");
    private static final Entity ENTITY2 = new Entity(ENTITY_TYPE1, "entity2");
    private static final Entity ENTITY3 = new Entity(ENTITY_TYPE1, "entity3");
    private static final Entity ENTITY4 = new Entity(ENTITY_TYPE1, "entity4");
    private static final SchemaContext SCHEMA_CONTEXT = SchemaContextHelper.entityOwners();

    private ActorSystem leaderSystem;
    private ActorSystem follower1System;
    private ActorSystem follower2System;

    private final DatastoreContext.Builder leaderDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(5);

    private final DatastoreContext.Builder followerDatastoreContextBuilder =
            DatastoreContext.newBuilder().shardHeartbeatIntervalInMillis(100).shardElectionTimeoutFactor(10000);

    private DistributedDataStore leaderDistributedDataStore;
    private DistributedDataStore follower1DistributedDataStore;
    private DistributedDataStore follower2DistributedDataStore;
    private DistributedEntityOwnershipService leaderEntityOwnershipService;
    private DistributedEntityOwnershipService follower1EntityOwnershipService;
    private DistributedEntityOwnershipService follower2EntityOwnershipService;
    private IntegrationTestKit leaderTestKit;
    private IntegrationTestKit follower1TestKit;
    private IntegrationTestKit follower2TestKit;

    @Mock
    private EntityOwnershipCandidate leaderMockCandidate;

    @Mock
    private EntityOwnershipCandidate follower1MockCandidate;

    @Mock
    private EntityOwnershipCandidate follower2MockCandidate;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        leaderSystem = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member1"));
        Cluster.get(leaderSystem).join(MEMBER_1_ADDRESS);

        follower1System = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member2"));
        Cluster.get(follower1System).join(MEMBER_1_ADDRESS);

        follower2System = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig("Member3"));
        Cluster.get(follower2System).join(MEMBER_1_ADDRESS);
    }

    @After
    public void tearDown() {
        JavaTestKit.shutdownActorSystem(leaderSystem);
        JavaTestKit.shutdownActorSystem(follower1System);
        JavaTestKit.shutdownActorSystem(follower2System);
    }

    private void initDatastores(String type) {
        leaderTestKit = new IntegrationTestKit(leaderSystem, leaderDatastoreContextBuilder);
        leaderDistributedDataStore = leaderTestKit.setupDistributedDataStore(
                type, MODULE_SHARDS_CONFIG, false, SCHEMA_CONTEXT);

        follower1TestKit = new IntegrationTestKit(follower1System, followerDatastoreContextBuilder);
        follower1DistributedDataStore = follower1TestKit.setupDistributedDataStore(
                type, MODULE_SHARDS_CONFIG, false, SCHEMA_CONTEXT);

        follower2TestKit = new IntegrationTestKit(follower2System, followerDatastoreContextBuilder);
        follower2DistributedDataStore = follower2TestKit.setupDistributedDataStore(
                type, MODULE_SHARDS_CONFIG, false, SCHEMA_CONTEXT);

        leaderDistributedDataStore.waitTillReady();
        follower1DistributedDataStore.waitTillReady();
        follower2DistributedDataStore.waitTillReady();

        leaderEntityOwnershipService = new DistributedEntityOwnershipService(leaderDistributedDataStore);
        leaderEntityOwnershipService.start();

        follower1EntityOwnershipService = new DistributedEntityOwnershipService(follower1DistributedDataStore);
        follower1EntityOwnershipService.start();

        follower2EntityOwnershipService = new DistributedEntityOwnershipService(follower2DistributedDataStore);
        follower2EntityOwnershipService.start();

        leaderTestKit.waitUntilLeader(leaderDistributedDataStore.getActorContext(),
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
    }

    @Test
    public void test() throws Exception {
        initDatastores("test");

        // Register leader candidate for entity1 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY1, leaderMockCandidate);
        verify(leaderMockCandidate, timeout(5000)).ownershipChanged(ENTITY1, false, true);
        reset(leaderMockCandidate);

        // Register leader candidate for entity1_2 (same id, different type) and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY1_2, leaderMockCandidate);
        verify(leaderMockCandidate, timeout(5000)).ownershipChanged(ENTITY1_2, false, true);
        reset(leaderMockCandidate);

        // Register follower1 candidate for entity1 and verify it gets added but doesn't become owner

        follower1EntityOwnershipService.registerCandidate(ENTITY1, follower1MockCandidate);
        verifyCandidates(leaderDistributedDataStore, ENTITY1, "member-1", "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY1, "member-1");
        verify(follower1MockCandidate, never()).ownershipChanged(any(Entity.class), anyBoolean(), anyBoolean());

        // Register follower1 candidate for entity2 and verify it becomes owner

        follower1EntityOwnershipService.registerCandidate(ENTITY2, follower1MockCandidate);
        verify(follower1MockCandidate, timeout(5000)).ownershipChanged(ENTITY2, false, true);
        reset(follower1MockCandidate);

        // Register follower2 candidate for entity2 and verify it gets added but doesn't become owner

        follower2EntityOwnershipService.registerCandidate(ENTITY2, follower2MockCandidate);
        verifyCandidates(leaderDistributedDataStore, ENTITY2, "member-2", "member-3");
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-2");
        verify(follower2MockCandidate, never()).ownershipChanged(any(Entity.class), anyBoolean(), anyBoolean());

        // Unregister follower1 candidate for entity2 and verify follower2 becomes owner

        follower1EntityOwnershipService.unregisterCandidate(ENTITY2, follower1MockCandidate);
        verify(follower2MockCandidate, timeout(5000)).ownershipChanged(ENTITY2, false, true);

        // Register follower1 candidate for entity3 and verify it becomes owner

        follower1EntityOwnershipService.registerCandidate(ENTITY3, follower1MockCandidate);
        verify(follower1MockCandidate, timeout(5000)).ownershipChanged(ENTITY3, false, true);
        verifyOwner(leaderDistributedDataStore, ENTITY3, "member-2");

        // Register follower2 candidate for entity4 and verify it becomes owner

        follower2EntityOwnershipService.registerCandidate(ENTITY4, follower2MockCandidate);
        verify(follower2MockCandidate, timeout(5000)).ownershipChanged(ENTITY4, false, true);
        reset(follower2MockCandidate);

        // Register follower1 candidate for entity4 and verify it gets added but doesn't become owner

        reset(follower1MockCandidate);
        follower1EntityOwnershipService.registerCandidate(ENTITY4, follower1MockCandidate);
        verifyCandidates(leaderDistributedDataStore, ENTITY4, "member-3", "member-2");
        verifyOwner(leaderDistributedDataStore, ENTITY4, "member-3");
        verify(follower1MockCandidate, never()).ownershipChanged(any(Entity.class), anyBoolean(), anyBoolean());

        // Shutdown follower2 and verify it's owned entities (entity 2 & 4) get re-assigned

        reset(follower1MockCandidate);
        JavaTestKit.shutdownActorSystem(follower2System);

        verify(follower1MockCandidate, timeout(15000)).ownershipChanged(ENTITY4, false, true);
        verifyOwner(leaderDistributedDataStore, ENTITY2, ""); // no other candidate

        // Register leader candidate for entity2 and verify it becomes owner

        leaderEntityOwnershipService.registerCandidate(ENTITY2, leaderMockCandidate);
        verify(leaderMockCandidate, timeout(5000)).ownershipChanged(ENTITY2, false, true);
        verifyOwner(leaderDistributedDataStore, ENTITY2, "member-1");

        // Unregister leader candidate for entity2 and verify the owner is cleared

        leaderEntityOwnershipService.unregisterCandidate(ENTITY2, leaderMockCandidate);
        verifyOwner(leaderDistributedDataStore, ENTITY2, "");
    }

    private void verifyCandidates(DistributedDataStore dataStore, Entity entity, String... expCandidates) throws Exception {
        AssertionError lastError = null;
        Stopwatch sw = Stopwatch.createStarted();
        while(sw.elapsed(TimeUnit.MILLISECONDS) <= 5000) {
            Optional<NormalizedNode<?, ?>> possible = dataStore.newReadOnlyTransaction().read(
                    entityPath(entity.getType(), entity.getId()).node(Candidate.QNAME)).get(5, TimeUnit.SECONDS);
            try {
                assertEquals("Candidates not found for " + entity, true, possible.isPresent());
                Collection<String> actual = new ArrayList<>();
                for(MapEntryNode candidate: ((MapNode)possible.get()).getValue()) {
                    actual.add(candidate.getChild(CANDIDATE_NAME_NODE_ID).get().getValue().toString());
                }

                assertEquals("Candidates for " + entity, Arrays.asList(expCandidates), actual);
                return;
            } catch (AssertionError e) {
                lastError = e;
                Uninterruptibles.sleepUninterruptibly(300, TimeUnit.MILLISECONDS);
            }
        }

        throw lastError;
    }

    private void verifyOwner(final DistributedDataStore dataStore, Entity entity, String expOwner) throws Exception {
        AbstractEntityOwnershipTest.verifyOwner(expOwner, entity.getType(), entity.getId(),
                new Function<YangInstanceIdentifier, NormalizedNode<?,?>>() {
                    @Override
                    public NormalizedNode<?, ?> apply(YangInstanceIdentifier path) {
                        try {
                            return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
                        } catch (Exception e) {
                            return null;
                        }
                    }
                });
    }
}
