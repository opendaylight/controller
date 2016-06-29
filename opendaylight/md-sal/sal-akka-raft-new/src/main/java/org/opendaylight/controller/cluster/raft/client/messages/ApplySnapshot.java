package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

public class ApplySnapshot implements Serializable {
    private static final long serialVersionUID = 1L;
    private final byte[] snapshot;

    public ApplySnapshot(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public byte[] getSnapshot() {
        return snapshot;
    }
}
