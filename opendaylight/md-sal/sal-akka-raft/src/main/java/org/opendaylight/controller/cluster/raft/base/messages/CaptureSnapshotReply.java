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

    // Suppresses the FindBugs warning about storing a reference to an externally mutable object into the internal
    // representation of this object. This is fine in this case since this class is merely a DTO message and does not
    // process 'snapshot' internally. Also it would be inefficient to create a copy as the byte[] could be large.
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public CaptureSnapshotReply(byte [] snapshot) {
        this.snapshot = snapshot;
    }

    // Suppresses the FindBugs warning about exposing the internal representation of a mutable object value stored in
    // a field. This is fine in this case since this class is merely a DTO message and does not process 'snapshot'
    // internally. Also it would be inefficient to create a return copy as the byte[] could be large.
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte [] getSnapshot() {
        return snapshot;
    }
}
