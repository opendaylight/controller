/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persisted;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.persistence.PayloadHandler;
import org.opendaylight.controller.cluster.persistence.SerializablePayload;

abstract class AbstractPayloadHandler<T extends SerializablePayload> implements PayloadHandler {

    protected abstract void doWrite(DataOutput out, T payload) throws IOException;

    protected abstract T doRead(DataInput in) throws IOException;

    @Override
    public final void writeTo(DataOutput out, SerializablePayload payload) throws IOException {
        out.writeByte(payload.getPayloadType().getOrdinalByte());
        doWrite(out, (T) payload);
    }

    @Override
    public final SerializablePayload readFrom(final DataInput in) throws IOException {
        return doRead(in);
    }
}
