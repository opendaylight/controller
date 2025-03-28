/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.ForwardingDataPersistenceProvider;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.SnapshotFileFormat;

/**
 * Storage facade sitting between {@link RaftActor} and persistence provider, taking care of multiplexing between
 * persistent and non-persistent mode of operation.
 */
@NonNullByDefault
final class PersistenceControl extends ForwardingDataPersistenceProvider {
    /**
     * A bridge for dispatching to either {@link DataPersistenceProvider#persist(Object, Consumer)} or
     * {@link DataPersistenceProvider#persistAsync(Object, Consumer)}.
     */
    @FunctionalInterface
    private interface PersistMethod {

        <T> void invoke(DataPersistenceProvider provider, T entry, Consumer<T> callback);
    }

    private final DisabledRaftStorage disabledStorage;
    private final EnabledRaftStorage enabledStorage;

    private DataPersistenceProvider delegate;

    @VisibleForTesting
    PersistenceControl(final DisabledRaftStorage disabledStorage, final EnabledRaftStorage enabledStorage) {
        this.enabledStorage = requireNonNull(enabledStorage);
        this.disabledStorage = requireNonNull(disabledStorage);
        delegate = disabledStorage;
    }

    PersistenceControl(final RaftActor raftActor, final SnapshotFileFormat preferredFormat,
            final Configuration streamConfig) {
        this(new DisabledRaftStorage(raftActor.memberId(), raftActor, raftActor.self(), preferredFormat, streamConfig),
            new PekkoRaftStorage(raftActor, preferredFormat, streamConfig));
    }

    void start() throws IOException {
        try {
            disabledStorage.start();
        } catch (IOException de) {
            try {
                enabledStorage.start();
            } catch (IOException ee) {
                ee.addSuppressed(de);
                throw ee;
            }
            throw de;
        }
    }

    void stop() {
        enabledStorage.stop();
        disabledStorage.stop();
    }

    @Override
    protected DataPersistenceProvider delegate() {
        return delegate;
    }

    boolean becomePersistent() {
        if (delegate.isRecoveryApplicable()) {
            return false;
        }
        delegate = enabledStorage;
        return true;
    }

    void becomeTransient() {
        delegate = disabledStorage;
    }

    @Deprecated
    void setDelegate(final DataPersistenceProvider delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public <T> void persist(final T entry, final Consumer<T> callback) {
        callPersist(DataPersistenceProvider::persist, entry, callback);
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        callPersist(DataPersistenceProvider::persistAsync, entry, callback);
    }

    private <T> void callPersist(final PersistMethod method, final T obj, final Consumer<T> callback) {
        // TODO: revisit this statement with EntryStore
        //
        //   We persist the ClusterConfig but not the ReplicatedLogEntry to avoid gaps in the journal indexes
        //   on recovery if data persistence is later enabled.
        if (!delegate.isRecoveryApplicable() && obj instanceof ReplicatedLogEntry entry
            && entry.getData() instanceof ClusterConfig serverConfig) {
            method.invoke(enabledStorage, serverConfig, p -> callback.accept(obj));
        } else  {
            method.invoke(delegate, obj, callback);
        }
    }
}
