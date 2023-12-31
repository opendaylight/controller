/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static java.util.Objects.requireNonNull;
import static org.opendaylight.controller.akka.segjournal.UuidEntrySerdes.UUID_ENTRY_SERDES;

import akka.actor.ActorSystem;
import com.codahale.metrics.Histogram;
import com.google.common.base.VerifyException;
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import java.util.ArrayList;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;

/**
 * Abstraction of a data journal. This provides a unified interface towards {@link SegmentedJournalActor}, allowing
 * specialization for various formats.
 */
abstract sealed class DataJournal permits DataJournalV0 {
    // Mirrors fields from associated actor
    final @NonNull String persistenceId;
    private final Histogram messageSize;

    // Tracks largest message size we have observed either during recovery or during write
    private int largestObservedSize;

    DataJournal(final String persistenceId, final Histogram messageSize) {
        this.persistenceId = requireNonNull(persistenceId);
        this.messageSize = requireNonNull(messageSize);
    }

    static DataJournal create(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final File directory, final int maxEntrySize, final int maxSegmentSize,
            final int autoUpgrade) {
        // FIXME: consider what version the directory is (i.e. does it have uuids?) and if not, and autoUpgrade >= 1,
        //        only then upgrade to DataJournalV1. When migrating, the uuids needs to be recovered from the contents
        //        of data.

        final var dataCount = directory.list((dir, name) -> dir.equals(directory) && name.startsWith("data"));
        final var uuidsCount = directory.list((dir, name) -> dir.equals(directory) && name.startsWith("uuids"));

        final boolean oldVersion = dataCount != null && dataCount.length > 0 && uuidsCount != null
                && uuidsCount.length == 0;

        var entries = SegmentedJournal.<DataJournalEntry>builder()
            .withStorageLevel(storage).withDirectory(directory).withName("data")
            .withNamespace(JournalSerdes.builder()
                .register(new DataJournalEntrySerdes(system), FromPersistence.class, ToPersistence.class)
                .build())
            .withMaxEntrySize(maxEntrySize).withMaxSegmentSize(maxSegmentSize)
            .build();

        if (oldVersion && autoUpgrade < 1) {
            return new DataJournalV0(persistenceId, messageSize, entries);
        }

        final var uuids = SegmentedJournal.<UuidEntry>builder()
            .withStorageLevel(storage).withDirectory(directory).withName("uuids")
            .withNamespace(JournalSerdes.builder()
                .register(UUID_ENTRY_SERDES, UuidEntry.class, UuidEntry.class)
                .build())
            .withMaxEntrySize(maxEntrySize).withMaxSegmentSize(maxSegmentSize)
            .build();

        if (oldVersion) {
            final ArrayList<UuidEntry> writerUuids = new ArrayList<>();
            // upgrade

            try (var reader = entries.openReader(0)) {
                var indexed = reader.tryNext();
                writerUuids.add(new UuidEntry(((FromPersistence) indexed.entry())
                        .toRepr(persistenceId, indexed.index()).writerUuid(), indexed.index()));
                var lastUuid = writerUuids.get(0).writerUuid();
                for (indexed = reader.tryNext(); indexed != null; indexed = reader.tryNext()) {
                    if (indexed.entry() instanceof FromPersistence fromPersistence) {
                        //writerUuids.add(fromPersistence);
                        if (fromPersistence.toRepr(persistenceId, indexed.index()).writerUuid().equals(lastUuid)) {
                            continue;
                        }
                        writerUuids.add(new UuidEntry(((FromPersistence) indexed.entry())
                                .toRepr(persistenceId, indexed.index()).writerUuid(), indexed.index()));
                    } else {
                        throw new VerifyException("Unexpected entry " + indexed.entry());
                    }
                }
                for (var entry : writerUuids) {
                    final var writer = uuids.writer();
                    writer.append(entry);
                    writer.flush();
                }
            }
        }

        return new DataJournalV1(persistenceId, messageSize, entries, uuids);
    }

    final void recordMessageSize(final int size) {
        messageSize.update(size);
        updateLargestSize(size);
    }

    final void updateLargestSize(final int size) {
        if (size > largestObservedSize) {
            largestObservedSize = size;
        }
    }

    /**
     * Return the last sequence number completely written to the journal.
     *
     * @return Last written sequence number, {@code -1} if there are no in the journal.
     */
    abstract long lastWrittenSequenceNr();

    /**
     * Delete all messages up to specified sequence number.
     *
     * @param sequenceNr Sequence number to delete to.
     */
    abstract void deleteTo(long sequenceNr);

    /**
     * Delete all messages up to specified sequence number.
     *
     * @param sequenceNr Sequence number to compact to.
     */
    abstract void compactTo(long sequenceNr);

    /**
     * Close this journal, freeing up resources associated with it.
     */
    abstract void close();

    /**
     * Handle a request to replay messages.
     *
     * @param message Request message
     * @param fromSequenceNr Sequence number to replay from, adjusted for deletions
     */
    abstract void handleReplayMessages(@NonNull ReplayMessages message, long fromSequenceNr);

    /**
     * Handle a request to store some messages.
     *
     * @param message Request message
     * @return number of bytes written
     */
    abstract long handleWriteMessages(@NonNull WriteMessages message);
}
