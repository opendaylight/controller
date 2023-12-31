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
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import java.io.File;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WrittenMessages;

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
        final boolean oldVersion = isOldVersion(directory);

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

        final var journalV1 = new DataJournalV1(persistenceId, messageSize, entries, uuids);
        if (oldVersion) {
            journalV1.upgradeFromV0();
        }

        return journalV1;
    }

    private static boolean isOldVersion(File directory) {
        final var dataCount = directory.list((dir, name) -> dir.equals(directory) && name.startsWith("data"));
        final var uuidsCount = directory.list((dir, name) -> dir.equals(directory) && name.startsWith("uuids"));
        return dataCount != null && dataCount.length > 0 && uuidsCount != null && uuidsCount.length == 0;
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
     * @param message {@link WriteMessages} message
     * @return a {@link WrittenMessages} object
     */
    abstract @NonNull WrittenMessages handleWriteMessages(@NonNull WriteMessages message);

    /**
     * Flush all messages to durable storage.
     */
    abstract void flush();
}
