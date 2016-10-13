/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class CaptureSnapshotReply {
    private final byte [] snapshot;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Stores a reference to an externally mutable byte[] "
            + "object but this is OK since this class is merely a DTO and does not process byte[] internally. "
            + "Also it would be inefficient to create a copy as the byte[] could be large.")
    public CaptureSnapshotReply(byte [] snapshot) {
        this.snapshot = snapshot;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposes a mutable object stored in a field but "
            + "this is OK since this class is merely a DTO and does not process the byte[] internally. "
            + "Also it would be inefficient to create a return copy as the byte[] could be large.")
    public byte [] getSnapshot() {
        return snapshot;
    }
}
