/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership;

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
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_ID_QNAME;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_OWNERS_PATH;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.ENTITY_QNAME;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.candidatePath;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityEntryWithOwner;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityOwnersWithCandidate;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityOwnersWithEntityTypeEntry;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityPath;
import static org.opendaylight.controller.cluster.entityownership.EntityOwnersModel.entityTypeEntryWithEntityEntry;

import akka.actor.ActorRef;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.entityownership.messages.RegisterListenerLocal;
import org.opendaylight.controller.cluster.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.entityownership.messages.UnregisterListenerLocal;
import org.opendaylight.controller.cluster.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Unit tests for DistributedEntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipServiceTest extends AbstractClusterRefEntityOwnershipTest {
    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");
    static int ID_COUNTER = 1;

    private final String dataStoreName = "config" + ID_COUNTER++;
    private AbstractDataStore dataStore;

    @Before
    public void setUp() {
        DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreName(dataStoreName)
                .shardInitializationTimeout(10, TimeUnit.SECONDS).build();

        Configuration configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider()) {
            @Override
            public Collection<MemberName> getUniqueMemberNamesForAllShards() {
                return Sets.newHashSet(MemberName.forName("member-1"));
            }
        };

        DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(), configuration,
                mockContextFactory, null);

        dataStore.onGlobalContextUpdated(SchemaContextHelper.entityOwners());
    }

    @After
    public void tearDown() {
        dataStore.close();
    }

    private static <T> T verifyMessage(final DistributedEntityOwnershipService mock, final Class<T> type) {
        final ArgumentCaptor<T> message = ArgumentCaptor.forClass(type);
        verify(mock).executeLocalEntityOwnershipShardOperation(message.capture());
        return message.getValue();
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        DistributedEntityOwnershipService service = DistributedEntityOwnershipService.start(dataStore.getActorUtils(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());

        Future<ActorRef> future = dataStore.getActorUtils().findLocalShardAsync(
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
        ActorRef shardActor = Await.result(future, FiniteDuration.create(10, TimeUnit.SECONDS));
        assertNotNull(DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);

        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorUtils(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, entity);
        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE, entityId,
                dataStore.getActorUtils().getCurrentMemberName().getName());

        // Register the same entity - should throw exception

        try {
            service.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch (CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", entity, e.getEntity());
        }

        // Register a different entity - should succeed
        reset(service);

        DOMEntity entity2 = new DOMEntity(ENTITY_TYPE2, entityId);
        DOMEntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2);

        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE2, entityId,
                dataStore.getActorUtils().getCurrentMemberName().getName());

        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        DistributedEntityOwnershipService service = spy(DistributedEntityOwnershipService.start(
            dataStore.getActorUtils(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        DOMEntity entity = new DOMEntity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));

        DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

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
            dataStore.getActorUtils(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);
        DOMEntityOwnershipListener listener = mock(DOMEntityOwnershipListener.class);

        DOMEntityOwnershipListenerRegistration reg = service.registerListener(entity.getType(), listener);

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
            dataStore.getActorUtils(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final Shard mockShard = Mockito.mock(Shard.class);
        ShardDataTree shardDataTree = new ShardDataTree(mockShard, SchemaContextHelper.entityOwners(),
            TreeType.OPERATIONAL);

        when(service.getLocalEntityOwnershipShardDataTree()).thenReturn(shardDataTree.getDataTree());

        DOMEntity entity1 = new DOMEntity(ENTITY_TYPE, "one");
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithEntityTypeEntry(entityTypeEntryWithEntityEntry(entity1.getType(),
                entityEntryWithOwner(entity1.getIdentifier(), "member-1"))), shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.IS_OWNER);

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE,
                entity1.getIdentifier(), "member-2"), shardDataTree);
        writeNode(entityPath(entity1.getType(), entity1.getIdentifier()),
                entityEntryWithOwner(entity1.getIdentifier(), "member-2"), shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.OWNED_BY_OTHER);

        writeNode(entityPath(entity1.getType(), entity1.getIdentifier()), entityEntryWithOwner(entity1.getIdentifier(),
                ""), shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.NO_OWNER);

        DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, "two");
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertFalse("getOwnershipState present", state.isPresent());

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity2.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(entityPath(entity2.getType(), entity2.getIdentifier()), ImmutableNodes.mapEntry(ENTITY_QNAME,
                ENTITY_ID_QNAME, entity2.getIdentifier()), shardDataTree);
        verifyGetOwnershipState(service, entity2, EntityOwnershipState.NO_OWNER);

        deleteNode(candidatePath(entityPath(entity2.getType(), entity2.getIdentifier()), "member-1"), shardDataTree);
        Optional<EntityOwnershipState> state2 = service.getOwnershipState(entity2);
        assertFalse("getOwnershipState present", state2.isPresent());
        service.close();
    }

    @Test
    public void testIsCandidateRegistered() throws CandidateAlreadyRegisteredException {
        DistributedEntityOwnershipService service = DistributedEntityOwnershipService.start(dataStore.getActorUtils(),
                EntityOwnerSelectionStrategyConfig.newBuilder().build());

        final DOMEntity test = new DOMEntity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));

        service.close();
    }

    private static void verifyGetOwnershipState(final DistributedEntityOwnershipService service, final DOMEntity entity,
            final EntityOwnershipState expState) {
        Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertTrue("getOwnershipState present", state.isPresent());
        assertEquals("EntityOwnershipState", expState, state.get());
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void verifyEntityCandidate(final ActorRef entityOwnershipShard, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName, path -> {
            try {
                return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static void verifyRegisterCandidateLocal(final DistributedEntityOwnershipService service,
            final DOMEntity entity) {
        RegisterCandidateLocal regCandidate = verifyMessage(service, RegisterCandidateLocal.class);
        assertEquals("getEntity", entity, regCandidate.getEntity());
    }

    private static void verifyEntityOwnershipCandidateRegistration(final DOMEntity entity,
            final DOMEntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }
}
