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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal interface towards storage entities.
 */
public abstract sealed class RaftStorage implements DataPersistenceProvider
        permits DisabledRaftStorage, EnabledRaftStorage {
    @NonNullByDefault
    protected abstract class CancellableTask<T> implements Runnable {
        private final Callback<T> callback;

        protected CancellableTask(final Callback<T> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        public final void run() {
            if (tasks.remove(this)) {
                runImpl();
            } else {
                LOG.debug("{}: not executing task {}", memberId, this);
            }
        }

        @SuppressWarnings("checkstyle:illegalCatch")
        private void runImpl() {
            final T result;
            try {
                result = compute();
            } catch (Exception e) {
                complete(e, null);
                return;
            }
            complete(null, result);
        }

        protected abstract T compute() throws Exception;

        private void cancel(final CancellationException cause) {
            if (tasks.remove(this)) {
                complete(cause, null);
            } else {
                LOG.debug("{}: not cancelling task {}", memberId, this);
            }
        }

        private void complete(final @Nullable Exception failure, final @Nullable T success) {
            executeInSelf.executeInSelf(() -> callback.invoke(failure, success));
        }
    }

    @NonNullByDefault
    private final class StreamSnapshotTask<S extends StateSnapshot> extends CancellableTask<InstallableSnapshot> {
        private final StateSnapshot.Writer<S> writer;
        private final EntryInfo lastIncluded;
        private final S snapshot;

        StreamSnapshotTask(final Callback<InstallableSnapshot> callback, final EntryInfo lastIncluded, final S snapshot,
                final StateSnapshot.Writer<S> writer) {
            super(callback);
            this.lastIncluded = requireNonNull(lastIncluded);
            this.snapshot = requireNonNull(snapshot);
            this.writer = requireNonNull(writer);
        }

        @Override
        protected InstallableSnapshot compute() throws IOException {
            try (var outer = new FileBackedOutputStream(streamConfig)) {
                try (var inner = compression.encodeOutput(outer)) {
                    writer.writeSnapshot(snapshot, inner);
                }
                return new InstallableSnapshotSource(lastIncluded, compression.nativeSource(outer.toStreamSource()));
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RaftStorage.class);

    protected final @NonNull ExecuteInSelfActor executeInSelf;
    protected final @NonNull CompressionType compression;
    protected final @NonNull String memberId;
    protected final @NonNull Path directory;

    private final Set<CancellableTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private final @NonNull Configuration streamConfig;

    private ExecutorService executor;

    protected RaftStorage(final String memberId, final ExecuteInSelfActor executeInSelf, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        this.memberId = requireNonNull(memberId);
        this.executeInSelf = requireNonNull(executeInSelf);
        this.directory = requireNonNull(directory);
        this.compression = requireNonNull(compression);
        this.streamConfig = requireNonNull(streamConfig);
    }

    // FIXME: this class should also be tracking the last snapshot bytes -- i.e. what AbstractLeader.SnapshotHolder.
    //        for file-based enabled storage, this means keeping track of the last snapshot we have. For disabled the
    //        case is similar, except we want to have a smarter strategy:
    //         - for small snapshots just use memory and throw them away as sson as unreferenced
    //         - for large snapshots keep them on disk even after they become unreferenced -- for some time, or journal
    //           activity.

    public final void start() throws IOException {
        if (executor != null) {
            throw new IllegalStateException("Storage " + memberId + " already started");
        }

        Files.createDirectories(directory);

        final var local = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
            .name(memberId + "-%d", ThreadLocalRandom.current().nextInt(0, 1_000_000))
            .factory());
        executor = local;
        LOG.debug("{}: started executor", memberId);

        try {
            postStart();
        } catch (IOException e) {
            executor = null;
            stopExecutor(local);
            throw e;
        }
    }

    private void stopExecutor(final ExecutorService service) {
        LOG.debug("{}: stopped executor with {} remaining tasks", memberId, service.shutdownNow().size());
    }

    protected abstract void postStart() throws IOException;

    public final void stop() {
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId + " already stopped");
        }

        try {
            preStop();
        } finally {
            executor = null;
            stopExecutor(local);
            cancelTasks();
        }
    }

    private void cancelTasks() {
        final var cause = new CancellationException("Storage closed");
        tasks.forEach(task -> task.cancel(cause));
    }

    protected abstract void preStop();

    @Override
    @NonNullByDefault
    public final <T extends StateSnapshot> void streamToInstall(final EntryInfo lastIncluded, final T snapshot,
            final StateSnapshot.Writer<T> writer, final Callback<InstallableSnapshot> callback) {
        final var local = checkNotClosed();
        final var task = new StreamSnapshotTask<>(callback, lastIncluded, snapshot, writer);
        tasks.add(task);
        local.execute(task);
    }

    @Override
    public final String toString() {
        return addToStringAtrributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return helper
            .add("memberId", memberId)
            .add("directory", directory)
            .add("compression", compression)
            .add("streams", streamConfig);
    }

    private ExecutorService checkNotClosed() {
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId + " already stopped");
        }
        return local;
    }
}
