/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persisted;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.io.ChunkedByteArray;
import org.opendaylight.controller.cluster.persistence.PayloadRegistry;

public final class CommitTransactionPayloadChunkedHandler
        extends AbstractPayloadHandler<CommitTransactionPayload.Chunked> {

    static {
        PayloadRegistry.INSTANCE.registerHandler(PayloadRegistry.PayloadTypeCommon.COMMIT_TRANSACTION_PAYLOAD_CHUNKED,
                new CommitTransactionPayloadChunkedHandler());
    }

    private CommitTransactionPayloadChunkedHandler() {
    }

    @Override
    protected void doWrite(DataOutput out, CommitTransactionPayload.Chunked payload) throws IOException {
        out.writeInt(payload.serializedSize());
        payload.writeBytes(out);
    }

    @Override
    protected CommitTransactionPayload.Chunked doRead(DataInput in) throws IOException {
        int length = in.readInt();
        return new CommitTransactionPayload.Chunked(ChunkedByteArray.readFrom(in, length,
                CommitTransactionPayload.MAX_ARRAY_SIZE));
    }
}
