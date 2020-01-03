/*
 * Copyright (c) 2019 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.akka.segjournal;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.persistence.AtomicWrite;
import akka.persistence.PersistentRepr;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.MoreObjects;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.SegmentedJournalReader;
import io.atomix.storage.journal.SegmentedJournalWriter;
import io.atomix.utils.serializer.Namespace;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.FromPersistence;
import org.opendaylight.controller.akka.segjournal.DataJournalEntry.ToPersistence;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.Iterator;
import scala.collection.SeqLike;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * This actor handles a single PersistentActor's journal. The journal is split into two {@link SegmentedJournal}s:
 * <ul>
 *     <li>A memory-mapped data journal, containing actual data entries</li>
 *     <li>A simple file journal, containing sequence numbers of last deleted entry</li>
 * </ul>
 *
 * <p>
 * This is a conscious design decision to minimize the amount of data that is being stored in the data journal while
 * speeding up normal operations. Since the SegmentedJournal is an append-only linear log and Akka requires the ability
 * to delete persistence entries, we need ability to mark a subset of a SegmentedJournal as deleted. While we could
 * treat such delete requests as normal events, this leads to a mismatch between SegmentedJournal indices (as exposed by
 * {@link Indexed}) and Akka sequence numbers -- requiring us to potentially perform costly deserialization to find the
 * index corresponding to a particular sequence number, or maintain moderately-complex logic and data structures to
 * perform that mapping in sub-linear time complexity.
 *
 * <p>
 * Split-file approach allows us to treat sequence numbers and indices as equivalent, without maintaining any explicit
 * mapping information. The only additional information we need to maintain is the last deleted sequence number.
 *
 * @author Robert Varga
 */
final class SegmentedJournalActor extends AbstractActor {
    abstract static class AsyncMessage<T> {
        final Promise<T> promise = Promise.apply();
    }

    private static final class ReadHighestSequenceNr extends AsyncMessage<Long> {
        private final long fromSequenceNr;

        ReadHighestSequenceNr(final long fromSequenceNr) {
            this.fromSequenceNr = fromSequenceNr;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("fromSequenceNr", fromSequenceNr).toString();
        }
    }

    private static final class ReplayMessages extends AsyncMessage<Void> {
        private final long fromSequenceNr;
        private final long toSequenceNr;
        private final long max;
        private final Consumer<PersistentRepr> replayCallback;

        ReplayMessages(final long fromSequenceNr,
                final long toSequenceNr, final long max, final Consumer<PersistentRepr> replayCallback) {
            this.fromSequenceNr = fromSequenceNr;
            this.toSequenceNr = toSequenceNr;
            this.max = max;
            this.replayCallback = requireNonNull(replayCallback);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("fromSequenceNr", fromSequenceNr)
                    .add("toSequenceNr", toSequenceNr).add("max", max).toString();
        }
    }

    static final class WriteMessages {
        private final List<AtomicWrite> requests = new ArrayList<>();
        private final List<Promise<Optional<Exception>>> results = new ArrayList<>();

        Future<Optional<Exception>> add(final AtomicWrite write) {
            final Promise<Optional<Exception>> promise = Promise.apply();
            requests.add(write);
            results.add(promise);
            return promise.future();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("requests", requests).toString();
        }
    }

    private static final class DeleteMessagesTo extends AsyncMessage<Void> {
        final long toSequenceNr;

        DeleteMessagesTo(final long toSequenceNr) {
            this.toSequenceNr = toSequenceNr;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("toSequenceNr", toSequenceNr).toString();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SegmentedJournalActor.class);
    private static final Namespace DELETE_NAMESPACE = Namespace.builder().register(Long.class).build();
    private static final int DELETE_SEGMENT_SIZE = 64 * 1024;

    private final String persistenceId;
    private final StorageLevel storage;
    private final int maxSegmentSize;
    private final int maxEntrySize;
    private final File directory;

    // Tracks the time it took us to write a batch of messages
    private Timer batchWriteTime;
    // Tracks the number of individual messages written
    private Meter messageWriteCount;
    // Tracks the size distribution of messages
    private Histogram messageSize;

    private SegmentedJournal<DataJournalEntry> dataJournal;
    private SegmentedJournal<Long> deleteJournal;
    private long lastDelete;

    // Tracks largest message size we have observed either during recovery or during write
    private int largestObservedSize;

    SegmentedJournalActor(final String persistenceId, final File directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        this.persistenceId = requireNonNull(persistenceId);
        this.directory = requireNonNull(directory);
        this.storage = requireNonNull(storage);
        this.maxEntrySize = maxEntrySize;
        this.maxSegmentSize = maxSegmentSize;
    }

    static Props props(final String persistenceId, final File directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        return Props.create(SegmentedJournalActor.class, requireNonNull(persistenceId), directory, storage,
            maxEntrySize, maxSegmentSize);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeleteMessagesTo.class, this::handleDeleteMessagesTo)
                .match(ReadHighestSequenceNr.class, this::handleReadHighestSequenceNr)
                .match(ReplayMessages.class, this::handleReplayMessages)
                .match(WriteMessages.class, this::handleWriteMessages)
                .matchAny(this::handleUnknown)
                .build();
    }

    @Override
    public void preStart() throws Exception {
        LOG.debug("{}: actor starting", persistenceId);
        super.preStart();

        final MetricRegistry registry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        final String actorName = self().path().parent().toStringWithoutAddress() + '/' + directory.getName();

        batchWriteTime = registry.timer(MetricRegistry.name(actorName, "batchWriteTime"));
        messageWriteCount = registry.meter(MetricRegistry.name(actorName, "messageWriteCount"));
        messageSize = registry.histogram(MetricRegistry.name(actorName, "messageSize"));
    }

    @Override
    public void postStop() throws Exception {
        LOG.debug("{}: actor stopping", persistenceId);
        if (dataJournal != null) {
            dataJournal.close();
            LOG.debug("{}: data journal closed", persistenceId);
            dataJournal = null;
        }
        if (deleteJournal != null) {
            deleteJournal.close();
            LOG.debug("{}: delete journal closed", persistenceId);
            deleteJournal = null;
        }
        LOG.debug("{}: actor stopped", persistenceId);
        super.postStop();
    }

    static AsyncMessage<Void> deleteMessagesTo(final long toSequenceNr) {
        return new DeleteMessagesTo(toSequenceNr);
    }

    static AsyncMessage<Long> readHighestSequenceNr(final long fromSequenceNr) {
        return new ReadHighestSequenceNr(fromSequenceNr);
    }

    static AsyncMessage<Void> replayMessages(final long fromSequenceNr, final long toSequenceNr, final long max,
            final Consumer<PersistentRepr> replayCallback) {
        return new ReplayMessages(fromSequenceNr, toSequenceNr, max, replayCallback);
    }

    private void handleDeleteMessagesTo(final DeleteMessagesTo message) {
        ensureOpen();

        LOG.debug("{}: delete messages {}", persistenceId, message);
        final long to = Long.min(dataJournal.writer().getLastIndex(), message.toSequenceNr);
        LOG.debug("{}: adjusted delete to {}", persistenceId, to);

        if (lastDelete < to) {
            LOG.debug("{}: deleting entries up to {}", persistenceId, to);

            lastDelete = to;
            final SegmentedJournalWriter<Long> deleteWriter = deleteJournal.writer();
            final Indexed<Long> entry = deleteWriter.append(lastDelete);
            deleteWriter.commit(entry.index());
            dataJournal.writer().commit(lastDelete);

            LOG.debug("{}: compaction started", persistenceId);
            dataJournal.compact(lastDelete + 1);
            deleteJournal.compact(entry.index());
            LOG.debug("{}: compaction finished", persistenceId);
        } else {
            LOG.debug("{}: entries up to {} already deleted", persistenceId, lastDelete);
        }

        message.promise.success(null);
    }

    private void handleReadHighestSequenceNr(final ReadHighestSequenceNr message) {
        LOG.debug("{}: looking for highest sequence on {}", persistenceId, message);
        final Long sequence;
        if (directory.isDirectory()) {
            ensureOpen();
            sequence = dataJournal.writer().getLastIndex();
        } else {
            sequence = 0L;
        }

        LOG.debug("{}: highest sequence is {}", message, sequence);
        message.promise.success(sequence);
    }

    @SuppressWarnings("checkstyle:illegalCatch")
    private void handleReplayMessages(final ReplayMessages message) {
        LOG.debug("{}: replaying messages {}", persistenceId, message);
        ensureOpen();

        final long from = Long.max(lastDelete + 1, message.fromSequenceNr);
        LOG.debug("{}: adjusted fromSequenceNr to {}", persistenceId, from);

        try (SegmentedJournalReader<DataJournalEntry> reader = dataJournal.openReader(from)) {
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

    @SuppressWarnings("checkstyle:illegalCatch")
    private void handleWriteMessages(final WriteMessages message) {
        ensureOpen();

        final SegmentedJournalWriter<DataJournalEntry> writer = dataJournal.writer();
        final long startTicks = System.nanoTime();
        final int count = message.requests.size();
        final long start = writer.getLastIndex();

        for (int i = 0; i < count; ++i) {
            final long mark = writer.getLastIndex();
            try {
                writeRequest(writer, message.requests.get(i));
            } catch (Exception e) {
                LOG.warn("{}: failed to write out request", persistenceId, e);
                message.results.get(i).success(Optional.of(e));
                writer.truncate(mark);
                continue;
            }

            message.results.get(i).success(Optional.empty());
        }
        writer.flush();
        batchWriteTime.update(System.nanoTime() - startTicks, TimeUnit.NANOSECONDS);
        messageWriteCount.mark(writer.getLastIndex() - start);
    }

    private void writeRequest(final SegmentedJournalWriter<DataJournalEntry> writer, final AtomicWrite request) {
        // Cast is needed for Eclipse because of https://bugs.eclipse.org/bugs/show_bug.cgi?id=468276
        final Iterator<PersistentRepr> it = ((SeqLike<PersistentRepr, ?>) request.payload()).iterator();
        while (it.hasNext()) {
            final PersistentRepr repr = it.next();
            final Object payload = repr.payload();
            if (!(payload instanceof Serializable)) {
                throw new UnsupportedOperationException("Non-serializable payload encountered " + payload.getClass());
            }

            final int size = writer.append(new ToPersistence(repr)).size();
            messageSize.update(size);
            updateLargestSize(size);
        }
    }

    private void handleUnknown(final Object message) {
        LOG.error("{}: Received unknown message {}", persistenceId, message);
    }

    private void updateLargestSize(final int size) {
        if (size > largestObservedSize) {
            largestObservedSize = size;
        }
    }

    private void ensureOpen() {
        if (dataJournal != null) {
            verifyNotNull(deleteJournal);
            return;
        }

        deleteJournal = SegmentedJournal.<Long>builder().withDirectory(directory).withName("delete")
                .withNamespace(DELETE_NAMESPACE).withMaxSegmentSize(DELETE_SEGMENT_SIZE).build();
        final Indexed<Long> lastEntry = deleteJournal.writer().getLastEntry();
        lastDelete = lastEntry == null ? 0 : lastEntry.entry();

        dataJournal = SegmentedJournal.<DataJournalEntry>builder()
                .withStorageLevel(storage).withDirectory(directory).withName("data")
                .withNamespace(Namespace.builder()
                    .register(new DataJournalEntrySerializer(context().system()),
                        FromPersistence.class, ToPersistence.class)
                    .build())
                .withMaxEntrySize(maxEntrySize).withMaxSegmentSize(maxSegmentSize)
                .build();
        final SegmentedJournalWriter<DataJournalEntry> writer = dataJournal.writer();
        writer.commit(lastDelete);
        LOG.debug("{}: journal open with last index {}, deleted to {}", persistenceId, writer.getLastIndex(),
            lastDelete);
    }
}
