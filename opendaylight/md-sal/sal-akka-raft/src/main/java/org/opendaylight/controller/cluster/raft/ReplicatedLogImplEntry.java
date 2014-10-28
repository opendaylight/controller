/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ReplicatedLogImplEntry implements ReplicatedLogEntry {
    private static final long serialVersionUID = 1L;

    private transient long index;
    private transient long term;
    private transient Payload payload;

    public ReplicatedLogImplEntry() {
    }

    public ReplicatedLogImplEntry(long index, long term, Payload payload) {

        this.index = index;
        this.term = term;
        this.payload = payload;
    }

    @Override
    public Payload getData() {
        return payload;
    }

    @Override
    public long getTerm() {
        return term;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        index = in.readLong();
        term = in.readLong();
        payload = (Payload) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(index);
        out.writeLong(term);
        out.writeObject(payload);
    }

    @Override
    public String toString() {
        return "ReplicatedLogImplEntry [index=" + index + ", term=" + term + ", payload=" + payload + "]";
    }
}
