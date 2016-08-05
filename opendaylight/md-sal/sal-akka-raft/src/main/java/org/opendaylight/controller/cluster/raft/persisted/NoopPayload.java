/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Payload used for no-op log entries that are put into the journal by the PreLeader in order to commit
 * entries from the prior term.
 *
 * @author Thomas Pantelis
 */
public final class NoopPayload extends Payload implements Serializable {
    public static final NoopPayload INSTANCE = new NoopPayload();

    // There is no need for Externalizable
    private static final class Proxy implements Serializable {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("static-method")
        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Proxy PROXY = new Proxy();

    private NoopPayload() {
    }

    @Override
    public int size() {
        return 0;
    }

    @SuppressWarnings("static-method")
    private Object writeReplace() {
        return PROXY;
    }
}
