/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.client.messages.GetSnapshotReply;
import org.opendaylight.controller.cluster.raft.persisted.EmptyState;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    @NonNullByDefault
    static final class CommitSnapshot {
        static final CommitSnapshot INSTANCE = new CommitSnapshot();

        private CommitSnapshot() {
            // Hidden on purpose
        }

        @Override
        public String toString() {
            return "commit_snapshot";
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RaftActorSnapshotMessageSupport.class);

    private final RaftActorContext context;
    private final RaftActorSnapshotCohort cohort;

    private FiniteDuration snapshotReplyActorTimeout = FiniteDuration.create(30, TimeUnit.SECONDS);

    RaftActorSnapshotMessageSupport(final RaftActorContext context, final RaftActorSnapshotCohort cohort) {
        this.context = requireNonNull(context);
        this.cohort = requireNonNull(cohort);
        context.getSnapshotManager().setSnapshotCohort(cohort);
    }

    RaftActorSnapshotCohort getSnapshotCohort() {
        return cohort;
    }

    boolean handleSnapshotMessage(final Object message, final ActorRef sender) {
        switch (message) {
            case ApplyLeaderSnapshot msg -> context.getSnapshotManager().applyFromLeader(msg);
            case SaveSnapshotSuccess msg -> onSaveSnapshotSuccess(msg);
            case SaveSnapshotFailure msg -> onSaveSnapshotFailure(msg);
            case CaptureSnapshotReply msg -> onCaptureSnapshotReply(msg);
            case CommitSnapshot msg -> context.getSnapshotManager().commit(-1, -1);
            case GetSnapshot msg -> onGetSnapshot(sender, msg);
            case SnapshotComplete msg -> LOG.debug("{}: SnapshotComplete received", context.getId());
            default -> {
                return false;
            }
        }
        return true;
    }

    private void onCaptureSnapshotReply(final CaptureSnapshotReply reply) {
        LOG.debug("{}: CaptureSnapshotReply received by actor", context.getId());

        context.getSnapshotManager().persist(reply.snapshotState(), Optional.ofNullable(reply.installSnapshotStream()),
                context.getTotalMemory());
    }

    private void onSaveSnapshotFailure(final SaveSnapshotFailure saveSnapshotFailure) {
        LOG.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(final SaveSnapshotSuccess success) {
        long sequenceNumber = success.metadata().sequenceNr();

        LOG.info("{}: SaveSnapshotSuccess received for snapshot, sequenceNr: {}", context.getId(), sequenceNumber);

        context.getSnapshotManager().commit(sequenceNumber, success.metadata().timestamp());
    }

    private void onGetSnapshot(final ActorRef sender, final GetSnapshot getSnapshot) {
        LOG.debug("{}: onGetSnapshot", context.getId());


        if (context.getPersistenceProvider().isRecoveryApplicable()) {
            CaptureSnapshot captureSnapshot = context.getSnapshotManager().newCaptureSnapshot(
                    context.getReplicatedLog().lastMeta(), -1, true);

            final FiniteDuration timeout =
                    getSnapshot.getTimeout().map(Timeout::duration).orElse(snapshotReplyActorTimeout);

            ActorRef snapshotReplyActor = context.actorOf(GetSnapshotReplyActor.props(captureSnapshot,
                    context.termInfo(), sender, timeout, context.getId(), context.getPeerServerInfo(true)));

            cohort.createSnapshot(snapshotReplyActor, null);
        } else {
            Snapshot snapshot = Snapshot.create(EmptyState.INSTANCE, List.of(),
                    -1, -1, -1, -1, context.termInfo(), context.getPeerServerInfo(true));

            sender.tell(new GetSnapshotReply(context.getId(), snapshot), context.getActor());
        }
    }

    @VisibleForTesting
    void setSnapshotReplyActorTimeout(final FiniteDuration snapshotReplyActorTimeout) {
        this.snapshotReplyActorTimeout = snapshotReplyActorTimeout;
    }
}
