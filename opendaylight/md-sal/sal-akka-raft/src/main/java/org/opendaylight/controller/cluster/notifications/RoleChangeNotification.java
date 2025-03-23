/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.notifications;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.RaftRole;

/**
 * Notification message representing a Role change of a cluster member.
 * Roles generally are Leader, Follower and Candidate. But can be based on the consensus strategy/implementation.
 * The Listener could be in a separate ActorSystem and hence this message needs to be Serializable.
 */
@NonNullByDefault
public record RoleChangeNotification(String memberId, @Nullable String oldRole, String newRole)
        implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = -2873869509490117116L;

    public RoleChangeNotification {
        requireNonNull(memberId);
        requireNonNull(newRole);
    }

    public RoleChangeNotification(final String memberId, final RaftRole newRole, final @Nullable RaftRole oldRole) {
        this(memberId, oldRole != null ? oldRole.name() : null, newRole.name());
    }
}
