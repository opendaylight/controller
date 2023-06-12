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

public final class CommitTransactionPayloadSimpleHandler
        extends AbstractPayloadHandler<CommitTransactionPayload.Simple> {

    @Override
    protected void doWriteSkipPayloadType(final DataOutput out, final CommitTransactionPayload.Simple payload)
            throws IOException {
        out.writeInt(payload.serializedSize());
        payload.writeBytes(out);
    }

    @Override
    protected CommitTransactionPayload.Simple doRead(final DataInput in) throws IOException {
        int serializedSize = in.readInt();
        byte[] serializedData = new byte[serializedSize];
        in.readFully(serializedData);
//        try {
//            in.readFully(serializedData);
//        } catch (Exception e) {
//            String message = e.getMessage();
//        }
        return new CommitTransactionPayload.Simple(serializedData);
    }
}
