/*
 * Copyright (c) 2021 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.eos.akka;

import akka.actor.testkit.typed.javadsl.ActorTestKit;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.eos.dom.api.DOMEntity;

public class SingleNodeTest extends AbstractNativeEosTest {

    public static final DOMEntity ENTITY_1 = new DOMEntity("test-type", "entity-1");
    public static final DOMEntity ENTITY_2 = new DOMEntity("test-type-2", "entity-2");

    private static final String DEFAULT_DATACENTER = "dc-default";

    private ClusterNode clusterNode;

    @Before
    public void setUp() throws Exception {
        clusterNode = startup(2550, List.of("member-1"));
    }

    @After
    public void tearDown() {
        ActorTestKit.shutdown(clusterNode.getActorSystem());
    }

    @Test
    public void testNotificationPriorToCandidateRegistration() {
        final MockEntityOwnershipListener listener = registerListener(clusterNode, ENTITY_1);
        verifyNoNotifications(listener);

        registerCandidates(clusterNode, ENTITY_1, "member-1");
        verifyListenerState(listener, ENTITY_1, true, true, false);
    }

    @Test
    public void testListenerPriorToAddingCandidates() {
        final MockEntityOwnershipListener listener = registerListener(clusterNode, ENTITY_1);

        registerCandidates(clusterNode, ENTITY_1, "member-1");
        waitUntillOwnerPresent(clusterNode, ENTITY_1);

        reachableMember(clusterNode, "member-2", DEFAULT_DATACENTER);
        reachableMember(clusterNode, "member-3", DEFAULT_DATACENTER);

        registerCandidates(clusterNode, ENTITY_1, "member-2", "member-3");
        verifyListenerState(listener, ENTITY_1, true, true, false);

        unregisterCandidates(clusterNode, ENTITY_1, "member-1");
        verifyListenerState(listener, ENTITY_1, true, false, true);
    }

    @Test
    public void testListenerRegistrationAfterCandidates() {
        registerCandidates(clusterNode, ENTITY_1, "member-1", "member-2", "member-3");
        waitUntillOwnerPresent(clusterNode, ENTITY_1);

        reachableMember(clusterNode, "member-2", DEFAULT_DATACENTER);
        reachableMember(clusterNode, "member-3", DEFAULT_DATACENTER);

        final MockEntityOwnershipListener listener = registerListener(clusterNode, ENTITY_1);
        verifyListenerState(listener, ENTITY_1, true, true, false);

        unregisterCandidates(clusterNode, ENTITY_1, "member-1", "member-2");
        verifyListenerState(listener, ENTITY_1, true, false, true);
    }

    @Test
    public void testMultipleEntities() {
        registerCandidates(clusterNode, ENTITY_1, "member-1", "member-2", "member-3");
        waitUntillOwnerPresent(clusterNode, ENTITY_1);

        reachableMember(clusterNode, "member-2", DEFAULT_DATACENTER);
        reachableMember(clusterNode, "member-3", DEFAULT_DATACENTER);

        final MockEntityOwnershipListener listener1 = registerListener(clusterNode, ENTITY_1);
        final MockEntityOwnershipListener listener2 = registerListener(clusterNode, ENTITY_2);

        verifyListenerState(listener1, ENTITY_1, true, true, false);
        verifyNoNotifications(listener2);

        unregisterCandidates(clusterNode, ENTITY_1, "member-1");
        verifyListenerState(listener1, ENTITY_1, true, false, true);
        verifyNoNotifications(listener2);

        registerCandidates(clusterNode, ENTITY_2, "member-2");
        verifyListenerState(listener1, ENTITY_1, true, false, true);
        verifyListenerState(listener2, ENTITY_2, true, false, false);

        unregisterCandidates(clusterNode, ENTITY_2, "member-2");

        verifyListenerState(listener1, ENTITY_1, true, false, true);
        verifyListenerState(listener2, ENTITY_2, false, false, false);
    }
}
