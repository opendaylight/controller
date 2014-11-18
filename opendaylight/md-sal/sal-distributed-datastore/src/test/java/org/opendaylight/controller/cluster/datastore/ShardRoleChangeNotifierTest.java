package org.opendaylight.controller.cluster.datastore;


import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.utils.MessageCollectorActor;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListener;
import org.opendaylight.controller.cluster.notifications.RegisterRoleChangeListenerReply;
import org.opendaylight.controller.cluster.notifications.ShardRoleChangeNotification;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.base.messages.RaftRoleChanged;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ShardRoleChangeNotifierTest extends AbstractActorTest  {

    @Test
    public void testHandleRegisterRoleChangeListener() throws Exception {
        new JavaTestKit(getSystem()) {{
            String memberId = "testHandleRegisterRoleChangeListener";
            ActorRef listenerActor =  getSystem().actorOf(Props.create(MessageCollectorActor.class));

            TestActorRef<ShardRoleChangeNotifier> notifierTestActorRef = TestActorRef.create(
                getSystem(), ShardRoleChangeNotifier.getProps(memberId), memberId);

            notifierTestActorRef.tell(new RegisterRoleChangeListener(), listenerActor);

            RegisterRoleChangeListenerReply reply = (RegisterRoleChangeListenerReply)
                MessageCollectorActor.getFirstMatching(listenerActor, RegisterRoleChangeListenerReply.class);
            assertNotNull(reply);

            ShardRoleChangeNotification notification = (ShardRoleChangeNotification)
                MessageCollectorActor.getFirstMatching(listenerActor, ShardRoleChangeNotification.class);
            assertNull(notification);
        }};

    }

    @Test
    public void testHandleRaftRoleChanged() throws Exception {
        new JavaTestKit(getSystem()) {{
            String memberId = "testHandleRegisterRoleChangeListenerWithNotificationSet";
            ActorRef listenerActor =  getSystem().actorOf(Props.create(MessageCollectorActor.class));
            ActorRef shardActor =  getTestActor();

            TestActorRef<ShardRoleChangeNotifier> notifierTestActorRef = TestActorRef.create(
                getSystem(), ShardRoleChangeNotifier.getProps(memberId), memberId);

            ShardRoleChangeNotifier shardRoleChangeNotifier = notifierTestActorRef.underlyingActor();

            notifierTestActorRef.tell(new RaftRoleChanged(memberId, RaftState.Candidate, RaftState.Leader), shardActor);

            // no notification should be sent as listener has not yet registered
            assertNull(MessageCollectorActor.getFirstMatching(listenerActor, ShardRoleChangeNotification.class));

            // listener registers after role has been changed, ensure we sent the latest role change after a reply
            notifierTestActorRef.tell(new RegisterRoleChangeListener(), listenerActor);

            RegisterRoleChangeListenerReply reply = (RegisterRoleChangeListenerReply)
                MessageCollectorActor.getFirstMatching(listenerActor, RegisterRoleChangeListenerReply.class);
            assertNotNull(reply);

            ShardRoleChangeNotification notification = (ShardRoleChangeNotification)
                MessageCollectorActor.getFirstMatching(listenerActor, ShardRoleChangeNotification.class);
            assertNotNull(notification);
            assertEquals(RaftState.Candidate.name(), notification.getOldRole());
            assertEquals(RaftState.Leader.name(), notification.getNewRole());

        }};

    }
}


