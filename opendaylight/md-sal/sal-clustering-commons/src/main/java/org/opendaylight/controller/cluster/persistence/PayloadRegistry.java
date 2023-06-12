/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.persistence;

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
        if (payloadTypeOrdinal < 0 || payloadTypeOrdinal >= types.length) {
            return null;
        }
        return getHandler(types[payloadTypeOrdinal]);
    }


    public enum PayloadTypeCommon {
        //TODO: find another way, this should not be here. It should be mockable
        MOCK_PAYLOAD,
        COMMIT_TRANSACTION_PAYLOAD_SIMPLE,
        COMMIT_TRANSACTION_PAYLOAD_CHUNKED,
        CREATE_LOCAL_HISTORY_PAYLOAD,
        PURGE_TRANSACTION_PAYLOAD,
        ABORT_TRANSACTION_PAYLOAD,
        CLOSE_LOCAL_HISTORY_PAYLOAD,
        REPLICATED_LOG_ENTRY,
        DISABLE_TRACKING_PAYLOAD,
        SKIP_TRANSACTION_PAYLOAD,
        PURGE_LOCAL_HISTORY_PAYLOAD,
        SERVER_CONFIGURATION_PAYLOAD,
        NOOP_PAYLOAD,
        KEY_VALUE_PAYLOAD,
        UPDATE_ELECTION_TERM,
        APPLY_JOURNAL_ENTRIES;

        public byte getOrdinalByte() {
            return Integer.valueOf(ordinal()).byteValue();
        }

    }
}
