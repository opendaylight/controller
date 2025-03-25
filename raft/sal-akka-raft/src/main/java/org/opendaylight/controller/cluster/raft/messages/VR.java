/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link RequestVoteReply}.
 */
final class VR implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // Flags
    private static final int VOTE_GRANTED = 0x10;

    private RequestVoteReply requestVoteReply;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public VR() {
        // For Externalizable
    }

    VR(final RequestVoteReply requestVoteReply) {
        this.requestVoteReply = requireNonNull(requestVoteReply);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, requestVoteReply.getTerm(), requestVoteReply.isVoteGranted() ? VOTE_GRANTED : 0);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException {
        final byte hdr = WritableObjects.readLongHeader(in);
        requestVoteReply = new RequestVoteReply(WritableObjects.readLongBody(in, hdr),
            (WritableObjects.longHeaderFlags(hdr) & VOTE_GRANTED) != 0);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(requestVoteReply);
    }
}
