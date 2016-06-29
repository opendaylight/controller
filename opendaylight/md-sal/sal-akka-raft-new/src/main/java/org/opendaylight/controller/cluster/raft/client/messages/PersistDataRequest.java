package org.opendaylight.controller.cluster.raft.client.messages;

import akka.actor.ActorRef;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.yangtools.concepts.Identifier;

import java.io.Serializable;

public class PersistDataRequest implements Serializable {
    private final ActorRef clientActor;
    private final Identifier identifier;
    private final Payload data;

    public PersistDataRequest(ActorRef clientActor, Identifier identifier, Payload data) {
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
