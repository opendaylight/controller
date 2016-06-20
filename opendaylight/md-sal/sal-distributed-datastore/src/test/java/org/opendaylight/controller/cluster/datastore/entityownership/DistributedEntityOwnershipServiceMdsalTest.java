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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.binding.api.clustering.Entity;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListener;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.opendaylight.yangtools.yang.parser.spi.source.SourceException;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangInferencePipeline;
import org.opendaylight.yangtools.yang.parser.stmt.rfc6020.YangStatementSourceImpl;
import org.opendaylight.yangtools.yang.parser.util.NamedFileInputStream;
import akka.actor.ActorRef;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Unit tests for DistributedEntityOwnershipServiceMdsal.
 *
 */
public class DistributedEntityOwnershipServiceMdsalTest extends AbstractEntityOwnershipTest {

    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final String NAME = "foo";
    static int ID_COUNTER = 1;

    private final String dataStoreName = "config" + ID_COUNTER++;
    private DistributedDataStore dataStore;
    @Mock
    private SchemaService mockSchemaService;
    @Mock
    private ListenerRegistration<SchemaContextListener> mockSchemaCtxListReg;

    @Before
    public void setUp() throws SourceException, FileNotFoundException, ReactorException, URISyntaxException {
        MockitoAnnotations.initMocks(this);
        final DatastoreContext datastoreContext = DatastoreContext.newBuilder().dataStoreName(dataStoreName)
                .shardInitializationTimeout(10, TimeUnit.SECONDS).build();

        final Configuration configuration = new ConfigurationImpl(new EmptyModuleShardConfigProvider()) {
            @Override
            public Collection<MemberName> getUniqueMemberNamesForAllShards() {
                return Sets.newHashSet(MemberName.forName("member-1"));
            }
        };

        final SchemaContext schemaCx = loadSchemaContext("/test-data/");
        doReturn(schemaCx).when(mockSchemaService).getGlobalContext();
        doReturn(schemaCx).when(mockSchemaService).getSessionContext();
        doReturn(mockSchemaCtxListReg).when(mockSchemaService)
                .registerSchemaContextListener(any(BindingToNormalizedNodeCodec.class));
        doNothing().when(mockSchemaCtxListReg).close();


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
        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal
                .start(service, mockSchemaService);

        final Future<ActorRef> future = dataStore.getActorContext()
                .findLocalShardAsync(DOMDistributedEntityOwnershipServiceMdsal.ENTITY_OWNERSHIP_SHARD_NAME);
        final ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DOMDistributedEntityOwnershipServiceMdsal.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);
        bindService.close();
        verify(mockSchemaCtxListReg).close();
        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));
        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal
                .start(service, mockSchemaService);

        final Entity entity = new Entity(ENTITY_TYPE, NAME);
        final DOMEntity expEntity = new DOMEntity(ENTITY_TYPE, NAME);

        final EntityOwnershipCandidateRegistration reg = bindService.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, expEntity);
        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE, expEntity.getIdentifier(),
                dataStore.getActorContext().getCurrentMemberName().getName());

        // Register the same entity - should throw exception

        try {
            bindService.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch (final CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", expEntity, e.getEntity());
        }

        // Register a different entity - should succeed
        reset(service);

        final Entity entity2 = new Entity(ENTITY_TYPE2, "foo");
        final DOMEntity expEntity2 = new DOMEntity(ENTITY_TYPE2, "foo");
        final EntityOwnershipCandidateRegistration reg2 = bindService.registerCandidate(entity2);
        verifyRegisterCandidateLocal(service, expEntity2);
        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyEntityCandidate(service.getLocalEntityOwnershipShard(), ENTITY_TYPE2, expEntity2.getIdentifier(),
                dataStore.getActorContext().getCurrentMemberName().getName());

        bindService.close();
        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));
        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal
                .start(service, mockSchemaService);

        final Entity entity = new Entity(ENTITY_TYPE, NAME);
        final DOMEntity expEntity = new DOMEntity(ENTITY_TYPE, NAME);
        final EntityOwnershipCandidateRegistration reg = bindService.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(service, expEntity);

        reset(service);
        reg.close();
        final DOMUnregisterCandidateLocal unregCandidate = verifyMessage(service, DOMUnregisterCandidateLocal.class);
        assertEquals("getEntity", expEntity, unregCandidate.getEntity());

        // Re-register - should succeed.
        reset(service);
        bindService.registerCandidate(entity);
        verifyRegisterCandidateLocal(service, expEntity);

        bindService.close();
        service.close();
    }

    @Test
    public void testListenerRegistration() {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));
        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal
                .start(service, mockSchemaService);

        final Entity entity = new Entity(ENTITY_TYPE, NAME);
        final EntityOwnershipListener listener = mock(EntityOwnershipListener.class);

        final EntityOwnershipListenerRegistration reg = bindService.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);
        assertEquals("getEntityType", entity.getType(), reg.getEntityType());
        assertEquals("getInstance", listener, reg.getInstance());

        final DOMRegisterListenerLocal regListener = verifyMessage(service, DOMRegisterListenerLocal.class);
        assertEquals("getEntityType", entity.getType(), regListener.getEntityType());

        reset(service);
        reg.close();
        final DOMUnregisterListenerLocal unregListener = verifyMessage(service, DOMUnregisterListenerLocal.class);
        assertEquals("getEntityType", entity.getType(), unregListener.getEntityType());

        bindService.close();
        service.close();
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        final DOMDistributedEntityOwnershipServiceMdsal service = spy(DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build()));

        final ShardDataTree shardDataTree = new ShardDataTree(SchemaContextHelper.entityOwners(), TreeType.OPERATIONAL);

        when(service.getLocalEntityOwnershipShardDataTree()).thenReturn(shardDataTree.getDataTree());

        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal.start(service,
                mockSchemaService);

        final Entity entity1 = new Entity(ENTITY_TYPE, "one");
        final DOMEntity expEntity1 = new DOMEntity(ENTITY_TYPE, "one");
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, expEntity1.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithEntityTypeEntry(entityTypeEntryWithEntityEntry(entity1.getType(),
                entityEntryWithOwner(expEntity1.getIdentifier(), "member-1"))), shardDataTree);
        verifyGetOwnershipState(bindService, entity1, EntityOwnershipState.from(true, true));

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, expEntity1.getIdentifier(), "member-2"),
                shardDataTree);
        writeNode(entityPath(entity1.getType(), expEntity1.getIdentifier()),
                entityEntryWithOwner(expEntity1.getIdentifier(), "member-2"), shardDataTree);
        verifyGetOwnershipState(bindService, entity1, EntityOwnershipState.from(false, true));

        writeNode(entityPath(entity1.getType(), expEntity1.getIdentifier()),
                entityEntryWithOwner(expEntity1.getIdentifier(), ""), shardDataTree);
        verifyGetOwnershipState(bindService, entity1, EntityOwnershipState.from(false, false));

        final Entity entity2 = new Entity(ENTITY_TYPE, "two");
        final DOMEntity expEntity2 = new DOMEntity(ENTITY_TYPE, "two");
        final Optional<EntityOwnershipState> state = bindService.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state.isPresent());

        writeNode(ENTITY_OWNERS_PATH, entityOwnersWithCandidate(ENTITY_TYPE, expEntity2.getIdentifier(), "member-1"),
                shardDataTree);
        writeNode(entityPath(entity2.getType(), expEntity2.getIdentifier()),
                ImmutableNodes.mapEntry(ENTITY_QNAME, ENTITY_ID_QNAME, expEntity2.getIdentifier()), shardDataTree);
        verifyGetOwnershipState(bindService, entity2, EntityOwnershipState.from(false, false));

        deleteNode(candidatePath(entityPath(entity2.getType(), expEntity2.getIdentifier()), "member-1"), shardDataTree);
        final Optional<EntityOwnershipState> state2 = bindService.getOwnershipState(entity2);
        assertEquals("getOwnershipState present", false, state2.isPresent());
        bindService.close();
        service.close();
    }

    @Test
    public void testIsCandidateRegistered() throws CandidateAlreadyRegisteredException {
        final DOMDistributedEntityOwnershipServiceMdsal service = DOMDistributedEntityOwnershipServiceMdsal
                .start(dataStore.getActorContext(), EntityOwnerSelectionStrategyConfig.newBuilder().build());
        final DistributedEntityOwnershipServiceMdsal bindService = DistributedEntityOwnershipServiceMdsal
                .start(service, mockSchemaService);

        final Entity test = new Entity("test-type", "test");

        assertFalse(bindService.isCandidateRegistered(test));

        bindService.registerCandidate(test);

        assertTrue(bindService.isCandidateRegistered(test));

        bindService.close();
        service.close();
    }

    private static void verifyGetOwnershipState(final DistributedEntityOwnershipServiceMdsal service,
            final Entity entity, final EntityOwnershipState expState) {
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

    private static void verifyEntityOwnershipCandidateRegistration(final Entity entity,
            final EntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }

    private static Collection<File> loadFiles(final String resourceDirectory) throws FileNotFoundException {
        final String path = DistributedEntityOwnershipServiceMdsalTest.class.getResource(resourceDirectory).getPath();
        final File testDir = new File(path);
        final String[] fileList = testDir.list();
        final List<File> testFiles = new ArrayList<File>();
        if (fileList == null) {
            throw new FileNotFoundException(resourceDirectory);
        }
        for (int i = 0; i < fileList.length; i++) {
            final String fileName = fileList[i];
            if (new File(testDir, fileName).isDirectory() == false) {
                testFiles.add(new File(testDir, fileName));
            }
        }
        return testFiles;
    }

    private static SchemaContext loadSchemaContext(final String resourceDirectory)
            throws SourceException, ReactorException, FileNotFoundException, URISyntaxException {
        return parseYangSources(loadFiles(resourceDirectory));
    }

    private static SchemaContext parseYangSources(final Collection<File> testFiles)
            throws SourceException, ReactorException, FileNotFoundException {
        final CrossSourceStatementReactor.BuildAction reactor = YangInferencePipeline.RFC6020_REACTOR.newBuild();
        for (final File testFile : testFiles) {
            reactor.addSource(new YangStatementSourceImpl(new NamedFileInputStream(testFile, testFile.getPath())));
        }

        return reactor.buildEffective();
    }
}
