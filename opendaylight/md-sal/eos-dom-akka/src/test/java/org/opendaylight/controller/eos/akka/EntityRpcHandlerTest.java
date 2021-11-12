/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import akka.actor.ActorSystem;
import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.javadsl.Adapter;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.EntityType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntitiesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.GetEntityOwnerOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.NodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.entity.owners.norev.get.entities.output.EntitiesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;

public class EntityRpcHandlerTest extends AbstractNativeEosTest {
    static final String ENTITY_TYPE = "test";

    private ActorSystem system1;
    private ActorSystem system2;

    private AkkaEntityOwnershipService service1;
    private AkkaEntityOwnershipService service2;

    @Before
    public void setUp() throws Exception {
        system1 = startupActorSystem(2550, List.of("member-1"), TWO_NODE_SEED_NODES);
        system2 = startupActorSystem(2551, List.of("member-2"), TWO_NODE_SEED_NODES, "dc-backup");

        service1 = new AkkaEntityOwnershipService(system1, CODEC_CONTEXT);
        service2 = new AkkaEntityOwnershipService(system2, CODEC_CONTEXT);

        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(Adapter.toTyped(system2));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = new ArrayList<>();
            cluster.state().getMembers().forEach(members::add);
            if (members.size() != 2) {
                return false;
            }

            for (final Member member : members) {
                if (!member.status().equals(MemberStatus.up())) {
                    return false;
                }
            }

            return true;
        });
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        service1.close();
        service2.close();
        ActorTestKit.shutdown(Adapter.toTyped(system1), Duration.ofSeconds(20));
        ActorTestKit.shutdown(Adapter.toTyped(system2), Duration.ofSeconds(20));
    }

    /*
     * Tests entity rpcs handled both by the owner supervisor(service1) and with an idle supervisor(falling
     * back to distributed-data in an inactive datacenter). This covers both the available cases, datacenters and case
     * in which the node with active akka-singleton is shutdown and another one takes over.
     */
    @Test
    public void testEntityRetrievalWithUnavailableSupervisor() throws Exception {
        final YangInstanceIdentifier entityId = YangInstanceIdentifier.create(new NodeIdentifier(NetworkTopology.QNAME),
                new NodeIdentifier(Topology.QNAME),
                NodeIdentifierWithPredicates.of(Topology.QNAME, QName.create(Topology.QNAME, "topology-id"), "test"),
                new NodeIdentifier(Node.QNAME),
                NodeIdentifierWithPredicates.of(Node.QNAME, QName.create(Node.QNAME, "node-id"), "test://test-node"));

        final DOMEntity entity = new DOMEntity(ENTITY_TYPE, entityId);

        final DOMEntityOwnershipCandidateRegistration reg = service1.registerCandidate(entity);

        await().untilAsserted(() -> {
            final RpcResult<GetEntityOutput> getEntityResult = service1.getEntity(new GetEntityInputBuilder()
                            .setName(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)))
                            .setType(new EntityType(ENTITY_TYPE))
                            .build())
                    .get();

            assertEquals(getEntityResult.getResult().getOwnerNode().getValue(), "member-1");
            assertEquals(getEntityResult.getResult().getCandidateNodes().get(0).getValue(), "member-1");
        });

        // keep this under ask timeout to make sure the singleton actor in the inactive datacenter responds with failure
        // immediately, so that the rpc actor retries with distributed-data asap
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            final GetEntitiesOutput getEntitiesResult =
                    service2.getEntities(new GetEntitiesInputBuilder().build()).get().getResult();

            assertEquals(getEntitiesResult.getEntities().size(), 1);
            assertTrue(getEntitiesResult.getEntities().get(new EntitiesKey(
                            new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)),
                            new EntityType(ENTITY_TYPE)))
                    .getCandidateNodes().contains(new NodeName("member-1")));
            assertTrue(getEntitiesResult.getEntities().get(new EntitiesKey(
                            new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)),
                            new EntityType(ENTITY_TYPE)))
                    .getOwnerNode().getValue().equals("member-1"));
        });

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            final GetEntityOutput getEntityResult = service2.getEntity(new GetEntityInputBuilder()
                            .setName(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)))
                            .setType(new EntityType(ENTITY_TYPE))
                            .build())
                    .get().getResult();

            assertEquals(getEntityResult.getOwnerNode().getValue(), "member-1");
            assertEquals(getEntityResult.getCandidateNodes().get(0).getValue(), "member-1");
        });

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            final GetEntityOwnerOutput getOwnerResult = service2.getEntityOwner(new GetEntityOwnerInputBuilder()
                            .setName(new EntityName(CODEC_CONTEXT.fromYangInstanceIdentifier(entityId)))
                            .setType(new EntityType(ENTITY_TYPE))
                            .build())
                    .get().getResult();

            assertEquals(getOwnerResult.getOwnerNode().getValue(), "member-1");
        });

    }
}
