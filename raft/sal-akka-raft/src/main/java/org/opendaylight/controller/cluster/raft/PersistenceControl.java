/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.controller.cluster.raft.spi.DisabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EnabledRaftStorage;
import org.opendaylight.controller.cluster.raft.spi.EntryJournal;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorage;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * Storage facade sitting between {@link RaftActor} and persistence provider, taking care of multiplexing between
 * persistent and non-persistent mode of operation.
 */
@NonNullByDefault
final class PersistenceControl extends PersistenceProvider {
    // FIXME: Allow this to be configured, end-to-end. May require restart to deal with journal lifecycle.
    private static final boolean DEFAULT_JOURNAL_MAPPED = true;

    private final DisabledRaftStorage disabledStorage;
    private final EnabledRaftStorage enabledStorage;

    private RaftStorage storage;

    private PersistenceControl(final DisabledRaftStorage disabledStorage, final EnabledRaftStorage enabledStorage) {
        super(disabledStorage, disabledStorage);
        this.enabledStorage = requireNonNull(enabledStorage);
        this.disabledStorage = requireNonNull(disabledStorage);
        storage = disabledStorage;
    }

    PersistenceControl(final RaftActor raftActor, final RaftStorageCompleter completer, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        this(new DisabledRaftStorage(completer, directory, compression, streamConfig),
            new EnabledRaftStorage(completer, directory, compression, streamConfig, DEFAULT_JOURNAL_MAPPED));
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

    /**
     * {@return the underlying EntryJournal, if available}
     */
    @Nullable EntryJournal journal() {
        return storage instanceof EnabledRaftStorage enabled ? enabled.journal() : null;
    }

    @Override
    RaftStorageCompleter completer() {
        return storage.completer();
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
        setStorage(newStorage, newStorage);
    }

    void saveVotingConfig(final @Nullable VotingConfig votingConfig) throws IOException {
        enabledStorage.saveSnapshot(new RaftSnapshot(votingConfig), EntryInfo.of(-1, -1), null, Instant.now());
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("storage", storage);
    }
}
