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
import akka.actor.UntypedActor;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    }

    @Override
    public void onReceive(Object message) {
        if(message instanceof CaptureSnapshotReply) {
            Snapshot snapshot = Snapshot.create(((CaptureSnapshotReply)message).getSnapshot(),
                    params.captureSnapshot .getUnAppliedEntries(),
                    params.captureSnapshot.getLastIndex(), params.captureSnapshot.getLastTerm(),
                    params.captureSnapshot.getLastAppliedIndex(), params.captureSnapshot.getLastAppliedTerm(),
                    params.electionTerm.getCurrentTerm(), params.electionTerm.getVotedFor());

            LOG.debug("Received CaptureSnapshotReply, sending {}", snapshot);

            params.replyToActor.tell(new GetSnapshotReply(SerializationUtils.serialize(snapshot)), getSelf());
            getSelf().tell(PoisonPill.getInstance(), getSelf());
        }
    }

    public static Props props(CaptureSnapshot captureSnapshot, ElectionTerm electionTerm, ActorRef replyToActor) {
        return Props.create(GetSnapshotReplyActor.class, new Params(captureSnapshot, electionTerm, replyToActor));
    }

    private static final class Params {
        final CaptureSnapshot captureSnapshot;
        final ActorRef replyToActor;
        final ElectionTerm electionTerm;

        Params(CaptureSnapshot captureSnapshot, ElectionTerm electionTerm, ActorRef replyToActor) {
            this.captureSnapshot = Preconditions.checkNotNull(captureSnapshot);
            this.electionTerm = Preconditions.checkNotNull(electionTerm);
            this.replyToActor = Preconditions.checkNotNull(replyToActor);
        }
    }
}
