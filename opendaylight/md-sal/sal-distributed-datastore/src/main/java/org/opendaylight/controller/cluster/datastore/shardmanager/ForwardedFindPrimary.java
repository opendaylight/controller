/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.shardmanager;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.Set;
import org.apache.pekko.actor.Address;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.datastore.messages.FindPrimary;

/**
 * A {@link FindPrimary} message which making its rounds around the cluster. It contains the message itself and the
 * set of Actor paths, in format produced via {@link ShardPeerAddressResolver#getShardManagerActorPathBuilder(Address)}.
 */
@NonNullByDefault
record ForwardedFindPrimary(FindPrimary message, Set<String> previousActorPaths) implements Serializable {
    ForwardedFindPrimary {
        requireNonNull(message);
        previousActorPaths = Set.copyOf(previousActorPaths);
        if (previousActorPaths.isEmpty()) {
            throw new IllegalArgumentException("previousActorPaths most not be empty");
        }
    }
}
