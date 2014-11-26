/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example.messages;

import com.google.protobuf.GeneratedMessage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.example.KeyValueMessages;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

public class KeyValue extends Payload implements Serializable {
    private String key;
    private String value;

    public KeyValue() {
    }

    public KeyValue(String key, String value){
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override public String toString() {
        return "KeyValue{" +
            "key='" + key + '\'' +
            ", value='" + value + '\'' +
            '}';
    }

    // override this method to return  the protobuff related extension fields and their values
    @Override public Map<GeneratedMessage.GeneratedExtension, String> encode() {
        Map<GeneratedMessage.GeneratedExtension, String> map = new HashMap<>();
        map.put(KeyValueMessages.key, getKey());
        map.put(KeyValueMessages.value, getValue());
        return map;
    }

    // override this method to assign the values from protobuff
    @Override public Payload decode(
        AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payloadProtoBuff) {
        String key = payloadProtoBuff.getExtension(KeyValueMessages.key);
        String value = payloadProtoBuff.getExtension(KeyValueMessages.value);
        this.setKey(key);
        this.setValue(value);
        return this;
    }

    @Override
    public int size() {
        return this.value.length() + this.key.length();
    }

}
