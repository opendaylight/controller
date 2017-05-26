/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.Preconditions;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * A {@link ReplicatedLogEntry} implementation.
 *
 * @author Thomas Pantelis
 */
public final class SimpleReplicatedLogEntry implements ReplicatedLogEntry, MigratedSerializable {
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
    private final boolean migrated;

    private SimpleReplicatedLogEntry(long index, long term, Payload payload, boolean migrated) {
        this.index = index;
        this.term = term;
        this.payload = Preconditions.checkNotNull(payload);
        this.migrated = migrated;
    }

    /**
     * Constructs an instance.
     *
     * @param index the index
     * @param term the term
     * @param payload the payload
     */
    public SimpleReplicatedLogEntry(final long index, final long term, final Payload payload) {
        this(index, term, payload, false);
    }

    @Deprecated
    public static ReplicatedLogEntry createMigrated(final long index, final long term, final Payload payload) {
        return new SimpleReplicatedLogEntry(index, term, payload, true);
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
    public void setPersistencePending(boolean pending) {
        persistencePending = pending;
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    @Override
    public Object writeReplace() {
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
    public boolean equals(Object obj) {
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
