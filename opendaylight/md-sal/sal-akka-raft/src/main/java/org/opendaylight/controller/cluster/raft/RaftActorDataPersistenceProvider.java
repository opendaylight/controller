/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.japi.Procedure;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;

/**
 * The DelegatingPersistentDataProvider used by RaftActor to override the configured persistent provider to
 * persist ReplicatedLogEntry's based on whether or not the payload is a PersistentPayload instance.
 *
 * @author Thomas Pantelis
 */
final class RaftActorDataPersistenceProvider implements DataPersistenceProvider {
    private final PersistentDataProvider persistentProvider;

    private DataPersistenceProvider delegate;

    RaftActorDataPersistenceProvider(final DataPersistenceProvider delegate,
            final PersistentDataProvider persistentProvider) {
        this.delegate = delegate;
        this.persistentProvider = requireNonNull(persistentProvider);
    }

    DataPersistenceProvider getDelegate() {
        return delegate;
    }

    void setDelegate(final DataPersistenceProvider delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public boolean isRecoveryApplicable() {
        return delegate.isRecoveryApplicable();
    }

    @Override
    public <T> void persist(final T entry, final Procedure<T> procedure) {
        doPersist(entry, procedure, false);
    }

    @Override
    public <T> void persistAsync(final T entry, final Procedure<T> procedure) {
        doPersist(entry, procedure, true);
    }

    private <T> void doPersist(final T entry, final Procedure<T> procedure, final boolean async) {
        if (!delegate.isRecoveryApplicable() && entry instanceof ReplicatedLogEntry replicatedLogEntry
            && replicatedLogEntry.getData() instanceof ClusterConfig serverConfig) {
            // TODO: revisit this statement with EntryStore
            //
            //   We persist the ClusterConfig but not the ReplicatedLogEntry to avoid gaps in the journal indexes
            //   on recovery if data persistence is later enabled.
            if (async) {
                persistentProvider.persistAsync(serverConfig, p -> procedure.apply(entry));
            } else {
                persistentProvider.persist(serverConfig, p -> procedure.apply(entry));
            }
            return;
        }

        if (async) {
            delegate.persistAsync(entry, procedure);
        } else {
            delegate.persist(entry, procedure);
        }
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
