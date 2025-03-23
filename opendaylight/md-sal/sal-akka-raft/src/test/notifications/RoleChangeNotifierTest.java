/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.testkit.TestProbe;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleChangeNotifierTest {
    private static final String MEMBER_ID = "member-1";
    private static final int LISTENER_COUNT = 3;

    private List<TestProbe> listeners;
    private ActorSystem system;
    private ActorRef notifier;

    @BeforeEach
    void beforeEach() {
        system = ActorSystem.apply();
        notifier = system.actorOf(RoleChangeNotifier.getProps(MEMBER_ID));
        listeners = Stream.generate(() -> new TestProbe(system)).limit(LISTENER_COUNT).toList();
    }

    @AfterEach
    void afterEach() {
        TestKit.shutdownActorSystem(system);
    }

    @Test
    void testHandleReceiveRoleChange() {
        registerListeners();
        final var msg = new RoleChanged(MEMBER_ID, "old", "new");
        notifier.tell(msg, ActorRef.noSender());
        checkListenerRoleChangeNotification(msg);
    }

    @Test
    void testHandleReceiveLeaderStateChanged() {
        registerListeners();
        final var msg = new LeaderStateChanged(MEMBER_ID, "leader", (short) 0);
        notifier.tell(msg, ActorRef.noSender());
        checkListenerLeaderStateChanged(msg);
    }

    @Test
    void testHandleReceiveRegistrationAfterRoleChange() {
        final var roleChanged1 = new RoleChanged(MEMBER_ID, "old1", "new1");
        final var lastRoleChanged = new RoleChanged(MEMBER_ID, "old2", "new2");
        notifier.tell(roleChanged1, ActorRef.noSender());
        notifier.tell(lastRoleChanged, ActorRef.noSender());
        registerListeners();
        checkListenerRoleChangeNotification(lastRoleChanged);
    }

    @Test
    void testHandleReceiveRegistrationAfterLeaderStateChange() {
        final var leaderStateChanged1 = new LeaderStateChanged(MEMBER_ID, "leader1", (short) 0);
        final var lastLeaderStateChanged = new LeaderStateChanged(MEMBER_ID, "leader2", (short) 1);
        notifier.tell(leaderStateChanged1, ActorRef.noSender());
        notifier.tell(lastLeaderStateChanged, ActorRef.noSender());
        registerListeners();
        checkListenerLeaderStateChanged(lastLeaderStateChanged);
    }

    private void registerListeners() {
        for (var listener : listeners) {
            notifier.tell(new RegisterRoleChangeListener(), listener.ref());
            listener.expectMsgClass(RegisterRoleChangeListenerReply.class);
        }
    }

    private void checkListenerRoleChangeNotification(final RoleChanged roleChanged) {
        for (var listener : listeners) {
            final RoleChangeNotification received = listener.expectMsgClass(RoleChangeNotification.class);
            assertEquals(roleChanged.memberId(), received.getMemberId());
            assertEquals(roleChanged.newRole(), received.getNewRole());
            assertEquals(roleChanged.oldRole(), received.getOldRole());
        }
    }

    private void checkListenerLeaderStateChanged(final LeaderStateChanged leaderStateChanged) {
        for (var listener : listeners) {
            assertEquals(leaderStateChanged, listener.expectMsgClass(LeaderStateChanged.class));
        }
    }
}