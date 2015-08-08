/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.entityownership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.DatastoreContext;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.RegisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.entityownership.messages.UnregisterCandidateLocal;
import org.opendaylight.controller.cluster.datastore.identifiers.ShardIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.MockClusterWrapper;
import org.opendaylight.controller.cluster.datastore.utils.MockConfiguration;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidate;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * Unit tests for DistributedEntityOwnershipService.
 *
 * @author Thomas Pantelis
 */
public class DistributedEntityOwnershipServiceTest extends AbstractActorTest {
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
        dataStore = new DistributedDataStore(getSystem(), new MockClusterWrapper(),
                new MockConfiguration(Collections.<String, List<String>>emptyMap()), datastoreContext );

        dataStore.onGlobalContextUpdated(TestModel.createTestContext());
    }

    @After
    public void tearDown() {
        dataStore.getActorContext().getShardManager().tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    @Test
    public void testEntityOwnershipShardCreated() throws Exception {
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore);
        service.start();

        Future<ActorRef> future = dataStore.getActorContext().findLocalShardAsync(
                DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME);
        ActorRef shardActor = Await.result(future, Duration.create(10, TimeUnit.SECONDS));
        assertNotNull(DistributedEntityOwnershipService.ENTITY_OWNERSHIP_SHARD_NAME + " not found", shardActor);

        service.close();
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final TestShardPropsCreator shardPropsCreator = new TestShardPropsCreator();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore) {
            @Override
            protected EntityOwnershipShardPropsCreator newShardPropsCreator() {
                return shardPropsCreator;
            }
        };

        service.start();

        shardPropsCreator.expectShardMessage(RegisterCandidateLocal.class);

        Entity entity = new Entity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity, candidate);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(shardPropsCreator, entity, candidate);

        // Register the same entity - should throw exception

        EntityOwnershipCandidate candidate2 = mock(EntityOwnershipCandidate.class);
        try {
            service.registerCandidate(entity, candidate2);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch(CandidateAlreadyRegisteredException e) {
            // expected
            assertSame("getCandidate", candidate, e.getRegisteredCandidate());
            assertEquals("getEntity", entity, e.getEntity());
        }

        // Register a different entity - should succeed

        Entity entity2 = new Entity(ENTITY_TYPE2, YangInstanceIdentifier.of(QNAME));
        shardPropsCreator.expectShardMessage(RegisterCandidateLocal.class);

        EntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2, candidate);

        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyRegisterCandidateLocal(shardPropsCreator, entity2, candidate);

        service.close();
    }

    @Test
    public void testCloseCandidateRegistration() throws Exception {
        final TestShardPropsCreator shardPropsCreator = new TestShardPropsCreator();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService(dataStore) {
            @Override
            protected EntityOwnershipShardPropsCreator newShardPropsCreator() {
                return shardPropsCreator;
            }
        };

        service.start();

        shardPropsCreator.expectShardMessage(RegisterCandidateLocal.class);

        Entity entity = new Entity(ENTITY_TYPE, YangInstanceIdentifier.of(QNAME));
        EntityOwnershipCandidate candidate = mock(EntityOwnershipCandidate.class);

        EntityOwnershipCandidateRegistration reg = service.registerCandidate(entity, candidate);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyRegisterCandidateLocal(shardPropsCreator, entity, candidate);

        shardPropsCreator.expectShardMessage(UnregisterCandidateLocal.class);

        reg.close();

        UnregisterCandidateLocal unregCandidate = shardPropsCreator.waitForShardMessage();
        assertEquals("getEntity", entity, unregCandidate.getEntity());

        // Re-register - should succeed.

        shardPropsCreator.expectShardMessage(RegisterCandidateLocal.class);

        service.registerCandidate(entity, candidate);

        verifyRegisterCandidateLocal(shardPropsCreator, entity, candidate);

        service.close();
    }

    @Test
    public void testRegisterListener() {
    }

    private void verifyRegisterCandidateLocal(final TestShardPropsCreator shardPropsCreator, Entity entity,
            EntityOwnershipCandidate candidate) {
        RegisterCandidateLocal regCandidate = shardPropsCreator.waitForShardMessage();
        assertSame("getCandidate", candidate, regCandidate.getCandidate());
        assertEquals("getEntity", entity, regCandidate.getEntity());
    }

    private void verifyEntityOwnershipCandidateRegistration(Entity entity, EntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getEntity", entity, reg.getEntity());
    }

    static class TestShardPropsCreator extends EntityOwnershipShardPropsCreator {
        private final AtomicReference<CountDownLatch> messageReceived = new AtomicReference<>();
        private final AtomicReference<Object> receivedMessage = new AtomicReference<>();
        private final AtomicReference<Class<?>> messageClass = new AtomicReference<>();

        @Override
        public Props newProps(ShardIdentifier shardId, Map<String, String> peerAddresses,
                DatastoreContext datastoreContext, SchemaContext schemaContext) {
            return Props.create(TestEntityOwnershipShard.class, shardId, peerAddresses, datastoreContext,
                    schemaContext, messageClass, messageReceived, receivedMessage);
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
    }

    static class TestEntityOwnershipShard extends EntityOwnershipShard {
        private final AtomicReference<CountDownLatch> messageReceived;
        private final AtomicReference<Object> receivedMessage;
        private final AtomicReference<Class<?>> messageClass;

        protected TestEntityOwnershipShard(ShardIdentifier name, Map<String, String> peerAddresses,
                DatastoreContext datastoreContext, SchemaContext schemaContext, AtomicReference<Class<?>> messageClass,
                AtomicReference<CountDownLatch> messageReceived, AtomicReference<Object> receivedMessage) {
            super(name, peerAddresses, datastoreContext, schemaContext);
            this.messageClass = messageClass;
            this.messageReceived = messageReceived;
            this.receivedMessage = receivedMessage;
        }

        @Override
        public void onReceiveCommand(final Object message) throws Exception {
            try {
                super.onReceiveCommand(message);
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
