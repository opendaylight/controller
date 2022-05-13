/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example.messages;

import org.opendaylight.controller.cluster.raft.messages.Payload;

public final class KeyValue extends Payload {
    private static final long serialVersionUID = 1L;

    private String key;
    private String value;

    public KeyValue() {
    }

    public KeyValue(String key, String value) {
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
    public String toString() {
        return "KeyValue{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }

    @Override
    public int size() {
        return value.length() + key.length();
    }

    @Override
    protected Object writeReplace() {
        return new KVv1(value, key);
    }
}
