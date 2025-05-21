/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalAction;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalAppendEntry;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalApplyTo;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalReplayFrom;
import org.opendaylight.controller.cluster.raft.PekkoRaftStorage.JournalReset;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
final class JournalWriterTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(JournalWriterTask.class);

    private final BlockingQueue<JournalAction<?>> queue;
    private final ExecuteInSelfActor actor;

    final EntryJournalV1 journal;

    JournalWriterTask(final ExecuteInSelfActor actor, final EntryJournalV1 journal,
            final BlockingQueue<JournalAction<?>> queue) {
        this.actor = requireNonNull(actor);
        this.journal = requireNonNull(journal);
        this.queue = requireNonNull(queue);
    }

    private String memberId() {
        return journal.memberId();
    }

    @Override
    public void run() {
        LOG.debug("{}: started task {}", memberId(), this);

        final var batch = new ArrayDeque<JournalAction<?>>();
        final var sw = Stopwatch.createStarted();
        final var aborted = new AtomicReference<@Nullable CancellationException>();

        while (true) {
            // Attempt to drain all elements first
            queue.drainTo(batch);
            if (batch.isEmpty()) {
                // Nothing to do: wait for some work to show up
                LOG.debug("{}: waiting for ", memberId());
                final JournalAction<?> first;
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
            LOG.debug("{}: received {} entries in {}", memberId(), batchSize, sw);
            sw.reset();

            for (var action = batch.poll(); action != null; action = batch.poll()) {
                final var ioFailure = aborted.get();
                if (ioFailure != null) {
                    final var cb = action.callback();
                    actor.executeInSelf(() -> cb.invoke(ioFailure, null));
                    return;
                }

                final Runnable completion = switch (action) {
                    case JournalAppendEntry(var entry, var callback) -> {
                        final long journalIndex;
                        try {
                            journalIndex = journal.persistEntry(entry);
                        } catch (IOException e) {
                            aborted.set(new CancellationException("Previous operation failed"));
                            yield () -> callback.invoke(e, null);
                        }
                        yield () -> callback.invoke(null, journalIndex);
                    }
                    case JournalReset(var nextIndex) -> {
                        try {
                            journal.resetTo(nextIndex);
                            yield null;
                        } catch (IOException e) {
                            yield () -> {
                                throw new UncheckedIOException(e);
                            };
                        }
                    }
                    case JournalApplyTo(var lastApplied) -> {
                        try {
                            journal.setApplyTo(lastApplied);
                            yield null;
                        } catch (IOException e) {
                            yield () -> {
                                throw new UncheckedIOException(e);
                            };
                        }
                    }
                    case JournalReplayFrom(var fromIndex) -> {
                        try {
                            journal.setReplayFrom(fromIndex);
                            yield null;
                        } catch (IOException e) {
                            yield (Runnable) () -> {
                                throw new UncheckedIOException(e);
                            };
                        }
                    }
                };

                if (completion != null) {
                    actor.executeInSelf(completion);
                }
            }

            LOG.debug("{}: persisted {} entries in {}", memberId(), batchSize, sw);
            sw.reset();
        }
    }
}