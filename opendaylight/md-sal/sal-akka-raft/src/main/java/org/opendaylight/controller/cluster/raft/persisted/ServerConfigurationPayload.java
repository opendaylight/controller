/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import org.opendaylight.controller.cluster.raft.messages.Payload;

/**
 * Payload data for server configuration log entries.
 *
 * @author Thomas Pantelis
 */
@Deprecated(since = "11.0.0", forRemoval = true)
abstract class ServerConfigurationPayload extends Payload {
    static final class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private List<ServerInfo> serverInfo;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ClusterConfig payload) {
            serverInfo = payload.serverInfo();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serverInfo.size());
            for (var info : serverInfo) {
                out.writeObject(info.peerId());
                out.writeBoolean(info.isVoting());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();

            final var builder = ImmutableList.<ServerInfo>builderWithExpectedSize(size);
            for (int i = 0; i < size; ++i) {
                final var id = (String) in.readObject();
                final var voting = in.readBoolean();
                builder.add(new ServerInfo(id, voting));
            }
            serverInfo = builder.build();
        }

        @java.io.Serial
        private Object readResolve() {
            return new ClusterConfig(serverInfo);
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private ServerConfigurationPayload() {
        // Exists only as holder of Proxy
    }
}
