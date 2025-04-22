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
import java.util.Objects;
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

    private final @Nullable VotingInfo votingInfo;

    private int serializedSize = -1;

    public ClusterConfig() {
        votingInfo = null;
    }

    public ClusterConfig(final @Nullable VotingInfo votingInfo) {
        this.votingInfo = votingInfo;
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
            final var vi = delta.votingInfo;
            if (vi != null) {
                final var map = vi.memberToVoting();
                out.writeInt(map.size());
                for (var entry : map.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeBoolean(entry.getValue());
                }
            } else {
                out.writeInt(0);
            }
        };
    }

    /**
     * Returns known {@link VotingInfo} structures.
     *
     * @return known {@link VotingInfo} structures
     */
    public @Nullable VotingInfo votingInfo() {
        return votingInfo;
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
        return Objects.hash(votingInfo);
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof ClusterConfig other && votingInfo.equals(other.votingInfo);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("votingInfo", votingInfo).toString();
    }

    @Override
    protected Object writeReplace() {
        return new ServerConfigurationPayload.Proxy(this);
    }
}
