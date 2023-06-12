/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.persistence;

import com.google.common.base.Verify;
import java.util.HashMap;
import java.util.Map;

public final class PayloadRegistry {

    public static final PayloadRegistry INSTANCE = new PayloadRegistry();
    private final Map<PayloadTypeCommon, PayloadHandler> handlers = new HashMap<>();

    private PayloadRegistry() {

    }

    public void registerHandler(final PayloadTypeCommon payloadTypeCommon, final PayloadHandler handler) {
        handlers.put(payloadTypeCommon, handler);
    }

    public PayloadHandler getHandler(final PayloadTypeCommon payloadTypeCommon) {
        return handlers.get(payloadTypeCommon);
    }

    public PayloadHandler getHandler(final byte payloadTypeOrdinal) {
        final PayloadTypeCommon[] types = PayloadTypeCommon.values();
        Verify.verify(types.length > payloadTypeOrdinal);
        return getHandler(types[payloadTypeOrdinal]);
    }


    public enum PayloadTypeCommon {
        COMMIT_TRANSACTION_PAYLOAD_SIMPLE,
        COMMIT_TRANSACTION_PAYLOAD_CHUNKED,
        CREATE_LOCAL_HISTORY_PAYLOAD,
        PURGE_TRANSACTION_PAYLOAD,
        ABORT_TRANSACTION_PAYLOAD,
        CLOSE_LOCAL_HISTORY_PAYLOAD,
        DISABLE_TRACKING_PAYLOAD,
        REPLICATED_LOG_ENTRY,
        SKIP_TRANSACTION_PAYLOAD,
        PURGE_LOCAL_HISTORY_PAYLOAD,
        SERVER_CONFIGURATION_PAYLOAD,
        NOOP_PAYLOAD,
        KEY_VALUE_PAYLOAD;

        public byte getOrdinalByte() {
            return Integer.valueOf(ordinal()).byteValue();
        }

    }
}
