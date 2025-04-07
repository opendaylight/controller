/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.AbstractRaftCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RAFT cluster configuration. This payload is always persisted, no matter whether or not we are persisting other data
 * distributed via {@link ReplicatedLogEntry}.
 */
// FIXME: rename to 'SetRaftConfiguration' or somesuch, perhaps with interface + non-serializable record
@NonNullByDefault
public final class ClusterConfig extends AbstractRaftCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfig.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // FIXME: should be a Map<String, ServerInfo>, but then it really should be something like 'ElectionPolicy'
    private final ImmutableList<ServerInfo> serverInfo;

    private int serializedSize = -1;

    public ClusterConfig(final ServerInfo serverInfo) {
        this.serverInfo = ImmutableList.of(serverInfo);
    }

    public ClusterConfig(final ServerInfo... serverInfo) {
        this.serverInfo = ImmutableList.copyOf(serverInfo);
    }

    public ClusterConfig(final List<ServerInfo> serverInfo) {
        this.serverInfo = ImmutableList.copyOf(serverInfo);
    }

    public static Reader<ClusterConfig> reader() {
        return in -> {
            final var siCount = in.readInt();
            if (siCount < 0) {
                throw new IOException("Invalid ServerInfo count " + siCount);
            }

            final var siBuilder = ImmutableList.<ServerInfo>builderWithExpectedSize(siCount);
            for (int i = 0; i < siCount; i++) {
                siBuilder.add(new ServerInfo(in.readUTF(), in.readBoolean()));
            }
            return new ClusterConfig(siBuilder.build());
        };
    }

    public static Writer<ClusterConfig> writer() {
        return (delta, out) -> {
            final var si = delta.serverInfo();
            out.writeInt(si.size());
            for (var info : si) {
                out.writeUTF(info.peerId());
                out.writeBoolean(info.isVoting());
            }
        };
    }

    /**
     * Returns known {@link ServerInfo} structures.
     *
     * @return known {@link ServerInfo} structures
     */
    public List<ServerInfo> serverInfo() {
        return serverInfo;
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
        return serverInfo.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof ClusterConfig other && serverInfo.equals(other.serverInfo);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("serverInfo", serverInfo).toString();
    }

    @Override
    protected Object writeReplace() {
        return new ServerConfigurationPayload.Proxy(this);
    }
}
