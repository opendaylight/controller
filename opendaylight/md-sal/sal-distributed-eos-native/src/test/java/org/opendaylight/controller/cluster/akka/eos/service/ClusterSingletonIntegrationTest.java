/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos.service;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.actor.typed.javadsl.Adapter;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import akka.testkit.javadsl.TestKit;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.akka.eos.AbstractNativeEosTest;
import org.opendaylight.mdsal.eos.dom.api.DOMEntityOwnershipService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonService;
import org.opendaylight.mdsal.singleton.common.api.ClusterSingletonServiceRegistration;
import org.opendaylight.mdsal.singleton.common.api.ServiceGroupIdentifier;
import org.opendaylight.mdsal.singleton.dom.impl.DOMClusterSingletonServiceProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterSingletonIntegrationTest extends AbstractNativeEosTest {

    private static final Logger LOG = LoggerFactory.getLogger(ClusterSingletonIntegrationTest.class);

    private MockNativeEntityOwnershipService node1;
    private MockNativeEntityOwnershipService node2;
    private MockNativeEntityOwnershipService node3;

    private MockSingletonService singletonNode1;
    private MockSingletonService singletonNode2;
    private MockSingletonService singletonNode3;


    @Before
    public void setUp() throws Exception {
        node1 = startupNativeService(2550, List.of("member-1"), THREE_NODE_SEED_NODES);
        node2 = startupNativeService(2551, List.of("member-2"), THREE_NODE_SEED_NODES);
        node3 = startupNativeService(2552, List.of("member-3"), THREE_NODE_SEED_NODES);

        singletonNode1 = new MockSingletonService(node1);
        singletonNode1.initializeProvider();

        singletonNode2 = new MockSingletonService(node2);
        singletonNode2.initializeProvider();

        singletonNode3 = new MockSingletonService(node3);
        singletonNode3.initializeProvider();

        waitUntillNodeReady(node3);
    }

    @After
    public void tearDown() {
        ActorTestKit.shutdown(Adapter.toTyped(node1.getActorSystem()), Duration.ofSeconds(20));
        ActorTestKit.shutdown(Adapter.toTyped(node2.getActorSystem()), Duration.ofSeconds(20));
        ActorTestKit.shutdown(Adapter.toTyped(node3.getActorSystem()), Duration.ofSeconds(20));
    }

    @Test
    public void testSingletonOwnershipNotDropped() {
        final MockClusterSingletonService service = new MockClusterSingletonService("member-1", "service-1");
        singletonNode1.registerClusterSingletonService(service);

        verifyServiceActive(service);

        final MockClusterSingletonService service2 = new MockClusterSingletonService("member-2", "service-1");
        singletonNode2.registerClusterSingletonService(service2);

        verifyServiceInactive(service2, 2);
    }

    @Test
    public void testSingletonOwnershipHandoff() {
        final MockClusterSingletonService service = new MockClusterSingletonService("member-1", "service-1");
        final ClusterSingletonServiceRegistration registration =
                singletonNode1.registerClusterSingletonService(service);

        verifyServiceActive(service);

        final MockClusterSingletonService service2 = new MockClusterSingletonService("member-2", "service-1");
        singletonNode2.registerClusterSingletonService(service2);

        verifyServiceInactive(service2, 2);

        registration.close();
        verifyServiceInactive(service);
        verifyServiceActive(service2);
    }

    @Test
    public void testSingletonOwnershipHandoffOnNodeShutdown() throws Exception {
        MockClusterSingletonService service2 = new MockClusterSingletonService("member-2", "service-1");
        ClusterSingletonServiceRegistration registration2 =
                singletonNode2.registerClusterSingletonService(service2);

        verifyServiceActive(service2);

        final MockClusterSingletonService service3 = new MockClusterSingletonService("member-3", "service-1");
        final ClusterSingletonServiceRegistration registration3 =
                singletonNode3.registerClusterSingletonService(service3);

        verifyServiceInactive(service3, 2);

        LOG.debug("Shutting down node2");
        TestKit.shutdownActorSystem(node2.getActorSystem());
        verifyServiceActive(service3);

        node2 = startupNativeService(2551, List.of("member-1"), THREE_NODE_SEED_NODES);
        singletonNode2 = new MockSingletonService(node2);
        singletonNode2.initializeProvider();

        waitUntillNodeReady(node2);
        service2 = new MockClusterSingletonService("member-2", "service-1");
        singletonNode2.registerClusterSingletonService(service2);

        verifyServiceActive(service3);
        verifyServiceInactive(service2, 5);
    }

    private void waitUntillNodeReady(MockNativeEntityOwnershipService node) {
        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(Adapter.toTyped(node.getActorSystem()));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = new ArrayList<>();
            cluster.state().getMembers().forEach(members::add);
            if (members.size() != 3) {
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

    private static void verifyServiceActive(MockClusterSingletonService service) {
        await().untilAsserted(() -> assertTrue(service.isActivated()));
    }

    private static void verifyServiceActive(MockClusterSingletonService service, long delay) {
        await().pollDelay(delay, TimeUnit.SECONDS).untilAsserted(() -> assertTrue(service.isActivated()));
    }

    private static void verifyServiceInactive(MockClusterSingletonService service) {
        await().untilAsserted(() -> assertFalse(service.isActivated()));
    }

    private static void verifyServiceInactive(MockClusterSingletonService service, long delay) {
        await().pollDelay(delay, TimeUnit.SECONDS).untilAsserted(() -> assertFalse(service.isActivated()));
    }

    private static class MockClusterSingletonService implements ClusterSingletonService {

        private final String member;
        private final ServiceGroupIdentifier identifier;
        private boolean activated = false;

        MockClusterSingletonService(String member, String identifier) {
            this.member = member;
            this.identifier = ServiceGroupIdentifier.create(identifier);
        }

        @Override
        public void instantiateServiceInstance() {
            LOG.debug("{} : Activating service: {}", member, identifier);
            activated = true;
        }

        @Override
        public ListenableFuture<? extends Object> closeServiceInstance() {
            LOG.debug("{} : Closing service: {}", member, identifier);
            activated = false;
            return Futures.immediateFuture(null);
        }

        @Override
        public ServiceGroupIdentifier getIdentifier() {
            return identifier;
        }

        public boolean isActivated() {
            return activated;
        }
    }

    private static class MockSingletonService extends DOMClusterSingletonServiceProviderImpl {
        MockSingletonService(DOMEntityOwnershipService entityOwnershipService) {
            super(entityOwnershipService);
        }
    }
}
