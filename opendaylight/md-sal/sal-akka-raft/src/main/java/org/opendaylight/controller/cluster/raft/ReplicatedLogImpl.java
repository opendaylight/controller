/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.raft.base.messages.DeleteEntries;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 */
class ReplicatedLogImpl extends AbstractReplicatedLogImpl {
    private static final int DATA_SIZE_DIVIDER = 5;

    private long dataSizeSinceLastSnapshot = 0L;
    private final RaftActorContext context;

    private final Procedure<DeleteEntries> deleteProcedure = new Procedure<DeleteEntries>() {
        @Override
        public void apply(final DeleteEntries notUsed) {
        }
    };

    static ReplicatedLog newInstance(final Snapshot snapshot, final RaftActorContext context) {
        return new ReplicatedLogImpl(snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(),
                snapshot.getUnAppliedEntries(), context);
    }

    static ReplicatedLog newInstance(final RaftActorContext context) {
        return new ReplicatedLogImpl(-1L, -1L, Collections.<ReplicatedLogEntry>emptyList(), context);
    }

    private ReplicatedLogImpl(final long snapshotIndex, final long snapshotTerm, final List<ReplicatedLogEntry> unAppliedEntries,
            final RaftActorContext context) {
        super(snapshotIndex, snapshotTerm, unAppliedEntries);
        this.context = Preconditions.checkNotNull(context);
    }

    @Override
    public void removeFromAndPersist(final long logEntryIndex) {
        // FIXME: Maybe this should be done after the command is saved
        long adjustedIndex = removeFrom(logEntryIndex);
        if(adjustedIndex >= 0) {
            context.getPersistenceProvider().persist(new DeleteEntries(adjustedIndex), deleteProcedure);
        }
    }

    @Override
    public void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry) {
        appendAndPersist(replicatedLogEntry, null);
    }

    @Override
    public void captureSnapshotIfReady(final ReplicatedLogEntry replicatedLogEntry) {
        long journalSize = replicatedLogEntry.getIndex() + 1;
        long dataThreshold = context.getTotalMemory() *
                context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;

        if ((journalSize % context.getConfigParams().getSnapshotBatchCount() == 0
                || getDataSizeForSnapshotCheck() > dataThreshold)) {

            boolean started = context.getSnapshotManager().capture(replicatedLogEntry,
                    context.getCurrentBehavior().getReplicatedToAllIndex());
            if (started) {
                if (!context.hasFollowers()) {
                    dataSizeSinceLastSnapshot = 0;
                }
            }
        }
    }

    private long getDataSizeForSnapshotCheck() {
        long dataSizeForCheck = dataSize();
        if (!context.hasFollowers()) {
            // When we do not have followers we do not maintain an in-memory log
            // due to this the journalSize will never become anything close to the
            // snapshot batch count. In fact will mostly be 1.
            // Similarly since the journal's dataSize depends on the entries in the
            // journal the journal's dataSize will never reach a value close to the
            // memory threshold.
            // By maintaining the dataSize outside the journal we are tracking essentially
            // what we have written to the disk however since we no longer are in
            // need of doing a snapshot just for the sake of freeing up memory we adjust
            // the real size of data by the DATA_SIZE_DIVIDER so that we do not snapshot as often
            // as if we were maintaining a real snapshot
            dataSizeForCheck = dataSizeSinceLastSnapshot / DATA_SIZE_DIVIDER;
        }
        return dataSizeForCheck;
    }

    @Override
    public void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry,
            final Procedure<ReplicatedLogEntry> callback)  {

        if (context.getLogger().isDebugEnabled()) {
            context.getLogger().debug("{}: Append log entry and persist {} ", context.getId(), replicatedLogEntry);
        }

        // FIXME : By adding the replicated log entry to the in-memory journal we are not truly ensuring durability of the logs
        append(replicatedLogEntry);

        // When persisting events with persist it is guaranteed that the
        // persistent actor will not receive further commands between the
        // persist call and the execution(s) of the associated event
        // handler. This also holds for multiple persist calls in context
        // of a single command.
        context.getPersistenceProvider().persist(replicatedLogEntry,
            new Procedure<ReplicatedLogEntry>() {
                @Override
                public void apply(final ReplicatedLogEntry param) throws Exception {
                    context.getLogger().debug("{}: persist complete {}", context.getId(), param);

                    int logEntrySize = param.size();
                    dataSizeSinceLastSnapshot += logEntrySize;

                    if (callback != null) {
                        callback.apply(param);
                    }
                }
            }
        );
    }
}