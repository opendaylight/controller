/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.EntryJournalV1;
import org.opendaylight.controller.cluster.raft.spi.JournalWriteTask;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link EnabledRaftStorage} backed by Pekko Persistence of an {@link RaftActor}.
 */
// FIXME: remove this class once we have both Snapshots and Entries stored in files
final class PekkoRaftStorage extends EnabledRaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(PekkoRaftStorage.class);
    private static final AtomicLong WRITER_COUNTER = new AtomicLong();

    // FIXME: we should have a queue push timeout, similar to Pekko circuit breaker to deal with queue waits
    private JournalWriteTask task;
    private Thread thread;
    private final boolean mapped;

    @NonNullByDefault
    PekkoRaftStorage(final RaftStorageCompleter completer, final Path directory, final CompressionType compression,
            final Configuration streamConfig, final boolean mapped) {
        super(completer, directory, compression, streamConfig);
        this.mapped = mapped;
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

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        doPersistEntry(entry, completer().syncWithCurrentMessage(callback), true);
    }

    @Override
    @NonNullByDefault
    public void startPersistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        doPersistEntry(entry, callback, false);
    }

    private void doPersistEntry(final ReplicatedLogEntry entry, final RaftCallback<Long> callback, final boolean sync) {
        try {
            task.appendEntry(entry, requireNonNull(callback), sync);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to start persist", e);
        }
    }

    @Override
    public void discardTail(final long fromIndex) {
        try {
            task.syncDiscardTail(fromIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete tail entries", e);
        }
    }

    @Override
    public void discardHead(final long sequenceNumber) {
        try {
            // FIXME: journalIndex
            task.discardHead(sequenceNumber);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to delete head entries", e);
        }
    }

    @Override
    public void markLastApplied(final long lastApplied) {
        try {
            task.setApplyTo(lastApplied);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to update last applied index", e);
        }
    }

    @Override
    public EntryJournal journal() {
        return task.journal();
    }
}
