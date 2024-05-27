/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteSource;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Internal message sent from the SnapshotManager to its associated leader when a snapshot capture is complete to
 * prompt the leader to install the snapshot on its followers as needed.
 */
public final class SendInstallSnapshot {
    private final @NonNull Snapshot snapshot;
    private final @NonNull ByteSource snapshotBytes;

    public SendInstallSnapshot(@NonNull Snapshot snapshot, @NonNull ByteSource snapshotBytes) {
        this.snapshot = requireNonNull(snapshot);
        this.snapshotBytes = requireNonNull(snapshotBytes);
    }

    public @NonNull Snapshot getSnapshot() {
        return snapshot;
    }

    public @NonNull ByteSource getSnapshotBytes() {
        return snapshotBytes;
    }
}
