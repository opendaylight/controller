/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import akka.dispatch.ControlMessage;
import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
// FIXME: do not implement MigratedSerializable once Proxy is gone
public final class NoopPayload extends Payload implements ControlMessage, MigratedSerializable {
    // There is no need for Externalizable
    @Deprecated(since = "7.0.0", forRemoval = true)
    private static final class Proxy implements Serializable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;
        private static final @NonNull NoopPayload INSTANCE = new NoopPayload(true);

        @java.io.Serial
        private Object readResolve() {
            return INSTANCE;
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final @NonNull NP PROXY = new NP();
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative
    private static final int PROXY_SIZE = SerializationUtils.serialize(PROXY).length;

    public static final @NonNull NoopPayload INSTANCE = new NoopPayload(false);

    private final boolean migrated;

    private NoopPayload(final boolean migrated) {
        this.migrated = migrated;
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
    public boolean isMigrated() {
        return migrated;
    }

    // FIXME: protected once not MigratedSerializable
    @Override
    public Object writeReplace() {
        return PROXY;
    }
}
