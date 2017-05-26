/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Message sent to change the raft voting status for servers.
 *
 * @author Thomas Pantelis
 */
public class ChangeServersVotingStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Boolean> serverVotingStatusMap;
    private final Collection<String> serversVisited;

    public ChangeServersVotingStatus(@Nonnull Map<String, Boolean> serverVotingStatusMap) {
        this(serverVotingStatusMap, Collections.emptySet());
    }

    public ChangeServersVotingStatus(@Nonnull Map<String, Boolean> serverVotingStatusMap,
            @Nonnull Collection<String> serversVisited) {
        this.serverVotingStatusMap = new HashMap<>(Preconditions.checkNotNull(serverVotingStatusMap));
        this.serversVisited = ImmutableSet.copyOf(Preconditions.checkNotNull(serversVisited));
    }

    @Nonnull
    public Map<String, Boolean> getServerVotingStatusMap() {
        return serverVotingStatusMap;
    }

    @Nonnull
    public Collection<String> getServersVisited() {
        return serversVisited;
    }

    @Override
    public String toString() {
        return "ChangeServersVotingStatus [serverVotingStatusMap=" + serverVotingStatusMap
                + (serversVisited != null ? ", serversVisited=" + serversVisited : "") + "]";
    }
}
