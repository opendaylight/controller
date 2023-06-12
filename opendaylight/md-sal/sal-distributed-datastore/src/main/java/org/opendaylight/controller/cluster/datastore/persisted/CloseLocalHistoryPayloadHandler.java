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

public final class CloseLocalHistoryPayloadHandler extends AbstractPayloadHandler<CloseLocalHistoryPayload> {

    @Override
    protected void doWriteSkipPayloadType(final DataOutput out, final CloseLocalHistoryPayload payload)
            throws IOException {
        out.write(payload.serialized());
    }

    @Override
    protected CloseLocalHistoryPayload doRead(final DataInput in) throws IOException {
        final LocalHistoryIdentifier localHistoryId = LocalHistoryIdentifier.readFrom(in);
        return CloseLocalHistoryPayload.create(localHistoryId, DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY);
    }
}
