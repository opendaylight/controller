/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.io.Serializable;

/**
 * Reply message to GetLastAppliedIndex.
 *
 * @author Thomas Pantelis
 */
public class GetLastAppliedIndexReply implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long index;
    private final String serverId;

    public GetLastAppliedIndexReply(String serverId, long index) {
        this.serverId = serverId;
        this.index = index;
    }

    public long getIndex() {
        return index;
    }

    public String getServerId() {
        return serverId;
    }

    @Override
    public String toString() {
        return "GetLastAppliedIndexReply [index=" + index + ", serverId=" + serverId + "]";
    }
}
