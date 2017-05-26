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
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
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
        private final Optional<OutputStream> installSnapshotStream;
        private final ActorRef replyTo;

        SerializeSnapshot(final ShardDataTreeSnapshot snapshot, final Optional<OutputStream> installSnapshotStream,
                final ActorRef replyTo) {
            this.snapshot = Preconditions.checkNotNull(snapshot);
            this.installSnapshotStream = Preconditions.checkNotNull(installSnapshotStream);
            this.replyTo = Preconditions.checkNotNull(replyTo);
        }

        ShardDataTreeSnapshot getSnapshot() {
            return snapshot;
        }

        Optional<OutputStream> getInstallSnapshotStream() {
            return installSnapshotStream;
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
            onSerializeSnapshot((SerializeSnapshot) message);
        } else {
            unknownMessage(message);
        }
    }

    private void onSerializeSnapshot(final SerializeSnapshot request) {
        Optional<OutputStream> installSnapshotStream = request.getInstallSnapshotStream();
        if (installSnapshotStream.isPresent()) {
            try (ObjectOutputStream out = new ObjectOutputStream(installSnapshotStream.get())) {
                request.getSnapshot().serialize(out);
            } catch (IOException e) {
                // TODO - we should communicate the failure in the CaptureSnapshotReply.
                LOG.error("Error serializing snapshot", e);
            }
        }

        request.getReplyTo().tell(new CaptureSnapshotReply(new ShardSnapshotState(request.getSnapshot()),
                installSnapshotStream), ActorRef.noSender());
    }

    /**
     * Sends a request to a ShardSnapshotActor to process a snapshot and send a CaptureSnapshotReply.
     *
     * @param snapshotActor the ShardSnapshotActor
     * @param snapshot the snapshot to process
     * @param installSnapshotStream Optional OutputStream that is present if the snapshot is to also be installed
     *        on a follower.
     * @param replyTo the actor to which to send the CaptureSnapshotReply
     */
    public static void requestSnapshot(final ActorRef snapshotActor, final ShardDataTreeSnapshot snapshot,
            final Optional<OutputStream> installSnapshotStream, final ActorRef replyTo) {
        snapshotActor.tell(new SerializeSnapshot(snapshot, installSnapshotStream, replyTo), ActorRef.noSender());
    }

    public static Props props() {
        return Props.create(ShardSnapshotActor.class);
    }
}
