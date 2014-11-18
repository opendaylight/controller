/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.controller.cluster.raft.base.messages;

import org.opendaylight.controller.cluster.raft.RaftState;

/**
 * Role Change message initiated from the Raft Actor when a the behavior/role changes.
 *
 */
public class RaftRoleChanged {
    private String memberId;
    private RaftState oldRole;
    private RaftState newRole;

    public RaftRoleChanged(String memberId, RaftState oldRole, RaftState newRole) {
        this.memberId = memberId;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public String getMemberId() {
        return memberId;
    }

    public RaftState getOldRole() {
        return oldRole;
    }

    public RaftState getNewRole() {
        return newRole;
    }
}
