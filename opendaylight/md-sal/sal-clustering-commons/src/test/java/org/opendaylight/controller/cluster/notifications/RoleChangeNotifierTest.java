/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RoleChangeNotifierTest {

    private static final String MEMBER_ID = "member-1";
    private static final int LISTENER_COUNT = 3;
    private ActorSystem system;
    private List<TestProbe> listeners;
    private ActorRef notifier;

    @Before
    public void setUp() throws Exception {
        system = ActorSystem.apply();
        notifier = system.actorOf(RoleChangeNotifier.getProps(MEMBER_ID));
        listeners = new ArrayList<>(LISTENER_COUNT);
        for (int i = 0; i < LISTENER_COUNT; i++) {
            listeners.add(new TestProbe(system));
        }
    }

    @After
    public void tearDown() throws Exception {
        JavaTestKit.shutdownActorSystem(system);
    }

    @Test
    public void testHandleReceiveRoleChange() throws Exception {
        registerListeners();
        final RoleChanged msg = new RoleChanged(MEMBER_ID, "old", "new");
        notifier.tell(msg, ActorRef.noSender());
        checkListenerRoleChangeNotification(msg);
    }

    @Test
    public void testHandleReceiveLeaderStateChanged() throws Exception {
        registerListeners();
        final LeaderStateChanged msg = new LeaderStateChanged(MEMBER_ID, "leader", (short) 0);
        notifier.tell(msg, ActorRef.noSender());
        checkListenerLeaderStateChanged(msg);
    }

    @Test
    public void testHandleReceiveRegistrationAfterRoleChange() throws Exception {
        final RoleChanged roleChanged1 = new RoleChanged(MEMBER_ID, "old1", "new1");
        final RoleChanged lastRoleChanged = new RoleChanged(MEMBER_ID, "old2", "new2");
        notifier.tell(roleChanged1, ActorRef.noSender());
        notifier.tell(lastRoleChanged, ActorRef.noSender());
        registerListeners();
        checkListenerRoleChangeNotification(lastRoleChanged);
    }

    @Test
    public void testHandleReceiveRegistrationAfterLeaderStateChange() throws Exception {
        final LeaderStateChanged leaderStateChanged1 = new LeaderStateChanged(MEMBER_ID, "leader1", (short) 0);
        final LeaderStateChanged lastLeaderStateChanged = new LeaderStateChanged(MEMBER_ID, "leader2", (short) 1);
        notifier.tell(leaderStateChanged1, ActorRef.noSender());
        notifier.tell(lastLeaderStateChanged, ActorRef.noSender());
        registerListeners();
        checkListenerLeaderStateChanged(lastLeaderStateChanged);
    }

    private void registerListeners() {
        for (final TestProbe listener : listeners) {
            notifier.tell(new RegisterRoleChangeListener(), listener.ref());
            listener.expectMsgClass(RegisterRoleChangeListenerReply.class);
        }
    }

    private void checkListenerRoleChangeNotification(final RoleChanged roleChanged) {
        for (final TestProbe listener : listeners) {
            final RoleChangeNotification received = listener.expectMsgClass(RoleChangeNotification.class);
            Assert.assertEquals(roleChanged.getMemberId(), received.getMemberId());
            Assert.assertEquals(roleChanged.getOldRole(), received.getOldRole());
            Assert.assertEquals(roleChanged.getNewRole(), received.getNewRole());
        }
    }

    private void checkListenerLeaderStateChanged(final LeaderStateChanged leaderStateChanged) {
        for (final TestProbe listener : listeners) {
            final LeaderStateChanged received = listener.expectMsgClass(LeaderStateChanged.class);
            Assert.assertEquals(leaderStateChanged.getMemberId(), received.getMemberId());
            Assert.assertEquals(leaderStateChanged.getLeaderId(), received.getLeaderId());
            Assert.assertEquals(leaderStateChanged.getLeaderPayloadVersion(), received.getLeaderPayloadVersion());
        }
    }

}