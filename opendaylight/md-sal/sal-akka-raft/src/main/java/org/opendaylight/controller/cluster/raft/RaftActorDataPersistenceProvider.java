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
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.DelegatingPersistentDataProvider;
import org.opendaylight.controller.cluster.raft.spi.PersistentDataProvider;

/**
 * The DelegatingPersistentDataProvider used by RaftActor to override the configured persistent provider to
 * persist ReplicatedLogEntry's based on whether or not the payload is a PersistentPayload instance.
 *
 * @author Thomas Pantelis
 */
final class RaftActorDataPersistenceProvider extends DelegatingPersistentDataProvider {
    private final PersistentDataProvider persistentProvider;

    RaftActorDataPersistenceProvider(final DataPersistenceProvider delegate,
            final PersistentDataProvider persistentProvider) {
        super(delegate);
        this.persistentProvider = requireNonNull(persistentProvider);
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
        if (!getDelegate().isRecoveryApplicable() && entry instanceof ReplicatedLogEntry replicatedLogEntry
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
        } else if (async) {
            super.persistAsync(entry, procedure);
        } else {
            super.persist(entry, procedure);
        }
    }
}
