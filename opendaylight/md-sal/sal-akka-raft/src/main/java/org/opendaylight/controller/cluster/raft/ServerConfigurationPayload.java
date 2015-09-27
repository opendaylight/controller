/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.opendaylight.controller.protobuff.messages.cluster.raft.AppendEntriesMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payload data for server configuration log entries.
 *
 * @author Thomas Pantelis
 */
public class ServerConfigurationPayload extends Payload implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ServerConfigurationPayload.class);

    private final List<String> newServerConfig;
    private final List<String> oldServerConfig;
    private transient int serializedSize = -1;

    public ServerConfigurationPayload(List<String> newServerConfig, List<String> oldServerConfig) {
        this.newServerConfig = newServerConfig;
        this.oldServerConfig = oldServerConfig;
    }

    public List<String> getNewServerConfig() {
        return newServerConfig;
    }


    public List<String> getOldServerConfig() {
        return oldServerConfig;
    }

    @Override
    public int size() {
        if(serializedSize < 0) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bos);
                out.writeObject(newServerConfig);
                out.writeObject(oldServerConfig);
                out.close();

                serializedSize = bos.toByteArray().length;
            } catch (IOException e) {
                serializedSize = 0;
                LOG.error("Error serializing", e);
            }
        }

        return serializedSize;
    }

    @Override
    @Deprecated
    @SuppressWarnings("rawtypes")
    public <T> Map<GeneratedExtension, T> encode() {
        return null;
    }

    @Override
    @Deprecated
    public Payload decode(AppendEntriesMessages.AppendEntries.ReplicatedLogEntry.Payload payload) {
        return null;
    }
}
