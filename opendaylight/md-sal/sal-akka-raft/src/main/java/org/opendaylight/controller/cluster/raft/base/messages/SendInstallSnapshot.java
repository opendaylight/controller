/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.Snapshot;

/**
 * Internal message sent from the SnapshotManager to its associated leader. The leader is expected to apply the
 * {@link Snapshot} to its state.
 */
public final class SendInstallSnapshot {
    private final Snapshot snapshot;

    public SendInstallSnapshot(@Nonnull Snapshot snapshot) {
        this.snapshot = Preconditions.checkNotNull(snapshot);
    }

    @Nonnull public Snapshot getSnapshot() {
        return snapshot;
    }
}
