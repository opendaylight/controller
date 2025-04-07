/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.raft.api.EntryMeta;

/**
 * Interface to a access and manage {@link StateMachineCommand}-bearing entries with {@link EntryMeta}.
 */
public interface EntryStore {
    /**
     * Persists an entry to the applicable journal synchronously.
     *
     * @param <T> the type of the journal entry
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: replace with:
    //        void persist(Object entry) throws IOException
    <T> void persist(@NonNull T entry, @NonNull Consumer<T> callback);

    /**
     * Persists an entry to the applicable journal asynchronously.
     *
     * @param <T> the type of the journal entry
     * @param entry the journal entry to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: replace with:
    //        void persistAsync(T entry, BiConsumer<? super T, ? super Throwable> callback)
    <T> void persistAsync(@NonNull T entry, @NonNull Consumer<T> callback);

    /**
     * Deletes journal entries up to the given sequence number.
     *
     * @param sequenceNumber the sequence number
     */
    // FIXME: throws IOException
    void deleteMessages(long sequenceNumber);

    /**
     * Receive and potentially handle a {@link JournalProtocol} response.
     *
     * @param response A {@link JournalProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleJournalResponse(JournalProtocol.@NonNull Response response);
}
