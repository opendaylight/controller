/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.DecoratingRaftCallback;
import org.opendaylight.controller.cluster.raft.spi.ForwardingSnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;

final class CapturingSnapshotStore extends ForwardingSnapshotStore {
    final class CapturedCallback {
        private final RaftCallback<Instant> callback;
        private final Exception failure;
        private final Instant success;

        private CapturedCallback(final RaftCallback<Instant> callback, final Exception failure, final Instant success) {
            this.callback = requireNonNull(callback);
            this.failure = failure;
            this.success = success;
        }

        void complete() {
            final var latch = new CountDownLatch(1);
            completer.enqueueCompletion(() -> {
                callback.invoke(failure, success);
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    @NonNullByDefault
    private final class CaptureSaveSnapshot extends DecoratingRaftCallback<Instant> {
        CaptureSaveSnapshot(final RaftCallback<Instant> delegate) {
            super(delegate);
        }

        @Override
        public void invoke(final @Nullable Exception failure, final Instant success) {
            capture.set(new CapturedCallback(delegate, failure, success));
        }
    }

    private final AtomicReference<CapturedCallback> capture = new AtomicReference<>();
    private final @NonNull SnapshotStore delegate;
    private final @NonNull RaftStorageCompleter completer;

    @NonNullByDefault
    CapturingSnapshotStore(final SnapshotStore delegate, final RaftStorageCompleter completer) {
        this.delegate = requireNonNull(delegate);
        this.completer = requireNonNull(completer);
    }

    @Override
    @NonNullByDefault
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        super.saveSnapshot(raftSnapshot, lastIncluded, snapshot, new CaptureSaveSnapshot(callback));
    }

    @Override
    protected SnapshotStore delegate() {
        return delegate;
    }

    CapturedCallback awaitSaveSnapshot() {
        return await().atMost(Duration.ofSeconds(2)).until(capture::get, Objects::nonNull);
    }
}
