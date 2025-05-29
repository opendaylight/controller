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
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.spi.ForwardingSnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
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
            actor.executeInSelf(() -> {
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

    private final AtomicReference<CapturedCallback> capture = new AtomicReference<>();
    private final @NonNull SnapshotStore delegate;
    private final @NonNull ExecuteInSelfActor actor;

    @NonNullByDefault
    CapturingSnapshotStore(final SnapshotStore delegate, final ExecuteInSelfActor actor) {
        this.delegate = requireNonNull(delegate);
        this.actor = requireNonNull(actor);
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        super.saveSnapshot(raftSnapshot, lastIncluded, snapshot,
            (failure, success) -> capture.set(new CapturedCallback(callback, failure, success)));
    }

    @Override
    protected SnapshotStore delegate() {
        return delegate;
    }

    CapturedCallback awaitSaveSnapshot() {
        return await().atMost(Duration.ofSeconds(2)).until(capture::get, Objects::nonNull);
    }
}