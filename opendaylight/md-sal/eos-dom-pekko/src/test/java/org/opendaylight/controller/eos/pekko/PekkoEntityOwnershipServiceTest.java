/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.pekko;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.Adapter;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.apache.pekko.cluster.ddata.ORMap;
import org.apache.pekko.cluster.ddata.ORSet;
import org.apache.pekko.cluster.ddata.typed.javadsl.DistributedData;
import org.apache.pekko.cluster.ddata.typed.javadsl.Replicator;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import org.awaitility.Durations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.eos.pekko.bootstrap.command.RunningContext;
import org.opendaylight.controller.eos.pekko.owner.supervisor.command.OwnerSupervisorCommand;
import org.opendaylight.controller.eos.pekko.registry.candidate.CandidateRegistry;
import org.opendaylight.mdsal.eos.common.api.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.eos.common.api.EntityOwnershipState;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.NodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.get.entities.output.EntitiesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class PekkoEntityOwnershipServiceTest extends AbstractNativeEosTest {
    static final String ENTITY_TYPE = "test";
    static final String ENTITY_TYPE2 = "test2";
    static final QName QNAME = QName.create("test", "2015-08-11", "foo");

    private ActorSystem system;
    private org.apache.pekko.actor.typed.ActorSystem<Void> typedSystem;
    private PekkoEntityOwnershipService service;
    private ActorRef<Replicator.Command> replicator;

    @Before
    public void setUp() throws Exception {
        system = ActorSystem.create("ClusterSystem", ConfigFactory.load());
        typedSystem = Adapter.toTyped(system);
        replicator = DistributedData.get(typedSystem).replicator();

        service = new PekkoEntityOwnershipService(system, CODEC_CONTEXT);
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        service.close();
        ActorTestKit.shutdown(Adapter.toTyped(system), Duration.ofSeconds(20));
    }

    @Test
    public void testRegisterCandidate() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final Registration reg = service.registerCandidate(entity);
        assertNotNull(reg);

        verifyEntityCandidateRegistered(ENTITY_TYPE, entityId, "member-1");

        try {
            service.registerCandidate(entity);
            fail("Expected CandidateAlreadyRegisteredException");
        } catch (final CandidateAlreadyRegisteredException e) {
            // expected
            assertEquals("getEntity", entity, e.getEntity());
        }

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE2, entityId);
        final Registration reg2 = service.registerCandidate(entity2);

        assertNotNull(reg2);
        verifyEntityCandidateRegistered(ENTITY_TYPE2, entityId, "member-1");
    }

    @Test
    public void testUnregisterCandidate() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(QNAME);
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final Registration reg = service.registerCandidate(entity);
        assertNotNull(reg);

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

        final Registration reg = service.registerListener(entity.getType(), listener);

        assertNotNull("EntityOwnershipListenerRegistration null", reg);

        final Registration candidate = service.registerCandidate(entity);

        verifyListenerState(listener, entity, true, true, false);
        final int changes = listener.getChanges().size();

        reg.close();
        candidate.close();

        verifyEntityCandidateMissing(ENTITY_TYPE, entityId, "member-1");

        service.registerCandidate(entity);
        // check listener not called when listener registration closed
        await().pollDelay(Durations.TWO_SECONDS).until(() -> listener.getChanges().size() == changes);
    }

    @Test
    public void testGetOwnershipState() throws Exception {
        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, "one");

        final Registration registration = service.registerCandidate(entity);
        verifyGetOwnershipState(service, entity, EntityOwnershipState.IS_OWNER);

        final RunningContext runningContext = service.getRunningContext();
        registerCandidates(runningContext.getCandidateRegistry(), entity, "member-2");

        final ActorRef<OwnerSupervisorCommand> ownerSupervisor = runningContext.getOwnerSupervisor();
        reachableMember(ownerSupervisor, "member-2", DEFAULT_DATACENTER);
        unreachableMember(ownerSupervisor, "member-1", DEFAULT_DATACENTER);
        verifyGetOwnershipState(service, entity, EntityOwnershipState.OWNED_BY_OTHER);

        final DOMEntity entity2 = new DOMEntity(ENTITY_TYPE, "two");
        final Optional<EntityOwnershipState> state = service.getOwnershipState(entity2);
        assertFalse(state.isPresent());

        unreachableMember(ownerSupervisor, "member-2", DEFAULT_DATACENTER);
        verifyGetOwnershipState(service, entity, EntityOwnershipState.NO_OWNER);
    }

    @Test
    public void testIsCandidateRegistered() throws Exception {
        final DOMEntity test = new DOMEntity("test-type", "test");

        assertFalse(service.isCandidateRegistered(test));

        service.registerCandidate(test);

        assertTrue(service.isCandidateRegistered(test));
    }

    @Test
    public void testEntityRetrievalWithYiid() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.of(new NodeIdentifier(NetworkTopology.QNAME),
                new NodeIdentifier(Topology.QNAME),
                NodeIdentifierWithPredicates.of(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "test"),
                new NodeIdentifier(Node.QNAME),
                NodeIdentifierWithPredicates.of(Node.QNAME, QName.create(Node.QNAME, "node-id"), "test://test-node"));

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final Registration reg = service.registerCandidate(entity);

        assertNotNull(reg);
        verifyEntityCandidateRegistered(ENTITY_TYPE, entityId, "member-1");

        var result = service.getEntity(new GetEntityInputBuilder()
            .setName(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)))
            .setType(new EntityType(ENTITY_TYPE))
            .build())
            .get()
            .getResult();

        assertEquals(result.getOwnerNode().getValue(), "member-1");
        assertEquals(result.getCandidateNodes().get(0).getValue(), "member-1");

        // we should not be able to retrieve the entity when using string
        final String entityPathEncoded =
                "/network-topology:network-topology/topology[topology-id='test']/node[node-id='test://test-node']";

        result = service.getEntity(new GetEntityInputBuilder()
            .setName(new EntityName(entityPathEncoded))
            .setType(new EntityType(ENTITY_TYPE))
            .build())
            .get()
            .getResult();

        assertNull(result.getOwnerNode());
        assertEquals(List.of(), result.getCandidateNodes());

        final var getEntitiesResult = service.getEntities(new GetEntitiesInputBuilder().build()).get().getResult();
        final var entities = getEntitiesResult.nonnullEntities();
        assertEquals(1, entities.size());
        assertTrue(entities.get(new EntitiesKey(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)),
            new EntityType(ENTITY_TYPE))).getCandidateNodes().contains(new NodeName("member-1")));
        assertTrue(entities.get(new EntitiesKey(
                        new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)),
                        new EntityType(ENTITY_TYPE)))
                .getOwnerNode().getValue().equals("member-1"));

        final var getOwnerResult = service.getEntityOwner(new GetEntityOwnerInputBuilder()
            .setName(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)))
            .setType(new EntityType(ENTITY_TYPE))
            .build()).get().getResult();

        assertEquals(getOwnerResult.getOwnerNode().getValue(), "member-1");
    }

    private static void verifyGetOwnershipState(final DOMEntityOwnershipService service, final DOMEntity entity,
                                                final EntityOwnershipState expState) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertEquals(Optional.of(expState), service.getOwnershipState(entity));
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
}
