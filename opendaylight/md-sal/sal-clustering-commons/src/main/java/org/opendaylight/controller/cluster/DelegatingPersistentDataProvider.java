/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster;

import akka.japi.Procedure;
import akka.persistence.SnapshotSelectionCriteria;

/**
 * A DataPersistenceProvider implementation that delegates to another implementation.
 *
 * @author Thomas Pantelis
 */
public class DelegatingPersistentDataProvider implements DataPersistenceProvider {
    private DataPersistenceProvider delegate;

    public DelegatingPersistentDataProvider(DataPersistenceProvider delegate) {
        this.delegate = delegate;
    }

    public void setDelegate(DataPersistenceProvider delegate) {
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
    public <T> void persist(T o, Procedure<T> procedure) {
        delegate.persist(o, procedure);
    }

    @Override
    public void saveSnapshot(Object o) {
        delegate.saveSnapshot(o);
    }

    @Override
    public void deleteSnapshots(SnapshotSelectionCriteria criteria) {
        delegate.deleteSnapshots(criteria);
    }

    @Override
    public void deleteMessages(long sequenceNumber) {
        delegate.deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return delegate.getLastSequenceNumber();
    }
}
