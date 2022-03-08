/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedAbstractActor;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
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
        if (message instanceof CaptureSnapshotReply) {
            Snapshot snapshot = Snapshot.create(
                    ((CaptureSnapshotReply)message).getSnapshotState(),
                    params.captureSnapshot.getUnAppliedEntries(),
                    params.captureSnapshot.getLastIndex(), params.captureSnapshot.getLastTerm(),
                    params.captureSnapshot.getLastAppliedIndex(), params.captureSnapshot.getLastAppliedTerm(),
                    params.electionTerm.getCurrentTerm(), params.electionTerm.getVotedFor(),
                    params.peerInformation);

            LOG.debug("{}: Received CaptureSnapshotReply, sending {}", params.id, snapshot);

            params.replyToActor.tell(new GetSnapshotReply(params.id, snapshot), getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        } else if (message instanceof ReceiveTimeout) {
            LOG.warn("{}: Got ReceiveTimeout for inactivity - did not receive CaptureSnapshotReply within {} ms",
                    params.id, params.receiveTimeout.toMillis());

            params.replyToActor.tell(new akka.actor.Status.Failure(new TimeoutException(String.format(
                    "Timed out after %d ms while waiting for CaptureSnapshotReply",
                        params.receiveTimeout.toMillis()))), getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    public static Props props(final CaptureSnapshot captureSnapshot, final ElectionTerm electionTerm,
            final ActorRef replyToActor, final FiniteDuration receiveTimeout, final String id,
            final ServerConfigurationPayload updatedPeerInfo) {
        return Props.create(GetSnapshotReplyActor.class, new Params(captureSnapshot, electionTerm, replyToActor,
                receiveTimeout, id, updatedPeerInfo));
    }

    private static final class Params {
        final CaptureSnapshot captureSnapshot;
        final ActorRef replyToActor;
        final ElectionTerm electionTerm;
        final FiniteDuration receiveTimeout;
        final String id;
        final ServerConfigurationPayload peerInformation;

        Params(final CaptureSnapshot captureSnapshot, final ElectionTerm electionTerm, final ActorRef replyToActor,
                final FiniteDuration receiveTimeout, final String id, final ServerConfigurationPayload peerInfo) {
            this.captureSnapshot = requireNonNull(captureSnapshot);
            this.electionTerm = requireNonNull(electionTerm);
            this.replyToActor = requireNonNull(replyToActor);
            this.receiveTimeout = requireNonNull(receiveTimeout);
            this.id = requireNonNull(id);
            peerInformation = peerInfo;
        }
    }
}
