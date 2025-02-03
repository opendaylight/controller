/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.persisted.ShardDataTreeSnapshot;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.io.InputOutputStreamFactory;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an offload actor, which is given an isolated snapshot of the data tree. It performs the potentially
 * time-consuming operation of serializing the snapshot.
 */
public final class ShardSnapshotActor extends AbstractUntypedActorWithMetering {
    // Internal message
    private static final class SerializeSnapshot {
        private final ShardDataTreeSnapshot snapshot;
        private final Optional<OutputStream> installSnapshotStream;
        private final ActorRef replyTo;

        SerializeSnapshot(final ShardDataTreeSnapshot snapshot, final Optional<OutputStream> installSnapshotStream,
                final ActorRef replyTo) {
            this.snapshot = requireNonNull(snapshot);
            this.installSnapshotStream = requireNonNull(installSnapshotStream);
            this.replyTo = requireNonNull(replyTo);
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

    private static final Logger LOG = LoggerFactory.getLogger(ShardSnapshotActor.class);
    // actor name override used for metering. This does not change the "real" actor name
    private static final String ACTOR_NAME_FOR_METERING = "shard-snapshot";

    private final InputOutputStreamFactory streamFactory;

    private ShardSnapshotActor(final InputOutputStreamFactory streamFactory) {
        super(ACTOR_NAME_FOR_METERING);
        this.streamFactory = requireNonNull(streamFactory);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof SerializeSnapshot) {
            onSerializeSnapshot((SerializeSnapshot) message);
        } else {
            unknownMessage(message);
        }
    }

    private void onSerializeSnapshot(final SerializeSnapshot request) {
        final var installSnapshotStream = request.getInstallSnapshotStream();
        if (installSnapshotStream.isPresent()) {
            try (var out = getOutputStream(installSnapshotStream.orElseThrow())) {
                request.getSnapshot().serialize(out);
            } catch (IOException e) {
                // TODO - we should communicate the failure in the CaptureSnapshotReply.
                LOG.error("Error serializing snapshot", logName, e);
            }
        }

        request.getReplyTo().tell(new CaptureSnapshotReply(new ShardSnapshotState(request.getSnapshot()),
                installSnapshotStream), ActorRef.noSender());
    }

    private ObjectOutputStream getOutputStream(final OutputStream outputStream) throws IOException {
        return new ObjectOutputStream(streamFactory.wrapOutputStream(outputStream));
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

    public static Props props(final InputOutputStreamFactory streamFactory) {
        return Props.create(ShardSnapshotActor.class, streamFactory);
    }
}
