/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithEntityTypeEntry;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityTypeEntryWithEntityEntry;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.messages.GetShardDataTree;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Unit tests for DistributedEntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipServiceTest extends AbstractEntityOwnershipTest {
    static String ENTITY_TYPE = "test";
    static String ENTITY_TYPE2 = "test2";
    static int ID_COUNTER = 1;
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");

    private final String dataStoreType = "config" + ID_COUNTER++;
    private DistributedDataStore dataStore;

    @Before
    public void setUp() {
        DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreType(dataStoreType).
                shardInitializationTimeout(10, TimeUnit.SECONDS).build();

        Configuration configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider()) {
            @Override
            public Collection<String> getUniqueMemberNamesForAllShards() {
                return Sets.newHashSet("member-1");
            }
        };

        DatastoreContextFactory mockContextFactory = Mockito.mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(), configuration, mockContextFactory, null);

        dataStore.onGlobalContextUpdated(SchemaContextHelper.entityOwners());
    }

    @After
    public void tearDown() {
        dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build());
        service.start();

        Future<ActorRef> future = dataStore.getActorContext().findLocalShardAsync(
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
        ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);

        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final TestShardBuilder shardBuilder = new TestShardBuilder();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build()) {
            @Override
            protected EntityOwnershipShard.Builder newShardBuilder() {
                return shardBuilder;
            }
        };

        service.start();

        shardBuilder.expectShardMessage(RegisterCandidateLocal.class);

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        Entity entity = new Entity(ENTITY_TYPE, entityId);

        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(shardBuilder, entity);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE, entityId,
                dataStore.getActorContext().getCurrentMemberName());

        // Register the same entity - should throw exception

        try {
            service.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch(CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", entity, e.getEntity());
        }

        // Register a different entity - should succeed

        Entity entity2 = new Entity(ENTITY_TYPE2, entityId);
        shardBuilder.expectShardMessage(RegisterCandidateLocal.class);

        EntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2);

        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyRegisterCandidateLocal(shardBuilder, entity2);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE2, entityId,
                dataStore.getActorContext().getCurrentMemberName());

        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        final TestShardBuilder shardBuilder = new TestShardBuilder();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build()) {
            @Override
            protected EntityOwnershipShard.Builder newShardBuilder() {
                return shardBuilder;
            }
        };

        service.start();

        shardBuilder.expectShardMessage(RegisterCandidateLocal.class);

        Entity entity = new Entity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));

        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(shardBuilder, entity);

        shardBuilder.expectShardMessage(UnregisterCandidateLocal.class);

        reg.close();

        UnregisterCandidateLocal unregCandidate = shardBuilder.waitForShardMessage();
        assertEquals("getEntity", entity, unregCandidate.getEntity());

        // Re-register - should succeed.

        shardBuilder.expectShardMessage(RegisterCandidateLocal.class);

        service.registerCandidate(entity);

        verifyRegisterCandidateLocal(shardBuilder, entity);

        service.close();
    }

    @Test
    public void testListenerRegistration() {
        final TestShardBuilder shardBuilder = new TestShardBuilder();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build()) {
            @Override
            protected EntityOwnershipShard.Builder newShardBuilder() {
                return shardBuilder;
            }
        };

        service.start();

        shardBuilder.expectShardMessage(RegisterListenerLocal.class);

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipListener listener = mock(EntityOwnershipListener.class);

        EntityOwnershipListenerRegistration reg = service.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);
        assertEquals("getEntityType", entity.getType(), reg.getEntityType());
        assertEquals("getInstance", listener, reg.getInstance());

        RegisterListenerLocal regListener = shardBuilder.waitForShardMessage();
        assertSame("getListener", listener, regListener.getListener());
        assertEquals("getEntityType", entity.getType(), regListener.getEntityType());

        shardBuilder.expectShardMessage(UnregisterListenerLocal.class);

        reg.close();

        UnregisterListenerLocal unregListener = shardBuilder.waitForShardMessage();
        assertEquals("getEntityType", entity.getType(), unregListener.getEntityType());
        assertSame("getListener", listener, unregListener.getListener());

        service.close();
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        final TestShardBuilder shardBuilder = new TestShardBuilder();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build()) {
            @Override
            protected EntityOwnershipShard.Builder newShardBuilder() {
                return shardBuilder;
            }
        };

        service.start();

        ShardDataTree shardDataTree = new ShardDataTree(SchemaContextHelper.entityOwners());
        shardBuilder.setDataTree(shardDataTree.getDataTree());

        Entity entity1 = new Entity(ENTITY_TYPE, "one");
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithEntityTypeEntry(entityTypeEntryWithEntityEntry(entity1.getType(),
                entityEntryWithOwner(entity1.getId(), "member-1"))), shardDataTree);
        verifyGetOwnershipState(service, entity1, true, true);

        writeNode(entityPath(entity1.getType(), entity1.getId()), entityEntryWithOwner(entity1.getId(), "member-2"),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, false, true);

        writeNode(entityPath(entity1.getType(), entity1.getId()), entityEntryWithOwner(entity1.getId(), ""),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, false, false);

        Entity entity2 = new Entity(ENTITY_TYPE, "two");
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state.isPresent());

        writeNode(entityPath(entity2.getType(), entity2.getId()), ImmutableNodes.mapEntry(ENTITY_QNAME,
                ENTITY_ID_QNAME, entity2.getId()), shardDataTree);
        verifyGetOwnershipState(service, entity2, false, false);

        service.close();
    }

    @Test
    public void testIsCandidateRegistered() throws CandidateAlreadyRegisteredException {
        final TestShardBuilder shardBuilder = new TestShardBuilder();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore,
                EntityOwnerSelectionStrategyConfig.newBuilder().build()) {
            @Override
            protected EntityOwnershipShard.Builder newShardBuilder() {
                return shardBuilder;
            }
        };

        service.start();

        final Entity test = new Entity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));

        service.close();
    }

    private static void verifyGetOwnershipState(DistributedEntityOwnershipService service, Entity entity,
            boolean isOwner, boolean hasOwner) {
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertEquals("getOwnershipState present", true, state.isPresent());
        assertEquals("isOwner", isOwner, state.get().isOwner());
        assertEquals("hasOwner", hasOwner, state.get().hasOwner());
    }

    private void verifyEntityCandidate(ActorRef entityOwnershipShard, String entityType,
            YangInstanceIdentifier entityId, String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName,
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

    private static void verifyRegisterCandidateLocal(final TestShardBuilder shardBuilder, Entity entity) {
        RegisterCandidateLocal regCandidate = shardBuilder.waitForShardMessage();
        assertEquals("getEntity", entity, regCandidate.getEntity());
    }

    private static void verifyEntityOwnershipCandidateRegistration(Entity entity, EntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }

    static class TestShardBuilder extends EntityOwnershipShard.Builder {
        TestShardBuilder() {
            localMemberName("member-1").ownerSelectionStrategyConfig(
                    EntityOwnerSelectionStrategyConfig.newBuilder().build());
        }

        private final AtomicReference<CountDownLatch> messageReceived = new AtomicReference<>();
        private final AtomicReference<Object> receivedMessage = new AtomicReference<>();
        private final AtomicReference<Class<?>> messageClass = new AtomicReference<>();
        private final AtomicReference<DataTree> dataTree = new AtomicReference<>();

        @Override
        public Props props() {
            verify();
            return Props.create(TestEntityOwnershipShard.class,this, messageClass, messageReceived,
                    receivedMessage, dataTree);
        }

        @SuppressWarnings("unchecked")
        <T> T waitForShardMessage() {
            assertTrue("Message " + messageClass.get().getSimpleName() + " was not received",
                    Uninterruptibles.awaitUninterruptibly(messageReceived.get(), 5, TimeUnit.SECONDS));
            assertEquals("Message type", messageClass.get(), receivedMessage.get().getClass());
            return (T) receivedMessage.get();
        }

        void expectShardMessage(Class<?> ofType) {
            messageReceived.set(new CountDownLatch(1));
            receivedMessage.set(null);
            messageClass.set(ofType);
        }

        void setDataTree(DataTree tree) {
            this.dataTree.set(tree);
        }
    }

    static class TestEntityOwnershipShard extends EntityOwnershipShard {
        private final AtomicReference<CountDownLatch> messageReceived;
        private final AtomicReference<Object> receivedMessage;
        private final AtomicReference<Class<?>> messageClass;
        private final AtomicReference<DataTree> dataTree;

        protected TestEntityOwnershipShard(EntityOwnershipShard.Builder builder,
                AtomicReference<Class<?>> messageClass, AtomicReference<CountDownLatch> messageReceived,
                AtomicReference<Object> receivedMessage, AtomicReference<DataTree> dataTree) {
            super(builder);
            this.messageClass = messageClass;
            this.messageReceived = messageReceived;
            this.receivedMessage = receivedMessage;
            this.dataTree = dataTree;
        }

        @Override
        public void onReceiveCommand(final Object message) throws Exception {
            try {
                if(dataTree.get() != null && message instanceof GetShardDataTree) {
                    sender().tell(dataTree.get(), self());
                } else {
                    super.onReceiveCommand(message);
                }
            } finally {
                Class<?> expMsgClass = messageClass.get();
                if(expMsgClass != null && expMsgClass.equals(message.getClass())) {
                    receivedMessage.set(message);
                    messageReceived.get().countDown();
                }
            }
        }
    }
}
