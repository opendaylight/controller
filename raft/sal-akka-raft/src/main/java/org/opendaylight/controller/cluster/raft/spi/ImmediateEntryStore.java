/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * An immediate {@link EntryStore}. Offloads asynchronous persist responses via {@link RaftStorageCompleter}
 * exposed via {@link #completer()}.
 */
@NonNullByDefault
public interface ImmediateEntryStore extends EntryStore {
    @Override
    default void persistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        requireNonNull(entry);
        final var completer = completer();
        final var cb = completer.syncWithCurrentMessage(callback);
        completer.enqueueCompletion(() -> cb.invoke(null, 0L));
    }

    @Override
    default void startPersistEntry(final ReplicatedLogEntry entry, final PersistCallback callback) {
        requireNonNull(entry);
        requireNonNull(callback);
        completer().enqueueCompletion(() -> callback.invoke(null, 0L));
    }

    @Override
    default void discardHead(final long firstRetainedIndex) {
        // no-op
    }

    @Override
    default void discardTail(final long firstRemovedIndex) {
        // No-op
    }

    @Override
    default void checkpointLastApplied(final long commitJournalIndex) {
        // No-op
    }
}
