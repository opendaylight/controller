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

    private final SnapshotManager snapshotManager;

    RaftActorSnapshotMessageSupport(final SnapshotManager snapshotManager) {
        this.snapshotManager = requireNonNull(snapshotManager);
    }

    boolean handleSnapshotMessage(final Object message) {
        switch (message) {
            case ApplyLeaderSnapshot msg -> snapshotManager.applyFromLeader(msg);
            case SaveSnapshotSuccess msg -> onSaveSnapshotSuccess(msg);
            case SaveSnapshotFailure msg -> onSaveSnapshotFailure(msg);
            case CaptureSnapshotReply msg -> onCaptureSnapshotReply(msg);
            case CommitSnapshot msg -> snapshotManager.commit(-1, -1);
            case SnapshotComplete msg -> LOG.debug("{}: SnapshotComplete received", snapshotManager.memberId());
            default -> {
                return false;
            }
        }
        return true;
    }

    private void onCaptureSnapshotReply(final CaptureSnapshotReply reply) {
        LOG.debug("{}: CaptureSnapshotReply received by actor", snapshotManager.memberId());
        snapshotManager.persist(reply.snapshotState(), Optional.ofNullable(reply.installSnapshotStream()));
    }

    private void onSaveSnapshotFailure(final SaveSnapshotFailure failure) {
        LOG.error("{}: SaveSnapshotFailure received for snapshot Cause:", snapshotManager.memberId(), failure.cause());
        snapshotManager.rollback();
    }

    private void onSaveSnapshotSuccess(final SaveSnapshotSuccess success) {
        final var sequenceNumber = success.metadata().sequenceNr();
        LOG.info("{}: SaveSnapshotSuccess received for snapshot, sequenceNr: {}", snapshotManager.memberId(),
            sequenceNumber);
        snapshotManager.commit(sequenceNumber, success.metadata().timestamp());
    }
}
