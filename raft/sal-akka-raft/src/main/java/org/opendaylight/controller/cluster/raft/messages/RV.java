/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link RequestVote}.
 */
final class RV implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private RequestVote requestVote;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public RV() {
        // For Externalizable
    }

    RV(final RequestVote requestVote) {
        this.requestVote = requireNonNull(requestVote);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, requestVote.getTerm());
        out.writeObject(requestVote.getCandidateId());
        WritableObjects.writeLongs(out, requestVote.getLastLogIndex(), requestVote.getLastLogTerm());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        long term = WritableObjects.readLong(in);
        String candidateId = (String) in.readObject();

        final byte hdr = WritableObjects.readLongHeader(in);
        long lastLogIndex = WritableObjects.readFirstLong(in, hdr);
        long lastLogTerm = WritableObjects.readSecondLong(in, hdr);

        requestVote = new RequestVote(term, candidateId, lastLogIndex, lastLogTerm);
    }

    @java.io.Serial
    private Object readResolve() {
        return requestVote;
    }
}
