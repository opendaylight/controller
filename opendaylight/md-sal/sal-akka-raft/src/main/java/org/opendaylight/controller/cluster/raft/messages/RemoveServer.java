/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent to remove a replica (§4.1).
 */
public class RemoveServer implements Serializable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private final String serverId;

    public RemoveServer(final String serverId) {
        this.serverId = requireNonNull(serverId);
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public String toString() {
        return "RemoveServer{" + "serverId='" + serverId + '\'' + '}';
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeUTF(serverId);
    }

    public static @NonNull RemoveServer readFrom(final DataInput in) throws IOException {
        return new RemoveServer(in.readUTF());
    }
}
