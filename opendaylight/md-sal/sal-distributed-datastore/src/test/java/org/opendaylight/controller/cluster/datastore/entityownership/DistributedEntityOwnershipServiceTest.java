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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityOwnersWithEntityTypeEntry;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityPath;
import static org.opendaylight.controller.cluster.datastore.entityownership.EntityOwnersModel.entityTypeEntryWithEntityEntry;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
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
    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");
    static int ID_COUNTER = 1;

    private final String dataStoreName = "config" + ID_COUNTER++;
    private DistributedDataStore dataStore;

    @Before
    public void setUp() {
        DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreName(dataStoreName).
                shardInitializationTimeout(10, TimeUnit.SECONDS).build();

        Configuration configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider()) {
            @Override
            public Collection<String> getUniqueMemberNamesForAllShards() {
                return Sets.newHashSet("member-1");
            }
        };

        DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(), configuration, mockContextFactory, null);

        dataStore.onGlobalContextUpdated(SchemaContextHelper.entityOwners());
    }

    @After
    public void tearDown() {
        dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    private static <T> T verifyMessage(final DistributedEntityOwnershipService mock, final Class<T> type) {
        final ArgumentCaptor<T> message = ArgumentCaptor.forClass(type);
        verify(mock).executeLocalEntityOwnershipShardOperation(message.capture());
        return message.getValue();
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        DistributedEntityOwnershipService service = DistributedEntityOwnershipService.start(dataStore.getActorContext(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());

        Future<ActorRef> future = dataStore.getActorContext().findLocalShardAsync(
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
        ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);

        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        Entity entity = new Entity(ENTITY_TYPE, entityId);

        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, entity);
        verifyEntityOwnershipCandidateRegistration(entity, reg);
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
        reset(service);

        Entity entity2 = new Entity(ENTITY_TYPE2, entityId);
        EntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2);
        verifyRegisterCandidateLocal(service, entity2);
        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE2, entityId,
                dataStore.getActorContext().getCurrentMemberName());

        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        Entity entity = new Entity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));
        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(service, entity);

        reset(service);
        reg.close();
        UnregisterCandidateLocal unregCandidate = verifyMessage(service, UnregisterCandidateLocal.class);
        assertEquals("getEntity", entity, unregCandidate.getEntity());

        // Re-register - should succeed.
        reset(service);
        service.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, entity);

        service.close();
    }

    @Test
    public void testListenerRegistration() {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        Entity entity = new Entity(ENTITY_TYPE, entityId);
        EntityOwnershipListener listener = mock(EntityOwnershipListener.class);

        EntityOwnershipListenerRegistration reg = service.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);
        assertEquals("getEntityType", entity.getType(), reg.getEntityType());
        assertEquals("getInstance", listener, reg.getInstance());

        RegisterListenerLocal regListener = verifyMessage(service, RegisterListenerLocal.class);
        assertSame("getListener", listener, regListener.getListener());
        assertEquals("getEntityType", entity.getType(), regListener.getEntityType());

        reset(service);
        reg.close();
        UnregisterListenerLocal unregListener = verifyMessage(service, UnregisterListenerLocal.class);
        assertEquals("getEntityType", entity.getType(), unregListener.getEntityType());
        assertSame("getListener", listener, unregListener.getListener());

        service.close();
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        ShardDataTree shardDataTree = new ShardDataTree(SchemaContextHelper.entityOwners(), TreeType.OPERATIONAL);

        when(service.getLocalEntityOwnershipShardDataTree()).thenReturn(shardDataTree.getDataTree());

        Entity entity1 = new Entity(ENTITY_TYPE, "one");
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getId(), "member-1"), shardDataTree);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithEntityTypeEntry(entityTypeEntryWithEntityEntry(entity1.getType(),
                entityEntryWithOwner(entity1.getId(), "member-1"))), shardDataTree);
        verifyGetOwnershipState(service, entity1, true, true);

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getId(), "member-2"), shardDataTree);
        writeNode(entityPath(entity1.getType(), entity1.getId()), entityEntryWithOwner(entity1.getId(), "member-2"),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, false, true);

        writeNode(entityPath(entity1.getType(), entity1.getId()), entityEntryWithOwner(entity1.getId(), ""),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, false, false);

        Entity entity2 = new Entity(ENTITY_TYPE, "two");
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state.isPresent());

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity2.getId(), "member-1"), shardDataTree);
        writeNode(entityPath(entity2.getType(), entity2.getId()), ImmutableNodes.mapEntry(ENTITY_QNAME,
                ENTITY_ID_QNAME, entity2.getId()), shardDataTree);
        verifyGetOwnershipState(service, entity2, false, false);

        deleteNode(candidatePath(entityPath(entity2.getType(), entity2.getId()), "member-1"), shardDataTree);
        Optional<EntityOwnershipState> state2 = service.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state2.isPresent());
        service.close();
    }

    @Test
    public void testIsCandidateRegistered() throws CandidateAlreadyRegisteredException {
        DistributedEntityOwnershipService service = DistributedEntityOwnershipService.start(dataStore.getActorContext(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());

        final Entity test = new Entity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));

        service.close();
    }

    private static void verifyGetOwnershipState(final DistributedEntityOwnershipService service, final Entity entity,
            final boolean isOwner, final boolean hasOwner) {
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertEquals("getOwnershipState present", true, state.isPresent());
        assertEquals("isOwner", isOwner, state.get().isOwner());
        assertEquals("hasOwner", hasOwner, state.get().hasOwner());
    }

    private void verifyEntityCandidate(final ActorRef entityOwnershipShard, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName,
                path -> {
                    try {
                        return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    private static void verifyRegisterCandidateLocal(final DistributedEntityOwnershipService service, final Entity entity) {
        RegisterCandidateLocal regCandidate = verifyMessage(service, RegisterCandidateLocal.class);
        assertEquals("getEntity", entity, regCandidate.getEntity());
    }

    private static void verifyEntityOwnershipCandidateRegistration(final Entity entity, final EntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }

    static class TestEntityOwnershipShard extends EntityOwnershipShard {
        private final AtomicReference<CountDownLatch> messageReceived;
        private final AtomicReference<Object> receivedMessage;
        private final AtomicReference<Class<?>> messageClass;
        private final AtomicReference<DataTree> dataTree;

        protected TestEntityOwnershipShard(final EntityOwnershipShard.Builder builder,
                final AtomicReference<Class<?>> messageClass, final AtomicReference<CountDownLatch> messageReceived,
                final AtomicReference<Object> receivedMessage, final AtomicReference<DataTree> dataTree) {
            super(builder);
            this.messageClass = messageClass;
            this.messageReceived = messageReceived;
            this.receivedMessage = receivedMessage;
            this.dataTree = dataTree;
        }

        @Override
        public void handleCommand(final Object message) {
            try {
                if(dataTree.get() != null && message instanceof GetShardDataTree) {
                    sender().tell(dataTree.get(), self());
                } else {
                    super.handleCommand(message);
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
