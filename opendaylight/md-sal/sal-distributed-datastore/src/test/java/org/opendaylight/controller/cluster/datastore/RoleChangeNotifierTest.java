/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.DefaultLeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class RoleChangeNotifierTest extends AbstractActorTest {
    private TestKit testKit;

    @Before
    public void setup() {
        testKit = new TestKit(getSystem());
    }

    @Test
    public void testHandleRegisterRoleChangeListener() {
        String memberId = "testHandleRegisterRoleChangeListener";
        ActorRef listenerActor = getSystem().actorOf(MessageCollectorActor.props());

        TestActorRef<RoleChangeNotifier> notifierTestActorRef = TestActorRef.create(getSystem(),
            RoleChangeNotifier.getProps(memberId), memberId);

        notifierTestActorRef.tell(new RegisterRoleChangeListener(), listenerActor);

        RegisterRoleChangeListenerReply reply = MessageCollectorActor.getFirstMatching(listenerActor,
            RegisterRoleChangeListenerReply.class);
        assertNotNull(reply);

        RoleChangeNotification notification = MessageCollectorActor.getFirstMatching(listenerActor,
            RoleChangeNotification.class);
        assertNull(notification);
    }

    @Test
    public void testHandleRaftRoleChanged() {
        String memberId = "testHandleRegisterRoleChangeListenerWithNotificationSet";
        ActorRef listenerActor = getSystem().actorOf(MessageCollectorActor.props());
        ActorRef shardActor = testKit.getTestActor();

        TestActorRef<RoleChangeNotifier> notifierTestActorRef = TestActorRef.create(getSystem(),
            RoleChangeNotifier.getProps(memberId), memberId);

        notifierTestActorRef.tell(new RoleChanged(memberId, RaftState.Candidate, RaftState.Leader), shardActor);

        // no notification should be sent as listener has not yet
        // registered
        assertNull(MessageCollectorActor.getFirstMatching(listenerActor, RoleChangeNotification.class));

        // listener registers after role has been changed, ensure we
        // sent the latest role change after a reply
        notifierTestActorRef.tell(new RegisterRoleChangeListener(), listenerActor);

        RegisterRoleChangeListenerReply reply = MessageCollectorActor.getFirstMatching(listenerActor,
            RegisterRoleChangeListenerReply.class);
        assertNotNull(reply);

        RoleChangeNotification notification = MessageCollectorActor.getFirstMatching(listenerActor,
            RoleChangeNotification.class);
        assertNotNull(notification);
        assertEquals(RaftState.Candidate, notification.oldRole());
        assertEquals(RaftState.Leader, notification.newRole());
    }

    @Test
    public void testHandleLeaderStateChanged() {
        String actorId = "testHandleLeaderStateChanged";
        TestActorRef<RoleChangeNotifier> notifierTestActorRef = TestActorRef.create(getSystem(),
            RoleChangeNotifier.getProps(actorId), actorId);

        notifierTestActorRef.tell(new DefaultLeaderStateChanged("member1", "leader1", (short) 5), ActorRef.noSender());

        // listener registers after the sate has been changed, ensure we
        // sent the latest state change after a reply
        notifierTestActorRef.tell(new RegisterRoleChangeListener(), testKit.getRef());

        testKit.expectMsgClass(RegisterRoleChangeListenerReply.class);

        LeaderStateChanged leaderStateChanged = testKit.expectMsgClass(LeaderStateChanged.class);
        assertEquals("getMemberId", "member1", leaderStateChanged.memberId());
        assertEquals("getLeaderId", "leader1", leaderStateChanged.leaderId());
        assertEquals("getLeaderPayloadVersion", 5, leaderStateChanged.leaderPayloadVersion());

        notifierTestActorRef.tell(new DefaultLeaderStateChanged("member1", "leader2", (short) 6), ActorRef.noSender());

        leaderStateChanged = testKit.expectMsgClass(LeaderStateChanged.class);
        assertEquals("getMemberId", "member1", leaderStateChanged.memberId());
        assertEquals("getLeaderId", "leader2", leaderStateChanged.leaderId());
        assertEquals("getLeaderPayloadVersion", 6, leaderStateChanged.leaderPayloadVersion());
    }
}
