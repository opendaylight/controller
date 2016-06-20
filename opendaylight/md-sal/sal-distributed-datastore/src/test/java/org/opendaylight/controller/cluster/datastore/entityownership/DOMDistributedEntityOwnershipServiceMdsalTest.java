/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DatastoreContextFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.ShardDataTree;
import org.opendaylight.controller.cluster.datastore.config.Configuration;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationImpl;
import org.opendaylight.controller.cluster.datastore.config.EmptyModuleShardConfigProvider;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMRegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMRegisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMUnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.DOMUnregisterListenerLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.md.cluster.datastore.model.SchemaContextHelper;
import org.opendaylight.mdsal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import akka.actor.ActorRef;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

public class DOMDistributedEntityOwnershipServiceMdsalTest extends AbstractEntityOwnershipTest {

    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");
    static int ID_COUNTER = 1;

    private final String dataStoreName = "config" + ID_COUNTER++;
    private DistributedDataStore dataStore;

    @Before
    public void setUp() {
        final DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreName(dataStoreName)
                .shardInitializationTimeout(10, TimeUnit.SECONDS).build();

        final Configuration configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider()) {
            @Override
            public Collection<MemberName> getUniqueMemberNamesForAllShards() {
                return Sets.newHashSet(MemberName.forName("member-1"));
            }
        };

        final DatastoreContextFactory mockContextFactory = mock(DatastoreContextFactory.class);
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getBaseDatastoreContext();
        Mockito.doReturn(datastoreContext).when(mockContextFactory).getShardDatastoreContext(Mockito.anyString());

        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(), configuration, mockContextFactory,
                null);

        dataStore.onGlobalContextUpdated(SchemaContextHelper.entityOwners());
    }

    @After
    public void tearDown() {
        dataStore.close();
    }

    private static <T> T verifyMessage(final DOMDistributedEntityOwnershipServiceMdsal mock, final Class<T> type) {
        final ArgumentCaptor<T> message = ArgumentCaptor.forClass(type);
        verify(mock).executeLocalEntityOwnershipShardOperation(message.capture());
        return message.getValue();
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build());

        final Future<ActorRef> future = dataStore.getActorContext()
                .findLocalShardAsync(DOMDistributedEntityOwnershipServiceMdsal.ENTITY_OWNERSHIP_SHARD_NAME);
        final ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DOMDistributedEntityOwnershipServiceMdsal.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);

        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, entity);
        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE, entityId,
                dataStore.getActorContext().getCurrentMemberName().getName());

        // Register the same entity - should throw exception

        try {
            service.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch (final CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", entity, e.getEntity());
        }

        // Register a different entity - should succeed
        reset(service);

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE2, entityId);
        final DOMEntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2);
        verifyRegisterCandidateLocal(service, entity2);
        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE2, entityId,
                dataStore.getActorContext().getCurrentMemberName().getName());

        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));
        final DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(service, entity);

        reset(service);
        reg.close();
        final DOMUnregisterCandidateLocal unregCandidate = verifyMessage(service, DOMUnregisterCandidateLocal.class);
        assertEquals("getEntity", entity, unregCandidate.getEntity());

        // Re-register - should succeed.
        reset(service);
        service.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, entity);

        service.close();
    }

    @Test
    public void testListenerRegistration() {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);
        final DOMEntityOwnershipListener listener = mock(DOMEntityOwnershipListener.class);

        final DOMEntityOwnershipListenerRegistration reg = service.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);
        assertEquals("getEntityType", entity.getType(), reg.getEntityType());
        assertEquals("getInstance", listener, reg.getInstance());

        final DOMRegisterListenerLocal regListener = verifyMessage(service, DOMRegisterListenerLocal.class);
        assertSame("getListener", listener, regListener.getListener());
        assertEquals("getEntityType", entity.getType(), regListener.getEntityType());

        reset(service);
        reg.close();
        final DOMUnregisterListenerLocal unregListener = verifyMessage(service, DOMUnregisterListenerLocal.class);
        assertEquals("getEntityType", entity.getType(), unregListener.getEntityType());
        assertSame("getListener", listener, unregListener.getListener());

        service.close();
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final ShardDataTree shardDataTree = new ShardDataTree(SchemaContextHelper.entityOwners(), TreeType.OPERATIONAL);

        when(service.getLocalEntityOwnershipShardDataTree()).thenReturn(shardDataTree.getDataTree());

        final DOMEntity entity1 = new DOMEntity(ENTITY_TYPE, "one");
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithEntityTypeEntry(
                entityTypeEntryWithEntityEntry(entity1.getType(),
                        entityEntryWithOwner(entity1.getIdentifier(), "member-1"))),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.from(true, true));

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity1.getIdentifier(), "member-2"),
                shardDataTree);
        writeNode(entityPath(entity1.getType(), entity1.getIdentifier()),
                entityEntryWithOwner(entity1.getIdentifier(), "member-2"),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.from(false, true));

        writeNode(entityPath(entity1.getType(), entity1.getIdentifier()),
                entityEntryWithOwner(entity1.getIdentifier(), ""),
                shardDataTree);
        verifyGetOwnershipState(service, entity1, EntityOwnershipState.from(false, false));

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, "two");
        final Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state.isPresent());

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, entity2.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(entityPath(entity2.getType(), entity2.getIdentifier()),
                ImmutableNodes.mapEntry(ENTITY_QNAME, ENTITY_ID_QNAME, entity2.getIdentifier()), shardDataTree);
        verifyGetOwnershipState(service, entity2, EntityOwnershipState.from(false, false));

        deleteNode(candidatePath(entityPath(entity2.getType(), entity2.getIdentifier()), "member-1"), shardDataTree);
        final Optional<EntityOwnershipState> state2 = service.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state2.isPresent());
        service.close();
    }

    @Test
    public void testIsCandidateRegistered() throws CandidateAlreadyRegisteredException {
        final DOMDistributedEntityOwnershipServiceMdsal service = DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build());

        final DOMEntity test = new DOMEntity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));

        service.close();
    }

    private static void verifyGetOwnershipState(final DOMDistributedEntityOwnershipServiceMdsal service,
            final DOMEntity entity,
            final EntityOwnershipState expState) {
        final Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
        assertEquals("getOwnershipState present", true, state.isPresent());
        assertEquals("isOwner", expState, state.get());
    }

    private void verifyEntityCandidate(final ActorRef entityOwnershipShard, final String entityType,
            final YangInstanceIdentifier entityId, final String candidateName) {
        verifyEntityCandidate(entityType, entityId, candidateName, path -> {
            try {
                return dataStore.newReadOnlyTransaction().read(path).get(5, TimeUnit.SECONDS).get();
            } catch (final Exception e) {
                return null;
            }
        });
    }

    private static void verifyRegisterCandidateLocal(final DOMDistributedEntityOwnershipServiceMdsal service,
            final DOMEntity entity) {
        final DOMRegisterCandidateLocal regCandidate = verifyMessage(service, DOMRegisterCandidateLocal.class);
        assertEquals("getEntity", entity, regCandidate.getEntity());
    }

    private static void verifyEntityOwnershipCandidateRegistration(final DOMEntity entity,
            final DOMEntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }
}
