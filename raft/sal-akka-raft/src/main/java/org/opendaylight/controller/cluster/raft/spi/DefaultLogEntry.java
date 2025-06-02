/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.yangtools.concepts.Immutable;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Default {@link LogEntry} implementation.
 */
@NonNullByDefault
public record DefaultLogEntry(long index, long term, StateMachineCommand command) implements Immutable, LogEntry {
    public DefaultLogEntry {
        requireNonNull(command);
    }

    public static DefaultLogEntry of(final LogEntry entry) {
        return entry instanceof DefaultLogEntry dle ? dle
            : new DefaultLogEntry(entry.index(), entry.term(), entry.command());
    }

    public static DefaultLogEntry readFrom(final ObjectInput in) throws IOException, ClassNotFoundException {
        final var hdr = WritableObjects.readLongHeader(in);
        return new DefaultLogEntry(WritableObjects.readFirstLong(in, hdr),
            WritableObjects.readSecondLong(in, hdr), (StateMachineCommand) in.readObject());
    }

    public static void writeTo(final LogEntry entry, final ObjectOutput out) throws IOException {
        WritableObjects.writeLongs(out, entry.index(), entry.term());
        out.writeObject(entry.command().toSerialForm());
    }
}
