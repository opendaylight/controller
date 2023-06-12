/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import akka.dispatch.ControlMessage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.persistence.PayloadHandler;
import org.opendaylight.controller.cluster.persistence.PayloadRegistry;
import org.opendaylight.controller.cluster.persistence.SerializablePayload;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
public final class NoopPayload extends Payload implements ControlMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final @NonNull NP PROXY = new NP();
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative
    private static final int PROXY_SIZE = SerializationUtils.serialize(PROXY).length;

    public static final @NonNull NoopPayload INSTANCE = new NoopPayload();

    private NoopPayload() {
        // Hidden on purpose
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int serializedSize() {
        return PROXY_SIZE;
    }

    @Override
    protected Object writeReplace() {
        return PROXY;
    }

    @Override
    public PayloadRegistry.PayloadTypeCommon getPayloadType() {
        return PayloadRegistry.PayloadTypeCommon.NOOP_PAYLOAD;
    }

    static class NoopPayloadHandler implements PayloadHandler {

        static {
            PayloadRegistry.INSTANCE.registerHandler(PayloadRegistry.PayloadTypeCommon.NOOP_PAYLOAD,
                    new NoopPayloadHandler());
        }

        @Override
        public void writeTo(DataOutput out, SerializablePayload payload) throws IOException {
            out.write(payload.getPayloadType().getOrdinalByte());
        }

        @Override
        public SerializablePayload readFrom(DataInput in) throws IOException {
            return new NoopPayload();
        }
    }
}
