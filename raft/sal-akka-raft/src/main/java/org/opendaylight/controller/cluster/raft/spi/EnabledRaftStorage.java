/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RaftStorage} backing persistent mode of {@link RaftActor} operation.
 */
public final class EnabledRaftStorage extends RaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(EnabledRaftStorage.class);
    private static final AtomicLong WRITER_COUNTER = new AtomicLong();

    private final boolean mapped;

    // FIXME: we should have a queue push timeout, similar to Pekko circuit breaker to deal with queue waits
    private JournalWriteTask task;
    private Thread thread;

    @NonNullByDefault
    public EnabledRaftStorage(final RaftStorageCompleter completer, final Path directory,
            final CompressionType compression, final Configuration streamConfig, final boolean mapped) {
        super(completer, directory, compression, streamConfig);
        this.mapped = mapped;
    }

    /**
     * {@return the underlying EntryJournal}
     */
    @NonNullByDefault
    public EntryJournal journal() {
        return task.journal();
    }

    @Override
    @NonNullByDefault
    public void startPersistEntry(final ReplicatedLogEntry entry, final RaftCallback<Long> callback) {
        try {
            task.appendEntry(entry, callback);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to start persist", e);
        }
    }

    @Override
    public void discardHead(final long firstRetainedIndex) {
        try {
            task.discardHead(firstRetainedIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete head entries", e);
        }
    }

    @Override
    public void discardTail(final long firstRemovedIndex) {
        try {
            task.syncDiscardTail(firstRemovedIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete tail entries", e);
        }
    }

    @Override
    public void checkpointLastApplied(final long commitJournalIndex) {
        try {
            task.setApplyTo(commitJournalIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to update last applied index", e);
        }
    }


    // TODO: at least
    //   - creates the directory if not present
    //   - creates a 'lock' file and java.nio.channels.FileChannel.tryLock()s it
    //   - scans the directory to:
    //     - clean up any temporary files
    //     - determine nextSequence

    @Override
    protected void postStart() throws IOException {
        final var journal = new EntryJournalV1(memberId(), directory, compression, mapped);
        LOG.info("{}: journal open: applyTo={}", memberId(), journal.applyToJournalIndex());
        task = new JournalWriteTask(completer(), journal, 2048);
        thread = Thread.ofVirtual().name(memberId() + "-writer-" + WRITER_COUNTER.incrementAndGet()).start(task);
    }

    // FIXME:  and more: more things:
    //   - terminates any incomplete operations, reporting a CancellationException to them
    //   - unlocks the file
    // - stop() that:
    // - a lava.lang.ref.Clearner which does the same as stop()
    // For scalability this locking should really be done on top-level stateDir so we have one file open for all shards,
    // not one file per shard.

    @Override
    protected void preStop() {
        LOG.debug("{}: terminating thread {}", memberId(), thread);
        var journal = task.processAndTerminate();
        try {
            thread.join();
        } catch (InterruptedException e) {
            LOG.warn("{}: interrupted while waiting for writer to complete, forcing cancellation", memberId(), e);
            thread.interrupt();
            task.cancelAndTerminate();
            return;
        } finally {
            task = null;
            thread = null;
        }

        journal.close();
        LOG.info("{}: journal closed", memberId());
    }
}
