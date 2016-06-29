package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

public class ApplySnapshot implements Serializable {
    private final byte[] snapshot;

    public ApplySnapshot(byte[] snapshot) {
        this.snapshot = snapshot;
    }

    public byte[] getSnapshot() {
        return snapshot;
    }
}
