package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

public class RecoveryComplete implements Serializable {
    private static final long serialVersionUID = 1L;
    public final static RecoveryComplete INSTANCE = new RecoveryComplete();
    private RecoveryComplete() {
        // For force reuse!
    }
}
