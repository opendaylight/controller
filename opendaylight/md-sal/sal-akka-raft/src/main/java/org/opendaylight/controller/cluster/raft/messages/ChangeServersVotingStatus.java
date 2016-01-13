/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        this(serverVotingStatusMap, null);
    }

    public ChangeServersVotingStatus(@Nonnull Map<String, Boolean> serverVotingStatusMap,
            @Nullable Collection<String> serversVisited) {
        this.serverVotingStatusMap = new HashMap<>(Preconditions.checkNotNull(serverVotingStatusMap));
        this.serversVisited = serversVisited == null ? null : new HashSet<>(serversVisited);
    }

    public Map<String, Boolean> getServerVotingStatusMap() {
        return serverVotingStatusMap;
    }

    @Nullable
    public Collection<String> getServersVisited() {
        return serversVisited;
    }

    @Override
    public String toString() {
        return "ChangeServersVotingStatus [serverVotingStatusMap=" + serverVotingStatusMap + ", "
                + (serversVisited != null ? "serversVisited=" + serversVisited : "") + "]";
    }
}
