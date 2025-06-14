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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
final class JournalWriterTask implements Runnable {

    private sealed interface Action {
        // Nothing else
    }

    private static final class TerminateAction implements Action {
        static final TerminateAction INSTANCE = new TerminateAction();
    }

    private sealed interface JournalAction<T> extends Action {

        RaftCallback<T> callback();
    }

    private record JournalAppendEntry(LogEntry entry, RaftCallback<Long> callback) implements JournalAction<Long> {
        JournalAppendEntry {
            requireNonNull(entry);
            requireNonNull(callback);
        }
    }

    private record JournalSetApplyTo(long journalIndex) implements AbortingActorAction {
        // Nothing else
    }

    private record JournalDiscardHead(long journalIndex) implements AbortingActorAction {
        // Nothing else
    }

    private record JournalDiscardTail(long journalIndex) implements AbortingActorAction {
        // Nothing else
    }

    private sealed interface AbortingActorAction extends JournalAction<Void> {
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

    private static final Logger LOG = LoggerFactory.getLogger(JournalWriterTask.class);

    private final AtomicReference<@Nullable CancellationException> aborted = new AtomicReference<>();
    private final ArrayBlockingQueue<Action> queue;
    private final ExecuteInSelfActor actor;
    private final EntryJournalV1 journal;
    private final Stopwatch sw;

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
        this.actor = requireNonNull(actor);
        this.journal = requireNonNull(journal);
        queue = new ArrayBlockingQueue<>(queueCapacity);
        sw = Stopwatch.createUnstarted(ticker);
    }

    private String memberId() {
        return journal.memberId();
    }

    public void signalTerminate() throws InterruptedException {
        queue.put(TerminateAction.INSTANCE);
    }

    public void appendEntry(final LogEntry entry, final RaftCallback<Long> callback) throws InterruptedException {
        queue.put(new JournalAppendEntry(entry, callback));
    }

    @Override
    public void run() {
        LOG.debug("{}: task {} started", memberId(), this);

        // Reused between loops
        final var batch = new ArrayDeque<Action>();

        boolean keepRunning;
        do {
            sw.start();

            // Attempt to drain all elements first
            queue.drainTo(batch);
            if (batch.isEmpty()) {
                // Nothing to do: wait for some work to show up
                LOG.debug("{}: waiting for ", memberId());
                final Action first;
                try {
                    first = queue.take();
                } catch (InterruptedException e) {
                    // Should never happen, really
                    throw new IllegalStateException("interrupted while waiting, waiting for next command", e);
                }

                // We have an entry, let's process it
                batch.add(first);
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
        final var completions = new ArrayList<Runnable>();
        boolean keepRunning = true;

        // FIXME: not quite, as we want to perform intra-batch things:
        //        - multiple JournalApplyTo's should be squashed into a single one, where effective
        //        - a JournalApplyTo bump is implied with JournalDiscardHead, so there we can get a free update
        //        - JournalAppendEntries should be cancelled with subsequent JournalDiscardTail
        //        - a TerminateAction should cancel all remaining entries
        // FIXME: separately, the batch should have a mass 'flush()' operation

        // Not iteration as we want to free entries as soon as possible
        for (var nextAction = actions.poll(); nextAction != null; nextAction = actions.poll()) {
            switch (nextAction) {
                case JournalAction<?> action -> {
                    // propagate cancellation if set
                    final var cancellation = aborted.get();
                    if (cancellation != null) {
                        completions.add(failAction(action, cancellation));
                        continue;
                    }

                    try {
                        switch (action) {
                            case JournalAppendEntry(var entry, var callback) -> {
                                final long journalIndex = journal.appendEntry(entry);
                                completions.add(() -> callback.invoke(null, journalIndex));
                            }
                            case JournalDiscardHead(var journalIndex) -> journal.discardHead(journalIndex);
                            case JournalDiscardTail(var journalIndex) -> journal.discardTail(journalIndex);
                            case JournalSetApplyTo(var journalIndex) -> journal.setApplyTo(journalIndex);
                        }
                    } catch (IOException e) {
                        completions.add(abortAndFailAction(action, e));
                    }
                }
                case TerminateAction action -> {
                    aborted.compareAndSet(null, new CancellationException("No further operations allowed"));
                    keepRunning = false;
                }
            }
        }

        if (!completions.isEmpty()) {
            actor.executeInSelf(() -> completions.forEach(Runnable::run));
        }

        return keepRunning;
    }

    private void abort() {
        aborted.compareAndSet(null, new CancellationException("Previous operation failed"));
    }

    private Runnable abortAndFailAction(final JournalAction<?> action, final Exception cause) {
        abort();
        return failAction(action, cause);
    }

    private static Runnable failAction(final JournalAction<?> action, final Exception cause) {
        final var cb = action.callback();
        return () -> cb.invoke(cause, null);
    }
}