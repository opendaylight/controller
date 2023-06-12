package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.PayloadRegistry.PayloadType;

import java.io.DataOutput;

public class CommitTransactionPayload {

    void writeTo(DataOutput out){}
    PayloadType getPayloadType(){
        return PayloadType.COMMIT_TRANSACTION_PAYLOAD_SIMPLE;
    }
}
