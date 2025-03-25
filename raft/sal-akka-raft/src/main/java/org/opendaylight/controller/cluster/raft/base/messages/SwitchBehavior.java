/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.RaftActor;

@NonNullByDefault
public sealed interface SwitchBehavior {

    long newTerm();

    /**
     * Request a {@link RaftActor} to become a follower in specified term.
     */
    record BecomeFollower(long newTerm) implements SwitchBehavior {
        // Nothing else
    }

    /**
     * Request a {@link RaftActor} to become a leader in specified term.
     */
    record BecomeLeader(long newTerm) implements SwitchBehavior {
        // Nothing else
    }
}
