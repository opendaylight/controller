/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.behaviors.PreLeader;
import org.opendaylight.controller.cluster.raft.spi.AbstractRaftCommand;

/**
 * Payload used for no-op log entries that are put into the journal by the {@link PreLeader} in order to commit entries
 * from the prior term.
 *
 * @author Thomas Pantelis
 */
// FIXME: Rename to SetRaftLeader with a @NonNull leaderId()
// FIXME: not ControlMessage?
// FIXME: Should contain RaftVersions constant to indicate minimum required participant version. Incompatible peers
//        should reject to persist the payload, eventually leading to new elections if necessary.
@NonNullByDefault
public final class NoopPayload extends AbstractRaftCommand implements ControlMessage {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    private static final NP PROXY = new NP();
    // Estimate to how big the proxy is. Note this includes object stream overhead, so it is a bit conservative
    private static final int PROXY_SIZE = SerializationUtils.serialize(PROXY).length;

    public static final NoopPayload INSTANCE = new NoopPayload();

    private NoopPayload() {
        // Hidden on purpose
    }

    public static Reader<NoopPayload> reader() {
        return in -> INSTANCE;
    }

    public static Writer<NoopPayload> writer() {
        return (delta, out) -> {
            requireNonNull(delta);
            requireNonNull(out);
        };
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
    public Serializable toSerialForm() {
        return PROXY;
    }
}
