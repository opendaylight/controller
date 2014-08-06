/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import com.google.protobuf.ByteString;

public class CaptureSnapshotReply {
    private ByteString snapshot;

    public CaptureSnapshotReply(ByteString snapshot) {
        this.snapshot = snapshot;
    }

    public ByteString getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ByteString snapshot) {
        this.snapshot = snapshot;
    }
}
