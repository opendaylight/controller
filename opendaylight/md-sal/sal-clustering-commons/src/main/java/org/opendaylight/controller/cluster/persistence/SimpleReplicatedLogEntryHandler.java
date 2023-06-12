/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.persistence;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.ReplicatedLogEntry;

public final class SimpleReplicatedLogEntryHandler implements PayloadHandler {

    static {
        PayloadRegistry.INSTANCE.registerHandler(PayloadRegistry.PayloadTypeCommon.REPLICATED_LOG_ENTRY,
                new SimpleReplicatedLogEntryHandler());
    }

    private SimpleReplicatedLogEntryHandler() {

    }

    @Override
    public void writeTo(final DataOutput out, final SerializablePayload payload) throws IOException {
        final ReplicatedLogEntry entry = (ReplicatedLogEntry) payload;
        out.writeByte(payload.getPayloadType().getOrdinalByte());
        out.writeLong(entry.getIndex());
        out.writeLong(entry.getTerm());
        final SerializablePayload entryPayload = entry.getData();
        final PayloadHandler entryPayloadHandler = PayloadRegistry.INSTANCE.getHandler(entryPayload.getPayloadType());
        entryPayloadHandler.writeTo(out, payload);
    }

    @Override
    public SerializablePayload readFrom(final DataInput in) throws IOException {
        final long index = in.readLong();
        final long term = in.readLong();
        final byte entryPayloadType = in.readByte();
        final PayloadHandler payloadHandler = PayloadRegistry.INSTANCE.getHandler(entryPayloadType);
        final SerializablePayload payload = payloadHandler.readFrom(in);
        return new SimpleReplicatedLogEntry(index, term, payload);
    }
}
