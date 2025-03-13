/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.japi.Procedure;
import org.apache.pekko.persistence.AbstractPersistentActor;
import org.apache.pekko.persistence.DeleteMessagesSuccess;
import org.apache.pekko.persistence.DeleteSnapshotsSuccess;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;

/**
 * A DataPersistenceProvider implementation with persistence enabled.
 */
// Non-final for testing
class PersistentDataProvider implements DataPersistenceProvider {
    private final AbstractPersistentActor persistentActor;

    PersistentDataProvider(final AbstractPersistentActor persistentActor) {
        this.persistentActor = requireNonNull(persistentActor, "persistentActor can't be null");
    }

    @Override
    public boolean isRecoveryApplicable() {
        return true;
    }

    @Override
    public <T> void persist(final T entry, final Procedure<T> procedure) {
        persistentActor.persist(entry, procedure);
    }

    @Override
    public <T> void persistAsync(final T entry, final Procedure<T> procedure) {
        persistentActor.persistAsync(entry, procedure);
    }

    @Override
    public void saveSnapshot(final Snapshot snapshot) {
        persistentActor.saveSnapshot(snapshot);
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        persistentActor.deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        persistentActor.deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return persistentActor.lastSequenceNr();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return response instanceof DeleteMessagesSuccess;
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return response instanceof DeleteSnapshotsSuccess;
    }
}
