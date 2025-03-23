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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent to change the raft voting status for servers.
 *
 * @author Thomas Pantelis
 */
public final class ChangeServersVotingStatus implements ServerChangeRequest<ServerChangeReply> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final Map<String, Boolean> serverVotingStatusMap;
    private final Collection<String> serversVisited;

    public ChangeServersVotingStatus(final @NonNull Map<String, Boolean> serverVotingStatusMap) {
        this(serverVotingStatusMap, ImmutableSet.of());
    }

    public ChangeServersVotingStatus(final @NonNull Map<String, Boolean> serverVotingStatusMap,
            final @NonNull Collection<String> serversVisited) {
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
}
