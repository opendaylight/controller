/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import static org.awaitility.Awaitility.await;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class ThreeNodeReachabilityTest extends AbstractNativeEosTest {
    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    private ClusterNode node1 = null;
    private ClusterNode node2 = null;
    private ClusterNode node3 = null;

    @Before
    public void setUp() throws Exception {
        node1 = startupRemote(2550, List.of("member-1"), TWO_NODE_SEED_NODES);
        node2 = startupRemote(2551, List.of("member-2"), TWO_NODE_SEED_NODES);

        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(node2.getActorSystem());
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = ImmutableList.copyOf(cluster.state().getMembers());
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
    public void tearDown() {
        ActorTestKit.shutdown(node1.getActorSystem(), Duration.ofSeconds(20));
        ActorTestKit.shutdown(node2.getActorSystem(), Duration.ofSeconds(20));


        if (node3 != null) {
            ActorTestKit.shutdown(node3.getActorSystem(), Duration.ofSeconds(20));
        }
    }

    @Test
    public void testNodeLateStart() throws Exception {
        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node2, ENTITY_1, "member-2");

        registerCandidates(node2, ENTITY_2, "member-2");
        waitUntillOwnerPresent(node2, ENTITY_2);
        registerCandidates(node1, ENTITY_2, "member-1");

        final MockEntityOwnershipListener firstEntityListener1 = registerListener(node1, ENTITY_1);
        final MockEntityOwnershipListener firstEntityListener2 = registerListener(node2, ENTITY_1);

        verifyListenerState(firstEntityListener1, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, false);

        final MockEntityOwnershipListener secondEntityListener1 = registerListener(node1, ENTITY_2);
        final MockEntityOwnershipListener secondEntityListener2 = registerListener(node2, ENTITY_2);

        verifyListenerState(secondEntityListener1, ENTITY_2, true, false, false);
        verifyListenerState(secondEntityListener2, ENTITY_2, true, true, false);

        unregisterCandidates(node1, ENTITY_1, "member-1");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, true);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, true, false);

        unregisterCandidates(node2, ENTITY_1, "member-2");

        verifyListenerState(firstEntityListener1, ENTITY_1, false, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, false, false, true);

        startNode3();

        final MockEntityOwnershipListener firstEntityListener3 = registerListener(node3, ENTITY_1);
        verifyListenerState(firstEntityListener3, ENTITY_1, false, false, false);

        final MockEntityOwnershipListener secondEntityListener3 = registerListener(node3, ENTITY_2);
        verifyListenerState(secondEntityListener3, ENTITY_2, true, false, false);

        registerCandidates(node3, ENTITY_1, "member-3");
        waitUntillOwnerPresent(node3, ENTITY_1);

        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, false);

        verifyListenerState(firstEntityListener3, ENTITY_1, true, true, false);
    }

    @Test
    public void testReachabilityChangesDuringRuntime() throws Exception {
        startNode3();

        registerCandidates(node2, ENTITY_1, "member-2");
        // we want singleton on node1 but owner on node2
        waitUntillOwnerPresent(node2, ENTITY_1);

        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node3, ENTITY_1, "member-3");

        registerCandidates(node2, ENTITY_2, "member-2");
        waitUntillOwnerPresent(node2, ENTITY_2);
        registerCandidates(node1, ENTITY_2, "member-1");

        final MockEntityOwnershipListener firstEntityListener1 = registerListener(node1, ENTITY_1);
        final MockEntityOwnershipListener firstEntityListener2 = registerListener(node2, ENTITY_1);
        final MockEntityOwnershipListener firstEntityListener3 = registerListener(node3, ENTITY_1);

        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);

        final MockEntityOwnershipListener secondEntityListener1 = registerListener(node1, ENTITY_2);
        final MockEntityOwnershipListener secondEntityListener2 = registerListener(node2, ENTITY_2);
        final MockEntityOwnershipListener secondEntityListener3 = registerListener(node3, ENTITY_2);

        verifyListenerState(secondEntityListener1, ENTITY_2, true, false, false);
        verifyListenerState(secondEntityListener2, ENTITY_2, true, true, false);
        verifyListenerState(secondEntityListener3, ENTITY_2, true, false, false);

        unreachableMember(node1, "member-2", "dc-default");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, true);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);

        verifyListenerState(secondEntityListener1, ENTITY_2, true, true, false);
        verifyListenerState(secondEntityListener2, ENTITY_2, true, false, true);
        verifyListenerState(secondEntityListener3, ENTITY_2, true, false, false);

        unreachableMember(node1, "member-3", "dc-default");

        verifyListenerState(firstEntityListener1, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, false, true);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);

        unregisterCandidates(node1, ENTITY_1, "member-1", "dc-default");
        unregisterCandidates(node1, ENTITY_2, "member-1", "dc-default");

        verifyListenerState(firstEntityListener1, ENTITY_1, false, false, true);
        verifyListenerState(firstEntityListener2, ENTITY_1, false, false, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, false, false, false);

        verifyListenerState(secondEntityListener1, ENTITY_2, false, false, true);
        verifyListenerState(secondEntityListener2, ENTITY_2, false, false, false);
        verifyListenerState(secondEntityListener3, ENTITY_2, false, false, false);

        reachableMember(node1, "member-2", "dc-default");
        verifyListenerState(firstEntityListener1, ENTITY_1, true, false, false);
        verifyListenerState(firstEntityListener2, ENTITY_1, true, true, false);
        verifyListenerState(firstEntityListener3, ENTITY_1, true, false, false);
    }

    @Test
    public void testSingletonMoving() throws Exception {
        final MockEntityOwnershipListener listener1 = registerListener(node2, ENTITY_1);
        final MockEntityOwnershipListener listener2 = registerListener(node2, ENTITY_2);
        verifyNoNotifications(listener1);
        verifyNoNotifications(listener2);

        registerCandidates(node1, ENTITY_1, "member-1");
        registerCandidates(node2, ENTITY_1, "member-2");

        registerCandidates(node2, ENTITY_2, "member-2");
        waitUntillOwnerPresent(node2, ENTITY_2);
        registerCandidates(node1, ENTITY_2, "member-1");
        // end up with node1 - member-1, node2 - member-2 owners
        verifyListenerState(listener1, ENTITY_1, true, false, false);
        verifyListenerState(listener2, ENTITY_2, true, true, false);

        ActorTestKit.shutdown(node1.getActorSystem(), Duration.ofSeconds(20));

        verifyListenerState(listener1, ENTITY_1, true, true, false);
        verifyListenerState(listener2, ENTITY_2, true, true, false);

        startNode3(2);

        final MockEntityOwnershipListener listener3 = registerListener(node3, ENTITY_2);
        verifyListenerState(listener3, ENTITY_2, true, false, false);

        node1 = startupRemote(2550, List.of("member-1"));

        final Cluster cluster = Cluster.get(node2.getActorSystem());
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = ImmutableList.copyOf(cluster.state().getMembers());
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

        final MockEntityOwnershipListener node1Listener = registerListener(node1, ENTITY_1);
        verifyListenerState(node1Listener, ENTITY_1, true, false, false);
    }

    private void startNode3() throws Exception {
        startNode3(3);
    }

    private void startNode3(final int membersPresent) throws Exception {
        node3 = startupRemote(2552, List.of("member-3"), THREE_NODE_SEED_NODES);

        // need to wait until all nodes are ready
        final Cluster cluster = Cluster.get(node2.getActorSystem());
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            final List<Member> members = ImmutableList.copyOf(cluster.state().getMembers());
            if (members.size() != membersPresent) {
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
}
