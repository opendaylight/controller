package org.opendaylight.controller.cluster.raft.client.messages;

import akka.actor.ActorRef;
import akka.dispatch.ControlMessage;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;

import java.io.Serializable;

public class ApplyState implements ControlMessage, Serializable {
    private final ActorRef clientActor;
    private final Identifier identifier;
    private final Payload data;

    public ApplyState(ActorRef clientActor, Identifier identifier, Payload data) {
        this.clientActor = clientActor;
        this.identifier = identifier;
        this.data = data;
    }

    public ActorRef getClientActor() {
        return clientActor;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Payload getData() {
        return data;
    }
}
