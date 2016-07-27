/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.GeneratedMessage;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
public final class NoopPayload extends Payload implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final NoopPayload INSTANCE = new NoopPayload();

    private NoopPayload() {
    }

    @Override
    public int size() {
        return 0;
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override public Map<GeneratedMessage.GeneratedExtension<?, ?>, String> encode() {
        Map<GeneratedMessage.GeneratedExtension<?, ?>, String> map = new HashMap<>();
        return map;
    }

    @Override public Payload decode(
            AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payloadProtoBuff) {
        return this;
    }
}
