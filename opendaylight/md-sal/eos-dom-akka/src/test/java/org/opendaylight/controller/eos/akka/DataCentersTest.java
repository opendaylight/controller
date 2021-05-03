/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class DataCentersTest extends AbstractNativeEosTest {

    private ClusterNode node1 = null;
    private ClusterNode node2 = null;
    private ClusterNode node3 = null;
    private ClusterNode node4 = null;
    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    @Before
    public void setUp() throws Exception {
        node1 = startupWithDatacenter(2550, Collections.singletonList("member-1"), DATACENTER_SEED_NODES, "dc-primary");
        node2 = startupWithDatacenter(2551, Collections.singletonList("member-2"), DATACENTER_SEED_NODES, "dc-primary");
        node3 = startupWithDatacenter(2552, Collections.singletonList("member-3"), DATACENTER_SEED_NODES, "dc-backup");
        node4 = startupWithDatacenter(2553, Collections.singletonList("member-4"), DATACENTER_SEED_NODES, "dc-backup");

        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(node4.getActorSystem());
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = new ArrayList<>();
            cluster.state().getMembers().forEach(members::add);
            if (members.size() != 4) {
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

    @Test
    public void testDatacenterActivation() throws Exception {
        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node3, ENTITY_1, "member-3");
        registerCandidates(node4, ENTITY_1, "member-4");

        activateDatacenter(node1).get();

        waitUntillOwnerPresent(node1, ENTITY_1);
        final MockEntityOwnershipListener listener1 = registerListener(node1, ENTITY_1);
        verifyListenerState(listener1, ENTITY_1, true, true, false);

        final MockEntityOwnershipListener listener2 = registerListener(node3, ENTITY_1);
        verifyListenerState(listener2, ENTITY_1, true, false, false);

        unregisterCandidates(node1, ENTITY_1, "member-1");

        verifyListenerState(listener1, ENTITY_1, false, false, true);
        verifyListenerState(listener2, ENTITY_1, false, false, false);

        deactivateDatacenter(node1).get();
        activateDatacenter(node4).get();

        verifyListenerState(listener1, ENTITY_1, true, false, false);
        verifyListenerState(listener2, ENTITY_1, true, true, false);

        unregisterCandidates(node3, ENTITY_1, "member-3");

        // checking index after notif so current + 1
        verifyListenerState(listener1, ENTITY_1, true, false, false);
        verifyListenerState(listener2, ENTITY_1, true, false, true);

        deactivateDatacenter(node3).get();
        activateDatacenter(node2).get();

        // no candidate in dc-primary so no owners after datacenter activation
        verifyListenerState(listener1, ENTITY_1, false, false, false);
        verifyListenerState(listener2, ENTITY_1, false, false, false);
    }

    @Test
    public void testDataCenterShutdown() throws Exception {
        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node3, ENTITY_1, "member-3");
        registerCandidates(node4, ENTITY_1, "member-4");

        activateDatacenter(node1).get();

        waitUntillOwnerPresent(node1, ENTITY_1);
        final MockEntityOwnershipListener listener1 = registerListener(node1, ENTITY_1);
        verifyListenerState(listener1, ENTITY_1, true, true, false);

        final MockEntityOwnershipListener listener2 = registerListener(node3, ENTITY_1);
        verifyListenerState(listener2, ENTITY_1, true, false, false);

        unregisterCandidates(node1, ENTITY_1, "member-1");

        verifyListenerState(listener1, ENTITY_1, false, false, true);
        verifyListenerState(listener2, ENTITY_1, false, false, false);

        ActorTestKit.shutdown(node1.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node2.getActorSystem(), Duration.ofSeconds(20));

        activateDatacenter(node3).get();
        verifyListenerState(listener2, ENTITY_1, true, true, false);

        unregisterCandidates(node3, ENTITY_1, "member-3");
        verifyListenerState(listener2, ENTITY_1, true, false, true);
    }

    @After
    public void tearDown() {
        ActorTestKit.shutdown(node1.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node2.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node3.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node4.getActorSystem(), Duration.ofSeconds(20));
    }

}
