package org.opendaylight.controller.cluster.raft.behaviors;

import akka.actor.ActorRef;
import akka.actor.Props;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.controller.cluster.raft.utils.DoNothingActor;

public class FollowerTest extends AbstractRaftActorBehaviorTest {

    private final ActorRef followerActor = getSystem().actorOf(Props.create(
        DoNothingActor.class));


    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }

    @Override protected RaftActorContext createActorContext() {
        return new MockRaftActorContext("test", getSystem(), followerActor);
    }

}
