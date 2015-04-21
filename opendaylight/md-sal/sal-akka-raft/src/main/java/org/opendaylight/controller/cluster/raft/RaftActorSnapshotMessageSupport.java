/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import akka.persistence.SaveSnapshotFailure;
import akka.persistence.SaveSnapshotSuccess;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    static final String COMMIT_SNAPSHOT = "commit_snapshot";

    private final DataPersistenceProvider persistence;
    private final RaftActorContext context;
    private final RaftActorBehavior currentBehavior;
    private final RaftActorSnapshotCohort cohort;
    private final Logger log;

    private final Procedure<Void> createSnapshotProcedure = new Procedure<Void>() {
        @Override
        public void apply(Void notUsed) throws Exception {
            cohort.createSnapshot(context.getActor());
        }
    };

    RaftActorSnapshotMessageSupport(DataPersistenceProvider persistence, RaftActorContext context,
            RaftActorBehavior currentBehavior, RaftActorSnapshotCohort cohort) {
        this.persistence = persistence;
        this.context = context;
        this.currentBehavior = currentBehavior;
        this.cohort = cohort;
        this.log = context.getLogger();

        context.getSnapshotManager().setCreateSnapshotCallable(createSnapshotProcedure);
    }

    boolean handleSnapshotMessage(Object message) {
        if(message instanceof ApplySnapshot ) {
            onApplySnapshot(((ApplySnapshot) message).getSnapshot());
            return true;
        } else if (message instanceof SaveSnapshotSuccess) {
            onSaveSnapshotSuccess((SaveSnapshotSuccess) message);
            return true;
        } else if (message instanceof SaveSnapshotFailure) {
            onSaveSnapshotFailure((SaveSnapshotFailure) message);
            return true;
        } else if (message instanceof CaptureSnapshotReply) {
            onCaptureSnapshotReply(((CaptureSnapshotReply) message).getSnapshot());
            return true;
        } else if (message.equals(COMMIT_SNAPSHOT)) {
            context.getSnapshotManager().commit(persistence, -1);
            return true;
        } else {
            return false;
        }
    }

    private void onCaptureSnapshotReply(byte[] snapshotBytes) {
        log.debug("{}: CaptureSnapshotReply received by actor: snapshot size {}", context.getId(), snapshotBytes.length);

        context.getSnapshotManager().persist(persistence, snapshotBytes, currentBehavior, context.getTotalMemory());
    }

    private void onSaveSnapshotFailure(SaveSnapshotFailure saveSnapshotFailure) {
        log.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(SaveSnapshotSuccess success) {
        log.info("{}: SaveSnapshotSuccess received for snapshot", context.getId());

        long sequenceNumber = success.metadata().sequenceNr();

        context.getSnapshotManager().commit(persistence, sequenceNumber);
    }

    private void onApplySnapshot(Snapshot snapshot) {
        if(log.isDebugEnabled()) {
            log.debug("{}: ApplySnapshot called on Follower Actor " +
                    "snapshotIndex:{}, snapshotTerm:{}", context.getId(), snapshot.getLastAppliedIndex(),
                snapshot.getLastAppliedTerm());
        }

        cohort.applySnapshot(snapshot.getState());

        //clears the followers log, sets the snapshot index to ensure adjusted-index works
        context.setReplicatedLog(ReplicatedLogImpl.newInstance(snapshot, context, persistence,
                currentBehavior));
        context.setLastApplied(snapshot.getLastAppliedIndex());
    }
}
