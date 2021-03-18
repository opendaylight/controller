/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.service;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.ActorRef;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.AskPattern;
import akka.cluster.ddata.ORMap;
import akka.cluster.ddata.ORSet;
import akka.cluster.ddata.typed.javadsl.DistributedData;
import akka.cluster.ddata.typed.javadsl.Replicator;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.awaitility.Durations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.akka.eos.AbstractNativeEosTest;
import org.opendaylight.controller.cluster.akka.eos.bootstrap.command.RunningContext;
import org.opendaylight.controller.cluster.akka.eos.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.cluster.akka.eos.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class NativeEntityOwnershipServiceTest extends AbstractNativeEosTest {
    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");
    static int ID_COUNTER = 1;

    private ActorSystem system;
    private akka.actor.typed.ActorSystem<Void> typedSystem;
    private NativeEntityOwnershipService service;
    private ActorRef<Replicator.Command> replicator;

    @Before
    public void setUp() throws Exception {
        system = ActorSystem.create("ClusterSystem", ConfigFactory.load());
        typedSystem = Adapter.toTyped(this.system);
        replicator = DistributedData.get(typedSystem).replicator();

        service = new NativeEntityOwnershipService(system);
    }

    @After
    public void tearDown() {
        ActorTestKit.shutdown(Adapter.toTyped(system));
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyEntityCandidateRegistered(ENTITY_TYPE, entityId, "member-1");

        try {
            service.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch (final CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", entity, e.getEntity());
        }

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE2, entityId);
        final DOMEntityOwnershipCandidateRegistration reg2 = service.registerCandidate(entity2);

        verifyEntityOwnershipCandidateRegistration(entity2, reg2);
        verifyEntityCandidateRegistered(ENTITY_TYPE2, entityId, "member-1");
    }

    @Test
    public void testUnregisterCandidate() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final DOMEntityOwnershipCandidateRegistration reg = service.registerCandidate(entity);

        verifyEntityOwnershipCandidateRegistration(entity, reg);
        verifyEntityCandidateRegistered(ENTITY_TYPE, entityId, "member-1");

        reg.close();
        verifyEntityCandidateMissing(ENTITY_TYPE, entityId, "member-1");

        service.registerCandidate(entity);
        verifyEntityCandidateRegistered(ENTITY_TYPE, entityId, "member-1");
    }

    @Test
    public void testListenerRegistration() throws Exception {

        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);
        final MockEntityOwnershipListener listener = new MockEntityOwnershipListener("member-1");

        final DOMEntityOwnershipListenerRegistration reg = service.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);
        assertEquals("getEntityType", entity.getType(), reg.getEntityType());
        assertEquals("getInstance", listener, reg.getInstance());

        final DOMEntityOwnershipCandidateRegistration candidate = service.registerCandidate(entity);

        verifyListenerState(listener, entity, 0, true, true, false);

        reg.close();
        candidate.close();

        service.registerCandidate(entity);
        // check listener not called when listener registration closed
        await().pollDelay(Durations.TWO_SECONDS).until(() -> listener.getChanges().size() == 1);
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, "one");

        final DOMEntityOwnershipCandidateRegistration registration = service.registerCandidate(entity);
        verifyGetOwnershipState(service, entity, EntityOwnershipState.IS_OWNER);

        final RunningContext runningContext = service.getRunningContext();
        registerCandidates(runningContext.getCandidateRegistry(), entity, "member-2");

        final ActorRef<OwnerSupervisorCommand> ownerSupervisor = runningContext.getOwnerSupervisor();
        reachableMember(ownerSupervisor, "member-2");
        unreachableMember(ownerSupervisor, "member-1");
        verifyGetOwnershipState(service, entity, EntityOwnershipState.OWNED_BY_OTHER);

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, "two");
        final Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertFalse(state.isPresent());

        unreachableMember(ownerSupervisor, "member-2");
        verifyGetOwnershipState(service, entity, EntityOwnershipState.NO_OWNER);
    }

    @Test
    public void testIsCandidateRegistered() throws Exception {
        final DOMEntity test = new DOMEntity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));
    }

    private static void verifyGetOwnershipState(final DOMEntityOwnershipService service, final DOMEntity entity,
                                                final EntityOwnershipState expState) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            final Optional<EntityOwnershipState> state = service.getOwnershipState(entity);
            assertTrue("getOwnershipState present", state.isPresent());
            assertEquals("EntityOwnershipState", expState, state.get());
        });
    }

    private void verifyEntityCandidateRegistered(final String entityType,
                                                 final YangInstanceIdentifier entityId,
                                                 final String candidateName) {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> doVerifyEntityCandidateRegistered(entityType, entityId, candidateName));
    }

    private void doVerifyEntityCandidateRegistered(final String entityType,
                                                   final YangInstanceIdentifier entityId,
                                                   final String candidateName)
            throws ExecutionException, InterruptedException {
        final Map<DOMEntity, ORSet<String>> entries = getCandidateData();
        final DOMEntity entity = new DOMEntity(entityType, entityId);
        assertTrue(entries.containsKey(entity));
        assertTrue(entries.get(entity).getElements().contains(candidateName));
    }

    private void verifyEntityCandidateMissing(final String entityType,
                                              final YangInstanceIdentifier entityId,
                                              final String candidateName) {
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> doVerifyEntityCandidateMissing(entityType, entityId, candidateName));
    }

    private void doVerifyEntityCandidateMissing(final String entityType,
                                                final YangInstanceIdentifier entityId,
                                                final String candidateName)
            throws ExecutionException, InterruptedException {
        final Map<DOMEntity, ORSet<String>> entries = getCandidateData();
        final DOMEntity entity = new DOMEntity(entityType, entityId);
        assertTrue(entries.containsKey(entity));
        assertFalse(entries.get(entity).getElements().contains(candidateName));
    }

    private Map<DOMEntity, ORSet<String>> getCandidateData() throws ExecutionException, InterruptedException {
        final CompletionStage<Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>>> ask =
                AskPattern.ask(replicator, replyTo ->
                                new Replicator.Get<>(
                                        CandidateRegistry.KEY,
                                        Replicator.readLocal(),
                                        replyTo),
                        Duration.ofSeconds(5),
                        typedSystem.scheduler());

        final Replicator.GetResponse<ORMap<DOMEntity, ORSet<String>>> response = ask.toCompletableFuture().get();
        assertTrue(response instanceof Replicator.GetSuccess);

        final Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>> success =
                (Replicator.GetSuccess<ORMap<DOMEntity, ORSet<String>>>) response;

        return success.get(CandidateRegistry.KEY).getEntries();
    }

    private static void verifyEntityOwnershipCandidateRegistration(final DOMEntity entity,
                                                                   final DOMEntityOwnershipCandidateRegistration reg) {
        assertNotNull("EntityOwnershipCandidateRegistration null", reg);
        assertEquals("getInstance", entity, reg.getInstance());
    }
}