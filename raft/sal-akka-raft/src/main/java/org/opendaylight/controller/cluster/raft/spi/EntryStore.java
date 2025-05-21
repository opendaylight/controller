/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.raft.api.EntryMeta;

/**
 * Interface to a access and manage {@link StateMachineCommand}-bearing entries with {@link EntryMeta}. This interface
 * is inherently asynchronous, with the assumption that each request is enqueued to a background thread, which processes
 * requests in batches. Synchronization with the enclosing {@link RaftActor} is either asynchronous
 * (via {@link RaftCallback}s), or defined to terminate the actor on failure.
 */
@NonNullByDefault
public interface EntryStore {
    /**
     * {@return the {@link RaftStorageCompleter}}
     */
    RaftStorageCompleter completer();

    /**
     * Persists an entry to the applicable journal synchronously. The contract is that the callback will be invoked
     * before {@link RaftActor} sees any other message.
     *
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    void persistEntry(ReplicatedLogEntry entry, RaftCallback<Long> callback);

    /**
     * Persists an entry to the applicable journal asynchronously.
     *
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: really just a boolean 'async' flag's difference
    void startPersistEntry(ReplicatedLogEntry entry, RaftCallback<Long> callback);

    /**
     * Deletes journal entries up to, but not including, the given {@code journalIndex}.
     *
     * @param firstRetainedIndex the {@code journalIndex} of the first retained entry
     */
    // FIXME: guarantee sync as discardTail()
    void discardHead(long firstRetainedIndex);

    /**
     * Delete entries starting from, and including, specified index. The contract is that the callback will be invoked
     * before {@link RaftActor} sees any other message.
     *
     * @param firstRemovedIndex the {@code journalIndex} of first entry to delete
     */
    void discardTail(long firstRemovedIndex);

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
     * @param commitJournalIndex {@code the journalIndex} of the entry which is covered by {@code commitIndex} and has
     *        been observed as {@code lastApplied}.
     */
    // FIXME: guarantee sync as discardTail()
    void checkpointLastApplied(long commitJournalIndex);
}
