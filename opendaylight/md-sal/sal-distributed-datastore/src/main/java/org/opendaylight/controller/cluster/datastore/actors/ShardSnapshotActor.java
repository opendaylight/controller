/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.messages.CreateSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.AbstractShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;

/**
 * This is an offload actor, which is given an isolated snapshot of the data tree. It performs the potentially
 * time-consuming operation of serializing the snapshot.
 *
 * @author Robert Varga
 */
public final class ShardSnapshotActor extends AbstractUntypedActorWithMetering {
  //actor name override used for metering. This does not change the "real" actor name
    private static final String ACTOR_NAME_FOR_METERING = "shard-snapshot";
    private final AbstractShardDataTreeSnapshot snapshot;

    private ShardSnapshotActor(final AbstractShardDataTreeSnapshot snapshot) {
        super(ACTOR_NAME_FOR_METERING);
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof CreateSnapshot) {
            // We handle only this message and afterwards we simply die
            getSender().tell(new CaptureSnapshotReply(snapshot.serialize()), getSelf());
            getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
        } else {
            unknownMessage(message);
        }
    }

    public static Props props(final AbstractShardDataTreeSnapshot snapshot) {
        return Props.create(ShardSnapshotActor.class, () -> new ShardSnapshotActor(snapshot));
    }
}
