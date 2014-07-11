package org.opendaylight.controller.cluster.raft.behaviors;

import org.opendaylight.controller.cluster.raft.RaftActorContext;

import java.util.Collections;

public class CandidateTest extends AbstractRaftActorBehaviorTest {

    @Override protected RaftActorBehavior createBehavior(RaftActorContext actorContext) {
        return new Candidate(actorContext, Collections.EMPTY_LIST);
    }
}
