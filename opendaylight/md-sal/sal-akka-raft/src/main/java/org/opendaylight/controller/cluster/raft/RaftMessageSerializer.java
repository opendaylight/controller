/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.serialization.JSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;

public class RaftMessageSerializer extends JSerializer {
    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            return RaftMessagedRegistry.readMessageFrom(input);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] toBinary(Object o) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final DataOutput out = new DataOutputStream(bos);
        if (o instanceof RaftRPC message) {
            try {
                RaftMessagedRegistry.writeMessageTo(out, message);
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Serialization error: not a RaftRPC message");

    }

    @Override
    public boolean includeManifest() {
        return false;
    }

    @Override
    public int identifier() {
        return 306289;
    }
}