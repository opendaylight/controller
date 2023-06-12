/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.Verify;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.persistence.PayloadHandler;
import org.opendaylight.controller.cluster.persistence.SerializablePayload;

public class UpdateElectionTermPayloadHandler implements PayloadHandler {
    @Override
    public void writeTo(final DataOutput out, final SerializablePayload payload) throws IOException {
        Verify.verify(payload instanceof UpdateElectionTerm);
        final UpdateElectionTerm updateElectionTerm = (UpdateElectionTerm)payload;
        out.write(updateElectionTerm.getPayloadType().getOrdinalByte());
        out.writeLong(updateElectionTerm.getCurrentTerm());
        out.writeUTF(updateElectionTerm.getVotedFor());
    }

    @Override
    public SerializablePayload readFrom(final DataInput in) throws IOException {
        final long term = in.readLong();
        final String votedFor = in.readUTF();
        return new UpdateElectionTerm(term, votedFor);
    }
}
