/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Reply to {@link FindLeader} message, containing the address of the leader actor, as known to the raft actor which
 * sent the message. If the responding actor does not have knowledge of the leader, {@link #getLeaderActor()} will
 * return {@link Optional#empty()}.
 *
 * <p>This message is intended for testing purposes only.
 */
@VisibleForTesting
public final class FindLeaderReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String leaderActor;

    public FindLeaderReply(final @Nullable String leaderActor) {
        this.leaderActor = leaderActor;
    }

    public Optional<String> getLeaderActor() {
        return Optional.ofNullable(leaderActor);
    }
}
