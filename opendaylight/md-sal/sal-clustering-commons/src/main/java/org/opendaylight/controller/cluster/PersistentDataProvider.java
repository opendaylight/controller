/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import static java.util.Objects.requireNonNull;

import akka.persistence.AbstractPersistentActor;
import akka.persistence.DeleteMessagesSuccess;
import akka.persistence.DeleteSnapshotsSuccess;
import akka.persistence.JournalProtocol;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import java.util.function.Consumer;

/**
 * A DataPersistenceProvider implementation with persistence enabled.
 */
public class PersistentDataProvider implements DataPersistenceProvider {
    private final AbstractPersistentActor persistentActor;

    public PersistentDataProvider(final AbstractPersistentActor persistentActor) {
        this.persistentActor = requireNonNull(persistentActor, "persistentActor can't be null");
    }

    @Override
    public boolean isRecoveryApplicable() {
        return true;
    }

    @Override
    public <T extends PersistentData> void persist(final T entry, final Consumer<T> callback) {
        persistentActor.persist(entry, callback::accept);
    }

    @Override
    public <T extends PersistentData> void persistAsync(final T entry, final Consumer<T> callback) {
        persistentActor.persistAsync(entry, callback::accept);
    }

    @Override
    public void saveSnapshot(final Object snapshot) {
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
