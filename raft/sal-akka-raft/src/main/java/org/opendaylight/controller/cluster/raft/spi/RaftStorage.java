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
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
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
public abstract sealed class RaftStorage implements EntryStore, SnapshotStore
        permits DisabledRaftStorage, EnabledRaftStorage {
    @NonNullByDefault
    protected abstract class CancellableTask<T> implements Runnable {
        private final RaftCallback<T> callback;

        protected CancellableTask(final RaftCallback<T> callback) {
            this.callback = requireNonNull(callback);
        }

        @Override
        public final void run() {
            if (tasks.remove(this)) {
                runImpl();
            } else {
                LOG.debug("{}: not executing task {}", memberId(), this);
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
                LOG.debug("{}: not cancelling task {}", memberId(), this);
            }
        }

        private void complete(final @Nullable Exception failure, final @Nullable T success) {
            completer.enqueueCompletion(() -> callback.invoke(failure, success));
        }
    }

    @NonNullByDefault
    private final class StreamSnapshotTask extends CancellableTask<InstallableSnapshot> {
        private final EntryInfo lastIncluded;
        private final ToStorage<?> snapshot;

        StreamSnapshotTask(final RaftCallback<InstallableSnapshot> callback, final EntryInfo lastIncluded,
                final ToStorage<?> snapshot) {
            super(callback);
            this.lastIncluded = requireNonNull(lastIncluded);
            this.snapshot = requireNonNull(snapshot);
        }

        @Override
        protected InstallableSnapshot compute() throws IOException {
            try (var outer = new FileBackedOutputStream(streamConfig)) {
                try (var inner = compression.encodeOutput(outer)) {
                    snapshot.writeTo(inner);
                }
                return new InstallableSnapshotSource(lastIncluded, compression.nativeSource(outer.toStreamSource()));
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RaftStorage.class);
    private static final HexFormat HF = HexFormat.of().withUpperCase();
    private static final String FILENAME_START_STR = "snapshot-";

    protected final @NonNull RaftStorageCompleter completer;
    protected final @NonNull CompressionType compression;
    protected final @NonNull Path directory;

    private final Set<CancellableTask<?>> tasks = ConcurrentHashMap.newKeySet();
    private final @NonNull Configuration streamConfig;

    private ExecutorService executor;

    protected RaftStorage(final RaftStorageCompleter completer, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        this.completer = requireNonNull(completer);
        this.directory = requireNonNull(directory);
        this.compression = requireNonNull(compression);
        this.streamConfig = requireNonNull(streamConfig);
    }

    protected final String memberId() {
        return completer.memberId();
    }

    // FIXME: this class should also be tracking the last snapshot bytes -- i.e. what AbstractLeader.SnapshotHolder.
    //        for file-based enabled storage, this means keeping track of the last snapshot we have. For disabled the
    //        case is similar, except we want to have a smarter strategy:
    //         - for small snapshots just use memory and throw them away as sson as unreferenced
    //         - for large snapshots keep them on disk even after they become unreferenced -- for some time, or journal
    //           activity.

    public final void start() throws IOException {
        if (executor != null) {
            throw new IllegalStateException("Storage " + memberId() + " already started");
        }

        Files.createDirectories(directory);

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
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId() + " already stopped");
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
    public final @NonNull RaftStorageCompleter completer() {
        return completer;
    }

    @Override
    public final @Nullable SnapshotFile lastSnapshot() throws IOException {
        final var files = listFiles();
        if (files.isEmpty()) {
            LOG.debug("{}: no eligible files found", memberId());
            return null;
        }

        final var first = files.getLast();
        LOG.debug("{}: picked {} as the latest file", memberId(), first);
        return first;
    }

    @Override
    @NonNullByDefault
    public final void streamToInstall(final EntryInfo lastIncluded, final ToStorage<?> snapshot,
            final RaftCallback<InstallableSnapshot> callback) {
        submitTask(new StreamSnapshotTask(callback, lastIncluded, snapshot));
    }

    @Override
    public final void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        requireNonNull(raftSnapshot);
        requireNonNull(lastIncluded);

        // Allocate before enqueuing to allow multiple snapshots to work concurrently and still get the right results
        final var timestamp = Instant.now();

        submitTask(new CancellableTask<>(callback) {
            @Override
            protected Instant compute() throws IOException {
                saveSnapshot(raftSnapshot, lastIncluded, snapshot, timestamp);
                return timestamp;
            }
        });
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final Instant timestamp) throws IOException {
        final var format = SnapshotFileFormat.latest();
        final var baseName = new StringBuilder()
            .append(FILENAME_START_STR)
            .append(HF.toHexDigits(timestamp.getEpochSecond()))
            .append('-')
            .append(HF.toHexDigits(timestamp.getNano()));

        final var tmpPath = directory.resolve(baseName + ".tmp");
        final var filePath = directory.resolve(baseName + format.extension());

        LOG.debug("{}: starting snapshot writeout to {}", memberId(), tmpPath);

        try {
            format.createNew(tmpPath, timestamp, lastIncluded, raftSnapshot.votingConfig(), compression,
                raftSnapshot.unappliedEntries(), compression, snapshot).close();
            Files.move(tmpPath, filePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.deleteIfExists(tmpPath);
            throw e;
        }

        LOG.debug("{}: finished snapshot writeout to {}", memberId(), filePath);
        retainSnapshots(timestamp);
    }

    private void retainSnapshots(final Instant firstRetained) {
        final List<SnapshotFile> files;
        try {
            files = listFiles();
        } catch (IOException e) {
            LOG.warn("{}: failed to list snapshots, will retry next time", memberId(), e);
            return;
        }

        for (var file : files) {
            if (firstRetained.compareTo(file.timestamp()) <= 0) {
                LOG.debug("{}: retaining snapshot {}", memberId(), file);
                break;
            }

            try {
                // we should not have concurrent access, but it is okay if the file disappears independently
                Files.deleteIfExists(file.path());
            } catch (IOException e) {
                LOG.warn("{}: failed to delete snapshot {}, will retry next time", memberId(), file, e);
                continue;
            }

            LOG.debug("{}: deleted snapshot {}", memberId(), file);
        }
    }

    protected final void submitTask(final @NonNull CancellableTask<?> task) {
        final var local = checkNotClosed();
        tasks.add(task);
        local.execute(task);
    }

    @Override
    public final String toString() {
        return addToStringAtrributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return helper
            .add("directory", directory)
            .add("compression", compression)
            .add("streams", streamConfig)
            .add("completer", completer);
    }

    private ExecutorService checkNotClosed() {
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId() + " already stopped");
        }
        return local;
    }

    // Ordered by ascending timestamp, i.e. oldest snapshot first
    private List<SnapshotFile> listFiles() throws IOException {
        if (!Files.exists(directory)) {
            LOG.debug("{}: directory {} does not exist", memberId(), directory);
            return List.of();
        }

        final List<SnapshotFile> ret;
        try (var paths = Files.list(directory)) {
            ret = paths.map(this::pathToFile).filter(Objects::nonNull)
                .sorted(Comparator.comparing(SnapshotFile::timestamp))
                .toList();
        }
        LOG.trace("{}: recognized files: {}", memberId(), ret);
        return ret;
    }

    private @Nullable SnapshotFile pathToFile(final Path path) {
        if (!Files.isRegularFile(path)) {
            LOG.debug("{}: skipping non-file {}", memberId(), path);
            return null;
        }
        if (!Files.isReadable(path)) {
            LOG.debug("{}: skipping unreadable file {}", memberId(), path);
            return null;
        }
        final var name = path.getFileName().toString();
        if (!name.startsWith(FILENAME_START_STR)) {
            LOG.debug("{}: skipping unrecognized file {}", memberId(), path);
            return null;
        }
        final var format = SnapshotFileFormat.forFileName(name);
        if (format == null) {
            LOG.debug("{}: skipping unhandled file {}", memberId(), path);
            return null;
        }
        LOG.debug("{}: selected {} to handle file {}", memberId(), format, path);

        try {
            return format.open(path);
        } catch (IOException e) {
            LOG.warn("{}: cannot open {}, skipping", memberId(), path, e);
            return null;
        }
    }
}
