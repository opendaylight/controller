/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.SnapshotFileFormat;
import org.opendaylight.raft.spi.SnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal interface towards storage entities.
 */
public abstract sealed class RaftStorage implements DataPersistenceProvider
        permits DisabledRaftStorage, EnabledRaftStorage {
    private abstract class CancellableTask<T> implements Runnable {
        private final BiConsumer<? super T, ? super Throwable> callback;

        CancellableTask(final BiConsumer<? super T, ? super Throwable> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        @SuppressWarnings("checkstyle:illegalCatch")
        public final void run() {
            if (!tasks.remove(this)) {
                LOG.debug("{}: not executing task {}", memberId(), this);
                return;
            }

            final T result;
            try {
                result = compute();
            } catch (Exception e) {
                callback.accept(null, e);
                return;
            }
            callback.accept(result, null);
        }

        abstract @NonNull T compute() throws Exception;
    }

    private final class StreamSnapshotTask extends CancellableTask<SnapshotSource> {
        private final WritableSnapshot snapshot;

        StreamSnapshotTask(final BiConsumer<SnapshotSource, ? super Throwable> callback,
                final WritableSnapshot snapshot) {
            super(callback);
            this.snapshot = requireNonNull(snapshot);
        }

        @Override
        SnapshotSource compute() throws IOException {
            try (var outer = new FileBackedOutputStream(streamConfig)) {
                try (var inner = preferredFormat.encodeOutput(outer)) {
                    snapshot.writeTo(inner);
                }
                return preferredFormat.sourceFor(outer.asByteSource()::openStream);
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RaftStorage.class);

    private final Set<CancellableTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private final @NonNull SnapshotFileFormat preferredFormat;
    private final @NonNull Configuration streamConfig;

    private ExecutorService executor;

    protected RaftStorage(final SnapshotFileFormat preferredFormat, final Configuration streamConfig) {
        this.preferredFormat = requireNonNull(preferredFormat);
        this.streamConfig = requireNonNull(streamConfig);
    }

    // FIXME: this class should also be tracking the last snapshot bytes -- i.e. what AbstractLeader.SnapshotHolder.
    //        for file-based enabled storage, this means keeping track of the last snapshot we have. For disabled the
    //        case is similar, except we want to have a smarter strategy:
    //         - for small snapshots just use memory and throw them away as sson as unreferenced
    //         - for large snapshots keep them on disk even after they become unreferenced -- for some time, or journal
    //           activity.

    /**
     * Returns the member name associated with this storage.
     *
     * @return the member name associated with this storage
     */
    protected abstract String memberId();

    public final void start() throws IOException {
        if (executor != null) {
            throw new IllegalStateException("Storage " + memberId() + " already started");
        }

        final var local = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name(memberId() + "-%d", ThreadLocalRandom.current().nextInt(0, 1_000_000))
            .factory());
        executor = local;
        LOG.debug("{}: started executor", memberId());

        try {
            postStart();
        } catch (IOException e) {
            executor = null;
            stopExecutor(local);
            throw e;
        }
    }

    private void stopExecutor(final ExecutorService service) {
        LOG.debug("{}: stopped executor with {} remaining tasks", memberId(), service.shutdownNow().size());
    }

    protected abstract void postStart() throws IOException;

    public final void stop() {
        final var local = checkNotClosed();

        try {
            preStop();
        } finally {
            executor = null;
            stopExecutor(local);
            cancelTasks();
        }
    }

    private void cancelTasks() {
        for (var task : tasks) {
            if (tasks.remove(task)) {
                task.callback.accept(null, new CancellationException("Storage closed"));
            } else {
                LOG.debug("{}: not cancelling task {}", memberId(), task);
            }
        }
    }

    protected abstract void preStop();

    @Override
    public final void streamToInstall(final WritableSnapshot snapshot,
            final BiConsumer<SnapshotSource, ? super Throwable> callback) {
        final var local = checkNotClosed();
        final var task = new StreamSnapshotTask(callback, snapshot);
        tasks.add(task);
        local.execute(task);
    }

    @Override
    public final String toString() {
        return addToStringAtrributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return helper.add("memberId", memberId()).add("preferredFormat", preferredFormat).add("streams", streamConfig);
    }

    private ExecutorService checkNotClosed() {
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId() + " already stopped");
        }
        return local;
    }
}
