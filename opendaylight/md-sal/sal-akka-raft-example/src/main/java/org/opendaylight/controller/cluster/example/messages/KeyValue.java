/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.messages.Payload;

public final class KeyValue extends Payload {
    private static final long serialVersionUID = 1L;

    private String key;
    private String value;

    public KeyValue() {
    }

    public KeyValue(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int size() {
        return value.length() + key.length();
    }

    @Override
    public int serializedSize() {
        // Should be a better estimate
        return size();
    }

    @Override
    public String toString() {
        return "KeyValue{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }

    @Override
    protected Object writeReplace() {
        return new KVv1(value, key);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeUTF(key);
        out.writeUTF(value);
    }

    public static @NonNull KeyValue readFrom(final DataInput in) throws IOException {
        final String key = in.readUTF();
        final String value = in.readUTF();
        return new KeyValue(key, value);
    }
}
