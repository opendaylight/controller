/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.example.messages;

import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.example.protobuff.messages.KeyValueMessages;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.messages.AppendEntriesMessages;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    @Override public Map<GeneratedMessage.GeneratedExtension, String> getProtoBuffExtensions() {
        Map<GeneratedMessage.GeneratedExtension, String> map = new HashMap<>();
        map.put(KeyValueMessages.key, getKey());
        map.put(KeyValueMessages.value, getValue());
        return map;
    }

    @Override public Payload constructClientPayload(AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payloadProtoBuff) {
        String key = payloadProtoBuff.getExtension(KeyValueMessages.key);
        String value = payloadProtoBuff.getExtension(KeyValueMessages.value);
        this.setKey(key);
        this.setValue(value);
        return this;
    }

}
