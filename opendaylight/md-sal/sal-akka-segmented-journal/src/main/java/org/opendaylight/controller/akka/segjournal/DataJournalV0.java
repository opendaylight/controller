/*
 * Copyright (c) 2019, 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import io.atomix.storage.journal.JournalReader;
import io.atomix.storage.journal.JournalWriter;
import io.atomix.storage.journal.SegmentedJournal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
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
sealed class DataJournalV0 extends DataJournal permits DataJournalV1 {
    private static final Logger LOG = LoggerFactory.getLogger(DataJournalV0.class);

    private final SegmentedJournal<DataJournalEntry> entries;

    DataJournalV0(final String persistenceId, final Histogram messageSize,
            final SegmentedJournal<DataJournalEntry> entries) {
        super(persistenceId, messageSize);
        this.entries = requireNonNull(entries);
    }

    JournalReader<DataJournalEntry> entriesReader(final long index) {
        return entries.openReader(index);
    }

    @Override
    final long lastWrittenSequenceNr() {
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
        flush();
        entries.close();
    }

    @Override
    void flush() {
        entries.writer().flush();
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    final void handleReplayMessages(final ReplayMessages message, final long fromSequenceNr) {
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
                    return toRepr(fromPersistence, index);
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

    PersistentRepr toRepr(final FromPersistence entry, final long sequenceNr) {
        return entry.toRepr(persistenceId, sequenceNr);
    }

    @Override
    @SuppressWarnings("checkstyle:illegalCatch")
    final WrittenMessages handleWriteMessages(final WriteMessages message) {
        final int count = message.size();
        final var responses = new ArrayList<>();
        final var writer = entries.writer();
        long writtenBytes = 0;

        for (int i = 0; i < count; ++i) {
            final long mark = writer.getLastIndex();
            final var request = message.getRequest(i);

            final var reprs = CollectionConverters.asJava(request.payload());
            LOG.trace("{}: append {}/{}: {} items at mark {}", persistenceId, i, count, reprs.size(), mark);
            try {
                writtenBytes += writePayload(writer, reprs);
            } catch (Exception e) {
                LOG.warn("{}: failed to write out request {}/{} reverting to {}", persistenceId, i, count, mark, e);
                responses.add(e);
                writer.truncate(mark);
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
            final var entry = writerRepr(writer, repr);
            final int size = entry.size();
            LOG.trace("{}: finished append of {} with {} bytes at {}", persistenceId, payload, size, entry.index());
            recordMessageSize(size);
            bytes += size;
        }
        return bytes;
    }

    Indexed<@NonNull ToPersistence> writerRepr(final JournalWriter<DataJournalEntry> writer,
            final PersistentRepr repr) {
        return writer.append(new ToPersistence(repr.manifest(), repr.writerUuid(), repr.payload()));
    }
}
