/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.Serializable;

/**
 * Message sent to add a new server/replica (§4.1).
 *
 * @author Thomas Pantelis
 */
public class AddServer implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String newServerId;
    private final boolean votingMember;

    public AddServer(String newServerId, boolean votingMember) {
        this.newServerId = newServerId;
        this.votingMember = votingMember;
    }

    public String getNewServerId() {
        return newServerId;
    }

    public boolean isVotingMember() {
        return votingMember;
    }

    @Override
    public String toString() {
        return "AddServer [newServerId=" + newServerId + ", votingMember=" + votingMember + "]";
    }
}
