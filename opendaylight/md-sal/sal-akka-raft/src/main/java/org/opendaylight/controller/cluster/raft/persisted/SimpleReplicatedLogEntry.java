/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private long index;
        private long term;
        private Payload data;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final SimpleReplicatedLogEntry replicatedLogEntry) {
            index = replicatedLogEntry.getIndex();
            term = replicatedLogEntry.getTerm();
            data = replicatedLogEntry.getData();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(index);
            out.writeLong(term);
            out.writeObject(data);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            index = in.readLong();
            term = in.readLong();
            data = (Payload) in.readObject();
        }

        private Object readResolve() {
            return new SimpleReplicatedLogEntry(index, term, data);
        }
    }

    private static final long serialVersionUID = 1L;
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative
    private static final int PROXY_SIZE = SerializationUtils.serialize(new Proxy()).length;

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
    public long getTerm() {
        return term;
    }

    @Override
    public long getIndex() {
        return index;
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

    @Override
    public void setPersistencePending(final boolean pending) {
        persistencePending = pending;
    }

    private Object writeReplace() {
        return new Proxy(this);
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
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        SimpleReplicatedLogEntry other = (SimpleReplicatedLogEntry) obj;
        return index == other.index && term == other.term && payload.equals(other.payload);
    }

    @Override
    public String toString() {
        return "SimpleReplicatedLogEntry [index=" + index + ", term=" + term + ", payload=" + payload + "]";
    }
}
