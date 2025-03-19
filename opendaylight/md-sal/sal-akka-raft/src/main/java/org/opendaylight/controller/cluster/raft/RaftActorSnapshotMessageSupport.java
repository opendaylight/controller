/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.apache.pekko.persistence.SaveSnapshotFailure;
import org.apache.pekko.persistence.SaveSnapshotSuccess;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.SnapshotManager.ApplyLeaderSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.SnapshotComplete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    RaftActorSnapshotMessageSupport(final RaftActorContext context) {
        this.context = requireNonNull(context);
    }

    boolean handleSnapshotMessage(final Object message) {
        switch (message) {
            case ApplyLeaderSnapshot msg -> context.getSnapshotManager().applyFromLeader(msg);
            case SaveSnapshotSuccess msg -> onSaveSnapshotSuccess(msg);
            case SaveSnapshotFailure msg -> onSaveSnapshotFailure(msg);
            case CaptureSnapshotReply msg -> onCaptureSnapshotReply(msg);
            case CommitSnapshot msg -> context.getSnapshotManager().commit(-1, -1);
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
        LOG.error("{}: SaveSnapshotFailure received for snapshot Cause:", context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(final SaveSnapshotSuccess success) {
        final var sequenceNumber = success.metadata().sequenceNr();

        LOG.info("{}: SaveSnapshotSuccess received for snapshot, sequenceNr: {}", context.getId(), sequenceNumber);

        context.getSnapshotManager().commit(sequenceNumber, success.metadata().timestamp());
    }
}
