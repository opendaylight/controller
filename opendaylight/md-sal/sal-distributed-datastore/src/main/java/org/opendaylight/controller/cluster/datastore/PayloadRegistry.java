package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.messages.handlers.CommitTransactionPayloadSimpleHandler;
import org.opendaylight.controller.cluster.datastore.messages.handlers.CreateLocalHistoryPayloadHandler;
import org.opendaylight.controller.cluster.datastore.messages.handlers.PurgeTransactionPayloadHandler;
import org.opendaylight.controller.cluster.raft.messages.Payload;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PayloadRegistry {


    public enum PayloadType {
        COMMIT_TRANSACTION_PAYLOAD_SIMPLE(new CommitTransactionPayloadSimpleHandler()),
        CREATE_LOCAL_HISTORY_PAYLOAD(new CreateLocalHistoryPayloadHandler()),
        PURGE_TRANSACTION_PAYLOAD(new PurgeTransactionPayloadHandler());
        private final PayloadHandler handler;

        PayloadType(final PayloadHandler handler) {
            this.handler = handler;
        }

        PayloadHandler getPayloadHandler(){
            return handler;
        };
    }

    public static Payload readPayloadFrom(DataInput in) throws IOException {
        byte payloadType = in.readByte();
        PayloadType type = PayloadType.values()[payloadType];
        return (Payload) type.getPayloadHandler().readFrom(in);
    }

    public static void writePayloadTo(DataOutput out,SerializablePayload payload) throws IOException {
        final PayloadType payloadType = payload.getPayloadType();
        out.writeByte(payloadType.ordinal());
        payloadType.getPayloadHandler().writeTo(out, payload);
    }
}
