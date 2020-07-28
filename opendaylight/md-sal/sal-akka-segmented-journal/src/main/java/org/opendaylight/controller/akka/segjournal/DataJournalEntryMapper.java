/*
 * Copyright (c) 2020 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import java.util.SortedMap;
import java.util.TreeMap;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromFragmentedPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataJournalEntryMapper {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalEntryMapper.class);

    private String persistenceId;
    private final SegmentedJournal<DataJournalEntry> dataJournal;
    private final SegmentedJournal<Long> deleteJournal;

    private SortedMap<Long, EntryPosition> sequenceNrToPositionMap = new TreeMap<>();

    DataJournalEntryMapper(final String persistenceId, final SegmentedJournal<DataJournalEntry> dataJournal,
        final SegmentedJournal<Long> deleteJournal) {
        this.persistenceId = requireNonNull(persistenceId);
        this.dataJournal = requireNonNull(dataJournal);
        this.deleteJournal = requireNonNull(deleteJournal);
    }

    /**
     * Return the {@link EntryPosition} mapped to the specified sequenceNr, if it exists. If the mapping does not exist
     * rebuild the map and search again.
     */
    @SuppressWarnings("checkstyle:illegalCatch")
    EntryPosition getPositionOfSequenceNr(final long sequenceNr) {
        // look in the map
        EntryPosition positionFromMapping = sequenceNrToPositionMap.get(sequenceNr);
        if (positionFromMapping != null) {
            return positionFromMapping;
        }

        // rebuild the map and try once again
        rebuildMap();
        return sequenceNrToPositionMap.get(sequenceNr);
    }

    /**
     * Read the relevant dataJournal (from lastDelete) and build the sequenceNr -> EntryPosition map accordingly.
     */
    @SuppressWarnings("checkstyle:illegalCatch")
    private void rebuildMap() {
        LOG.debug("Rebuilding the sequenceNr -> EntryPosition map");
        sequenceNrToPositionMap.clear();
        try (SegmentedJournalReader<DataJournalEntry> reader = dataJournal.openReader(getLastDelete() + 1)) {
            while (reader.hasNext()) {
                Indexed<DataJournalEntry> next = reader.next();
                DataJournalEntry entry = next.entry();
                if (entry instanceof FromFragmentedPersistence) {
                    FromFragmentedPersistence fragEntry = (FromFragmentedPersistence) entry;
                    if (fragEntry.getFragmentIndex() > 0) {
                        continue;
                    }
                }
                sequenceNrToPositionMap.put(entry.getSequenceNr(), EntryPosition.forEntry(next));
            }
        } catch (Exception e) {
            LOG.warn("{}: cannot read data journal - cannot rebuild map", persistenceId, e);
        }
    }

    private long getLastDelete() {
        final Indexed<Long> lastEntry = deleteJournal.writer().getLastEntry();
        return lastEntry != null ? lastEntry.entry() : 0L;
    }

    void updateMapping(final long sequenceNr, final EntryPosition newPosition) {
        requireNonNull(newPosition);
        sequenceNrToPositionMap.put(sequenceNr, newPosition);
    }

    /**
     * Remove all mapped entries with keys greater then toSequenceNr.
     */
    void truncate(final long toSequenceNr) {
        sequenceNrToPositionMap = new TreeMap<>(sequenceNrToPositionMap.headMap(toSequenceNr + 1));
    }

    long getHighestSequenceNr() {
        return !sequenceNrToPositionMap.isEmpty() ? sequenceNrToPositionMap.lastKey() : 0;
    }

    /**
     * Remove all entries with keys less then or equal to toSequenceNr.
     */
    void deleteUpToIncluding(final long toSequenceNr) {
        if (sequenceNrToPositionMap.isEmpty()) {
            return;
        }

        if (toSequenceNr >= sequenceNrToPositionMap.lastKey()) {
            sequenceNrToPositionMap.clear();
        } else {
            sequenceNrToPositionMap = new TreeMap<>(sequenceNrToPositionMap.tailMap(toSequenceNr + 1));
        }
    }
}
