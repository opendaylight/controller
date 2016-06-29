package org.opendaylight.controller.cluster.raft.client.messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class CreateSnapshotRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    // TODO: Change this to ActorSelection
    private final ActorRef actorRef;

    public CreateSnapshotRequest(ActorRef actorRef) {
        this.actorRef = actorRef;
    }

    public ActorRef getActorRef() {
        return actorRef;
    }
}
