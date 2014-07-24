package org.opendaylight.controller.cluster.raft.protobuff.client.messages;


import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.raft.protobuff.messages.AppendEntriesMessages;

import java.util.Map;

/**
 * Created by kramesha on 7/22/14.
 */
public abstract class Payload {
    private String clientPayloadClassName;

    public String getClientPayloadClassName() {
        return this.getClass().getName();
    }

    public void setClientPayloadClassName(String clientPayloadClassName) {
        this.clientPayloadClassName = clientPayloadClassName;
    }

    /**
     * Override this method and ensure to return a map of
     * protocol buffer Generated extensions and their payload field values
     * The Generated Extension key would be Extension proto file created (extending AppendEntries)
     * @param <T>
     * @return Map of <GeneratedMessage.GeneratedExtension, T>
     */
    public abstract <T extends Object> Map<GeneratedMessage.GeneratedExtension, T> getProtoBuffExtensions();

    /**
     * Given a Protocol Buffer payload, populate the fields of the payload from the profobuff object
     * @param payloadProtoBuff
     * @return
     */
    public abstract Payload constructClientPayload (AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payloadProtoBuff);



}
