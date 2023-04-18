/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Message sent to add a new server/replica (§4.1).
 *
 * @author Thomas Pantelis
 */
public class AddServer implements Serializable, SerializableMessage {
    private static final long serialVersionUID = 1L;

    private final String newServerId;
    private final String newServerAddress;
    private final boolean votingMember;

    public AddServer(String newServerId, String newServerAddress, boolean votingMember) {
        this.newServerId = newServerId;
        this.newServerAddress = newServerAddress;
        this.votingMember = votingMember;
    }

    public String getNewServerId() {
        return newServerId;
    }

    public String getNewServerAddress() {
        return newServerAddress;
    }

    public boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public String toString() {
        return "AddServer [newServerId=" + newServerId + ", newServerAddress=" + newServerAddress + ", votingMember="
                + votingMember + "]";
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        out.writeUTF(newServerId);
        out.writeUTF(newServerAddress);
        out.writeBoolean(votingMember);
    }

    public static @NonNull AddServer readFrom(final DataInput in) throws IOException {
        return new AddServer(in.readUTF(), in.readUTF(), in.readBoolean());
    }
}
