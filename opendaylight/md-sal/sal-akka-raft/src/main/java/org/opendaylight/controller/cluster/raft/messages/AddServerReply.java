/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Reply to an AddServer message (§4.1).
 *
 * @author Thomas Pantelis
 */
public final class AddServerReply extends AbstractServerChangeReply {
    private static final long serialVersionUID = 1L;

    public AddServerReply(ServerChangeStatus status, String leaderHint) {
        super(status, leaderHint);
    }

    public static @NonNull AddServerReply readFrom(final DataInput in) throws IOException {
        final ServerChangeStatus status = ServerChangeStatus.values()[in.readInt()];

        String leaderHint = null;
        if (in.readBoolean()) {
            leaderHint = in.readUTF();
        }

        return new AddServerReply(status, leaderHint);
    }
}
