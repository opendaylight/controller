/*
 * Copyright (c) 2023 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.base.Verify;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.persistence.PayloadHandler;
import org.opendaylight.controller.cluster.persistence.SerializablePayload;

public class ServerConfigurationPayloadHandler implements PayloadHandler {

    @Override
    public void writeTo(final DataOutput out, final SerializablePayload payload) throws IOException {
        Verify.verify(payload instanceof ServerConfigurationPayload);
        final ServerConfigurationPayload serverConfig = (ServerConfigurationPayload) payload;
        out.write(payload.getPayloadType().getOrdinalByte());
        out.writeInt(serverConfig.size());
        for (ServerInfo i : serverConfig.getServerConfig()) {
            out.writeChars(i.getId());
            out.writeBoolean(i.isVoting());
        }
    }

    @Override
    public SerializablePayload readFrom(DataInput in) throws IOException {
        final int serverInfosCount = in.readInt();
        final List<ServerInfo> serverInfos = new ArrayList<>(serverInfosCount);
        for (int i = 0; i < serverInfosCount; i++) {
            final String id = in.readUTF();
            final boolean isVoting = in.readBoolean();
            serverInfos.add(new ServerInfo(id, isVoting));
        }
        return new ServerConfigurationPayload(serverInfos);
    }
}
