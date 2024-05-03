/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.persistence.JournalProtocol;
import akka.persistence.SnapshotProtocol;
import akka.persistence.SnapshotSelectionCriteria;
import java.util.function.Consumer;

/**
 * A DataPersistenceProvider implementation that delegates to another implementation.
 *
 * @author Thomas Pantelis
 */
public class DelegatingPersistentDataProvider implements DataPersistenceProvider {
    private DataPersistenceProvider delegate;

    public DelegatingPersistentDataProvider(final DataPersistenceProvider delegate) {
        this.delegate = delegate;
    }

    public void setDelegate(final DataPersistenceProvider delegate) {
        this.delegate = delegate;
    }

    public DataPersistenceProvider getDelegate() {
        return delegate;
    }

    @Override
    public boolean isRecoveryApplicable() {
        return delegate.isRecoveryApplicable();
    }

    @Override
    public <T extends PersistentData> void persist(final T entry, final Consumer<T> callback) {
        delegate.persist(entry, callback);
    }

    @Override
    public <T extends PersistentData> void persistAsync(final T entry, final Consumer<T> callback) {
        delegate.persistAsync(entry, callback);
    }

    @Override
    public void saveSnapshot(final Object entry) {
        delegate.saveSnapshot(entry);
    }

    @Override
    public void deleteSnapshots(final SnapshotSelectionCriteria criteria) {
        delegate.deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        delegate.deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return delegate.getLastSequenceNumber();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return delegate.handleJournalResponse(response);
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return delegate.handleSnapshotResponse(response);
    }
}
