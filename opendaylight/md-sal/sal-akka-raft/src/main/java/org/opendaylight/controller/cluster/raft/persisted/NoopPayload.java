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
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
public final class NoopPayload extends Payload implements ControlMessage {
    public static final NoopPayload INSTANCE = new NoopPayload();

    // There is no need for Externalizable
    private static final class Proxy implements Serializable {
        private static final long serialVersionUID = 1L;

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Proxy PROXY = new Proxy();
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative
    private static final int PROXY_SIZE = SerializationUtils.serialize(PROXY).length;

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
}
