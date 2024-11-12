/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.io.ByteSource;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.SnapshotManager;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;

/**
 * Internal message, issued by follower behavior to its actor, eventually routed to {@link SnapshotManager}. Metadata
 * matches information conveyed in {@link InstallSnapshot}.
 */
@Beta
@NonNullByDefault
public record ApplyLeaderSnapshot(
        String leaderId,
        long term,
        ImmutableRaftEntryMeta lastEntry,
        ByteSource snapshot,
        @Nullable ServerConfigurationPayload serverConfig,
        ApplyLeaderSnapshot.Callback callback) {
    public ApplyLeaderSnapshot {
        requireNonNull(leaderId);
        requireNonNull(lastEntry);
        requireNonNull(snapshot);
        requireNonNull(callback);
        // TODO: sanity check term vs. lastEntry ?
    }

    public interface Callback {

        void onSuccess();

        void onFailure();
    }
}
