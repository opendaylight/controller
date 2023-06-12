package org.opendaylight.controller.cluster.datastore;

import java.io.DataInput;
import java.io.DataOutput;

public interface PayloadHandler {
    void writeTo(DataOutput out, SerializablePayload message);
    SerializablePayload readFrom(DataInput in);
}
