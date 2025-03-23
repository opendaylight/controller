/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.notifications;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.raft.api.RaftRoles;

/**
 * Role Change message initiated internally from the  Raft Actor when a the behavior/role changes.
 * Since its internal, need not be serialized.
 */
@NonNullByDefault
public record RoleChanged(String memberId, RaftRoles newRole, @Nullable RaftRoles oldRole) implements MemberNotication {
    public RoleChanged {
        requireNonNull(memberId);
        requireNonNull(newRole);
    }

    public RoleChanged(final String memberId, final RaftRoles newRole) {
        this(memberId, newRole, null);
    }
}
