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
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActorWithMetering;
import org.opendaylight.controller.cluster.datastore.persisted.ShardSnapshotState;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.raft.spi.InputOutputStreamFactory;

/**
 * This is an offload actor, which is given an isolated snapshot of the data tree. It performs the potentially
 * time-consuming operation of serializing the snapshot.
 */
public final class ShardSnapshotActor extends AbstractUntypedActorWithMetering {
    // Internal message
    @NonNullByDefault
    private record SerializeSnapshot(ShardSnapshotState state, OutputStream outputStream, ActorRef replyTo) {
        SerializeSnapshot {
            requireNonNull(state);
            requireNonNull(outputStream);
            requireNonNull(replyTo);
        }
    }

    //actor name override used for metering. This does not change the "real" actor name
    private static final String ACTOR_NAME_FOR_METERING = "shard-snapshot";

    private final InputOutputStreamFactory streamFactory;

    private ShardSnapshotActor(final InputOutputStreamFactory streamFactory) {
        super(ACTOR_NAME_FOR_METERING);
        this.streamFactory = requireNonNull(streamFactory);
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof SerializeSnapshot req) {
            onSerializeSnapshot(req);
        } else {
            unknownMessage(message);
        }
    }

    private void onSerializeSnapshot(final SerializeSnapshot request) {
        try (var out = getOutputStream(request.outputStream)) {
            request.state.getSnapshot().serialize(out);
        } catch (IOException e) {
            // TODO - we should communicate the failure in the CaptureSnapshotReply.
            LOG.error("Error serializing snapshot", e);
        }

        request.replyTo().tell(new CaptureSnapshotReply(request.state, request.outputStream), ActorRef.noSender());
    }

    private ObjectOutputStream getOutputStream(final OutputStream outputStream) throws IOException {
        return new ObjectOutputStream(streamFactory.wrapOutputStream(outputStream));
    }

    /**
     * Sends a request to a ShardSnapshotActor to process a snapshot and send a CaptureSnapshotReply.
     *
     * @param snapshotActor the ShardSnapshotActor
     * @param state the snapshot to process
     * @param outputStream OutputStream that is present if the snapshot is to also be installed on a follower.
     * @param replyTo the actor to which to send the CaptureSnapshotReply
     */
    @NonNullByDefault
    public static void requestSnapshot(final ActorRef snapshotActor, final ShardSnapshotState state,
            final OutputStream outputStream, final ActorRef replyTo) {
        snapshotActor.tell(new SerializeSnapshot(state, outputStream, replyTo), ActorRef.noSender());
    }

    public static Props props(final InputOutputStreamFactory streamFactory) {
        return Props.create(ShardSnapshotActor.class, streamFactory);
    }
}
