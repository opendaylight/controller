/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryStore;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorage;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;
import org.opendaylight.raft.spi.InstallableSnapshot;

/**
 * Storage facade sitting between {@link RaftActor} and persistence provider, taking care of multiplexing between
 * persistent and non-persistent mode of operation.
 */
@NonNullByDefault
final class PersistenceControl implements DataPersistenceProvider, TestablePersistence {
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

    private SnapshotStore snapshotStore;
    private EntryStore entryStore;
    private RaftStorage storage;

    @VisibleForTesting
    PersistenceControl(final DisabledRaftStorage disabledStorage, final EnabledRaftStorage enabledStorage) {
        this.enabledStorage = requireNonNull(enabledStorage);
        this.disabledStorage = requireNonNull(disabledStorage);
        storage = disabledStorage;
        entryStore = disabledStorage;
        snapshotStore = disabledStorage;
    }

    PersistenceControl(final RaftActor raftActor, final Path directory, final CompressionType compression,
            final Configuration streamConfig) {
        this(new DisabledRaftStorage(raftActor.memberId(), raftActor, directory, compression, streamConfig),
            new PekkoRaftStorage(raftActor, directory, compression, streamConfig));
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

    boolean isRecoveryApplicable() {
        return storage instanceof EnabledRaftStorage;
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        entryStore.persistEntry(entry, callback);
    }

    @Override
    public void startPersistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        entryStore.startPersistEntry(entry, callback);
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        entryStore.deleteEntries(fromIndex);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        entryStore.deleteMessages(sequenceNumber);
    }

    @Override
    public long lastSequenceNumber() {
        return entryStore.lastSequenceNumber();
    }

    @Override
    public void markLastApplied(final long lastAppliedIndex) {
        entryStore.markLastApplied(lastAppliedIndex);
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return entryStore.handleJournalResponse(response);
    }

    @Override
    public @Nullable SnapshotFile lastSnapshot() throws IOException {
        return snapshotStore.lastSnapshot();
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        snapshotStore.saveSnapshot(raftSnapshot, lastIncluded, snapshot, callback);
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final Instant timestamp) throws IOException {
        snapshotStore.saveSnapshot(raftSnapshot, lastIncluded, snapshot, timestamp);
    }

    @Override
    public void streamToInstall(final EntryInfo lastIncluded, final ToStorage<?> snapshot,
            final RaftCallback<InstallableSnapshot> callback) {
        snapshotStore.streamToInstall(lastIncluded, snapshot, callback);
    }

    @Override
    public void retainSnapshots(final Instant firstRetained) {
        snapshotStore.retainSnapshots(firstRetained);
    }

    @Override
    public EntryStore entryStore() {
        return entryStore;
    }

    @Override
    public SnapshotStore snapshotStore() {
        return snapshotStore;
    }

    @Override
    public <T extends SnapshotStore> T decorateSnapshotStore(
            final BiFunction<SnapshotStore, ExecuteInSelfActor, T> factory) {
        final var ret = verifyNotNull(factory.apply(snapshotStore, storage.actor()));
        snapshotStore = ret;
        return ret;
    }

    @Override
    public <T extends EntryStore> T decorateEntryStore(final BiFunction<EntryStore, ExecuteInSelfActor, T> factory) {
        final var ret = verifyNotNull(factory.apply(entryStore, storage.actor()));
        entryStore = ret;
        return ret;
    }

    boolean becomePersistent() {
        return switch (storage) {
            case DisabledRaftStorage disabled -> {
                setStorage(enabledStorage);
                yield true;
            }
            case EnabledRaftStorage enabled -> false;
        };
    }

    void becomeTransient() {
        switch (storage) {
            case DisabledRaftStorage disabled -> {
                // no-op
            }
            case EnabledRaftStorage enabled -> setStorage(disabledStorage);
        }
    }

    private void setStorage(final RaftStorage newStorage) {
        storage = newStorage;
        entryStore = newStorage;
        snapshotStore = newStorage;
    }

    void saveVotingConfig(final @Nullable VotingConfig votingConfig) throws IOException {
        enabledStorage.saveSnapshot(new RaftSnapshot(votingConfig, List.of()), EntryInfo.of(-1, -1), null,
            Instant.now());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("storage", storage).toString();
    }
}
