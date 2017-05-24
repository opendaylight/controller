/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import akka.dispatch.ControlMessage;
import com.google.common.base.Preconditions;
import java.io.OutputStream;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

public class CaptureSnapshotReply implements ControlMessage {
    private final Snapshot.State snapshotState;
    private final Optional<OutputStream> installSnapshotStream;

    public CaptureSnapshotReply(@Nonnull final Snapshot.State snapshotState,
            @Nonnull final Optional<OutputStream> installSnapshotStream) {
        this.snapshotState = Preconditions.checkNotNull(snapshotState);
        this.installSnapshotStream = Preconditions.checkNotNull(installSnapshotStream);
    }

    @Nonnull
    public Snapshot.State getSnapshotState() {
        return snapshotState;
    }

    @Nonnull
    public Optional<OutputStream> getInstallSnapshotStream() {
        return installSnapshotStream;
    }
}
