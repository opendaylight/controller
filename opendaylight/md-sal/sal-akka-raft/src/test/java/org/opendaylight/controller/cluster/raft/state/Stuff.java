/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.api.RaftActorAccess;

@NonNullByDefault
public abstract sealed class Stuff permits InactiveStuff, ActiveStuff {
    final RaftActorAccess actorAccess;
    final short payloadVersion;

    Stuff(final RaftActorAccess actorAccess, final short payloadVersion) {
        this.actorAccess = requireNonNull(actorAccess);
        this.payloadVersion = payloadVersion;
    }

    Stuff(final Stuff prev) {
        actorAccess = prev.actorAccess;
        payloadVersion = prev.payloadVersion;
    }

    final StateSnapshot takeSnapshot() {
        // FIXME: implement this
        throw new UnsupportedOperationException();
    }
}
