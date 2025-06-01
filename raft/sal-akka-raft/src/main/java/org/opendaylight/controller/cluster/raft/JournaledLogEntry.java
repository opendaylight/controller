/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;

/**
 * A {@link ReplicatedLogEntry} implementation used by {@link ReplicatedLogImpl}.
 */
@NonNullByDefault
final class JournaledLogEntry implements ReplicatedLogEntry {
    private final long index;
    private final long term;
    private final Payload payload;

    private boolean persistencePending;

    JournaledLogEntry(final long index, final long term, final Payload payload) {
        this.index = index;
        this.term = term;
        this.payload = requireNonNull(payload);
    }

    static JournaledLogEntry of(final LogEntry entry) {
        return entry instanceof JournaledLogEntry simple && !simple.isPersistencePending() ? simple
            : new JournaledLogEntry(entry.index(), entry.term(), entry.command().toSerialForm());
    }

    @Override
    public Payload command() {
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
        return 17 + payload.serializedSize();
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
    void setPersistencePending(final boolean pending) {
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
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof JournaledLogEntry other && index == other.index
            && term == other.term && payload.equals(other.payload);
    }

    @Override
    public String toString() {
        return "JournaledLogEntry [index=" + index + ", term=" + term + ", payload=" + payload + "]";
    }
}
