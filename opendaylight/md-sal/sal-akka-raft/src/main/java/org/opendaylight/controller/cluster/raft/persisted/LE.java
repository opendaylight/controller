/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link SimpleReplicatedLogEntry}.
 */
final class LE implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private long index;
    private long term;
    private Payload data;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public LE() {
        // For Externalizable
    }

    // For size estimation only, use full bit size
    LE(final Void dummy) {
        index = Long.MIN_VALUE;
        term = Long.MIN_VALUE;
        data = null;
    }

    LE(final SimpleReplicatedLogEntry logEntry) {
        index = logEntry.index();
        term = logEntry.term();
        data = logEntry.getData();
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLongs(out, index, term);
        out.writeObject(data);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final byte hdr = WritableObjects.readLongHeader(in);
        index = WritableObjects.readFirstLong(in, hdr);
        term = WritableObjects.readSecondLong(in, hdr);
        data = (Payload) in.readObject();
    }

    @java.io.Serial
    private Object readResolve() {
        return new SimpleReplicatedLogEntry(index, term, data);
    }
}
