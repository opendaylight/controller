/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import static java.util.Objects.requireNonNull;

import akka.japi.Procedure;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.DeleteMessagesSuccess;
import akka.persistence.JournalProtocol;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;

/**
 This class is meant to replace {@code PersistentDataProvider} in order to get rid of AbstractPersistentActor.
 But firstly we need to implement replacement for persisting journal messages
 */
public class NewPersistentDataProvider extends PersistentDataProvider {
    //TODO: to get rid of AbstractPersistentActor we will need some interface to provide persistenceId() and
    // snapshotSequenceNr() we override both methods so it shouldn't be problem
    private final AbstractPersistentActor persistentActor;
    private final SnapshotPersistenceProvider snapshotProvider;

    public NewPersistentDataProvider(final AbstractPersistentActor persistentActor,
                final SnapshotPersistenceProvider snapshotProvider) {
        super(persistentActor);
        this.persistentActor = requireNonNull(persistentActor, "persistentActor can't be null");
        this.snapshotProvider = requireNonNull(snapshotProvider, "persistentActor can't be null");
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
    public void saveSnapshot(final Object snapshot) {
        snapshotProvider.saveSnapshot(snapshot, persistentActor.persistenceId(), persistentActor.snapshotSequenceNr(),
                persistentActor.self());
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        snapshotProvider.deleteSnapshots(criteria, persistentActor.persistenceId(), persistentActor.self());
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
        return snapshotProvider.handleSnapshotResponse(response);
    }
}
