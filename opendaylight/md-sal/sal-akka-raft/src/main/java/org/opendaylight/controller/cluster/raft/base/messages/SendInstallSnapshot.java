/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

/**
 * Internal message sent from the SnapshotManager to its associated leader when a snapshot capture is complete to
 * prompt the leader to install the snapshot on its followers as needed.
 */
public final class SendInstallSnapshot {
    private final Snapshot snapshot;
    private final ByteSource snapshotBytes;

    public SendInstallSnapshot(@Nonnull Snapshot snapshot, @Nonnull ByteSource snapshotBytes) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
        this.snapshotBytes = Preconditions.checkNotNull(snapshotBytes);
    }

    @Nonnull
    public Snapshot getSnapshot() {
        return snapshot;
    }

    public ByteSource getSnapshotBytes() {
        return snapshotBytes;
    }
}
