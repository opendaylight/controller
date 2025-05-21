/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A {@link RaftStorage} backing persistent mode of {@link RaftActor} operation.
 */
@NonNullByDefault
public abstract non-sealed class EnabledRaftStorage extends RaftStorage {
    /**
     * An entry loaded from an {@link EntryStore}.
     */
    public sealed interface LoadedEntry {

        long journalIndex();
    }

    /**
     * An update to {@code lastApplied} index loaded from an {@link EntryStore}.
     */
    public record LoadedLastApplied(long journalIndex, long lastApplied) implements LoadedEntry {
        // Nothing else
    }

    /**
     * A {@link LogEntry} loaded from an {@link EntryStore}.
     */
    public record LoadedLogEntry(long journalIndex, long index, long term, StateMachineCommand command)
            implements LoadedEntry, LogEntry {
        public LoadedLogEntry {
            requireNonNull(command);
        }
    }

    protected EnabledRaftStorage(final String memberId, final ExecuteInSelfActor executeInSelf, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        super(memberId, executeInSelf, directory, compression, streamConfig);
    }

    @Override
    public final boolean isRecoveryApplicable() {
        return true;
    }
}
