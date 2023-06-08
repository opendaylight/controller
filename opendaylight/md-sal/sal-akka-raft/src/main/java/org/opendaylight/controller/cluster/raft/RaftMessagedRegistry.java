/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.messages.handlers.AppendEntriesHandler;
import org.opendaylight.controller.cluster.raft.messages.handlers.InstallSnapshotHandler;

public class RaftMessagedRegistry {

    public enum MessageType {
        APPEND_ENTRIES(new AppendEntriesHandler()),
        INSTALL_SNAPSHOT(new InstallSnapshotHandler());

        private final RaftMessageHandler handler;

        MessageType(final RaftMessageHandler handler) {
            this.handler = handler;
        }

        RaftMessageHandler getMessageHandler() {
            return handler;
        }

    }
    public static RaftRPC readMessageFrom(final DataInputStream input) throws IOException, ClassNotFoundException {
        byte messageTypeId = input.readByte();
        MessageType type =  MessageType.values()[messageTypeId];
        return type.getMessageHandler().readFrom(input);
    }

    public static void writeMessageTo(final DataOutput out, final RaftRPC message) throws IOException {
        final MessageType messageType = message.getMessageType();
        out.writeByte(messageType.ordinal());
        messageType.getMessageHandler().writeTo(out, message);
    }
}