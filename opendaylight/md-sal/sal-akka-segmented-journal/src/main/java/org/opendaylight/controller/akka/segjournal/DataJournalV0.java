/*
 * Copyright (c) 2019, 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Verify.verify;

import akka.actor.ActorSystem;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.io.Serializable;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;

/**
 * Version 0 data journal, where every journal entry maps to exactly one segmented file entry.
 *
 * @author Robert Varga
 */
final class DataJournalV0 extends DataJournal {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);

    private final SegmentedJournal<DataJournalEntry> entries;

    DataJournalV0(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final File directory, final int maxEntrySize, final int maxSegmentSize) {
        super(persistenceId, messageSize);
        entries = SegmentedJournal.<DataJournalEntry>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("data")
                .withNamespace(Namespace.builder()
                    .register(new DataJournalEntrySerializer(system), FromPersistence.class, ToPersistence.class)
                    .build())
                .withMaxEntrySize(maxEntrySize).withMaxSegmentSize(maxSegmentSize)
                .build();
    }

    @Override
    long lastWrittenSequenceNr() {
        return entries.writer().getLastIndex();
    }

    @Override
    void deleteTo(final long sequenceNr) {
        entries.writer().commit(sequenceNr);
    }

    @Override
    void compactTo(final long sequenceNr) {
        entries.compact(sequenceNr + 1);
    }

    @Override
    void close() {
        entries.close();
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    void handleReplayMessages(final ReplayMessages message, final long fromSequenceNr) {
        try (SegmentedJournalReader<DataJournalEntry> reader = entries.openReader(fromSequenceNr)) {
            int count = 0;
            while (reader.hasNext() && count < message.max) {
                final Indexed<DataJournalEntry> next = reader.next();
                if (next.index() > message.toSequenceNr) {
                    break;
                }

                LOG.trace("{}: replay {}", persistenceId, next);
                updateLargestSize(next.size());
                final DataJournalEntry entry = next.entry();
                verify(entry instanceof FromPersistence, "Unexpected entry %s", entry);

                final PersistentRepr repr = ((FromPersistence) entry).toRepr(persistenceId, next.index());
                LOG.debug("{}: replaying {}", persistenceId, repr);
                message.replayCallback.accept(repr);
                count++;
            }
            LOG.debug("{}: successfully replayed {} entries", persistenceId, count);
        } catch (Exception e) {
            LOG.warn("{}: failed to replay messages for {}", persistenceId, message, e);
            message.promise.failure(e);
        } finally {
            message.promise.success(null);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    void handleWriteMessages(final WriteMessages message) {
        final int count = message.size();
        final SegmentedJournalWriter<DataJournalEntry> writer = entries.writer();

        for (int i = 0; i < count; ++i) {
            final long mark = writer.getLastIndex();
            final AtomicWrite request = message.getRequest(i);
            try {
                for (PersistentRepr repr : CollectionConverters.asJava(request.payload())) {
                    final Object payload = repr.payload();
                    if (!(payload instanceof Serializable)) {
                        throw new UnsupportedOperationException("Non-serializable payload encountered "
                                + payload.getClass());
                    }

                    recordMessageSize(writer.append(new ToPersistence(repr)).size());
                }
            } catch (Exception e) {
                LOG.warn("{}: failed to write out request", persistenceId, e);
                message.setFailure(i, e);
                writer.truncate(mark);
                continue;
            }

            message.setSuccess(i);
        }
        writer.flush();
    }
}
