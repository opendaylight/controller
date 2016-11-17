/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.UntypedPersistentActor;
import com.google.common.base.Preconditions;

/**
 * A DataPersistenceProvider implementation with persistence enabled.
 */
public class PersistentDataProvider implements DataPersistenceProvider {

    private final UntypedPersistentActor persistentActor;

    public PersistentDataProvider(UntypedPersistentActor persistentActor) {
        this.persistentActor = Preconditions.checkNotNull(persistentActor, "persistentActor can't be null");
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
