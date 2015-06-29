/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;

/**
 * DataPersistenceProvider provides methods to persist data and is an abstraction of the akka-persistence persistence
 * API.
 */
public interface DataPersistenceProvider {
    /**
     * @return false if recovery is not applicable. In that case the provider is not persistent and may not have
     * anything to be recovered
     */
    boolean isRecoveryApplicable();

    /**
     * Persist a journal entry.
     *
     * @param o
     * @param procedure
     * @param <T>
     */
    <T> void persist(T o, Procedure<T> procedure);

    /**
     * Save a snapshot
     *
     * @param o
     */
    void saveSnapshot(Object o);

    /**
     * Delete snapshots based on the criteria
     *
     * @param criteria
     */
    void deleteSnapshots(SnapshotSelectionCriteria criteria);

    /**
     * Delete journal entries up to the sequence number
     *
     * @param sequenceNumber
     */
    void deleteMessages(long sequenceNumber);

    /**
     * Returns the last sequence number contained in the journal.
     */
    long getLastSequenceNumber();
}
