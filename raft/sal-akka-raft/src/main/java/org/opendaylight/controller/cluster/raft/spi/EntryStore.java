/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.raft.api.EntryMeta;

/**
 * Interface to a access and manage {@link StateMachineCommand}-bearing entries with {@link EntryMeta}.
 */
public interface EntryStore {
    /**
     * A {@link RaftCallback} reporting the {@code journalIndex} on success.
     */
    @NonNullByDefault
    @FunctionalInterface
    interface PersistCallback extends RaftCallback<Long> {
        default void invoke(final @Nullable Exception failure, final Long success) {
            if (failure != null) {
                Throwables.throwIfUnchecked(failure);
                throw failure instanceof IOException e ? new UncheckedIOException(e)
                    : new IllegalStateException("Failed to store entry", failure);
            }
            invoke();
        }

        void invoke();
    }

    /**
     * Persists an entry to the applicable journal synchronously. The contract is that the callback will be invoked
     * before {@link RaftActor} sees any other message.
     *
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: without callback and throwing IOException
    @NonNullByDefault
    void persistEntry(ReplicatedLogEntry entry, Runnable callback);

    /**
     * Persists an entry to the applicable journal asynchronously.
     *
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: Callback<ReplicatedLogEntry> instead of Consumer
    @NonNullByDefault
    void startPersistEntry(ReplicatedLogEntry entry, Runnable callback);

    /**
     * Delete entries starting from specified index.
     *
     * @param fromIndex the index of first entry to delete
     */
    void deleteEntries(long fromIndex);

    /**
     * Record a known value of {@code lastApplied} as a recovery optimization. If we can recover this information,
     * recovery can re-apply these entries before we attempt to talk to other members. It is okay to lose this marker,
     * as in that case we will just apply those entries as part of being a follower or becoming a leader.
     *
     * <p>This amounts to persisting a lower bound on {@code commitIndex}, which is explicitly volatile state. We could
     * remember that instead (or perhaps as well) -- but now we just derive it.
     *
     * <p>If we later discover that this index lies beyond current leader's {@code commitIndex}, we will ask for
     * a complete snapshot -- which is not particularly nice, but should happen seldom enough for it not to matter much.
     *
     * @param lastApplied lastApplied index to remember
     */
    void markLastApplied(long lastApplied);

    /**
     * Deletes journal entries up to the given sequence number.
     *
     * @param sequenceNumber the sequence number
     */
    // FIXME: throws IOException
    void deleteMessages(long sequenceNumber);

    /**
     * Returns the last sequence number contained in the journal.
     *
     * @return the last sequence number
     */
    long lastSequenceNumber();

    /**
     * Receive and potentially handle a {@link JournalProtocol} response.
     *
     * @param response A {@link JournalProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleJournalResponse(JournalProtocol.@NonNull Response response);
}
