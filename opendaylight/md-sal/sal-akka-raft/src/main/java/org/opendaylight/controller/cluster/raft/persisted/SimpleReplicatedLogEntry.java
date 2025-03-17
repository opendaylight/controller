/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * A {@link ReplicatedLogEntry} implementation.
 *
 * @author Thomas Pantelis
 */
public final class SimpleReplicatedLogEntry implements ReplicatedLogEntry, Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative.
    private static final int PROXY_SIZE = SerializationUtils.serialize(new LE((Void) null)).length;

    private final long index;
    private final long term;
    private final Payload payload;
    private boolean persistencePending;

    /**
     * Constructs an instance.
     *
     * @param index the index
     * @param term the term
     * @param payload the payload
     */
    public SimpleReplicatedLogEntry(final long index, final long term, final Payload payload) {
        this.index = index;
        this.term = term;
        this.payload = requireNonNull(payload);
    }

    @Override
    public Payload getData() {
        return payload;
    }

    @Override
    public long index() {
        return index;
    }

    @Override
    public long term() {
        return term;
    }

    @Override
    public int size() {
        return payload.size();
    }

    @Override
    public int serializedSize() {
        return PROXY_SIZE + payload.serializedSize();
    }

    @Override
    public boolean isPersistencePending() {
        return persistencePending;
    }

    /**
     * Sets whether or not persistence is pending for this entry.
     *
     * @param pending the new setting.
     */
    public void setPersistencePending(final boolean pending) {
        persistencePending = pending;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + payload.hashCode();
        result = prime * result + (int) (index ^ index >>> 32);
        result = prime * result + (int) (term ^ term >>> 32);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof SimpleReplicatedLogEntry other && index == other.index
            && term == other.term && payload.equals(other.payload);
    }

    @Override
    public String toString() {
        return "SimpleReplicatedLogEntry [index=" + index + ", term=" + term + ", payload=" + payload + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new LE(this);
    }
}
