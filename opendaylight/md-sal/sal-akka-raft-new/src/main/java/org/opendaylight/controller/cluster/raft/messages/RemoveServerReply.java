/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import javax.annotation.Nullable;

/**
 * Reply to a RemoveServer message (§4.1).
 */
public final class RemoveServerReply extends AbstractServerChangeReply {
    private static final long serialVersionUID = 1L;

    public RemoveServerReply(ServerChangeStatus status, @Nullable String leaderHint) {
        super(status, leaderHint);
    }
}
