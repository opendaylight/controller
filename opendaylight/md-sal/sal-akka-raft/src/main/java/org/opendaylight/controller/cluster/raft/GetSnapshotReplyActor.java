/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Temporary actor used to receive a CaptureSnapshotReply message and return a GetSnapshotReply instance.
 *
 * @author Thomas Pantelis
 */
class GetSnapshotReplyActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(GetSnapshotReplyActor.class);

    private final Params params;

    private GetSnapshotReplyActor(Params params) {
        this.params = params;

        getContext().setReceiveTimeout(params.receiveTimeout);
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof CaptureSnapshotReply) {
            Snapshot snapshot = Snapshot.create(((CaptureSnapshotReply)message).getSnapshot(),
                    params.captureSnapshot.getUnAppliedEntries(),
                    params.captureSnapshot.getLastIndex(), params.captureSnapshot.getLastTerm(),
                    params.captureSnapshot.getLastAppliedIndex(), params.captureSnapshot.getLastAppliedTerm(),
                    params.electionTerm.getCurrentTerm(), params.electionTerm.getVotedFor(),
                    params.peerInformation);

            LOG.debug("{}: Received CaptureSnapshotReply, sending {}", params.id, snapshot);

            params.replyToActor.tell(new GetSnapshotReply(params.id, SerializationUtils.serialize(snapshot)), getSelf());
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

    public static Props props(CaptureSnapshot captureSnapshot, ElectionTerm electionTerm, ActorRef replyToActor,
            Duration receiveTimeout, String id, ServerConfigurationPayload updatedPeerInfo) {
        return Props.create(GetSnapshotReplyActor.class, new Params(captureSnapshot, electionTerm, replyToActor,
                receiveTimeout, id, updatedPeerInfo));
    }

    private static final class Params {
        final CaptureSnapshot captureSnapshot;
        final ActorRef replyToActor;
        final ElectionTerm electionTerm;
        final Duration receiveTimeout;
        final String id;
        final ServerConfigurationPayload peerInformation;

        Params(CaptureSnapshot captureSnapshot, ElectionTerm electionTerm, ActorRef replyToActor,
                Duration receiveTimeout, String id, ServerConfigurationPayload peerInfo) {
            this.captureSnapshot = Preconditions.checkNotNull(captureSnapshot);
            this.electionTerm = Preconditions.checkNotNull(electionTerm);
            this.replyToActor = Preconditions.checkNotNull(replyToActor);
            this.receiveTimeout = Preconditions.checkNotNull(receiveTimeout);
            this.id = Preconditions.checkNotNull(id);
            this.peerInformation = peerInfo;
        }
    }
}
