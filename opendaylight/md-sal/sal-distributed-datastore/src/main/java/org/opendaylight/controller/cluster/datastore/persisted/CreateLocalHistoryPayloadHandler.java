/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.persisted;

import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.persistence.PayloadRegistry;


public final class CreateLocalHistoryPayloadHandler extends AbstractPayloadHandler<CreateLocalHistoryPayload> {

    static {
        PayloadRegistry.INSTANCE.registerHandler(PayloadRegistry.PayloadTypeCommon.CREATE_LOCAL_HISTORY_PAYLOAD,
                new CreateLocalHistoryPayloadHandler());
    }

    private CreateLocalHistoryPayloadHandler() {
    }

    @Override
    protected void doWrite(final DataOutput out, final CreateLocalHistoryPayload payload) throws IOException {
        //TODO: do we need to write the serializedSize first? out.writeInt(payload.serializedSize());
        out.write(payload.serialized());
    }

    @Override
    protected CreateLocalHistoryPayload doRead(final DataInput in) throws IOException {
        final LocalHistoryIdentifier localHistoryId = LocalHistoryIdentifier.readFrom(in);
        return CreateLocalHistoryPayload.create(localHistoryId, DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY);
    }
}
