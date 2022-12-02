/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.slf4j.Logger;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    static final Object COMMIT_SNAPSHOT = new Object() {
        @Override
        public String toString() {
            return "commit_snapshot";
        }
    };

    private final RaftActorContext context;
    private final RaftActorSnapshotCohort cohort;
    private final Logger log;

    private FiniteDuration snapshotReplyActorTimeout = FiniteDuration.create(30, TimeUnit.SECONDS);

    RaftActorSnapshotMessageSupport(final RaftActorContext context, final RaftActorSnapshotCohort cohort) {
        this.context = context;
        this.cohort = cohort;
        log = context.getLogger();

        context.getSnapshotManager().setCreateSnapshotConsumer(
            outputStream -> cohort.createSnapshot(context.getActor(), outputStream));
        context.getSnapshotManager().setSnapshotCohort(cohort);
    }

    RaftActorSnapshotCohort getSnapshotCohort() {
        return cohort;
    }

    boolean handleSnapshotMessage(final Object message, final ActorRef sender) {
        if (message instanceof ApplySnapshot applySnapshot) {
            onApplySnapshot(applySnapshot);
        } else if (message instanceof SaveSnapshotSuccess saveSnapshotSuccess) {
            onSaveSnapshotSuccess(saveSnapshotSuccess);
        } else if (message instanceof SaveSnapshotFailure saveSnapshotFailure) {
            onSaveSnapshotFailure(saveSnapshotFailure);
        } else if (message instanceof CaptureSnapshotReply captureSnapshotReply) {
            onCaptureSnapshotReply(captureSnapshotReply);
        } else if (COMMIT_SNAPSHOT.equals(message)) {
            context.getSnapshotManager().commit(-1, -1);
        } else if (message instanceof GetSnapshot getSnapshot) {
            onGetSnapshot(sender, getSnapshot);
        } else if (message instanceof SnapshotComplete) {
            log.debug("{}: SnapshotComplete received", context.getId());
        } else {
            return false;
        }

        return true;
    }

    private void onCaptureSnapshotReply(final CaptureSnapshotReply reply) {
        log.debug("{}: CaptureSnapshotReply received by actor", context.getId());

        context.getSnapshotManager().persist(reply.getSnapshotState(), reply.getInstallSnapshotStream(),
                context.getTotalMemory());
    }

    private void onSaveSnapshotFailure(final SaveSnapshotFailure saveSnapshotFailure) {
        log.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(final SaveSnapshotSuccess success) {
        long sequenceNumber = success.metadata().sequenceNr();

        log.info("{}: SaveSnapshotSuccess received for snapshot, sequenceNr: {}", context.getId(), sequenceNumber);

        context.getSnapshotManager().commit(sequenceNumber, success.metadata().timestamp());
    }

    private void onApplySnapshot(final ApplySnapshot message) {
        log.info("{}: Applying snapshot on follower:  {}", context.getId(), message.getSnapshot());

        context.getSnapshotManager().apply(message);
    }

    private void onGetSnapshot(final ActorRef sender, final GetSnapshot getSnapshot) {
        log.debug("{}: onGetSnapshot", context.getId());


        if (context.getPersistenceProvider().isRecoveryApplicable()) {
            CaptureSnapshot captureSnapshot = context.getSnapshotManager().newCaptureSnapshot(
                    context.getReplicatedLog().last(), -1, true);

            final FiniteDuration timeout =
                    getSnapshot.getTimeout().map(Timeout::duration).orElse(snapshotReplyActorTimeout);

            ActorRef snapshotReplyActor = context.actorOf(GetSnapshotReplyActor.props(captureSnapshot,
                    ImmutableElectionTerm.copyOf(context.getTermInformation()), sender, timeout, context.getId(),
                    context.getPeerServerInfo(true)));

            cohort.createSnapshot(snapshotReplyActor, Optional.empty());
        } else {
            Snapshot snapshot = Snapshot.create(
                    EmptyState.INSTANCE, Collections.<ReplicatedLogEntry>emptyList(),
                    -1, -1, -1, -1,
                    context.getTermInformation().getCurrentTerm(), context.getTermInformation().getVotedFor(),
                    context.getPeerServerInfo(true));

            sender.tell(new GetSnapshotReply(context.getId(), snapshot), context.getActor());
        }
    }

    @VisibleForTesting
    void setSnapshotReplyActorTimeout(final FiniteDuration snapshotReplyActorTimeout) {
        this.snapshotReplyActorTimeout = snapshotReplyActorTimeout;
    }
}
