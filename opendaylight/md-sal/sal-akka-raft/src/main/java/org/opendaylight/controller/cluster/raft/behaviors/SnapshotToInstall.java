/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteSource;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;

/**
 * A snapshot transferred from leader to a follower. Metadata matches information conveyed in {@link InstallSnapshot}.
 * This is an intermediary DTO transmitted via
 */
@NonNullByDefault
public record SnapshotToInstall(
        String leaderId,
        long term,
        ImmutableRaftEntryMeta lastEntry,
        ByteSource snapshot,
        @Nullable ServerConfigurationPayload serverConfig) {
    public SnapshotToInstall {
        requireNonNull(leaderId);
        requireNonNull(lastEntry);
        requireNonNull(snapshot);
        // TODO: sanity check term vs. lastEntry ?
    }
}
