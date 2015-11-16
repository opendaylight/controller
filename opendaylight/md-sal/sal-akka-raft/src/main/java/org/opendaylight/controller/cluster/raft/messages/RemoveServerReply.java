/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Reply to a RemoveServer message (§4.1).
 */
public class RemoveServerReply implements Serializable {
    private static final long serialVersionUID = 1L;
    private final ServerChangeStatus status;
    private final String leaderHint;

    public RemoveServerReply(ServerChangeStatus status, @Nullable String leaderHint) {
        this.status = status;
        this.leaderHint = leaderHint;
    }

    public ServerChangeStatus getStatus() {
        return status;
    }

    public String getLeaderHint() {
        return leaderHint;
    }

    @Override
    public String toString() {
        return "RemoveServerReply{" +
                "status=" + status +
                ", leaderHint='" + leaderHint + '\'' +
                '}';
    }
}
