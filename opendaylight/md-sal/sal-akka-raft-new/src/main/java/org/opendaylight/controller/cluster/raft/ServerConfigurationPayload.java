/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.common.base.Preconditions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
public class ServerConfigurationPayload extends Payload implements PersistentPayload, Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfigurationPayload.class);

    private final List<ServerInfo> serverConfig;
    private transient int serializedSize = -1;

    public ServerConfigurationPayload(@Nonnull List<ServerInfo> serverConfig) {
        this.serverConfig = Preconditions.checkNotNull(serverConfig);
    }

    @Nonnull
    public List<ServerInfo> getServerConfig() {
        return serverConfig;
    }

    @Override
    public int size() {
        if(serializedSize < 0) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(serverConfig);
                out.close();

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

    public static class ServerInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String id;
        private final boolean isVoting;

        public ServerInfo(@Nonnull String id, boolean isVoting) {
            this.id = Preconditions.checkNotNull(id);
            this.isVoting = isVoting;
        }

        @Nonnull
        public String getId() {
            return id;
        }

        public boolean isVoting() {
            return isVoting;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isVoting ? 1231 : 1237);
            result = prime * result + id.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            ServerInfo other = (ServerInfo) obj;
            return isVoting == other.isVoting && id.equals(other.id);
        }

        @Override
        public String toString() {
            return "ServerInfo [id=" + id + ", isVoting=" + isVoting + "]";
        }
    }
}
