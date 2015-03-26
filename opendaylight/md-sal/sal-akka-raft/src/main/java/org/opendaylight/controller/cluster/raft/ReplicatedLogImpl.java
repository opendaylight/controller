/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import akka.japi.Procedure;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.RaftActor.DeleteEntries;
import org.opendaylight.controller.cluster.raft.behaviors.RaftActorBehavior;

/**
 * Implementation of ReplicatedLog used by the RaftActor.
 *
 * @author Moiz Raja, Thomas Pantelis
 */
class ReplicatedLogImpl extends AbstractReplicatedLogImpl {
    private static final int DATA_SIZE_DIVIDER = 5;

    private long dataSizeSinceLastSnapshot = 0L;
    private final RaftActorContext context;
    private final DataPersistenceProvider persistence;
    private final RaftActorBehavior currentBehavior;

    private final Procedure<DeleteEntries> deleteProcedure = new Procedure<DeleteEntries>() {
        @Override
        public void apply(DeleteEntries param) {
            dataSize = 0;
            for (ReplicatedLogEntry entry : journal) {
                dataSize += entry.size();
            }
        }
    };

    static ReplicatedLog newInstance(Snapshot snapshot, RaftActorContext context,
            DataPersistenceProvider persistence, RaftActorBehavior currentBehavior) {
        return new ReplicatedLogImpl(snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm(),
                snapshot.getUnAppliedEntries(), context, persistence, currentBehavior);
    }

    static ReplicatedLog newInstance(RaftActorContext context,
            DataPersistenceProvider persistence, RaftActorBehavior currentBehavior) {
        return new ReplicatedLogImpl(-1L, -1L, Collections.<ReplicatedLogEntry>emptyList(), context,
                persistence, currentBehavior);
    }

    private ReplicatedLogImpl(long snapshotIndex, long snapshotTerm, List<ReplicatedLogEntry> unAppliedEntries,
            RaftActorContext context, DataPersistenceProvider persistence, RaftActorBehavior currentBehavior) {
        super(snapshotIndex, snapshotTerm, unAppliedEntries);
        this.context = context;
        this.persistence = persistence;
        this.currentBehavior = currentBehavior;
    }

    @Override
    public void removeFromAndPersist(long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);

        if (adjustedIndex < 0) {
            return;
        }

        // FIXME: Maybe this should be done after the command is saved
        journal.subList(adjustedIndex , journal.size()).clear();

        persistence.persist(new DeleteEntries(adjustedIndex), deleteProcedure);
    }

    @Override
    public void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry) {
        appendAndPersist(replicatedLogEntry, null);
    }

    @Override
    public void appendAndPersist(final ReplicatedLogEntry replicatedLogEntry,
            final Procedure<ReplicatedLogEntry> callback)  {

        if(context.getLogger().isDebugEnabled()) {
            context.getLogger().debug("{}: Append log entry and persist {} ", context.getId(), replicatedLogEntry);
        }

        // FIXME : By adding the replicated log entry to the in-memory journal we are not truly ensuring durability of the logs
        journal.add(replicatedLogEntry);

        // When persisting events with persist it is guaranteed that the
        // persistent actor will not receive further commands between the
        // persist call and the execution(s) of the associated event
        // handler. This also holds for multiple persist calls in context
        // of a single command.
        persistence.persist(replicatedLogEntry,
            new Procedure<ReplicatedLogEntry>() {
                @Override
                public void apply(ReplicatedLogEntry evt) throws Exception {
                    int logEntrySize = replicatedLogEntry.size();

                    dataSize += logEntrySize;
                    long dataSizeForCheck = dataSize;

                    dataSizeSinceLastSnapshot += logEntrySize;

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
                    long journalSize = replicatedLogEntry.getIndex() + 1;
                    long dataThreshold = context.getTotalMemory() *
                            context.getConfigParams().getSnapshotDataThresholdPercentage() / 100;

                    if ((journalSize % context.getConfigParams().getSnapshotBatchCount() == 0
                            || dataSizeForCheck > dataThreshold)) {

                        boolean started = context.getSnapshotManager().capture(replicatedLogEntry,
                                currentBehavior.getReplicatedToAllIndex());

                        if(started){
                            dataSizeSinceLastSnapshot = 0;
                        }
                    }

                    if (callback != null){
                        callback.apply(replicatedLogEntry);
                    }
                }
            }
        );
    }
}