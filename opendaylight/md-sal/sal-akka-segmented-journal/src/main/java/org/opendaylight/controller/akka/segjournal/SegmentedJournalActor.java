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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.JournalSerdes;
import io.atomix.storage.journal.SegmentedByteBufJournal;
import io.atomix.storage.journal.SegmentedJournal;
import io.atomix.storage.journal.StorageLevel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import org.apache.pekko.persistence.AtomicWrite;
import org.apache.pekko.persistence.PersistentRepr;
import org.opendaylight.controller.cluster.common.actor.MeteringBehavior;
import org.opendaylight.controller.cluster.reporting.MetricsReporter;
import org.opendaylight.controller.raft.journal.FromByteBufMapper;
import org.opendaylight.controller.raft.journal.ToByteBufMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * This actor handles a single PersistentActor's journal. The journal is split into two {@link SegmentedJournal}s:
 * <ul>
 *     <li>A memory-mapped data journal, containing actual data entries</li>
 *     <li>A simple file journal, containing sequence numbers of last deleted entry</li>
 * </ul>
 *
 * <p>This is a conscious design decision to minimize the amount of data that is being stored in the data journal while
 * speeding up normal operations. Since the SegmentedJournal is an append-only linear log and Pekko requires the ability
 * to delete persistence entries, we need ability to mark a subset of a SegmentedJournal as deleted. While we could
 * treat such delete requests as normal events, this leads to a mismatch between SegmentedJournal indices (as exposed by
 * {@link Indexed}) and Pekko sequence numbers -- requiring us to potentially perform costly deserialization to find the
 * index corresponding to a particular sequence number, or maintain moderately-complex logic and data structures to
 * perform that mapping in sub-linear time complexity.
 *
 * <p>Split-file approach allows us to treat sequence numbers and indices as equivalent, without maintaining any
 * explicit mapping information. The only additional information we need to maintain is the last deleted sequence
 * number.
 */
abstract sealed class SegmentedJournalActor extends AbstractActor {
    abstract static sealed class AsyncMessage<T> {
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

    static final class ReplayMessages extends AsyncMessage<Void> {
        private final long fromSequenceNr;
        final long toSequenceNr;
        final long max;
        final Consumer<PersistentRepr> replayCallback;

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
            final var promise = Promise.<Optional<Exception>>apply();
            requests.add(write);
            results.add(promise);
            return promise.future();
        }

        int size() {
            return requests.size();
        }

        AtomicWrite getRequest(final int index) {
            return requests.get(index);
        }

        void setFailure(final int index, final Exception cause) {
            results.get(index).success(Optional.of(cause));
        }

        void setSuccess(final int index) {
            results.get(index).success(Optional.empty());
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

    // responses == null on success, Exception on failure
    record WrittenMessages(WriteMessages message, List<Object> responses, long writtenBytes) {
        WrittenMessages {
            verify(responses.size() == message.size(), "Mismatched %s and %s", message, responses);
            verify(writtenBytes >= 0, "Unexpected length %s", writtenBytes);
        }

        private void complete() {
            for (int i = 0, size = responses.size(); i < size; ++i) {
                if (responses.get(i) instanceof Exception ex) {
                    message.setFailure(i, ex);
                } else {
                    message.setSuccess(i);
                }
            }
        }
    }

    /**
     * A {@link SegmentedJournalActor} which delays issuing a flush operation until a watermark is reached or when the
     * queue is empty.
     *
     * <p>The problem we are addressing is that there is a queue sitting in from of the actor, which we have no direct
     * access to. Since a flush involves committing data to durable storage, that operation can easily end up dominating
     * workloads.
     *
     * <p>We solve this by having an additional queue in which we track which messages were written and trigger a flush
     * only when the number of bytes we have written exceeds specified limit. The other part is that each time this
     * queue becomes non-empty, we send a dedicated message to self. This acts as a actor queue probe -- when we receive
     * it, we know we have processed all messages that were in the queue when we first delayed the write.
     *
     * <p>The combination of these mechanisms ensure we use a minimal delay while also ensuring we take advantage of
     * batching opportunities.
     */
    private static final class Delayed extends SegmentedJournalActor {
        private static final class Flush extends AsyncMessage<Void> {
            final long batch;

            Flush(final long batch) {
                this.batch = batch;
            }
        }

        private record UnflushedWrite(WrittenMessages message, Stopwatch start, long count) {
            UnflushedWrite {
                requireNonNull(message);
                requireNonNull(start);
            }
        }

        private final ArrayDeque<UnflushedWrite> unflushedWrites = new ArrayDeque<>();
        private final Stopwatch unflushedDuration = Stopwatch.createUnstarted();
        private final long maxUnflushedBytes;

        private long batch = 0;
        private long unflushedBytes = 0;

        Delayed(final String persistenceId, final Path directory, final StorageLevel storage,
                final int maxEntrySize, final int maxSegmentSize, final int maxUnflushedBytes) {
            super(persistenceId, directory, storage, maxEntrySize, maxSegmentSize);
            this.maxUnflushedBytes = maxUnflushedBytes;
        }

        @Override
        ReceiveBuilder addMessages(final ReceiveBuilder builder) {
            return super.addMessages(builder).match(Flush.class, this::handleFlush);
        }

        private void handleFlush(final Flush message) {
            if (message.batch == batch) {
                flushWrites();
            } else {
                LOG.debug("{}: batch {} not flushed by {}", persistenceId(), batch, message.batch);
            }
        }

        @Override
        void onWrittenMessages(final WrittenMessages message, final Stopwatch started, final long count) {
            boolean first = unflushedWrites.isEmpty();
            if (first) {
                unflushedDuration.start();
            }
            unflushedWrites.addLast(new UnflushedWrite(message, started, count));
            unflushedBytes = unflushedBytes + message.writtenBytes;
            if (unflushedBytes >= maxUnflushedBytes) {
                LOG.debug("{}: reached {} unflushed journal bytes", persistenceId(), unflushedBytes);
                flushWrites();
            } else if (first) {
                LOG.debug("{}: deferring journal flush", persistenceId());
                self().tell(new Flush(++batch), ActorRef.noSender());
            }
        }

        @Override
        void flushWrites() {
            final var unsyncedSize = unflushedWrites.size();
            if (unsyncedSize == 0) {
                // Nothing to flush
                return;
            }

            LOG.debug("{}: flushing {} journal writes after {}", persistenceId(), unsyncedSize,
                unflushedDuration.stop());
            flushJournal(unflushedBytes, unsyncedSize);

            final var sw = Stopwatch.createStarted();
            unflushedWrites.forEach(write -> completeWriteMessages(write.message, write.start, write.count));
            unflushedWrites.clear();
            unflushedBytes = 0;
            unflushedDuration.reset();
            LOG.debug("{}: completed {} flushed journal writes in {}", persistenceId(), unsyncedSize, sw);
        }
    }

    private static final class Immediate extends SegmentedJournalActor {
        Immediate(final String persistenceId, final Path directory, final StorageLevel storage,
                final int maxEntrySize, final int maxSegmentSize) {
            super(persistenceId, directory, storage, maxEntrySize, maxSegmentSize);
        }

        @Override
        void onWrittenMessages(final WrittenMessages message, final Stopwatch started, final long count) {
            flushJournal(message.writtenBytes, 1);
            completeWriteMessages(message, started, count);
        }

        @Override
        void flushWrites() {
            // No-op
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SegmentedJournalActor.class);
    private static final int DELETE_SEGMENT_SIZE = 64 * 1024;
    private static final FromByteBufMapper<Long> READ_MAPPER;
    private static final ToByteBufMapper<Long> WRITE_MAPPER;

    static {
        final var namespace = JournalSerdes.builder()
            .register(LongEntrySerdes.LONG_ENTRY_SERDES, Long.class)
            .build();

        READ_MAPPER = namespace.toReadMapper();
        WRITE_MAPPER = namespace.toWriteMapper();
    }

    private final String persistenceId;
    private final StorageLevel storage;
    private final int maxSegmentSize;
    private final int maxEntrySize;
    private final Path directory;

    // Tracks the time it took us to write a batch of messages
    private Timer batchWriteTime;
    // Tracks the number of individual messages written
    private Meter messageWriteCount;
    // Tracks the size distribution of messages
    private Histogram messageSize;
    // Tracks the number of messages completed for each flush
    private Histogram flushMessages;
    // Tracks the number of bytes completed for each flush
    private Histogram flushBytes;
    // Tracks the duration of flush operations
    private Timer flushTime;

    private DataJournal dataJournal;
    private SegmentedJournal<Long> deleteJournal;
    private long lastDelete;

    private SegmentedJournalActor(final String persistenceId, final Path directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize) {
        this.persistenceId = requireNonNull(persistenceId);
        this.directory = requireNonNull(directory);
        this.storage = requireNonNull(storage);
        this.maxEntrySize = maxEntrySize;
        this.maxSegmentSize = maxSegmentSize;
    }

    static Props props(final String persistenceId, final Path directory, final StorageLevel storage,
            final int maxEntrySize, final int maxSegmentSize, final int maxUnflushedBytes) {
        final var pid = requireNonNull(persistenceId);
        return maxUnflushedBytes > 0
            ? Props.create(Delayed.class, pid, directory, storage, maxEntrySize, maxSegmentSize, maxUnflushedBytes)
            : Props.create(Immediate.class, pid, directory, storage, maxEntrySize, maxSegmentSize);
    }

    final String persistenceId() {
        return persistenceId;
    }

    final void flushJournal(final long bytes, final int messages) {
        final var sw = Stopwatch.createStarted();
        dataJournal.flush();
        LOG.debug("{}: journal flush completed in {}", persistenceId, sw.stop());
        flushBytes.update(bytes);
        flushMessages.update(messages);
        flushTime.update(sw.elapsed(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

    @Override
    public Receive createReceive() {
        return addMessages(receiveBuilder())
            .matchAny(this::handleUnknown)
            .build();
    }

    ReceiveBuilder addMessages(final ReceiveBuilder builder) {
        return builder
            .match(DeleteMessagesTo.class, this::handleDeleteMessagesTo)
            .match(ReadHighestSequenceNr.class, this::handleReadHighestSequenceNr)
            .match(ReplayMessages.class, this::handleReplayMessages)
            .match(WriteMessages.class, this::handleWriteMessages);
    }

    @Override
    public void preStart() throws Exception {
        LOG.debug("{}: actor starting", persistenceId);
        super.preStart();

        final var registry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        final var actorName = self().path().parent().toStringWithoutAddress() + '/' + directory.getFileName();

        batchWriteTime = registry.timer(MetricRegistry.name(actorName, "batchWriteTime"));
        messageWriteCount = registry.meter(MetricRegistry.name(actorName, "messageWriteCount"));
        messageSize = registry.histogram(MetricRegistry.name(actorName, "messageSize"));
        flushBytes = registry.histogram(MetricRegistry.name(actorName, "flushBytes"));
        flushMessages = registry.histogram(MetricRegistry.name(actorName, "flushMessages"));
        flushTime = registry.timer(MetricRegistry.name(actorName, "flushTime"));
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
        flushWrites();

        final long to = Long.min(dataJournal.lastWrittenSequenceNr(), message.toSequenceNr);
        LOG.debug("{}: adjusted delete to {}", persistenceId, to);

        if (lastDelete < to) {
            LOG.debug("{}: deleting entries up to {}", persistenceId, to);

            lastDelete = to;
            final var deleteWriter = deleteJournal.writer();
            final var entry = deleteWriter.append(lastDelete);
            deleteWriter.commit(entry.index());
            dataJournal.deleteTo(lastDelete);

            LOG.debug("{}: compaction started", persistenceId);
            dataJournal.compactTo(lastDelete);
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
        if (Files.isDirectory(directory)) {
            ensureOpen();
            flushWrites();
            sequence = dataJournal.lastWrittenSequenceNr();
        } else {
            sequence = 0L;
        }

        LOG.debug("{}: highest sequence is {}", message, sequence);
        message.promise.success(sequence);
    }

    private void handleReplayMessages(final ReplayMessages message) {
        LOG.debug("{}: replaying messages {}", persistenceId, message);
        ensureOpen();
        flushWrites();

        final long from = Long.max(lastDelete + 1, message.fromSequenceNr);
        LOG.debug("{}: adjusted fromSequenceNr to {}", persistenceId, from);

        dataJournal.handleReplayMessages(message, from);
    }

    private void handleWriteMessages(final WriteMessages message) {
        ensureOpen();

        final var started = Stopwatch.createStarted();
        final long start = dataJournal.lastWrittenSequenceNr();
        final var writtenMessages = dataJournal.handleWriteMessages(message);

        onWrittenMessages(writtenMessages, started, dataJournal.lastWrittenSequenceNr() - start);
    }

    final void completeWriteMessages(final WrittenMessages message, final Stopwatch started, final long count) {
        batchWriteTime.update(started.stop().elapsed(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
        messageWriteCount.mark(count);
        // log message after statistics are updated
        LOG.debug("{}: write of {} bytes completed in {}", persistenceId, message.writtenBytes, started);
        message.complete();
    }

    /**
     * Handle a check of written messages.
     *
     * @param message Messages which were written
     * @param started Stopwatch started when the write started
     * @param count number of writes
     */
    abstract void onWrittenMessages(WrittenMessages message, Stopwatch started, long count);

    private void handleUnknown(final Object message) {
        LOG.error("{}: Received unknown message {}", persistenceId, message);
    }

    private void ensureOpen() {
        if (dataJournal != null) {
            verifyNotNull(deleteJournal);
            return;
        }

        final var sw = Stopwatch.createStarted();
        deleteJournal = new SegmentedJournal<>(SegmentedByteBufJournal.builder()
            .withDirectory(directory)
            .withName("delete")
            .withMaxSegmentSize(DELETE_SEGMENT_SIZE)
            .build(), READ_MAPPER, WRITE_MAPPER);
        final var lastDeleteRecovered = deleteJournal.openReader(deleteJournal.lastIndex())
            .tryNext((index, value, length) -> value);
        lastDelete = lastDeleteRecovered == null ? 0 : lastDeleteRecovered;

        dataJournal = new DataJournalV0(persistenceId, messageSize, context().system(), storage, directory,
            maxEntrySize, maxSegmentSize);
        dataJournal.deleteTo(lastDelete);
        LOG.debug("{}: journal open in {} with last index {}, deleted to {}", persistenceId, sw,
            dataJournal.lastWrittenSequenceNr(), lastDelete);
    }

    abstract void flushWrites();

}
