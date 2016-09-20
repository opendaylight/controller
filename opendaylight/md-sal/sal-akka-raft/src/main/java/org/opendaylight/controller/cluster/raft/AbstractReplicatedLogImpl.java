/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class handling the mapping of
 * logical LogEntry Index and the physical list index.
 */
public abstract class AbstractReplicatedLogImpl implements ReplicatedLog {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractReplicatedLogImpl.class);

    private final String logContext;

    // We define this as ArrayList so we can use ensureCapacity.
    private ArrayList<ReplicatedLogEntry> journal;

    private long snapshotIndex = -1;
    private long snapshotTerm = -1;

    // to be used for rollback during save snapshot failure
    private ArrayList<ReplicatedLogEntry> snapshottedJournal;
    private long previousSnapshotIndex = -1;
    private long previousSnapshotTerm = -1;
    private int dataSize = 0;

    protected AbstractReplicatedLogImpl(long snapshotIndex, long snapshotTerm, List<ReplicatedLogEntry> unAppliedEntries,
            String logContext) {
        this.snapshotIndex = snapshotIndex;
        this.snapshotTerm = snapshotTerm;
        this.logContext = logContext;

        this.journal = new ArrayList<>(unAppliedEntries.size());
        for(ReplicatedLogEntry entry: unAppliedEntries) {
            append(entry);
        }
    }

    protected AbstractReplicatedLogImpl() {
        this(-1L, -1L, Collections.<ReplicatedLogEntry>emptyList(), "");
    }

    protected int adjustedIndex(long logEntryIndex) {
        if (snapshotIndex < 0) {
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
    public long removeFrom(long logEntryIndex) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        if (adjustedIndex < 0 || adjustedIndex >= journal.size()) {
            // physical index should be less than list size and >= 0
            return -1;
        }

        for(int i = adjustedIndex; i < journal.size(); i++) {
            dataSize -= journal.get(i).size();
        }

        journal.subList(adjustedIndex , journal.size()).clear();

        return adjustedIndex;
    }

    @Override
    public boolean append(ReplicatedLogEntry replicatedLogEntry) {
        if(replicatedLogEntry.getIndex() > lastIndex()) {
            journal.add(replicatedLogEntry);
            dataSize += replicatedLogEntry.size();
            return true;
        } else {
            LOG.warn("{}: Cannot append new entry - new index {} is not greater than the last index {}",
                    logContext, replicatedLogEntry.getIndex(), lastIndex(), new Exception("stack trace"));
            return false;
        }
    }

    @Override
    public void increaseJournalLogCapacity(int amount) {
        journal.ensureCapacity(journal.size() + amount);
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(long logEntryIndex) {
        return getFrom(logEntryIndex, journal.size(), NO_MAX_SIZE);
    }

    @Override
    public List<ReplicatedLogEntry> getFrom(long logEntryIndex, int maxEntries, long maxDataSize) {
        int adjustedIndex = adjustedIndex(logEntryIndex);
        int size = journal.size();
        if (adjustedIndex >= 0 && adjustedIndex < size) {
            // physical index should be less than list size and >= 0
            int maxIndex = adjustedIndex + maxEntries;
            if(maxIndex > size){
                maxIndex = size;
            }

            if(maxDataSize == NO_MAX_SIZE) {
                return new ArrayList<>(journal.subList(adjustedIndex, maxIndex));
            } else {
                return copyJournalEntries(adjustedIndex, maxIndex, maxDataSize);
            }
        } else {
            return Collections.emptyList();
        }
    }

    private List<ReplicatedLogEntry> copyJournalEntries(int fromIndex, int toIndex, long maxDataSize) {
        List<ReplicatedLogEntry> retList = new ArrayList<>(toIndex - fromIndex);
        long totalSize = 0;
        for(int i = fromIndex; i < toIndex; i++) {
            ReplicatedLogEntry entry = journal.get(i);
            totalSize += entry.size();
            if(totalSize <= maxDataSize) {
                retList.add(entry);
            } else {
                if(retList.isEmpty()) {
                    // Edge case - the first entry's size exceeds the threshold. We need to return
                    // at least the first entry so add it here.
                    retList.add(entry);
                }

                break;
            }
        }

        return retList;
    }

    @Override
    public long size() {
       return journal.size();
    }

    @Override
    public int dataSize() {
        return dataSize;
    }

    @Override
    public boolean isPresent(long logEntryIndex) {
        if (logEntryIndex > lastIndex()) {
            // if the request logical index is less than the last present in the list
            return false;
        }
        int adjustedIndex = adjustedIndex(logEntryIndex);
        return adjustedIndex >= 0;
    }

    @Override
    public boolean isInSnapshot(long logEntryIndex) {
        return logEntryIndex <= snapshotIndex && snapshotIndex != -1;
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
    public void setSnapshotIndex(long snapshotIndex) {
        this.snapshotIndex = snapshotIndex;
    }

    @Override
    public void setSnapshotTerm(long snapshotTerm) {
        this.snapshotTerm = snapshotTerm;
    }

    @Override
    public void clear(int startIndex, int endIndex) {
        journal.subList(startIndex, endIndex).clear();
    }

    @Override
    public void snapshotPreCommit(long snapshotCapturedIndex, long snapshotCapturedTerm) {
        Preconditions.checkArgument(snapshotCapturedIndex >= snapshotIndex,
                "snapshotCapturedIndex must be greater than or equal to snapshotIndex");

        snapshottedJournal = new ArrayList<>(journal.size());

        List<ReplicatedLogEntry> snapshotJournalEntries = journal.subList(0, (int) (snapshotCapturedIndex - snapshotIndex));

        snapshottedJournal.addAll(snapshotJournalEntries);
        snapshotJournalEntries.clear();

        previousSnapshotIndex = snapshotIndex;
        setSnapshotIndex(snapshotCapturedIndex);

        previousSnapshotTerm = snapshotTerm;
        setSnapshotTerm(snapshotCapturedTerm);
    }

    @Override
    public void snapshotCommit() {
        snapshottedJournal = null;
        previousSnapshotIndex = -1;
        previousSnapshotTerm = -1;
        dataSize = 0;
        // need to recalc the datasize based on the entries left after precommit.
        for(ReplicatedLogEntry logEntry : journal) {
            dataSize += logEntry.size();
        }

    }

    @Override
    public void snapshotRollback() {
        snapshottedJournal.addAll(journal);
        journal = snapshottedJournal;
        snapshottedJournal = null;

        snapshotIndex = previousSnapshotIndex;
        previousSnapshotIndex = -1;

        snapshotTerm = previousSnapshotTerm;
        previousSnapshotTerm = -1;
    }

    @VisibleForTesting
    ReplicatedLogEntry getAtPhysicalIndex(int index) {
        return journal.get(index);
    }
}
