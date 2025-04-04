/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * This interface provides methods to persist data and is an abstraction of the akka-persistence persistence API.
 */
// FIXME: find a better name for this interface. It is heavily influenced by Pekko Persistence, most notably the weird
//        API around snapshots and message deletion -- which assumes the entity requesting it is the subclass itself.
public interface DataPersistenceProvider extends SnapshotStore {
    /**
     * Returns whether or not persistence recovery is applicable/enabled.
     *
     * @return {@code true} if recovery is applicable, otherwise false, in which case the provider is not persistent and
     *         may not have anything to be recovered
     */
    boolean isRecoveryApplicable();

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
     * Saves a snapshot.
     *
     * @param snapshot the snapshot object to save
     */
    // FIXME: replace with the below, combining the save functionality
    void saveSnapshot(@NonNull Snapshot snapshot);

    /**
     * Deletes snapshots based on the given criteria.
     *
     * @param criteria the search criteria
     */
    // FIXME: criteria == max size? max snapshots?
    // FIXME: throws IOException
    void deleteSnapshots(@NonNull SnapshotSelectionCriteria criteria);

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
    long getLastSequenceNumber();

    /**
     * Receive and potentially handle a {@link JournalProtocol} response.
     *
     * @param response A {@link JournalProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleJournalResponse(JournalProtocol.@NonNull Response response);

    /**
     * Receive and potentially handle a {@link SnapshotProtocol} response.
     *
     * @param response A {@link SnapshotProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleSnapshotResponse(SnapshotProtocol.@NonNull Response response);
}
