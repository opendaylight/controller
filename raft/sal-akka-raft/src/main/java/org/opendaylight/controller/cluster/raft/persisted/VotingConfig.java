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
import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.AbstractRaftCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RAFT cluster configuration. This command is always persisted, no matter whether or not we are persisting other data
 * distributed via {@link ReplicatedLogEntry}.
 */
@NonNullByDefault
public final class VotingConfig extends AbstractRaftCommand {
    private static final Logger LOG = LoggerFactory.getLogger(VotingConfig.class);
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // FIXME: should be a Map<String, ServerInfo>, but then it really should be something like 'ElectionPolicy'
    private final ImmutableList<ServerInfo> serverInfo;

    private int serializedSize = -1;

    public VotingConfig(final ServerInfo serverInfo) {
        this.serverInfo = ImmutableList.of(serverInfo);
    }

    public VotingConfig(final ServerInfo... serverInfo) {
        this.serverInfo = ImmutableList.copyOf(serverInfo);
    }

    public VotingConfig(final List<ServerInfo> serverInfo) {
        this.serverInfo = ImmutableList.copyOf(serverInfo);
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
            try (var bos = new ByteArrayOutputStream()) {
                try (var out = new ObjectOutputStream(bos)) {
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
    public Serializable toSerialForm() {
        return new ServerConfigurationPayload.Proxy(this);
    }

    @Override
    public int hashCode() {
        return serverInfo.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        return this == obj || obj instanceof VotingConfig other && serverInfo.equals(other.serverInfo);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("serverInfo", serverInfo).toString();
    }
}
