/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.PersistentPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload data for server configuration log entries.
 *
 * @author Thomas Pantelis
 */
public final class ServerConfigurationPayload extends Payload implements PersistentPayload, MigratedSerializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private List<ServerInfo> serverConfig;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final ServerConfigurationPayload payload) {
            this.serverConfig = payload.getServerConfig();
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
            serverConfig = new ArrayList<>(size);
            for (int i = 0; i < size; ++i) {
                final String id = (String) in.readObject();
                final boolean voting = in.readBoolean();
                serverConfig.add(new ServerInfo(id, voting));
            }
        }

        private Object readResolve() {
            return new ServerConfigurationPayload(serverConfig);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfigurationPayload.class);
    private static final long serialVersionUID = 1L;

    private final List<ServerInfo> serverConfig;
    private final boolean migrated;
    private int serializedSize = -1;

    private ServerConfigurationPayload(final @Nonnull List<ServerInfo> serverConfig, boolean migrated) {
        this.serverConfig = ImmutableList.copyOf(serverConfig);
        this.migrated = migrated;
    }

    public ServerConfigurationPayload(final @Nonnull List<ServerInfo> serverConfig) {
        this(serverConfig, false);
    }

    @Deprecated
    public static ServerConfigurationPayload createMigrated(final @Nonnull List<ServerInfo> serverConfig) {
        return new ServerConfigurationPayload(serverConfig, true);
    }

    @Deprecated
    @Override
    public boolean isMigrated() {
        return migrated;
    }

    public @Nonnull List<ServerInfo> getServerConfig() {
        return serverConfig;
    }

    @Override
    public int size() {
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
    public String toString() {
        return "ServerConfigurationPayload [serverConfig=" + serverConfig + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }
}
