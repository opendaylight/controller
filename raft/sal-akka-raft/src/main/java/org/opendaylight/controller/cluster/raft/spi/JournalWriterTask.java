/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

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
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.raft.spi.AveragingProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public final class JournalWriterTask implements Runnable {

    private sealed interface Action {
        // Nothing else
    }

    private static final class TerminateAction implements Action {
        static final TerminateAction INSTANCE = new TerminateAction();
    }

    private sealed interface JournalAction<T> extends Action {

        long enqueued();

        RaftCallback<T> callback();
    }

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

    private static final Logger LOG = LoggerFactory.getLogger(JournalWriterTask.class);

    private final AtomicReference<@Nullable CancellationException> aborted = new AtomicReference<>();
    private final ExecuteInSelfActor actor;
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

    public JournalWriterTask(final ExecuteInSelfActor actor, final EntryJournalV1 journal, final int queueCapacity) {
        this(Ticker.systemTicker(), actor, journal, queueCapacity);
    }

    @VisibleForTesting
    public JournalWriterTask(final Ticker ticker, final ExecuteInSelfActor actor, final EntryJournalV1 journal,
            final int queueCapacity) {
        this.ticker = requireNonNull(ticker);
        this.actor = requireNonNull(actor);
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
        return journal.memberId();
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
        // Interject into processing ...
        terminate();
        // ... defer to processAndTerminate ...
        final var toClose = processAndTerminate();
        // .. and ensure noone is accessing the journal before closing it
        journalLock.lock();
        try {
            toClose.close();
        } finally {
            journalLock.unlock();
        }
    }

    public EntryJournal processAndTerminate() {
        queueLock.lock();
        try {
            enqueue(TerminateAction.INSTANCE);
        } finally {
            queueLock.unlock();
        }
        return journal;
    }

    // Called with queueLock held
    private void enqueue(final Action action) {
        final var signalNotEmpty = queue.isEmpty();
        queue.addLast(action);
        if (signalNotEmpty) {
            // There is always at most one waiter
            notEmpty.signal();
        }
    }

    private void enqueueAndWait(final JournalAction<?> action) throws InterruptedException {
        final long delay;
        queueLock.lock();
        try {
            enqueue(action);
            delay = tracker.openTask(action.enqueued());
        } finally {
            queueLock.unlock();
        }

        // FIXME: add delay capping

        TimeUnit.NANOSECONDS.sleep(delay);
    }

    @Override
    public void run() {
        LOG.debug("{}: task {} started", memberId(), this);

        // Reused between loops
        final var batch = new ArrayDeque<Action>();
        final var sw = Stopwatch.createStarted(ticker);

        boolean keepRunning;
        do {
            // Attempt to drain all elements first
            queueLock.lock();
            try {
                while (true) {
                    if (!queue.isEmpty()) {
                        batch.addAll(queue);
                        queue.clear();
                        break;
                    }

                    LOG.debug("{}: waiting for ", memberId());
                    try {
                        notEmpty.await();
                    } catch (InterruptedException e) {
                        // Should never happen, really
                        throw new IllegalStateException("interrupted while waiting, waiting for next command", e);
                    }
                }
            } finally {
                queueLock.unlock();
            }

            final var batchSize = batch.size();
            LOG.debug("{}: received {} entries in {}", memberId(), batchSize, sw.stop());
            sw.reset().start();

            keepRunning = runBatch(batch);

            LOG.debug("{}: persisted {} entries in {}", memberId(), batchSize, sw.stop());
            sw.reset();
        } while (keepRunning);

        LOG.debug("{}: task {} stopped", memberId(), this);
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
                                    completions.add(() -> appendEntry.callback.invoke(null, journalIndex));
                                }
                                case JournalDiscardHead discardHead -> journal.discardHead(discardHead.journalIndex);
                                case JournalDiscardTail discardTail -> journal.discardTail(discardTail.journalIndex);
                                case JournalSetApplyTo setApplyTo -> journal.setApplyTo(setApplyTo.journalIndex);
                            }
                        } catch (IOException e) {
                            completions.add(abortAndFailAction(action, e));
                        }

                        closedTasks.add(new ClosedTask(action.enqueued(), ticker.read() - execStarted));
                    }
                    case TerminateAction action -> {
                        terminate();
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

        // Invoke callbacks as needed
        if (!completions.isEmpty()) {
            actor.executeInSelf(() -> completions.forEach(Runnable::run));
        }

        return keepRunning;
    }

    /**
     * Terminates all further processing with immediate effect a side-effect of this task being terminated.
     */
    private void terminate() {
        abort(new CancellationException("No further operations allowed"));
    }

    private void abort(final CancellationException cause) {
        aborted.compareAndSet(null, requireNonNull(cause));
    }

    private Runnable abortAndFailAction(final JournalAction<?> action, final Exception cause) {
        abort(new CancellationException("Previous operation failed"));
        return failAction(action, cause);
    }

    private static Runnable failAction(final JournalAction<?> action, final Exception cause) {
        final var cb = action.callback();
        return () -> cb.invoke(cause, null);
    }
}