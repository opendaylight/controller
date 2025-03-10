/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.ReceiveTimeout;
import org.apache.pekko.actor.UntypedAbstractActor;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Temporary actor used to receive a CaptureSnapshotReply message and return a GetSnapshotReply instance.
 *
 * @author Thomas Pantelis
 */
final class GetSnapshotReplyActor extends UntypedAbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(GetSnapshotReplyActor.class);

    private final Params params;

    GetSnapshotReplyActor(final Params params) {
        this.params = params;

        getContext().setReceiveTimeout(params.receiveTimeout);
    }

    @Override
    public void onReceive(final Object message) {
        if (message instanceof CaptureSnapshotReply msg) {
            Snapshot snapshot = Snapshot.create(msg.snapshotState(),
                params.captureSnapshot.getUnAppliedEntries(),
                params.captureSnapshot.getLastIndex(), params.captureSnapshot.getLastTerm(),
                params.captureSnapshot.getLastAppliedIndex(), params.captureSnapshot.getLastAppliedTerm(),
                params.termInfo, params.serverConfig);

            LOG.debug("{}: Received CaptureSnapshotReply, sending {}", params.id, snapshot);

            params.replyToActor.tell(new GetSnapshotReply(params.id, snapshot), self());
            self().tell(PoisonPill.getInstance(), self());
        } else if (message instanceof ReceiveTimeout) {
            LOG.warn("{}: Got ReceiveTimeout for inactivity - did not receive CaptureSnapshotReply within {} ms",
                    params.id, params.receiveTimeout.toMillis());

            params.replyToActor.tell(new org.apache.pekko.actor.Status.Failure(new TimeoutException(String.format(
                    "Timed out after %d ms while waiting for CaptureSnapshotReply",
                        params.receiveTimeout.toMillis()))), self());
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    public static Props props(final CaptureSnapshot captureSnapshot, final TermInfo termInfo,
            final ActorRef replyToActor, final FiniteDuration receiveTimeout, final String id,
            final ClusterConfig updatedPeerInfo) {
        return Props.create(GetSnapshotReplyActor.class, new Params(captureSnapshot, termInfo, replyToActor,
                receiveTimeout, id, updatedPeerInfo));
    }

    private record Params(
            CaptureSnapshot captureSnapshot,
            TermInfo termInfo,
            ActorRef replyToActor,
            FiniteDuration receiveTimeout,
            String id,
            ClusterConfig serverConfig) {
        Params {
            requireNonNull(captureSnapshot);
            requireNonNull(termInfo);
            requireNonNull(replyToActor);
            requireNonNull(receiveTimeout);
            requireNonNull(id);
        }
    }
}
