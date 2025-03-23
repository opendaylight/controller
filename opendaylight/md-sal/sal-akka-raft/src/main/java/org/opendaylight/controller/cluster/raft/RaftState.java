/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor.Created;
import org.opendaylight.controller.cluster.raft.RaftActor.Initialized;
import org.opendaylight.controller.cluster.raft.RaftActor.Initializing;
import org.opendaylight.controller.cluster.raft.RaftActor.Stopped;

/**
 * One of the possible lifecycle phases in the life of a raft actor.
 */
@NonNullByDefault
abstract sealed class RaftState permits Created, Initializing, Initialized, Stopped {
    private final String stateName;

    RaftState(final String stateName) {
        this.stateName = requireNonNull(stateName);
    }

    @Override
    public final String toString() {
        return addToStringAttributes(MoreObjects.toStringHelper(stateName)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper;
    }
}