/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import static java.util.Objects.requireNonNull;

import akka.japi.Procedure;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotSelectionCriteria;

/**
 * A DataPersistenceProvider implementation with persistence enabled.
 */
public class PersistentDataProvider implements DataPersistenceProvider {

    private final AbstractPersistentActor persistentActor;

    public PersistentDataProvider(AbstractPersistentActor persistentActor) {
        this.persistentActor = requireNonNull(persistentActor, "persistentActor can't be null");
    }

    @Override
    public boolean isRecoveryApplicable() {
        return true;
    }

    @Override
    public <T> void persist(T entry, Procedure<T> procedure) {
        persistentActor.persist(entry, procedure);
    }

    @Override
    public <T> void persistAsync(T entry, Procedure<T> procedure) {
        persistentActor.persistAsync(entry, procedure);
    }

    @Override
    public void saveSnapshot(Object snapshot) {
        persistentActor.saveSnapshot(snapshot);
    }

    @Override
    public void deleteSnapshots(SnapshotSelectionCriteria criteria) {
        persistentActor.deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(long sequenceNumber) {
        persistentActor.deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return persistentActor.lastSequenceNr();
    }
}
