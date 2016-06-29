package org.opendaylight.controller.cluster.raft.client.messages;

import java.io.Serializable;

public class ApplyLogRecoveryBatchReply implements Serializable {
    private static final long serialVersionUID = 1L;
    public final static ApplyLogRecoveryBatchReply INSTANCE =
            new ApplyLogRecoveryBatchReply();

    private ApplyLogRecoveryBatchReply() {
    }
}
