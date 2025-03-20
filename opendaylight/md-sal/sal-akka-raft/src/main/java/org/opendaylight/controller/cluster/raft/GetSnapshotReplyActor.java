/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.ReceiveTimeout;
import org.apache.pekko.actor.Status.Failure;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporary actor used to receive a CaptureSnapshotReply message and return a GetSnapshotReply instance.
 *
 * @author Thomas Pantelis
 */
final class GetSnapshotReplyActor extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(GetSnapshotReplyActor.class);

    private final @NonNull CaptureSnapshot captureSnapshot;
    private final @NonNull TermInfo termInfo;
    private final @NonNull ActorRef replyToActor;
    private final @NonNull Duration receiveTimeout;
    private final @NonNull String memberId;
    private final @Nullable ClusterConfig serverConfig;

    private GetSnapshotReplyActor(final CaptureSnapshot captureSnapshot, final TermInfo termInfo,
            final ActorRef replyToActor, final Duration receiveTimeout, final String memberId,
            final ClusterConfig serverConfig) {
        this.captureSnapshot = requireNonNull(captureSnapshot);
        this.termInfo = requireNonNull(termInfo);
        this.replyToActor = requireNonNull(replyToActor);
        this.receiveTimeout = requireNonNull(receiveTimeout);
        this.memberId = requireNonNull(memberId);
        this.serverConfig = serverConfig;

        getContext().setReceiveTimeout(receiveTimeout);
    }

    @NonNullByDefault
    static Props props(final CaptureSnapshot captureSnapshot, final TermInfo termInfo, final ActorRef replyToActor,
            final Duration receiveTimeout, final String id, final @Nullable ClusterConfig serverConfig) {
        return Props.create(GetSnapshotReplyActor.class, requireNonNull(captureSnapshot), requireNonNull(termInfo),
            requireNonNull(replyToActor), requireNonNull(receiveTimeout), requireNonNull(id), serverConfig);
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof CaptureSnapshotReply msg) {
            final var snapshot = Snapshot.create(msg.snapshotState(), captureSnapshot.getUnAppliedEntries(),
                captureSnapshot.getLastIndex(), captureSnapshot.getLastTerm(), captureSnapshot.getLastAppliedIndex(),
                captureSnapshot.getLastAppliedTerm(), termInfo, serverConfig);

            LOG.debug("{}: Received CaptureSnapshotReply, sending {}", memberId, snapshot);

            replyToActor.tell(new GetSnapshotReply(memberId, snapshot), ActorRef.noSender());
            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
        } else if (message instanceof ReceiveTimeout) {
            final var millis = receiveTimeout.toMillis();

            LOG.warn("{}: Got ReceiveTimeout for inactivity - did not receive CaptureSnapshotReply within {} ms",
                    memberId, millis);

            replyToActor.tell(new Failure(new TimeoutException(String.format(
                    "Timed out after %d ms while waiting for CaptureSnapshotReply", millis))), ActorRef.noSender());
            self().tell(PoisonPill.getInstance(), ActorRef.noSender());
        }
    }
}
