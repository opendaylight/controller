/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.japi.Procedure;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.slf4j.Logger;
import scala.concurrent.duration.Duration;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    static final String COMMIT_SNAPSHOT = "commit_snapshot";

    private final RaftActorContext context;
    private final RaftActorSnapshotCohort cohort;
    private final Logger log;

    private final Procedure<Void> createSnapshotProcedure = new Procedure<Void>() {
        @Override
        public void apply(Void notUsed) {
            cohort.createSnapshot(context.getActor());
        }
    };

    private final Procedure<byte[]> applySnapshotProcedure = new Procedure<byte[]>() {
        @Override
        public void apply(byte[] state) {
            cohort.applySnapshot(state);
        }
    };

    private Duration snapshotReplyActorTimeout = Duration.create(30, TimeUnit.SECONDS);

    RaftActorSnapshotMessageSupport(final RaftActorContext context, final RaftActorSnapshotCohort cohort) {
        this.context = context;
        this.cohort = cohort;
        this.log = context.getLogger();

        context.getSnapshotManager().setCreateSnapshotCallable(createSnapshotProcedure);
        context.getSnapshotManager().setApplySnapshotProcedure(applySnapshotProcedure);
    }

    boolean handleSnapshotMessage(Object message, ActorRef sender) {
        if (message instanceof ApplySnapshot ) {
            onApplySnapshot((ApplySnapshot) message);
        } else if (message instanceof SaveSnapshotSuccess) {
            onSaveSnapshotSuccess((SaveSnapshotSuccess) message);
        } else if (message instanceof SaveSnapshotFailure) {
            onSaveSnapshotFailure((SaveSnapshotFailure) message);
        } else if (message instanceof CaptureSnapshotReply) {
            onCaptureSnapshotReply(((CaptureSnapshotReply) message).getSnapshot());
        } else if (message.equals(COMMIT_SNAPSHOT)) {
            context.getSnapshotManager().commit(-1);
        } else if (message instanceof GetSnapshot) {
            onGetSnapshot(sender);
        } else {
            return false;
        }

        return true;
    }

    private void onCaptureSnapshotReply(byte[] snapshotBytes) {
        log.debug("{}: CaptureSnapshotReply received by actor: snapshot size {}", context.getId(), snapshotBytes.length);

        context.getSnapshotManager().persist(snapshotBytes, context.getTotalMemory());
    }

    private void onSaveSnapshotFailure(SaveSnapshotFailure saveSnapshotFailure) {
        log.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(SaveSnapshotSuccess success) {
        log.info("{}: SaveSnapshotSuccess received for snapshot", context.getId());

        long sequenceNumber = success.metadata().sequenceNr();

        context.getSnapshotManager().commit(sequenceNumber);
    }

    private void onApplySnapshot(ApplySnapshot message) {
        log.info("{}: Applying snapshot on follower with snapshotIndex: {}, snapshotTerm: {}", context.getId(),
                message.getSnapshot().getLastAppliedIndex(), message.getSnapshot().getLastAppliedTerm());

        context.getSnapshotManager().apply(message);
    }

    private void onGetSnapshot(ActorRef sender) {
        log.debug("{}: onGetSnapshot", context.getId());

        if(context.getPersistenceProvider().isRecoveryApplicable()) {
            CaptureSnapshot captureSnapshot = context.getSnapshotManager().newCaptureSnapshot(
                    context.getReplicatedLog().last(), -1, false);

            ActorRef snapshotReplyActor = context.actorOf(GetSnapshotReplyActor.props(captureSnapshot,
                    ImmutableElectionTerm.copyOf(context.getTermInformation()), sender,
                    snapshotReplyActorTimeout, context.getId(), context.getPeerServerInfo(true)));

            cohort.createSnapshot(snapshotReplyActor);
        } else {
            Snapshot snapshot = Snapshot.create(new byte[0], Collections.<ReplicatedLogEntry>emptyList(), -1, -1, -1, -1,
                    context.getTermInformation().getCurrentTerm(), context.getTermInformation().getVotedFor(),
                    context.getPeerServerInfo(true));

            sender.tell(new GetSnapshotReply(context.getId(), SerializationUtils.serialize(snapshot)),
                    context.getActor());
        }
    }

    @VisibleForTesting
    void setSnapshotReplyActorTimeout(Duration snapshotReplyActorTimeout) {
        this.snapshotReplyActorTimeout = snapshotReplyActorTimeout;
    }
}
