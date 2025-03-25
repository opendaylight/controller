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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.raft.spi.SnapshotFileFormat;
import org.opendaylight.raft.spi.SnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal interface towards storage entities.
 */
public abstract sealed class RaftStorage implements DataPersistenceProvider
        permits DisabledRaftStorage, EnabledRaftStorage {
    private static final Logger LOG = LoggerFactory.getLogger(RaftStorage.class);

    private final @NonNull SnapshotFileFormat preferredFormat;

    protected RaftStorage(final SnapshotFileFormat preferredFormat) {
        this.preferredFormat = requireNonNull(preferredFormat);
    }

    private ExecutorService executor;

    // FIXME: we should have the concept of being 'open', when we have a thread pool to perform the asynchronous part
    //        of RaftActorSnapshotCohort.createSnapshot(), using virtual-thread-per-task

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
            .name(memberId() + "-%d", ThreadLocalRandom.current().nextInt())
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
        }
    }

    protected abstract void preStop();

    protected final <T> @NonNull ListenableFuture<T> submit(final @NonNull Callable<T> task) {
        return doSubmit(requireNonNull(task));
    }

    protected final @NonNull ListenableFuture<Void> submit(final @NonNull Runnable task) {
        return doSubmit(Executors.<Void>callable(task, null));
    }

    private <T> @NonNull ListenableFuture<T> doSubmit(final @NonNull Callable<T> task) {
        final var local = executor;
        if (local == null) {
            throw new IllegalStateException("Storage " + memberId() + " is stopped");
        }
        return Futures.submit(task, local);
    }

    @Override
    public final void saveSnapshotForInstall(final WritableSnapshot snapshot,
            final BiConsumer<SnapshotSource, ? super Throwable> callback) {
        Futures.addCallback(submit(() -> writeSnapshot(snapshot)), new FutureCallback<>() {
            @Override
            public void onSuccess(final SnapshotSource result) {
                callback.accept(result, null);
            }

            @Override
            public void onFailure(final Throwable failure) {
                callback.accept(null, failure);
            }
        }, MoreExecutors.directExecutor());
    }

    private SnapshotSource writeSnapshot(final WritableSnapshot writeTo) throws IOException {
        try (var outer = newFileBackedStream()) {
            try (var inner = preferredFormat.encodeOutput(outer)) {
                writeTo.writeTo(inner);
            }
            return preferredFormat.sourceFor(outer.asByteSource()::openStream);
        }
    }

    protected abstract @NonNull FileBackedOutputStream newFileBackedStream();

    @Override
    public final String toString() {
        return addToStringAtrributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected @NonNull ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return helper.add("memberId", memberId()).add("preferredFormat", preferredFormat);
    }
}
