/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.OutputStream;
import java.util.Optional;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;

public class CaptureSnapshotReply {
    private final Snapshot.State snapshotState;
    private final Optional<OutputStream> installSnapshotStream;

    public CaptureSnapshotReply(final Snapshot.State snapshotState,
            final Optional<OutputStream> installSnapshotStream) {
        this.snapshotState = snapshotState;
        this.installSnapshotStream = installSnapshotStream;
    }

    public Snapshot.State getSnapshotState() {
        return snapshotState;
    }

    public Optional<OutputStream> getInstallSnapshotStream() {
        return installSnapshotStream;
    }
}
