/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class handling the mapping of
 * logical LogEntry Index and the physical list index.
 */
public abstract class AbstractReplicatedLogImpl implements ReplicatedLog {

    protected List<ReplicatedLogEntry> journal;
    protected ByteString snapshot;
    protected long snapshotIndex = -1;
    protected long snapshotTerm = -1;

    // to be used for rollback during save snapshot failure
    protected List<ReplicatedLogEntry> snapshottedJournal;
    protected ByteString previousSnapshot;
    protected long previousSnapshotIndex = -1;
    protected long previousSnapshotTerm = -1;

    public AbstractReplicatedLogImpl(ByteString state, long snapshotIndex,
        long snapshotTerm, List<ReplicatedLogEntry> unAppliedEntries) {
        this.snapshot = state;
        this.snapshotIndex = snapshotIndex;
        this.snapshotTerm = snapshotTerm;
        this.journal = new ArrayList<>(unAppliedEntries);
    }


    public AbstractReplicatedLogImpl() {
        this.snapshot = null;
        this.journal = new ArrayList<>();
    }

    protected int adjustedIndex(long logEntryIndex) {
        if(snapshotIndex < 0){
            return (int) logEntryIndex;
        }
        return (int) (logEntryIndex - (snapshotIndex + 1));
    }

    @Override
    public ReplicatedLogEntry get(long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);

        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return null;
        }

        return journal.get(adjustedIndex);
    }

    @Override
    public ReplicatedLogEntry last() {
        if (journal.isEmpty()) {
            return null;
        }
        // get the last entry directly from the physical index
        return journal.get(journal.size() - 1);
    }

    @Override
    public long lastIndex() {
        if (journal.isEmpty()) {
            // it can happen that after snapshot, all the entries of the
            // journal are trimmed till lastApplied, so lastIndex = snapshotIndex
            return snapshotIndex;
        }
        return last().getIndex();
    }

    @Override
    public long lastTerm() {
        if (journal.isEmpty()) {
            // it can happen that after snapshot, all the entries of the
            // journal are trimmed till lastApplied, so lastTerm = snapshotTerm
            return snapshotTerm;
        }
        return last().getTerm();
    }

    @Override
    public void removeFrom(long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return;
        }
        journal.subList(adjustedIndex , journal.size()).clear();
    }

    @Override
    public void append(ReplicatedLogEntry replicatedLogEntry) {
        journal.add(replicatedLogEntry);
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(long logEntryIndex) {
        return getFrom(logEntryIndex, journal.size());
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(long logEntryIndex, int max) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        int size = journal.size();
        List<ReplicatedLogEntry> entries = new ArrayList<>(100);
        if (adjustedIndex >= 0 && adjustedIndex < size) {
            // physical index should be less than list size and >= 0
            int maxIndex = adjustedIndex + max;
            if(maxIndex > size){
                maxIndex = size;
            }
            entries.addAll(journal.subList(adjustedIndex, maxIndex));
        }
        return entries;
    }


    @Override
    public long size() {
       return journal.size();
    }

    @Override
    public boolean isPresent(long logEntryIndex) {
        if (logEntryIndex > lastIndex()) {
            // if the request logical index is less than the last present in the list
            return false;
        }
        int adjustedIndex = adjustedIndex(logEntryIndex);
        return (adjustedIndex >= 0);
    }

    @Override
    public boolean isInSnapshot(long logEntryIndex) {
        return logEntryIndex <= snapshotIndex && snapshotIndex != -1;
    }

    @Override
    public ByteString getSnapshot() {
        return snapshot;
    }

    @Override
    public long getSnapshotIndex() {
        return snapshotIndex;
    }

    @Override
    public long getSnapshotTerm() {
        return snapshotTerm;
    }

    @Override
    public abstract void appendAndPersist(ReplicatedLogEntry replicatedLogEntry);

    @Override
    public abstract void removeFromAndPersist(long index);

    @Override
    public void setSnapshotIndex(long snapshotIndex) {
        this.snapshotIndex = snapshotIndex;
    }

    @Override
    public void setSnapshotTerm(long snapshotTerm) {
        this.snapshotTerm = snapshotTerm;
    }

    @Override
    public void setSnapshot(ByteString snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void clear(int startIndex, int endIndex) {
        journal.subList(startIndex, endIndex).clear();
    }

    @Override
    public void snapshotPreCommit(ByteString snapshot, long snapshotCapturedIndex, long snapshotCapturedTerm) {
        snapshottedJournal = new ArrayList<>(journal.size());

        snapshottedJournal.addAll(journal.subList(0, (int)(snapshotCapturedIndex - snapshotIndex)));
        clear(0, (int) (snapshotCapturedIndex - snapshotIndex));

        previousSnapshotIndex = snapshotIndex;
        setSnapshotIndex(snapshotCapturedIndex);

        previousSnapshotTerm = snapshotTerm;
        setSnapshotTerm(snapshotCapturedTerm);

        previousSnapshot = getSnapshot();
        setSnapshot(snapshot);
    }

    @Override
    public void snapshotCommit() {
        snapshottedJournal.clear();
        snapshottedJournal = null;
        previousSnapshotIndex = -1;
        previousSnapshotTerm = -1;
        previousSnapshot = null;
    }

    @Override
    public void snapshotRollback() {
        snapshottedJournal.addAll(journal);
        journal.clear();
        journal = snapshottedJournal;
        snapshottedJournal = null;

        snapshotIndex = previousSnapshotIndex;
        previousSnapshotIndex = -1;

        snapshotTerm = previousSnapshotTerm;
        previousSnapshotTerm = -1;

        snapshot = previousSnapshot;
        previousSnapshot = null;

    }
}
