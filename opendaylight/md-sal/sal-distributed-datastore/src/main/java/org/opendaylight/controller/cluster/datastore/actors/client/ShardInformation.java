package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ShardInformation {
    private final ABIVersion version;
    private final ActorRef actor;

    protected ShardInformation(final ActorRef actor, final ABIVersion version) {
        this.version = Preconditions.checkNotNull(version);
        this.actor = Preconditions.checkNotNull(actor);
    }

    public ActorRef getActor() {
        return actor;
    }

    public ABIVersion getVersion() {
        return version;
    }
}
