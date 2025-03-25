/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;

/**
 * The ServerRemoved message is sent to a server which has been removed successfully from the ServerConfiguration.
 * The Server can then choose to self destruct or notify it's parents as needed.
 */
public class ServerRemoved implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String serverId;

    public ServerRemoved(final String serverId) {
        this.serverId = requireNonNull(serverId);
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public String toString() {
        return "ServerRemoved [serverId=" + serverId + "]";
    }
}
