/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.util.Timeout;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Internal client message to get a snapshot of the current state based on whether or not persistence is enabled.
 * Returns a {@link GetSnapshotReply} instance.
 *
 * @author Thomas Pantelis
 */
public record GetSnapshot(@NonNull ActorRef replyTo, @Nullable Timeout timeout) {
    public GetSnapshot {
        requireNonNull(replyTo);
    }

    public GetSnapshot(final @NonNull ActorRef replyTo) {
        this(replyTo, null);
    }

    public Optional<Timeout> getTimeout() {
        return Optional.ofNullable(timeout);
    }
}
