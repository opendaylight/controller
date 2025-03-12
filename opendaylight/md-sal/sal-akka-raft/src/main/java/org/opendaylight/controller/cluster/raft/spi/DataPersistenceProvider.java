/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.spi;

import org.apache.pekko.japi.Procedure;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;

/**
 * This interface provides methods to persist data and is an abstraction of the akka-persistence persistence API.
 */
public interface DataPersistenceProvider {
    /**
     * Returns whether or not persistence recovery is applicable/enabled.
     *
     * @return true if recovery is applicable, otherwise false, in which case the provider is not persistent and may
     *         not have anything to be recovered
     */
    boolean isRecoveryApplicable();

    /**
     * Persists an entry to the applicable journal synchronously.
     *
     * @param entry the journal entry to persist
     * @param procedure the callback when persistence is complete
     * @param <T> the type of the journal entry
     */
    <T> void persist(T entry, Procedure<T> procedure);

    /**
     * Persists an entry to the applicable journal asynchronously.
     *
     * @param entry the journal entry to persist
     * @param procedure the callback when persistence is complete
     * @param <T> the type of the journal entry
     */
    <T> void persistAsync(T entry, Procedure<T> procedure);

    /**
     * Saves a snapshot.
     *
     * @param snapshot the snapshot object to save
     */
    void saveSnapshot(Object snapshot);

    /**
     * Deletes snapshots based on the given criteria.
     *
     * @param criteria the search criteria
     */
    void deleteSnapshots(SnapshotSelectionCriteria criteria);

    /**
     * Deletes journal entries up to the given sequence number.
     *
     * @param sequenceNumber the sequence number
     */
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
