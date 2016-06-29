package org.opendaylight.controller.cluster.raft.client.messages;

import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

import java.io.Serializable;
import java.util.List;

public class ApplyLogRecoveryBatchRequest implements Serializable {
    private final List<Payload> payloads;

    public ApplyLogRecoveryBatchRequest(List<Payload> payloads) {
        this.payloads = payloads;
    }

    public List<Payload> getPayloads() {
        return payloads;
    }
}
