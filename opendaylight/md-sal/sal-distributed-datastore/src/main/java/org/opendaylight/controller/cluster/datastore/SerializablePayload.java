package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.PayloadRegistry.PayloadType;

import java.io.DataInput;

public interface SerializablePayload {
    PayloadType getPayloadType();
    SerializablePayload readPayloadFrom(DataInput in);

}
