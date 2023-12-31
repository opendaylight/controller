/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;

import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import com.google.common.base.VerifyException;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.JournalWriter;
import io.atomix.storage.journal.SegmentedJournal;
import java.util.ArrayList;
import java.util.Comparator;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Version 1 data journal: writerUuid is tracked in separate state.
 */
final class DataJournalV1 extends DataJournalV0 {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV1.class);

    // Currently-known writerUuids, in natural sequenceNr order
    private final ArrayList<UuidEntry> writerUuids = new ArrayList<>();
    private final SegmentedJournal<UuidEntry> uuids;

    DataJournalV1(final String persistenceId, final Histogram messageSize,
            final SegmentedJournal<DataJournalEntry> entries, final SegmentedJournal<UuidEntry> uuids) {
        super(persistenceId, messageSize, entries);
        this.uuids = requireNonNull(uuids);

        try (var reader = uuids.openReader(0)) {
            for (var indexed = reader.tryNext(); indexed != null; indexed = reader.tryNext()) {
                writerUuids.add(indexed.entry());
            }
        }
        if (!writerUuids.isEmpty()) {
            // Defensive sort
            writerUuids.sort(Comparator.comparing(UuidEntry::sequenceNr));
        }
    }

    @Override
    void deleteTo(final long sequenceNr) {
        super.deleteTo(sequenceNr);

        // Never delete the last entry
        final var lastEntry = uuids.writer().getLastEntry();
        if (lastEntry == null) {
            // nothing to delete
            return;
        } else if (lastEntry.entry().sequenceNr() <= sequenceNr) {
            // instead delete up to last entry
            deleteTo(lastEntry.entry().sequenceNr() - 1);
            return;
        }

        final var lastIndex = lastEntry.index();
        long commitIndex = -1;
        try (var reader = uuids.openReader(0)) {
            for (var entry = reader.tryNext(); entry != null; entry = reader.tryNext()) {
                final var index = entry.index();
                if (index >= lastIndex || entry.entry().sequenceNr() > sequenceNr) {
                    break;
                }
                commitIndex = index;
            }
        }

        if (commitIndex >= 0) {
            uuids.writer().commit(commitIndex);
        }
    }

    @Override
    void compactTo(final long sequenceNr) {
        super.compactTo(sequenceNr);

        // Never to the last entry
        final var lastEntry = uuids.writer().getLastEntry();
        if (lastEntry == null) {
            // nothing to delete
            return;
        } else if (lastEntry.entry().sequenceNr() <= sequenceNr) {
            // instead compact up to last entry
            compactTo(lastEntry.entry().sequenceNr() - 1);
            return;
        }

        final long compactIndex = findCompactIndex(sequenceNr, lastEntry.index());
        if (compactIndex >= 0) {
            uuids.compact(compactIndex + 1);
        }
    }

    // TODO: This is suspiciously similar to deleteTo() perhaps this could be a shared utility, where compactTo() would
    //       check the commitIndex? More thought is needed.
    private long findCompactIndex(final long sequenceNr, final long lastIndex) {
        long commitIndex = -1;

        try (var reader = uuids.openReader(0)) {
            for (var indexed = reader.tryNext(); indexed != null; indexed = reader.tryNext()) {
                final var index = indexed.index();
                final var seqNr = indexed.entry().sequenceNr();
                if (index >= lastIndex || seqNr > sequenceNr) {
                    // check if there is no gap in sequenceNr in this case we cannot delete this entry yet and thus
                    // can compact only to previous index
                    if (seqNr != sequenceNr + 1) {
                        commitIndex--;
                    }
                    break;
                }
                commitIndex = index;
            }
        }

        return commitIndex;
    }

    @Override
    PersistentRepr toRepr(final FromPersistence entry, final long sequenceNr) {
        final var it = writerUuids.listIterator(writerUuids.size());
        while (it.hasPrevious()) {
            final var uuid = it.previous();
            if (uuid.sequenceNr() <= sequenceNr) {
                return entry.toRepr(persistenceId, sequenceNr, uuid.writerUuid());
            }
        }
        throw new VerifyException("No writerUuid found for " + sequenceNr + " in " + writerUuids);
    }

    @Override
    Indexed<@NonNull ToPersistence> writerRepr(final JournalWriter<DataJournalEntry> writer,
            final PersistentRepr repr) {
        // First write the entry to get the sequence number
        final var ret = writer.append(new ToPersistence(repr.manifest(), null, repr.payload()));

        recordUuid(repr.writerUuid(), ret.index());

        return ret;
    }

    private void recordUuid(final String writerUuid, final long sequenceNr) {
        if (writerUuids.isEmpty() || !writerUuids.get(writerUuids.size() - 1).writerUuid().equals(writerUuid)) {
            final var newUuid = new UuidEntry(writerUuid, sequenceNr);
            LOG.debug("{}: discovered {}", persistenceId, newUuid);

            final var writer = uuids.writer();
            writer.append(newUuid);
            writer.flush();
            writerUuids.add(newUuid);
            LOG.debug("{}: recorded {}", persistenceId, newUuid);
        }
    }

    void upgradeFromV0() {
        try (var reader = entriesReader(0)) {
            for (var indexed = reader.tryNext(); indexed != null; indexed = reader.tryNext()) {
                if (indexed.entry() instanceof FromPersistence fromPersistence) {
                    recordUuid(fromPersistence.writerUuid(), indexed.index());
                } else {
                    throw new VerifyException("Unexpected entry " + indexed.entry());
                }
            }
        }
    }
}
