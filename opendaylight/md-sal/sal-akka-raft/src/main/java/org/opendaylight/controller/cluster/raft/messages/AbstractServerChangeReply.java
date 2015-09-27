/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Abstract base class for a server configuration change reply.
 *
 * @author Thomas Pantelis
 */
public class AbstractServerChangeReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String leaderHint;
    private final ServerChangeStatus status;

    public AbstractServerChangeReply(ServerChangeStatus status, String leaderHint) {
        this.status = status;
        this.leaderHint = leaderHint;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getLeaderHint() {
        return leaderHint;
    }

    public ServerChangeStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [status=" + status + ", leaderHint=" + leaderHint + "]";
    }
}
