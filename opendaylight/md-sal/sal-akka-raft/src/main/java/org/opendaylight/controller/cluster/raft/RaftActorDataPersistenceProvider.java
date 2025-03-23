/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.ForwardingDataPersistenceProvider;

/**
 * The DelegatingPersistentDataProvider used by RaftActor to override the configured persistent provider to
 * persist ReplicatedLogEntry's based on whether or not the payload is a PersistentPayload instance.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
final class RaftActorDataPersistenceProvider extends ForwardingDataPersistenceProvider {
    private final PersistentDataProvider persistentProvider;
    private final NonPersistentDataProvider transientProvider;

    private DataPersistenceProvider delegate;

    @VisibleForTesting
    RaftActorDataPersistenceProvider(final PersistentDataProvider persistentProvider,
            final NonPersistentDataProvider transientProvider) {
        this.persistentProvider = requireNonNull(persistentProvider);
        this.transientProvider = requireNonNull(transientProvider);
        delegate = transientProvider;
    }

    RaftActorDataPersistenceProvider(final RaftActor raftActor) {
        this(new PersistentDataProvider(raftActor), new TransientDataProvider(raftActor));
    }

    @Override
    protected DataPersistenceProvider delegate() {
        return delegate;
    }

    boolean becomePersistent() {
        if (delegate.isRecoveryApplicable()) {
            return false;
        }
        delegate = persistentProvider;
        return true;
    }

    void becomeTransient() {
        delegate = transientProvider;
    }

    @Deprecated
    void setDelegate(final DataPersistenceProvider delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        doPersist(entry, callback, false);
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        doPersist(entry, callback, true);
    }

    private <T> void doPersist(final T entry, final Consumer<T> callback, final boolean async) {
        if (!delegate.isRecoveryApplicable() && entry instanceof ReplicatedLogEntry replicatedLogEntry
            && replicatedLogEntry.getData() instanceof ClusterConfig serverConfig) {
            // TODO: revisit this statement with EntryStore
            //
            //   We persist the ClusterConfig but not the ReplicatedLogEntry to avoid gaps in the journal indexes
            //   on recovery if data persistence is later enabled.
            if (async) {
                persistentProvider.persistAsync(serverConfig, p -> callback.accept(entry));
            } else {
                persistentProvider.persist(serverConfig, p -> callback.accept(entry));
            }
            return;
        }

        if (async) {
            delegate.persistAsync(entry, callback);
        } else {
            delegate.persist(entry, callback);
        }
    }
}
