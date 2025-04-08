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
import java.nio.file.Path;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.ForwardingDataPersistenceProvider;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

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

    PersistenceControl(final RaftActor raftActor, final Path directory, final CompressionType compression,
            final Configuration streamConfig) {
        this(new DisabledRaftStorage(raftActor.memberId(), raftActor, directory, raftActor.self(), compression,
            streamConfig), new PekkoRaftStorage(raftActor, directory, compression, streamConfig));
    }

    void start() throws IOException {
        disabledStorage.start();
        try {
            enabledStorage.start();
        } catch (IOException e) {
            disabledStorage.stop();
            throw e;
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
            && entry.command() instanceof ClusterConfig serverConfig) {
            method.invoke(enabledStorage, serverConfig, p -> callback.accept(obj));
        } else  {
            method.invoke(delegate, obj, callback);
        }
    }
}
