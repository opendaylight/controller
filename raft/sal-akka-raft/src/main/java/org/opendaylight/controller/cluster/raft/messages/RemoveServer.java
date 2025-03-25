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
 * Message sent to remove a replica (§4.1).
 */
public class RemoveServer implements Serializable {
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
}
