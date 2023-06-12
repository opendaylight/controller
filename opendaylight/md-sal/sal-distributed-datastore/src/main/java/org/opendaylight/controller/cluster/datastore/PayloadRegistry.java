package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.messages.handlers.CommitTransactionPayloadSimpleHandler;
import org.opendaylight.controller.cluster.datastore.messages.handlers.CreateLocalHistoryPayload;
import org.opendaylight.controller.cluster.datastore.messages.handlers.PurgeTransactionPayloadHandler;
import org.opendaylight.controller.cluster.raft.messages.Payload;

import java.io.DataInput;
import java.io.DataOutput;

public class PayloadRegistry {


    public enum PayloadType {
        COMMIT_TRANSACTION_PAYLOAD_SIMPLE(new CommitTransactionPayloadSimpleHandler()),
        CREATE_LOCAL_HISTORY_PAYLOAD(new CreateLocalHistoryPayload()),
        PURGE_TRANSACTION_PAYLOAD(new PurgeTransactionPayloadHandler());
        private final PayloadHandler handler;

        PayloadType(final PayloadHandler handler) {
            this.handler = handler;
        }

        PayloadHandler getPayloadHandler(){
            return handler;
        };
    }

    public static Payload readPayloadFrom(DataInput in){
        return null;
    }

    public static void writePayloadTo(DataOutput out,SerializablePayload payload){

    }
}
