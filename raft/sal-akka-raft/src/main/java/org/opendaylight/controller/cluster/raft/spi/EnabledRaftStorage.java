/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.common.actor.ExecuteInSelfActor;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.raft.spi.CompressionSupport;
import org.opendaylight.raft.spi.FileBackedOutputStream.Configuration;

/**
 * A {@link RaftStorage} backing persistent mode of {@link RaftActor} operation.
 */
@NonNullByDefault
public abstract non-sealed class EnabledRaftStorage extends RaftStorage {
    protected EnabledRaftStorage(final ExecuteInSelfActor executeInSelf, final CompressionSupport compression,
            final Configuration streamConfig) {
        super(executeInSelf, compression, streamConfig);
    }

    @Override
    public final boolean isRecoveryApplicable() {
        return true;
    }
}
