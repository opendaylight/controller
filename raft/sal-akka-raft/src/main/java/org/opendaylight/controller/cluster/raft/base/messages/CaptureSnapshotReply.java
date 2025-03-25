/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

public final class CaptureSnapshotReply implements ControlMessage {
    private final Snapshot.@NonNull State snapshotState;
    private final @Nullable OutputStream installSnapshotStream;

    public CaptureSnapshotReply(final Snapshot.@NonNull State snapshotState,
            final @Nullable OutputStream installSnapshotStream) {
        this.snapshotState = requireNonNull(snapshotState);
        this.installSnapshotStream = installSnapshotStream;
    }

    public Snapshot.@NonNull State snapshotState() {
        return snapshotState;
    }

    public @Nullable OutputStream installSnapshotStream() {
        return installSnapshotStream;
    }
}
