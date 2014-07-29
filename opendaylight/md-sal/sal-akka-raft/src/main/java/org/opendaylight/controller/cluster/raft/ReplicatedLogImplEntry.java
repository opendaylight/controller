/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

import java.io.Serializable;

public class ReplicatedLogImplEntry implements ReplicatedLogEntry,
    Serializable {

    private final long index;
    private final long term;
    private final Payload payload;

    public ReplicatedLogImplEntry(long index, long term, Payload payload) {

        this.index = index;
        this.term = term;
        this.payload = payload;
    }

    @Override public Payload getData() {
        return payload;
    }

    @Override public long getTerm() {
        return term;
    }

    @Override public long getIndex() {
        return index;
    }

    @Override public String toString() {
        return "Entry{" +
            "index=" + index +
            ", term=" + term +
            '}';
    }
}
