/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * A {@link ReplicatedLogEntry} implementation.
 *
 * @deprecated Use {@link org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry} instead.
 */
@Deprecated
public class ReplicatedLogImplEntry implements ReplicatedLogEntry, Serializable {
    private static final long serialVersionUID = -9085798014576489130L;

    private final long index;
    private final long term;
    private final Payload payload;
    private transient boolean persistencePending = false;

    /**
     * Constructs an instance.
     *
     * @param index the index
     * @param term the term
     * @param payload the payload
     */
    public ReplicatedLogImplEntry(final long index, final long term, final Payload payload) {
        this.index = index;
        this.term = term;
        this.payload = Preconditions.checkNotNull(payload);
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

    private Object readResolve() {
        return org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry.createMigrated(
                index, term, payload);
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

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ReplicatedLogImplEntry other = (ReplicatedLogImplEntry) obj;
        if (payload == null) {
            if (other.payload != null) {
                return false;
            }
        } else if (!payload.equals(other.payload)) {
            return false;
        }

        if (index != other.index) {
            return false;
        }

        if (term != other.term) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Entry{index=" + index + ", term=" + term + '}';
    }
}
