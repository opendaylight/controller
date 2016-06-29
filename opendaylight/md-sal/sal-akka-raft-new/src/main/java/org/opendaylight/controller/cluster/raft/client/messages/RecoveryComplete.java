package org.opendaylight.controller.cluster.raft.client.messages;

/**
 * Created by django on 7/1/16.
 */
public class RecoveryComplete {
    public final static RecoveryComplete INSTANCE = new RecoveryComplete();
    private RecoveryComplete() {
        // For force reuse!
    }
}
