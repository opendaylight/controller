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

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.notifications.LeaderStateChanged;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotification;
import org.opendaylight.controller.cluster.notifications.RoleChangeNotifier;
import org.opendaylight.controller.cluster.notifications.RoleChanged;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;

public class RoleChangeNotifierTest extends AbstractActorTest {

    @Test
    public void testHandleRegisterRoleChangeListener() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String memberId = "testHandleRegisterRoleChangeListener";
                ActorRef listenerActor = getSystem().actorOf(Props.create(MessageCollectorActor.class));

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
        };
    }

    @Test
    public void testHandleRaftRoleChanged() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String memberId = "testHandleRegisterRoleChangeListenerWithNotificationSet";
                ActorRef listenerActor = getSystem().actorOf(Props.create(MessageCollectorActor.class));
                ActorRef shardActor = getTestActor();

                TestActorRef<RoleChangeNotifier> notifierTestActorRef = TestActorRef.create(getSystem(),
                        RoleChangeNotifier.getProps(memberId), memberId);

                notifierTestActorRef.tell(
                        new RoleChanged(memberId, RaftState.Candidate.name(), RaftState.Leader.name()), shardActor);

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
                assertEquals(RaftState.Candidate.name(), notification.getOldRole());
                assertEquals(RaftState.Leader.name(), notification.getNewRole());

            }
        };

    }

    @Test
    public void testHandleLeaderStateChanged() throws Exception {
        new JavaTestKit(getSystem()) {
            {
                String actorId = "testHandleLeaderStateChanged";
                TestActorRef<RoleChangeNotifier> notifierTestActorRef = TestActorRef.create(getSystem(),
                        RoleChangeNotifier.getProps(actorId), actorId);

                notifierTestActorRef.tell(new LeaderStateChanged("member1", "leader1", (short) 5), ActorRef.noSender());

                // listener registers after the sate has been changed, ensure we
                // sent the latest state change after a reply
                notifierTestActorRef.tell(new RegisterRoleChangeListener(), getRef());

                expectMsgClass(RegisterRoleChangeListenerReply.class);

                LeaderStateChanged leaderStateChanged = expectMsgClass(LeaderStateChanged.class);
                assertEquals("getMemberId", "member1", leaderStateChanged.getMemberId());
                assertEquals("getLeaderId", "leader1", leaderStateChanged.getLeaderId());
                assertEquals("getLeaderPayloadVersion", 5, leaderStateChanged.getLeaderPayloadVersion());

                notifierTestActorRef.tell(new LeaderStateChanged("member1", "leader2", (short) 6), ActorRef.noSender());

                leaderStateChanged = expectMsgClass(LeaderStateChanged.class);
                assertEquals("getMemberId", "member1", leaderStateChanged.getMemberId());
                assertEquals("getLeaderId", "leader2", leaderStateChanged.getLeaderId());
                assertEquals("getLeaderPayloadVersion", 6, leaderStateChanged.getLeaderPayloadVersion());
            }
        };
    }
}
