/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.Objects;
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
    private final Payload command;

    private boolean persistencePending;

    JournaledLogEntry(final long index, final long term, final Payload command) {
        this.index = index;
        this.term = term;
        this.command = requireNonNull(command);
    }

    static JournaledLogEntry of(final LogEntry entry) {
        return entry instanceof JournaledLogEntry simple && !simple.isPersistencePending() ? simple
            : new JournaledLogEntry(entry.index(), entry.term(), entry.command().toSerialForm());
    }

    @Override
    public Payload command() {
        return command;
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
        return command.size();
    }

    @Override
    public int serializedSize() {
        // Assumes WritableObjects.writeLongs() for index/term
        return 17 + command.serializedSize();
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
        return Objects.hash(index, term, command);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof JournaledLogEntry other && index == other.index
            && term == other.term && command.equals(other.command);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("index", index)
            .add("term", term)
            .add("command", command)
            .toString();
    }
}
