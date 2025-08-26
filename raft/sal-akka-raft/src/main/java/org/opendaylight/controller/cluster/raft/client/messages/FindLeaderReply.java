/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.client.messages;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.jdt.annotation.NonNull;
import java.io.Serializable;
import java.util.Optional;
import org.apache.pekko.actor.ActorPath;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Reply to {@link FindLeader} message, containing the address of the leader actor, as known to the raft actor which
 * sent the message. If the responding actor does not have knowledge of the leader, {@link #leaderActorPath()} will
 * return {@code null}.
 *
 * <p>This message is intended for testing purposes only.
 */
// FIXME: 12.0.0, 11.0.x: introduce a 'record LRv1(@Nullable String leaderActo) serialization proxy
// FIXME: 13.0.x: make this class a record and reject attempts to deserialize
// FIXME: we do not want to be tied to ActorPath: at the end of the day we are talking to well-known entity behind
//        an HTTP/3 connection maintained somewhere.
@VisibleForTesting
public final class FindLeaderReply implements Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable String leaderActorPath;

    /**
     * Default constructor. Takes an {@link ActorPath} in its string form.
     *
     * @param leaderActorPath an {@link ActorPath}'s {@code toString()}
     */
    public FindLeaderReply(final @Nullable String leaderActorPath) {
        this.leaderActorPath = leaderActorPath;
    }

    /**
     * Convenience constructor translating an {@link ActorPath} to its string form.
     *
     * @param leaderActorPath the optional {@link ActorPath}
     */
    public FindLeaderReply(final @Nullable ActorPath leaderActorPath) {
        this(leaderActorPath != null ? leaderActorPath.toString() : null);
    }

    /**
     * {@return the actor reference to the leader, or {@code null} if no leader is known}
     */
    public @Nullable String leaderActorPath() {
        return leaderActorPath;
    }

    /**
     * return the actor reference to the leader, or empty if no leader is known.
     *
     * @return the actor reference to the leader, or empty if no leader is known
     * @deprecated Use {@link #leaderActorPath()} instead.
     */
    @Deprecated(since = "11.0.0", forRemoval = true)
    public @NonNull Optional<String> getLeaderActor() {
        return Optional.ofNullable(leaderActorPath);
    }
}
