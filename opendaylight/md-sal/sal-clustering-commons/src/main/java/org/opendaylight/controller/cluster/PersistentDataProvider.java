/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotSelectionCriteria;
import com.google.common.base.Preconditions;

/**
 * A DataPersistenceProvider implementation with persistence enabled.
 */
public class PersistentDataProvider implements DataPersistenceProvider {

    private final AbstractPersistentActor persistentActor;

    public PersistentDataProvider(final AbstractPersistentActor persistentActor) {
        this.persistentActor = Preconditions.checkNotNull(persistentActor, "persistentActor can't be null");
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
}
