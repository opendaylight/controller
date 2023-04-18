/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent to change the raft voting status for servers.
 *
 * @author Thomas Pantelis
 */
public class ChangeServersVotingStatus implements Serializable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private final Map<String, Boolean> serverVotingStatusMap;
    private final Collection<String> serversVisited;

    public ChangeServersVotingStatus(@NonNull Map<String, Boolean> serverVotingStatusMap) {
        this(serverVotingStatusMap, ImmutableSet.of());
    }

    public ChangeServersVotingStatus(@NonNull Map<String, Boolean> serverVotingStatusMap,
            @NonNull Collection<String> serversVisited) {
        this.serverVotingStatusMap = new HashMap<>(requireNonNull(serverVotingStatusMap));
        this.serversVisited = ImmutableSet.copyOf(requireNonNull(serversVisited));
    }

    public @NonNull Map<String, Boolean> getServerVotingStatusMap() {
        return serverVotingStatusMap;
    }

    public @NonNull Collection<String> getServersVisited() {
        return serversVisited;
    }

    @Override
    public String toString() {
        return "ChangeServersVotingStatus [serverVotingStatusMap=" + serverVotingStatusMap
                + (serversVisited != null ? ", serversVisited=" + serversVisited : "") + "]";
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.write(serverVotingStatusMap.size());
        serverVotingStatusMap.forEach((s, b) -> {
            try {
                out.writeUTF(s);
                out.writeBoolean(b);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        out.write(serversVisited.size());
        serversVisited.forEach(s -> {
            try {
                out.writeUTF(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static @NonNull ChangeServersVotingStatus readFrom(final DataInput in) throws IOException {
        int size = in.readInt();
        final var votingStatusMap = new HashMap<String, Boolean>(size);
        for (int i = 0; i < size; i++) {
            votingStatusMap.put(in.readUTF(), in.readBoolean());
        }

        size = in.readInt();
        var visited = new HashSet<String>(size);
        for (int i = 0; i < size; i++) {
            visited.add(in.readUTF());
        }

        return new ChangeServersVotingStatus(votingStatusMap, visited);
    }
}
