/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.messages.PersistentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload data for server configuration log entries.
 *
 * @author Thomas Pantelis
 */
public final class ServerConfigurationPayload extends Payload implements PersistentPayload {
    private static final class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private List<ServerInfo> serverConfig;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final ServerConfigurationPayload payload) {
            serverConfig = payload.getServerConfig();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeInt(serverConfig.size());
            for (ServerInfo i : serverConfig) {
                out.writeObject(i.getId());
                out.writeBoolean(i.isVoting());
            }
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            final int size = in.readInt();

            final var builder = ImmutableList.<ServerInfo>builderWithExpectedSize(size);
            for (int i = 0; i < size; ++i) {
                final String id = (String) in.readObject();
                final boolean voting = in.readBoolean();
                builder.add(new ServerInfo(id, voting));
            }
            serverConfig = builder.build();
        }

        @java.io.Serial
        private Object readResolve() {
            return new ServerConfigurationPayload(serverConfig);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfigurationPayload.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This field is not Serializable but this class "
            + "implements writeReplace to delegate serialization to a Proxy class and thus instances of this class "
            + "aren't serialized. FindBugs does not recognize this.")
    private final List<ServerInfo> serverConfig;
    private int serializedSize = -1;

    public ServerConfigurationPayload(final @NonNull List<ServerInfo> serverConfig) {
        this.serverConfig = ImmutableList.copyOf(serverConfig);
    }

    public @NonNull List<ServerInfo> getServerConfig() {
        return serverConfig;
    }

    @Override
    public int size() {
        return serializedSize();
    }

    @Override
    public int serializedSize() {
        if (serializedSize < 0) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                    out.writeObject(writeReplace());
                }

                serializedSize = bos.toByteArray().length;
            } catch (IOException e) {
                serializedSize = 0;
                LOG.error("Error serializing", e);
            }
        }

        return serializedSize;
    }

    @Override
    public int hashCode() {
        return serverConfig.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ServerConfigurationPayload other = (ServerConfigurationPayload) obj;
        return serverConfig.equals(other.serverConfig);
    }

    @Override
    public String toString() {
        return "ServerConfigurationPayload [serverConfig=" + serverConfig + "]";
    }

    @Override
    protected Object writeReplace() {
        return new Proxy(this);
    }
}
