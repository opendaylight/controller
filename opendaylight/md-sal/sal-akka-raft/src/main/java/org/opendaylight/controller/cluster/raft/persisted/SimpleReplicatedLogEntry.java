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

        private ReplicatedLogEntry replicatedLogEntry;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ReplicatedLogEntry replicatedLogEntry) {
            this.replicatedLogEntry = replicatedLogEntry;
        }

        static int estimatedSerializedSize(final ReplicatedLogEntry replicatedLogEntry) {
            return 8 /* index */ + 8 /* term */ + replicatedLogEntry.getData().size()
                    + 400 /* estimated extra padding for class info */;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(replicatedLogEntry.getIndex());
            out.writeLong(replicatedLogEntry.getTerm());
            out.writeObject(replicatedLogEntry.getData());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            replicatedLogEntry = new SimpleReplicatedLogEntry(in.readLong(), in.readLong(), (Payload) in.readObject());
        }

        private Object readResolve() {
            return replicatedLogEntry;
        }
    }

    private static final long serialVersionUID = 1L;

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
        return getData().size();
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

    public int estimatedSerializedSize() {
        return Proxy.estimatedSerializedSize(this);
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
