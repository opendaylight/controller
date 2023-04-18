/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Reply to a RemoveServer message (§4.1).
 */
public final class RemoveServerReply extends AbstractServerChangeReply {
    private static final long serialVersionUID = 1L;

    public RemoveServerReply(ServerChangeStatus status, @Nullable String leaderHint) {
        super(status, leaderHint);
    }

    public static @NonNull RemoveServerReply readFrom(final DataInput in) throws IOException {
        final ServerChangeStatus status = ServerChangeStatus.values()[in.readInt()];

        String leaderHint = null;
        if (in.readBoolean()) {
            leaderHint = in.readUTF();
        }

        return new RemoveServerReply(status, leaderHint);
    }
}
