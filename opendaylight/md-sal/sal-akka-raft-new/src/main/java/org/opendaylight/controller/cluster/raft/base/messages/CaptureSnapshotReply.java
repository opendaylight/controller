/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;


public class CaptureSnapshotReply {
    private final byte [] snapshot;

    public CaptureSnapshotReply(byte [] snapshot) {
        this.snapshot = snapshot;
    }

    public byte [] getSnapshot() {
        return snapshot;
    }
}
