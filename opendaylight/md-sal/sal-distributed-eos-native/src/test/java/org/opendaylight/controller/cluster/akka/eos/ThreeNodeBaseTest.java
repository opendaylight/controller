/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.akka.eos;

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

public class ThreeNodeBaseTest extends AbstractNativeEosTest {

    private ClusterNode node1;
    private ClusterNode node2;
    private ClusterNode node3;
    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    @Before
    public void setUp() throws Exception {
        node1 = startupRemote(2550, Collections.singletonList("member-1"));
        node2 = startupRemote(2551, Collections.singletonList("member-2"));
        node3 = startupRemote(2552, Collections.singletonList("member-3"));

        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(node3.getActorSystem());
        // need a longer timeout with classic remoting, artery.tcp doesnt need to wait as long for init
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

    @After
    public void tearDown() {
        // same issue with classic remoting as in setup
        ActorTestKit.shutdown(node1.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node2.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node3.getActorSystem(), Duration.ofSeconds(20));
    }

    @Test
    public void testInitialNotificationsWithoutOwner() throws Exception {
        final MockEntityOwnershipListener listener1 = registerListener(node1, ENTITY_1);
        verifyNoNotifications(listener1);

        final MockEntityOwnershipListener listener2 = registerListener(node2, ENTITY_1);
        verifyNoNotifications(listener2);

        final MockEntityOwnershipListener listener3 = registerListener(node3, ENTITY_1);
        verifyNoNotifications(listener3);
    }

    @Test
    public void testInitialNotificationsWithOwner() {
        registerCandidates(node1, ENTITY_1, "member-1");
        // make sure we register other candidates after the first is seen everywhere to prevent different results due
        // to timing
        waitUntillOwnerPresent(node3, ENTITY_1);

        registerCandidates(node2, ENTITY_1, "member-2");
        registerCandidates(node3, ENTITY_1, "member-3");

        final MockEntityOwnershipListener listener1 = registerListener(node1, ENTITY_1);
        verifyListenerState(listener1, ENTITY_1, true, true, false);

        final MockEntityOwnershipListener listener2 = registerListener(node2, ENTITY_1);
        verifyListenerState(listener2, ENTITY_1, true, false, false);

        final MockEntityOwnershipListener listener3 = registerListener(node3, ENTITY_1);
        verifyListenerState(listener3, ENTITY_1, true, false, false);
    }

    @Test
    public void testMultipleEntities() {
        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node2, ENTITY_1, "member-2");
        registerCandidates(node3, ENTITY_1, "member-3");

        waitUntillOwnerPresent(node3, ENTITY_1);

        registerCandidates(node2, ENTITY_2, "member-2");
        waitUntillOwnerPresent(node2, ENTITY_2);
        registerCandidates(node1, ENTITY_2, "member-1");

        final MockEntityOwnershipListener firstEntityListener1 = registerListener(node1, ENTITY_1);
        final MockEntityOwnershipListener firstEntityListener2 = registerListener(node2, ENTITY_1);
        final MockEntityOwnershipListener firstEntityListener3 = registerListener(node3, ENTITY_1);

        verifyListenerState(firstEntityListener1, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);

        final MockEntityOwnershipListener secondEntityListener1 = registerListener(node1, ENTITY_2);
        final MockEntityOwnershipListener secondEntityListener2 = registerListener(node2, ENTITY_2);
        final MockEntityOwnershipListener secondEntityListener3 = registerListener(node3, ENTITY_2);

        verifyListenerState(secondEntityListener1, ENTITY_2, true, false, false);
        verifyListenerState(secondEntityListener2, ENTITY_2, true, true, false);
        verifyListenerState(secondEntityListener3, ENTITY_2, true, false, false);

        unregisterCandidates(node1, ENTITY_1, "member-1");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, true);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);

        unregisterCandidates(node2, ENTITY_1, "member-2");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, true);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, true, false);

        unregisterCandidates(node3, ENTITY_1, "member-3");

        verifyListenerState(firstEntityListener1, ENTITY_1, false, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, false, false, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, false, false, true);

        // check second listener hasnt moved
        verifyListenerState(secondEntityListener1, ENTITY_2, true, false, false);
        verifyListenerState(secondEntityListener2, ENTITY_2, true, true, false);
        verifyListenerState(secondEntityListener3, ENTITY_2, true, false, false);

        registerCandidates(node1, ENTITY_1, "member-1");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);
    }
}
