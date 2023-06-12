package org.opendaylight.controller.cluster.datastore.messages.handlers;


import org.opendaylight.controller.cluster.datastore.PayloadHandler;
import org.opendaylight.controller.cluster.datastore.SerializablePayload;


import java.io.DataInput;
import java.io.DataOutput;


public class CreateLocalHistoryPayload implements PayloadHandler {
    @Override
    public void writeTo(DataOutput out, SerializablePayload message) {
    }

    @Override
    public SerializablePayload readFrom(DataInput in) {
        return null;
    }
}
