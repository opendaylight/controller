/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.state;

import org.apache.pekko.actor.ActorRef;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;

/**
 * Methods exposed by {@link ActiveState}. Broken out to allow separate implementation.
 */
@NonNullByDefault
public interface ActiveStateMethods {
    /**
     * Try to handle a message received by the {@link RaftActor}.
     *
     * @param message received message
     * @param sender sender of the message
     * @return {@code true} if the message has been handled
     */
    default boolean handleRaftActorMessage(final Object message, final @Nullable ActorRef sender) {
        return false;
    }
}
