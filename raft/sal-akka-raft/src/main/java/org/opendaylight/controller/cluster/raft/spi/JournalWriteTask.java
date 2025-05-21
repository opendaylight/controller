/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.base.Ticker;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.spi.AveragingProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public final class JournalWriteTask implements Runnable {
    /**
     * A queue entry. e.g. an action the task needs to take.
     */
    private sealed interface Action {
        // Nothing else
    }

    /**
     * An action on the journal, completing a {@link RaftCallback}.
     *
     * @param <T> result type
     */
    private sealed interface JournalAction<T> extends Action {
        /**
         * {@return the timer ticks when this action was enqueued}
         */
        long enqueued();

        /**
         * {@return the callback to complete}
         */
        RaftCallback<T> callback();
    }

    /**
     * Common utility causing an unchecked exception to be thrown on failure.
     */
    private sealed interface UncheckedJournalAction extends JournalAction<Void> {
        @Override
        default RaftCallback<Void> callback() {
            return (failure, success) -> {
                switch (failure) {
                    case null -> {
                        // No-op
                    }
                    case IOException ex -> throw new UncheckedIOException(ex);
                    default -> {
                        Throwables.throwIfUnchecked(failure);
                        throw new IllegalStateException("Unexpected failure", failure);
                    }
                }
            };
        }
    }

    private record JournalAppendEntry(long enqueued, LogEntry entry, RaftCallback<Long> callback)
            implements JournalAction<Long> {
        JournalAppendEntry {
            requireNonNull(entry);
            requireNonNull(callback);
        }
    }

    private record JournalDiscardHead(long enqueued, long journalIndex) implements UncheckedJournalAction {
        // Nothing else
    }

    private record JournalDiscardTail(long enqueued, long journalIndex) implements UncheckedJournalAction {
        // Nothing else
    }

    private record JournalSetApplyTo(long enqueued, long journalIndex) implements UncheckedJournalAction {
        // Nothing else
    }

    private record ClosedTask(long enqueuedTicks, long execNanos) {
        // Nothing else
    }

    /**
     * Terminate the task.
     *
     * @param cause termination cause
     */
    private record TerminateAction(CancellationException cause) implements Action {
        TerminateAction {
            requireNonNull(cause);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(JournalWriteTask.class);

    private final AtomicReference<@Nullable CancellationException> aborted = new AtomicReference<>();
    private final EntryStoreCompleter completer;
    private final Ticker ticker;

    // Journal and its locking
    private final ReentrantLock journalLock = new ReentrantLock();
    private final EntryJournalV1 journal;

    // Incoming queue and its locking/accounding
    private final ReentrantLock queueLock = new ReentrantLock();
    private final Condition notEmpty = queueLock.newCondition();
    private final ArrayDeque<Action> queue = new ArrayDeque<>();
    private final AveragingProgressTracker tracker;

    // TODO: also maintain the following metrics to provide SegmentedJournalActor parity
    //    // Tracks the time it took us to write a batch of messages
    //    private Timer batchWriteTime;
    //    // Tracks the number of individual messages written
    //    private Meter messageWriteCount;
    //    // Tracks the size distribution of messages
    //    private Histogram messageSize;
    //    // Tracks the number of messages completed for each flush
    //    private Histogram flushMessages;
    //    // Tracks the number of bytes completed for each flush
    //    private Histogram flushBytes;
    //    // Tracks the duration of flush operations
    //    private Timer flushTime;

    public JournalWriteTask(final EntryStoreCompleter completer, final EntryJournalV1 journal,
            final int queueCapacity) {
        this(Ticker.systemTicker(), completer, journal, queueCapacity);
    }

    @VisibleForTesting
    public JournalWriteTask(final Ticker ticker, final EntryStoreCompleter completer, final EntryJournalV1 journal,
            final int queueCapacity) {
        this.ticker = requireNonNull(ticker);
        this.completer = requireNonNull(completer);
        this.journal = requireNonNull(journal);
        tracker = new AveragingProgressTracker(queueCapacity);

        // TODO: the equivalent of:
        //        final var registry = MetricsReporter.getInstance(MeteringBehavior.DOMAIN).getMetricsRegistry();
        //        final var actorName = self().path().parent().toStringWithoutAddress() + '/' + directory.getFileName();
        //
        //        batchWriteTime = registry.timer(MetricRegistry.name(actorName, "batchWriteTime"));
        //        messageWriteCount = registry.meter(MetricRegistry.name(actorName, "messageWriteCount"));
        //        messageSize = registry.histogram(MetricRegistry.name(actorName, "messageSize"));
        //        flushBytes = registry.histogram(MetricRegistry.name(actorName, "flushBytes"));
        //        flushMessages = registry.histogram(MetricRegistry.name(actorName, "flushMessages"));
        //        flushTime = registry.timer(MetricRegistry.name(actorName, "flushTime"));
    }

    private String memberId() {
        return completer.memberId();
    }

    @Beta
    public EntryJournal journal() {
        return journal;
    }

    public void appendEntry(final LogEntry entry, final RaftCallback<Long> callback) throws InterruptedException {
        enqueueAndWait(new JournalAppendEntry(ticker.read(), entry, callback));
    }

    public void discardHead(final long journalIndex) throws InterruptedException {
        enqueueAndWait(new JournalDiscardHead(ticker.read(), journalIndex));
    }

    public void discardTail(final long journalIndex) throws InterruptedException {
        enqueueAndWait(new JournalDiscardTail(ticker.read(), journalIndex));
    }

    public void setApplyTo(final long journalIndex) throws InterruptedException {
        enqueueAndWait(new JournalSetApplyTo(ticker.read(), journalIndex));
    }

    public void cancelAndTerminate() {
        final var cause = new CancellationException("Abrupt termination");
        // Interject into processing ...
        abort(cause);
        // .. take the lock and enqueue ...
        enqueueTermination(cause);
        // .. and ensure noone is accessing the journal before closing it
        journalLock.lock();
        try {
            journal.close();
        } finally {
            journalLock.unlock();
        }
    }

    public EntryJournal processAndTerminate() {
        enqueueTermination(new CancellationException("Graceful termination"));
        return journal;
    }

    // Called with queueLock held
    private void lockedEnqueue(final Action action) {
        final var signalNotEmpty = queue.isEmpty();
        queue.addLast(action);
        if (signalNotEmpty) {
            // There is always at most one waiter
            notEmpty.signal();
        }
    }

    private void enqueue(final Action action) {
        queueLock.lock();
        try {
            lockedEnqueue(action);
        } finally {
            queueLock.unlock();
        }
    }

    private void enqueueAndWait(final JournalAction<?> action) throws InterruptedException {
        final long delay;
        queueLock.lock();
        try {
            lockedEnqueue(action);
            delay = tracker.openTask(action.enqueued());
        } finally {
            queueLock.unlock();
        }

        if (delay != 0) {
            // FIXME: add delay capping
            LOG.trace("{}: applying backpressure of {}ns", memberId(), delay);
            TimeUnit.NANOSECONDS.sleep(delay);
        }
    }

    private void enqueueTermination(final CancellationException cause) {
        enqueue(new TerminateAction(cause));
    }

    @Override
    public void run() {
        LOG.debug("{}: journal writer started", memberId());

        // Reused between loops
        final var batch = new ArrayDeque<Action>();

        boolean keepRunning;
        do {
            // Attempt to drain all elements first
            final int batchSize = fillBatch(batch);
            final var sw = Stopwatch.createStarted(ticker);
            keepRunning = runBatch(batch);
            LOG.debug("{}: completed {} commands in {}", memberId(), batchSize, sw.stop());
        } while (keepRunning);

        LOG.debug("{}: journal writer stopped", memberId());
    }

    private int fillBatch(final ArrayDeque<Action> batch) {
        final var sw = Stopwatch.createStarted(ticker);

        final int batchSize;
        queueLock.lock();
        try {
            batchSize = lockedFillBatch(batch);
        } catch (InterruptedException e) {
            // Should never happen, really. If it does, we just pretend we got a terminate command and let it play out.
            LOG.error("{}: interrupted while waiting to receive commands, terminating", e);
            Thread.currentThread().interrupt();
            batch.add(new TerminateAction(newCancellationWithCause("Thread interrupted", e)));
            return 1;
        } finally {
            queueLock.unlock();
        }

        LOG.debug("{}: received {} commands after {}", memberId(), batchSize, sw.stop());
        return batchSize;
    }

    // Called with with ququeLock held
    private int lockedFillBatch(final ArrayDeque<Action> batch) throws InterruptedException {
        int queueSize = queue.size();
        if (queueSize == 0) {
            LOG.debug("{}: waiting to receive commands", memberId());
            do {
                notEmpty.await();
            } while ((queueSize = queue.size()) == 0);
        }

        batch.addAll(queue);
        queue.clear();
        return queueSize;
    }

    @VisibleForTesting
    boolean runBatch(final Queue<Action> actions) {
        final var transmitTicks = ticker.read();
        final var closedTasks = new ArrayList<ClosedTask>(actions.size());
        final var completions = new ArrayList<Runnable>();
        boolean keepRunning = true;

        // FIXME: not quite, as we want to perform intra-batch things:
        //        - multiple JournalApplyTo's should be squashed into a single one, where effective
        //        - a JournalApplyTo bump is implied with JournalDiscardHead, so there we can get a free update
        //        - JournalAppendEntries should be cancelled with subsequent JournalDiscardTail
        //        - a TerminateAction should cancel all remaining entries
        // FIXME: separately, the batch should have a mass 'flush()' operation

        journalLock.lock();
        try {
            // Not iteration as we want to free entries as soon as possible
            for (var nextAction = actions.poll(); nextAction != null; nextAction = actions.poll()) {
                switch (nextAction) {
                    case JournalAction<?> action -> {
                        // propagate cancellation if set
                        final var cancellation = aborted.get();
                        if (cancellation != null) {
                            completions.add(failAction(action, cancellation));
                            closedTasks.add(new ClosedTask(action.enqueued(), 0));
                            continue;
                        }

                        final var execStarted = ticker.read();
                        try {
                            switch (action) {
                                case JournalAppendEntry appendEntry -> {
                                    final long journalIndex = journal.nextToWrite();
                                    // FIXME: record returned size to messageSize, which really is
                                    // 'serialized command size'
                                    journal.appendEntry(appendEntry.entry);

                                    // Do not retain entry for callback purposes
                                    final var cb = appendEntry.callback;
                                    completions.add(() -> cb.invoke(null, journalIndex));
                                }
                                case JournalDiscardHead discardHead -> journal.discardHead(discardHead.journalIndex);
                                case JournalDiscardTail discardTail -> journal.discardTail(discardTail.journalIndex);
                                case JournalSetApplyTo setApplyTo ->
                                    journal.setApplyToJournalIndex(setApplyTo.journalIndex);
                            }
                        } catch (IOException e) {
                            completions.add(abortAndFailAction(action, e));
                        }

                        closedTasks.add(new ClosedTask(action.enqueued(), ticker.read() - execStarted));
                    }
                    case TerminateAction(var cause) -> {
                        abort(cause);
                        keepRunning = false;
                    }
                }
            }
        } finally {
            journalLock.unlock();
        }

        // Update tasks statistics: note we do a bulk update under lock
        final var finished = ticker.read();
        queueLock.lock();
        try {
            for (var closed : closedTasks) {
                tracker.closeTask(finished, closed.enqueuedTicks, transmitTicks, closed.execNanos);
            }
        } finally {
            queueLock.unlock();
        }

        // Enqueue completions
        completer.enqueueCompletions(completions);

        return keepRunning;
    }

    private void abort(final CancellationException cause) {
        aborted.compareAndSet(null, requireNonNull(cause));
    }

    private Runnable abortAndFailAction(final JournalAction<?> action, final Exception cause) {
        abort(newCancellationWithCause("Previous action failed", cause));
        return failAction(action, cause);
    }

    private static Runnable failAction(final JournalAction<?> action, final Exception cause) {
        final var cb = action.callback();
        return () -> cb.invoke(cause, null);
    }

    private static CancellationException newCancellationWithCause(final String message, final Exception cause) {
        final var ret = new CancellationException(message);
        ret.initCause(cause);
        return ret;
    }
}
