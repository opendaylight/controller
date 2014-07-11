package org.opendaylight.controller.cluster.raft.behaviors;

import org.opendaylight.controller.cluster.raft.RaftActorContext;

public class FollowerTest extends AbstractRaftActorBehaviorTest {
    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Follower(actorContext);
    }
}
