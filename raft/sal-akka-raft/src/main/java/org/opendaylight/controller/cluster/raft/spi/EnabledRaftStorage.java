/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A {@link RaftStorage} backing persistent mode of {@link RaftActor} operation.
 */
@NonNullByDefault
public abstract non-sealed class EnabledRaftStorage extends RaftStorage {
    protected EnabledRaftStorage(final String memberId, final ExecuteInSelfActor executeInSelf, final Path directory,
            final CompressionType compression, final Configuration streamConfig) {
        super(memberId, executeInSelf, directory, compression, streamConfig);
    }

    @Override
    public final boolean isRecoveryApplicable() {
        return true;
    }

    /**
     * Persists a {@link VotingConfig} to the applicable journal synchronously. The contract is that the callback will
     * be invoked before {@link RaftActor} sees any other message.
     *
     * @param votingConfig the configuration to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: without callback and throwing IOException
    public abstract void persistVotingConfig(VotingConfig votingConfig, Consumer<VotingConfig> callback);

    /**
     * Persists a {@link VotingConfig} to the applicable journal synchronously. The contract is that the callback will
     * be invoked before {@link RaftActor} sees any other message.
     *
     * @param votingConfig the configuration to persist
     * @param callback the callback when persistence is complete
     */
    // FIXME: Callback<ReplicatedLogEntry> instead of Consumer
    public abstract void startPersistVotingConfig(VotingConfig votingConfig, Consumer<VotingConfig> callback);
}
