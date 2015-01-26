/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.notifications;

import java.io.Serializable;

/**
 * Notification message representing a Role change of a cluster member
 *
 * Roles generally are Leader, Follower and Candidate. But can be based on the consensus strategy/implementation
 *
 * The Listener could be in a separate ActorSystem and hence this message needs to be Serializable
 */
public class RoleChangeNotification implements Serializable {
    private static final long serialVersionUID = -2873869509490117116L;
    private String memberId;
    private String oldRole;
    private String newRole;

    public RoleChangeNotification(String memberId, String oldRole, String newRole) {
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
}
