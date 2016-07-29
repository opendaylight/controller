/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;

/**
 * This is an offload actor, which is given an isolated snapshot of the data tree. It performs the potentially
 * time-consuming operation of serializing the snapshot.
 *
 * @author Robert Varga
 */
public final class ShardSnapshotActor extends AbstractUntypedActorWithMetering {
    // Internal message
    private static final class SerializeSnapshot {
        private final ShardDataTreeSnapshot snapshot;
        private final ActorRef replyTo;

        SerializeSnapshot(final ShardDataTreeSnapshot snapshot, final ActorRef replyTo) {
            this.snapshot = Preconditions.checkNotNull(snapshot);
            this.replyTo = Preconditions.checkNotNull(replyTo);
        }

        ShardDataTreeSnapshot getSnapshot() {
            return snapshot;
        }

        ActorRef getReplyTo() {
            return replyTo;
        }
    }

    //actor name override used for metering. This does not change the "real" actor name
    private static final String ACTOR_NAME_FOR_METERING = "shard-snapshot";

    private ShardSnapshotActor() {
        super(ACTOR_NAME_FOR_METERING);
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof SerializeSnapshot) {
            final SerializeSnapshot request = (SerializeSnapshot) message;
            request.getReplyTo().tell(new CaptureSnapshotReply(request.getSnapshot().serialize()), ActorRef.noSender());
        } else {
            unknownMessage(message);
        }
    }

    public static void requestSnapshot(final ActorRef snapshotActor, ShardDataTreeSnapshot snapshot,
            final ActorRef replyTo) {
        snapshotActor.tell(new SerializeSnapshot(snapshot, replyTo), ActorRef.noSender());
    }

    public static Props props() {
        return Props.create(ShardSnapshotActor.class);
    }
}
