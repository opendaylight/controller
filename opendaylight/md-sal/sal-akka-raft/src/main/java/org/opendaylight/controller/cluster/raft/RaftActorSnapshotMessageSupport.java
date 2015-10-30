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
import org.opendaylight.controller.cluster.raft.base.messages.ApplySnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshot;
import org.opendaylight.controller.cluster.raft.base.messages.CaptureSnapshotReply;
import org.opendaylight.controller.cluster.raft.base.messages.GetSnapshot;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;
import org.slf4j.Logger;

/**
 * Handles snapshot related messages for a RaftActor.
 *
 * @author Thomas Pantelis
 */
class RaftActorSnapshotMessageSupport {
    static final String COMMIT_SNAPSHOT = "commit_snapshot";

    private final RaftActorContext context;
    private final RaftActorBehavior currentBehavior;
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

    RaftActorSnapshotMessageSupport(RaftActorContext context, RaftActorBehavior currentBehavior,
            RaftActorSnapshotCohort cohort) {
        this.context = context;
        this.currentBehavior = currentBehavior;
        this.cohort = cohort;
        this.log = context.getLogger();

        context.getSnapshotManager().setCreateSnapshotCallable(createSnapshotProcedure);
        context.getSnapshotManager().setApplySnapshotProcedure(applySnapshotProcedure);
    }

    boolean handleSnapshotMessage(Object message, ActorRef sender) {
        if(message instanceof ApplySnapshot ) {
            onApplySnapshot((ApplySnapshot) message);
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
            context.getSnapshotManager().commit(-1, currentBehavior);
            return true;
        } else if (message instanceof GetSnapshot) {
            onGetSnapshot(sender);
            return true;
        } else {
            return false;
        }
    }

    private void onCaptureSnapshotReply(byte[] snapshotBytes) {
        log.debug("{}: CaptureSnapshotReply received by actor: snapshot size {}", context.getId(), snapshotBytes.length);

        context.getSnapshotManager().persist(snapshotBytes, currentBehavior, context.getTotalMemory());
    }

    private void onSaveSnapshotFailure(SaveSnapshotFailure saveSnapshotFailure) {
        log.error("{}: SaveSnapshotFailure received for snapshot Cause:",
                context.getId(), saveSnapshotFailure.cause());

        context.getSnapshotManager().rollback();
    }

    private void onSaveSnapshotSuccess(SaveSnapshotSuccess success) {
        log.info("{}: SaveSnapshotSuccess received for snapshot", context.getId());

        long sequenceNumber = success.metadata().sequenceNr();

        context.getSnapshotManager().commit(sequenceNumber, currentBehavior);
    }

    private void onApplySnapshot(ApplySnapshot message) {
        log.info("{}: Applying snapshot on follower with snapshotIndex: {}, snapshotTerm: {}", context.getId(),
                message.getSnapshot().getLastAppliedIndex(), message.getSnapshot().getLastAppliedTerm());

        context.getSnapshotManager().apply(message);
    }

    private void onGetSnapshot(ActorRef sender) {
        log.debug("{}: onGetSnapshot", context.getId());

        CaptureSnapshot captureSnapshot = context.getSnapshotManager().newCaptureSnapshot(
                context.getReplicatedLog().last(), -1, false);

        ActorRef snapshotReplyActor = context.actorOf(SnapshotReplyActor.props(captureSnapshot,
                ImmutableElectionTerm.copyOf(context.getTermInformation()), sender));

        cohort.createSnapshot(snapshotReplyActor);
    }
}
