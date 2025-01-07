/*
 * Copyright (c) 2019, 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import com.codahale.metrics.Histogram;
import com.google.common.base.VerifyException;
import io.atomix.storage.journal.JournalReader;
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.JournalWriter;
import io.atomix.storage.journal.SegmentedByteBufJournal;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.persistence.PersistentRepr;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.ReplayMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WriteMessages;
import org.opendaylight.controller.akka.segjournal.SegmentedJournalActor.WrittenMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.jdk.javaapi.CollectionConverters;

/**
 * Version 0 data journal, where every journal entry maps to exactly one segmented file entry.
 */
final class DataJournalV0 extends DataJournal {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);

    private final SegmentedJournal<DataJournalEntry> entries;

    DataJournalV0(final String persistenceId, final Histogram messageSize, final ActorSystem system,
            final StorageLevel storage, final Path directory, final int maxEntrySize, final int maxSegmentSize) {
        super(persistenceId, messageSize);

        final var serdes = JournalSerdes.builder()
            .register(new DataJournalEntrySerdes(system), FromPersistence.class, ToPersistence.class)
            .build();

        entries = new SegmentedJournal<>(SegmentedByteBufJournal.builder()
            .withDirectory(directory)
            .withName("data")
            .withStorageLevel(storage)
            .withMaxEntrySize(maxEntrySize)
            .withMaxSegmentSize(maxSegmentSize)
            .build(), serdes.toReadMapper(), serdes.toWriteMapper());
    }

    @Override
    long lastWrittenSequenceNr() {
        return entries.lastIndex();
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
        flush();
        entries.close();
    }

    @Override
    void flush() {
        entries.writer().flush();
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    void handleReplayMessages(final ReplayMessages message, final long fromSequenceNr) {
        try (var reader = entries.openReader(fromSequenceNr)) {
            handleReplayMessages(reader, message);
        } catch (Exception e) {
            LOG.warn("{}: failed to replay messages for {}", persistenceId, message, e);
            message.promise.failure(e);
        } finally {
            message.promise.success(null);
        }
    }

    private void handleReplayMessages(final JournalReader<DataJournalEntry> reader, final ReplayMessages message) {
        int count = 0;
        while (count < message.max && reader.getNextIndex() <= message.toSequenceNr) {
            final var repr = reader.tryNext((index, entry, size) -> {
                LOG.trace("{}: replay index={} entry={}", persistenceId, index, entry);
                updateLargestSize(size);
                if (entry instanceof FromPersistence fromPersistence) {
                    return fromPersistence.toRepr(persistenceId, index);
                }
                throw new VerifyException("Unexpected entry " + entry);
            });

            if (repr == null) {
                break;
            }

            LOG.debug("{}: replaying {}", persistenceId, repr);
            message.replayCallback.accept(repr);
            count++;
        }
        LOG.debug("{}: successfully replayed {} entries", persistenceId, count);
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    WrittenMessages handleWriteMessages(final WriteMessages message) {
        final int count = message.size();
        final var responses = new ArrayList<>();
        final var writer = entries.writer();
        long writtenBytes = 0;

        for (int i = 0; i < count; ++i) {
            final long prevNextIndex = writer.getNextIndex();
            final var request = message.getRequest(i);

            final var reprs = CollectionConverters.asJava(request.payload());
            LOG.trace("{}: append {}/{}: {} items at mark {}", persistenceId, i, count, reprs.size(), prevNextIndex);
            try {
                writtenBytes += writePayload(writer, reprs);
            } catch (Exception e) {
                LOG.warn("{}: failed to write out request {}/{} reverting to {}", persistenceId, i, count,
                    prevNextIndex, e);
                responses.add(e);
                writer.reset(prevNextIndex);
                continue;
            }
            responses.add(null);
        }

        return new WrittenMessages(message, responses, writtenBytes);
    }

    private long writePayload(final JournalWriter<DataJournalEntry> writer, final List<PersistentRepr> reprs) {
        long bytes = 0;
        for (var repr : reprs) {
            final Object payload = repr.payload();
            if (!(payload instanceof Serializable)) {
                throw new UnsupportedOperationException("Non-serializable payload encountered "
                        + payload.getClass());
            }

            LOG.trace("{}: starting append of {}", persistenceId, payload);
            final var entry = writer.append(new ToPersistence(repr));
            final int size = entry.size();
            LOG.trace("{}: finished append of {} with {} bytes at {}", persistenceId, payload, size, entry.index());
            recordMessageSize(size);
            bytes += size;
        }
        return bytes;
    }
}
