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

public final class PurgeLocalHistoryPayloadHandler extends AbstractPayloadHandler<PurgeLocalHistoryPayload> {

    static {
        PayloadRegistry.INSTANCE.registerHandler(PayloadRegistry.PayloadTypeCommon.PURGE_LOCAL_HISTORY_PAYLOAD,
                new PurgeLocalHistoryPayloadHandler());
    }

    private PurgeLocalHistoryPayloadHandler() {
    }

    @Override
    protected void doWrite(final DataOutput out, final PurgeLocalHistoryPayload payload) throws IOException {
        out.write(payload.serialized());
    }

    @Override
    protected PurgeLocalHistoryPayload doRead(final DataInput in) throws IOException {
        final LocalHistoryIdentifier localHistoryId = LocalHistoryIdentifier.readFrom(in);
        return PurgeLocalHistoryPayload.create(localHistoryId, DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY);
    }
}
