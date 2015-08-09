/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.notifications;

/**
 * Role Change message initiated internally from the  Raft Actor when a the behavior/role changes.
 *
 * Since its internal , need not be serialized
 *
 */
public class RoleChanged {
    private String memberId;
    private String oldRole;
    private String newRole;

    public RoleChanged(String memberId, String oldRole, String newRole) {
        this.memberId = memberId;
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getOldRole() {
        return oldRole;
    }

    public String getNewRole() {
        return newRole;
    }

    @Override
    public String toString() {
        return "RoleChanged{" +
                "memberId='" + memberId + '\'' +
                ", oldRole='" + oldRole + '\'' +
                ", newRole='" + newRole + '\'' +
                '}';
    }
}
